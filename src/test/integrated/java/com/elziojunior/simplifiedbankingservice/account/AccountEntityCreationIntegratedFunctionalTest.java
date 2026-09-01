package com.elziojunior.simplifiedbankingservice.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.elziojunior.simplifiedbankingservice.model.api.AccountResponse;
import com.elziojunior.simplifiedbankingservice.model.api.CreateAccountRequest;
import com.elziojunior.simplifiedbankingservice.support.EphemeralPostgresGuard;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountEntityCreationIntegratedFunctionalTest {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRESQL = new PostgreSQLContainer<>("postgres:17.6-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    /** Proves every scenario is connected only to its disposable PostgreSQL Testcontainer. */
    @BeforeEach
    void verifyEphemeralDatabase() {
        EphemeralPostgresGuard.verify(dataSource, POSTGRESQL);
    }

    /** Proves unauthenticated HTTP creation persists the complete approved account shape. */
    @Test
    void shouldCreateAndPersistAnAccountThroughThePublicApi() {
        ResponseEntity<AccountResponse> response = create("Ada Lovelace", "100.00");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        AccountResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isPositive();
        assertThat(body.name()).isEqualTo("Ada Lovelace");
        assertThat(body.balance()).isEqualByComparingTo("100.00");
        assertThat(body.createdAt().getOffset()).isEqualTo(ZoneOffset.UTC);

        Map<String, Object> stored = jdbcTemplate.queryForMap(
                "SELECT id, name, balance, created_at FROM accounts WHERE id = ?",
                body.id());
        assertThat(stored.get("name")).isEqualTo("Ada Lovelace");
        assertThat((BigDecimal) stored.get("balance")).isEqualByComparingTo("100.00");
        assertThat(((Timestamp) stored.get("created_at")).toInstant()).isEqualTo(body.createdAt().toInstant());
    }

    /** Proves zero balances are accepted and independent requests receive unique identities. */
    @Test
    void shouldCreateZeroBalanceAccountsWithUniqueIdentifiers() {
        AccountResponse first = create("First", "0").getBody();
        AccountResponse second = create("Second", "0.00").getBody();

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(first.balance()).isEqualByComparingTo("0.00");
        assertThat(second.balance()).isEqualByComparingTo("0.00");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM accounts WHERE id IN (?, ?)",
                Integer.class,
                first.id(),
                second.id())).isEqualTo(2);
    }

    /** Proves HALF_EVEN normalization survives the HTTP, JPA, and PostgreSQL round trip. */
    @Test
    void shouldRoundMonetaryValuesHalfEvenAndPersistUtcInstants() {
        AccountResponse lowerTie = create("Lower tie", "1.225").getBody();
        AccountResponse upperTie = create("Upper tie", "1.235").getBody();

        assertThat(lowerTie).isNotNull();
        assertThat(upperTie).isNotNull();
        assertThat(lowerTie.balance()).isEqualByComparingTo("1.22");
        assertThat(upperTie.balance()).isEqualByComparingTo("1.24");
        assertThat(lowerTie.createdAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(upperTie.createdAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, lowerTie.id()))
                .isEqualByComparingTo("1.22");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, upperTie.id()))
                .isEqualByComparingTo("1.24");
    }

    /** Proves every invalid request is atomic and returns safe Problem Details. */
    @Test
    void shouldRejectInvalidRequestsWithoutPersistingAccounts() {
        int countBeforeRequests = accountCount();

        assertBadRequest(new CreateAccountRequest("   ", BigDecimal.ZERO), "Invalid request");
        assertBadRequest(new CreateAccountRequest("Negative sub-cent", new BigDecimal("-0.001")), "Invalid request");
        assertBadRequest(new CreateAccountRequest(
                "Overflow after rounding", new BigDecimal("99999999999999999.995")),
                "Invalid account creation request");

        assertThat(accountCount()).isEqualTo(countBeforeRequests);
    }

    /** Proves unsupported account operations stay absent and operational routes stay protected. */
    @Test
    void shouldPreserveApiAndOperationalSecurityBoundaries() {
        assertThat(restTemplate.getForEntity("/api/v1/accounts", String.class).getStatusCode())
                .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(restTemplate.getForEntity("/api/v1/accounts/1", String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(restTemplate.exchange(
                "/api/v1/accounts/1", HttpMethod.PUT, HttpEntity.EMPTY, String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(restTemplate.exchange(
                "/api/v1/accounts/1", HttpMethod.PATCH, HttpEntity.EMPTY, String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(restTemplate.exchange(
                "/api/v1/accounts/1", HttpMethod.DELETE, HttpEntity.EMPTY, String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(restTemplate.getForEntity("/actuator/health", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<AccountResponse> create(String name, String balance) {
        return restTemplate.postForEntity(
                "/api/v1/accounts",
                new CreateAccountRequest(name, new BigDecimal(balance)),
                AccountResponse.class);
    }

    private void assertBadRequest(CreateAccountRequest request, String title) {
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/accounts", request, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse(400, title));
    }

    private int accountCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM accounts", Integer.class);
    }

    private record ErrorResponse(int status, String title) {
    }
}
