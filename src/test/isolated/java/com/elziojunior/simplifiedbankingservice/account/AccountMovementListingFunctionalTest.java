package com.elziojunior.simplifiedbankingservice.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.elziojunior.simplifiedbankingservice.model.api.AccountMovementPageResponse;
import com.elziojunior.simplifiedbankingservice.model.api.AccountMovementResponse;
import com.elziojunior.simplifiedbankingservice.model.entity.MovementType;
import com.elziojunior.simplifiedbankingservice.service.TransferNotificationPublisher;
import com.elziojunior.simplifiedbankingservice.support.EphemeralPostgresGuard;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties =
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration")
class AccountMovementListingFunctionalTest {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRESQL = new PostgreSQLContainer<>("postgres:17.6-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @MockitoBean
    private TransferNotificationPublisher notificationPublisher;

    /** Proves every scenario is connected only to its disposable PostgreSQL Testcontainer. */
    @BeforeEach
    void verifyEphemeralDatabase() {
        EphemeralPostgresGuard.verify(dataSource, POSTGRESQL);
    }

    /** Proves ownership isolation, fixed pagination, and the ID tie-breaker against real PostgreSQL rows. */
    @Test
    void shouldPageOnlyOwnedMovementsInDeterministicOrder() {
        long accountId = createAccount();
        long otherAccountId = createAccount();
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-20T12:00:00Z");
        List<Long> movementIds = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            movementIds.add(createMovement(accountId, MovementType.CREDIT, createdAt, "1.00"));
        }
        long foreignMovementId = createMovement(otherAccountId, MovementType.CREDIT, createdAt, "999.00");
        movementIds.sort(Comparator.reverseOrder());

        AccountMovementPageResponse first = list(accountId);
        AccountMovementPageResponse second = list(accountId, "page", "1");

        assertThat(first.page()).isZero();
        assertThat(first.size()).isEqualTo(10);
        assertThat(first.totalElements()).isEqualTo(12);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(first.content()).extracting(AccountMovementResponse::id)
                .containsExactlyElementsOf(movementIds.subList(0, 10))
                .doesNotContain(foreignMovementId);
        assertThat(second.content()).extracting(AccountMovementResponse::id)
                .containsExactlyElementsOf(movementIds.subList(10, 12));
    }

    /** Proves the real repository applies inclusive start and exclusive end boundaries exactly. */
    @Test
    void shouldApplyHalfOpenDateRange() {
        long accountId = createAccount();
        OffsetDateTime start = OffsetDateTime.parse("2026-08-10T00:00:00Z");
        OffsetDateTime end = OffsetDateTime.parse("2026-08-20T00:00:00Z");
        createMovement(accountId, MovementType.CREDIT, start.minusNanos(1_000), "1.00");
        long atStart = createMovement(accountId, MovementType.CREDIT, start, "2.00");
        long inside = createMovement(accountId, MovementType.CREDIT, end.minusNanos(1_000), "3.00");
        createMovement(accountId, MovementType.CREDIT, end, "4.00");

        AccountMovementPageResponse response = list(
                accountId, "start", start.toString(), "end", end.toString());

        assertThat(response.content()).extracting(AccountMovementResponse::id)
                .containsExactly(inside, atStart);
        assertThat(response.totalElements()).isEqualTo(2);
    }

    /** Proves CREDIT, DEBIT, and combined range/type filters select only matching persisted movements. */
    @Test
    void shouldFilterMovementTypesAloneAndWithDates() {
        long accountId = createAccount();
        OffsetDateTime start = OffsetDateTime.parse("2026-08-10T00:00:00Z");
        OffsetDateTime end = OffsetDateTime.parse("2026-08-20T00:00:00Z");
        long oldCredit = createMovement(accountId, MovementType.CREDIT, start.minusDays(1), "1.00");
        long credit = createMovement(accountId, MovementType.CREDIT, start.plusDays(1), "2.00");
        long debit = createMovement(accountId, MovementType.DEBIT, start.plusDays(2), "3.00");

        AccountMovementPageResponse credits = list(accountId, "type", "CREDIT");
        AccountMovementPageResponse debits = list(accountId, "type", "DEBIT");
        AccountMovementPageResponse combined = list(
                accountId, "start", start.toString(), "end", end.toString(), "type", "CREDIT");

        assertThat(credits.content()).extracting(AccountMovementResponse::id).containsExactly(credit, oldCredit);
        assertThat(debits.content()).extracting(AccountMovementResponse::id).containsExactly(debit);
        assertThat(combined.content()).extracting(AccountMovementResponse::id).containsExactly(credit);
    }

