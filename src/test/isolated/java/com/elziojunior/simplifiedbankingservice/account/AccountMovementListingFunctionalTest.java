package com.elziojunior.simplifiedbankingservice.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
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
@Import(AccountMovementListingFunctionalTest.FixedClockConfiguration.class)
class AccountMovementListingFunctionalTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-01T12:00:00Z");

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
        OffsetDateTime createdAt = NOW.minusHours(1);
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

    /** Proves the default day and explicit week/month periods apply real computed PostgreSQL boundaries. */
    @Test
    void shouldApplyFixedLookbackPeriods() {
        long accountId = createAccount();
        createMovement(accountId, MovementType.CREDIT, NOW.minusMonths(1).minusNanos(1_000), "1.00");
        long inMonth = createMovement(accountId, MovementType.CREDIT, NOW.minusWeeks(1).minusDays(1), "2.00");
        long inWeek = createMovement(accountId, MovementType.CREDIT, NOW.minusDays(1).minusHours(1), "3.00");
        long atDayStart = createMovement(accountId, MovementType.CREDIT, NOW.minusDays(1), "4.00");
        long inDay = createMovement(accountId, MovementType.CREDIT, NOW.minusHours(1), "5.00");
        createMovement(accountId, MovementType.CREDIT, NOW, "6.00");

        AccountMovementPageResponse defaultDay = list(accountId);
        AccountMovementPageResponse week = list(accountId, "period", "1w");
        AccountMovementPageResponse month = list(accountId, "period", "1M");

        assertThat(defaultDay.content()).extracting(AccountMovementResponse::id).containsExactly(inDay, atDayStart);
        assertThat(week.content()).extracting(AccountMovementResponse::id)
                .containsExactly(inDay, atDayStart, inWeek);
        assertThat(month.content()).extracting(AccountMovementResponse::id)
                .containsExactly(inDay, atDayStart, inWeek, inMonth);
    }

    /** Proves CREDIT, DEBIT, and combined period/type filters select only matching persisted movements. */
    @Test
    void shouldFilterMovementTypesAloneAndWithPeriod() {
        long accountId = createAccount();
        long oldCredit = createMovement(accountId, MovementType.CREDIT, NOW.minusDays(5), "1.00");
        long credit = createMovement(accountId, MovementType.CREDIT, NOW.minusHours(2), "2.00");
        long debit = createMovement(accountId, MovementType.DEBIT, NOW.minusHours(1), "3.00");

        AccountMovementPageResponse credits = list(accountId, "period", "1w", "type", "CREDIT");
        AccountMovementPageResponse debits = list(accountId, "type", "DEBIT");
        AccountMovementPageResponse combined = list(accountId, "period", "1d", "type", "CREDIT");

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
        assertBadRequest(accountId, "period", "30d");
        assertBadRequest(accountId, "period", "1m");
        assertBadRequest(accountId, "type", "UNKNOWN");
        assertBadRequest(accountId, "start", "2026-08-01T00:00:00Z");
        assertBadRequest(accountId, "end", "2026-09-01T00:00:00Z");
        assertBadRequest(accountId, "sort", "createdAt");
    }

    /** Proves repeated reads leave every fixture row and financial value unchanged. */
    @Test
    void shouldNotMutateFinancialStateWhileListing() {
        long accountId = createAccount();
        createMovement(accountId, MovementType.CREDIT, NOW.minusHours(2), "5.00");
        createMovement(accountId, MovementType.DEBIT, NOW.minusHours(1), "2.00");
        List<Map<String, Object>> before = movementSnapshot(accountId);

        list(accountId);
        list(accountId, "type", "CREDIT");
        list(accountId, "period", "1M");

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

    @TestConfiguration
    static class FixedClockConfiguration {

        /** Supplies one stable request instant so period boundaries remain exact in the real application flow. */
        @Bean
        @Primary
        Clock movementTestClock() {
            return Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC);
        }
    }
}