    /** Proves empty history is successful, absent accounts are 404, and malformed query input is safely rejected. */
    @Test
    void shouldHandleEmptyUnknownAndInvalidQueries() {
        long accountId = createAccount();

        ResponseEntity<AccountMovementPageResponse> empty = get(accountId, AccountMovementPageResponse.class);
        ResponseEntity<ProblemDetail> missing = get(Long.MAX_VALUE, ProblemDetail.class);

        assertThat(empty.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(empty.getBody()).isNotNull();
        assertThat(empty.getBody().content()).isEmpty();
        assertThat(empty.getBody().totalElements()).isZero();
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(missing.getBody()).isNotNull();
        assertThat(missing.getBody().getDetail()).isEqualTo("The requested account does not exist.");
        assertBadRequest(accountId, "page", "-1");
        assertBadRequest(accountId, "start", "not-a-date");
        assertBadRequest(accountId, "type", "UNKNOWN");
        assertBadRequest(
                accountId,
                "start", "2026-08-20T00:00:00Z",
                "end", "2026-08-20T00:00:00Z");
        assertBadRequest(
                accountId,
                "start", "2026-08-21T00:00:00Z",
                "end", "2026-08-20T00:00:00Z");
    }

    /** Proves repeated reads leave every fixture row and financial value unchanged. */
    @Test
    void shouldNotMutateFinancialStateWhileListing() {
        long accountId = createAccount();
        createMovement(accountId, MovementType.CREDIT, OffsetDateTime.parse("2026-08-20T12:00:00Z"), "5.00");
        createMovement(accountId, MovementType.DEBIT, OffsetDateTime.parse("2026-08-21T12:00:00Z"), "2.00");
        List<Map<String, Object>> before = movementSnapshot(accountId);

        list(accountId);
        list(accountId, "type", "CREDIT");
        list(accountId, "start", "2026-08-01T00:00:00Z", "end", "2026-09-01T00:00:00Z");

        assertThat(movementSnapshot(accountId)).isEqualTo(before);
    }

    private long createAccount() {
        return jdbcTemplate.queryForObject(
                "INSERT INTO accounts (name, balance, created_at) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                "movement-fixture-" + UUID.randomUUID(),
                new BigDecimal("100.00"),
                OffsetDateTime.now());
    }

    private long createMovement(
            long accountId, MovementType type, OffsetDateTime createdAt, String amount) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO movements (account_id, operation_id, type, amount, created_at)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """,
                Long.class,
                accountId,
                UUID.randomUUID(),
                type.name(),
                new BigDecimal(amount),
                createdAt);
    }

    private AccountMovementPageResponse list(long accountId, String... queryParameters) {
        ResponseEntity<AccountMovementPageResponse> response = get(
                accountId, AccountMovementPageResponse.class, queryParameters);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private void assertBadRequest(long accountId, String... queryParameters) {
        ResponseEntity<ProblemDetail> response = get(accountId, ProblemDetail.class, queryParameters);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).doesNotContain("org.springframework", "java.time", "SQL");
    }

    private <T> ResponseEntity<T> get(long accountId, Class<T> responseType, String... queryParameters) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/api/v1/accounts/{accountId}/movements");
        for (int index = 0; index < queryParameters.length; index += 2) {
            uri.queryParam(queryParameters[index], queryParameters[index + 1]);
        }
        URI requestUri = uri.buildAndExpand(accountId).encode().toUri();
        return restTemplate.getForEntity(requestUri, responseType);
    }

    private List<Map<String, Object>> movementSnapshot(long accountId) {
        return jdbcTemplate.queryForList(
                """
                SELECT id, account_id, operation_id, type, amount, created_at
                FROM movements
                WHERE account_id = ?
                ORDER BY id
                """,
                accountId);
    }
}
