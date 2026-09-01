package com.elziojunior.simplifiedbankingservice.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.mockito.ArgumentCaptor;

import com.elziojunior.simplifiedbankingservice.model.api.CreateAccountRequest;
import com.elziojunior.simplifiedbankingservice.model.api.CreateTransferRequest;
import com.elziojunior.simplifiedbankingservice.model.api.AccountResponse;
import com.elziojunior.simplifiedbankingservice.model.api.TransferResponse;
import com.elziojunior.simplifiedbankingservice.model.api.TransferTokenResponse;
import com.elziojunior.simplifiedbankingservice.model.dto.TransferCompletedNotification;
import com.elziojunior.simplifiedbankingservice.service.TransferNotificationPublisher;
import com.elziojunior.simplifiedbankingservice.support.EphemeralPostgresGuard;

import javax.sql.DataSource;
import io.micrometer.core.instrument.MeterRegistry;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.jpa.properties.jakarta.persistence.lock.timeout=500",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"
})
class TransferFunctionalTest {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRESQL = new PostgreSQLContainer<>("postgres:17.6-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MeterRegistry meterRegistry;

    @MockitoBean
    private TransferNotificationPublisher notificationPublisher;

    /** Proves every scenario is connected only to its disposable PostgreSQL Testcontainer. */
    @BeforeEach
    void verifyEphemeralDatabase() {
        EphemeralPostgresGuard.verify(dataSource, POSTGRESQL);
    }

    /** Proves the HTTP/PostgreSQL success path commits atomically and requests one exact notification. */
    @Test
    void shouldCommitTransferAndPublishOneSourceNotification() {
        long source = createAccount("Source", "100.00");
        long destination = createAccount("Destination", "10.00");
        UUID token = issueToken();

        ResponseEntity<TransferResponse> response = transfer(token, source, destination, "12.345");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        TransferResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.amount()).isEqualByComparingTo("12.34");
        assertThat(balance(source)).isEqualByComparingTo("87.66");
        assertThat(balance(destination)).isEqualByComparingTo("22.34");
        assertThat(movementCount(body.transferId())).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM transfer_idempotency_tokens WHERE token = ? AND used_at IS NOT NULL",
                Integer.class, token)).isOne();

        ArgumentCaptor<TransferCompletedNotification> notification =
                ArgumentCaptor.forClass(TransferCompletedNotification.class);
        verify(notificationPublisher).publish(notification.capture());
        TransferCompletedNotification event = notification.getValue();
        OffsetDateTime usedAt = jdbcTemplate.queryForObject(
                "SELECT used_at FROM transfer_idempotency_tokens WHERE token = ?", OffsetDateTime.class, token);
        assertThat(event.eventId()).isNotNull().isNotEqualTo(body.transferId());
        assertThat(event.operationId()).isEqualTo(body.transferId());
        assertThat(event.recipientAccountId()).isEqualTo(source);
        assertThat(event.eventType()).isEqualTo(TransferCompletedNotification.TRANSFER_COMPLETED);
        assertThat(event.amount()).isEqualByComparingTo("12.34");
        assertThat(event.occurredAt()).isEqualTo(usedAt);
    }

    /** Proves identical retries replay while payload reuse is rejected without new effects. */
    @Test
    void shouldReplayIdenticalTokenAndRejectPayloadMismatch() {
        long source = createAccount("Source", "100.00");
        long destination = createAccount("Destination", "0.00");
        UUID token = issueToken();

        TransferResponse first = transfer(token, source, destination, "25.00").getBody();
        TransferResponse replay = transfer(token, source, destination, "25.000").getBody();
        ResponseEntity<TransferResponse> mismatch = transfer(token, source, destination, "26.00");

        assertThat(first).isNotNull();
        assertThat(replay).isEqualTo(first);
        assertThat(mismatch.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(balance(source)).isEqualByComparingTo("75.00");
        assertThat(balance(destination)).isEqualByComparingTo("25.00");
        assertThat(movementCount(first.transferId())).isEqualTo(2);
        ArgumentCaptor<TransferCompletedNotification> notification =
                ArgumentCaptor.forClass(TransferCompletedNotification.class);
        verify(notificationPublisher, times(1)).publish(notification.capture());
        assertThat(notification.getValue().operationId()).isEqualTo(first.transferId());
    }

    /** Proves invalid token, account, balance, and same-account cases leave no financial history. */
    @Test
    void shouldRejectInvalidTransferStatesAtomically() {
        long source = createAccount("Source", "10.00");
        long destination = createAccount("Destination", "0.00");

        assertThat(transfer(UUID.randomUUID(), source, destination, "1.00").getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(transfer(issueToken(), source, source, "1.00").getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(transfer(issueToken(), source, destination, "11.00").getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(transfer(issueToken(), source, Long.MAX_VALUE, "1.00").getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        UUID expired = issueToken();
        jdbcTemplate.update("""
                UPDATE transfer_idempotency_tokens
                SET created_at = now() - interval '2 minutes',
                    expires_at = now() - interval '1 minute'
                WHERE token = ?
                """, expired);
        assertThat(transfer(expired, source, destination, "1.00").getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        assertThat(balance(source)).isEqualByComparingTo("10.00");
        assertThat(balance(destination)).isEqualByComparingTo("0.00");
        assertThat(movementCountForAccount(source)).isZero();
        assertThat(movementCountForAccount(destination)).isZero();
        verifyNoInteractions(notificationPublisher);
    }

    /** Proves competing debits serialize without overdraft and conserve total money. */
    @Test
    void shouldSerializeCompetingDebitsAndConserveMoney() throws Exception {
        long source = createAccount("Hot source", "50.00");
        List<Long> destinations = new ArrayList<>();
        List<Callable<Integer>> requests = new ArrayList<>();
        for (int index = 0; index < 100; index++) {
            long destination = createAccount("Destination " + index, "0.00");
            destinations.add(destination);
            UUID token = issueToken();
            requests.add(() -> transfer(token, source, destination, "1.00").getStatusCode().value());
        }

        List<Integer> statuses;
        try (var executor = Executors.newFixedThreadPool(20)) {
            List<Future<Integer>> futures = executor.invokeAll(requests);
            statuses = futures.stream().map(this::result).toList();
        }

        assertThat(statuses).filteredOn(status -> status == HttpStatus.OK.value()).hasSize(50);
        assertThat(statuses).filteredOn(status -> status == HttpStatus.CONFLICT.value()).hasSize(50);
        assertThat(balance(source)).isEqualByComparingTo("0.00");
        BigDecimal destinationTotal = destinations.stream()
                .map(this::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(balance(source).add(destinationTotal)).isEqualByComparingTo("50.00");
        assertThat(movementCountForAccount(source)).isEqualTo(50);
        assertThat(destinations).hasSize(100);
        verify(notificationPublisher, times(50)).publish(any(TransferCompletedNotification.class));
    }

    /** Proves a contended account fails within the configured bound and rolls back token use. */
    @Test
    void shouldBoundAccountLockWaiting() throws SQLException {
        long source = createAccount("Locked source", "10.00");
        long destination = createAccount("Destination", "0.00");
        UUID token = issueToken();

        try (Connection connection = dataSource.getConnection(); PreparedStatement lock = connection.prepareStatement(
                "SELECT id FROM accounts WHERE id = ? FOR UPDATE")) {
            connection.setAutoCommit(false);
            lock.setLong(1, source);
            lock.executeQuery();

            long startedAt = System.nanoTime();
            ResponseEntity<TransferResponse> response = transfer(token, source, destination, "1.00");
            Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(elapsed).isLessThan(Duration.ofSeconds(3));
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT used_at FROM transfer_idempotency_tokens WHERE token = ?",
                    OffsetDateTime.class, token)).isNull();
            assertThat(movementCountForAccount(source)).isZero();
            assertThat(movementCountForAccount(destination)).isZero();
            assertThat(meterRegistry.counter(
                    "banking.api.lock.contention", "operation", "transfer.create").count()).isPositive();
            assertThat(meterRegistry.counter(
                    "banking.api.timeouts", "operation", "transfer.create").count()).isPositive();
            connection.rollback();
        }
        assertThat(balance(source)).isEqualByComparingTo("10.00");
        assertThat(balance(destination)).isEqualByComparingTo("0.00");
        verifyNoInteractions(notificationPublisher);
    }

    /** Proves opposite-direction transfers share ascending lock order and complete without deadlock. */
    @Test
    void shouldCompleteSimultaneousCrossTransfers() throws Exception {
        long first = createAccount("First", "20.00");
        long second = createAccount("Second", "20.00");
        UUID firstToken = issueToken();
        UUID secondToken = issueToken();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> forward = coordinatedTransfer(ready, start, firstToken, first, second);
        Callable<Integer> backward = coordinatedTransfer(ready, start, secondToken, second, first);

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> firstResult = executor.submit(forward);
            Future<Integer> secondResult = executor.submit(backward);
            assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(firstResult.get(3, TimeUnit.SECONDS)).isEqualTo(HttpStatus.OK.value());
            assertThat(secondResult.get(3, TimeUnit.SECONDS)).isEqualTo(HttpStatus.OK.value());
        }

        assertThat(balance(first)).isEqualByComparingTo("20.00");
        assertThat(balance(second)).isEqualByComparingTo("20.00");
        assertThat(movementCountForAccount(first)).isEqualTo(2);
        assertThat(movementCountForAccount(second)).isEqualTo(2);
        verify(notificationPublisher, times(2)).publish(any(TransferCompletedNotification.class));
    }

    private Callable<Integer> coordinatedTransfer(
            CountDownLatch ready, CountDownLatch start, UUID token, long source, long destination) {
        return () -> {
            ready.countDown();
            if (!start.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Cross-transfer start barrier timed out");
            }
            return transfer(token, source, destination, "5.00").getStatusCode().value();
        };
    }

    private int result(Future<Integer> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private long createAccount(String name, String balance) {
        AccountResponse body = restTemplate.postForEntity("/api/v1/accounts",
                new CreateAccountRequest(name, new BigDecimal(balance)), AccountResponse.class).getBody();
        assertThat(body).isNotNull();
        return body.id();
    }

    private UUID issueToken() {
        ResponseEntity<TransferTokenResponse> response = restTemplate.postForEntity(
                "/api/v1/transfer-tokens", null, TransferTokenResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().token();
    }

    private ResponseEntity<TransferResponse> transfer(UUID token, long source, long destination, String amount) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", token.toString());
        return restTemplate.exchange("/api/v1/transfers", HttpMethod.POST,
                new HttpEntity<>(new CreateTransferRequest(
                        source, destination, new BigDecimal(amount)), headers), TransferResponse.class);
    }

    private BigDecimal balance(long accountId) {
        return jdbcTemplate.queryForObject("SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, accountId);
    }

    private int movementCount(UUID operationId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM movements WHERE operation_id = ?",
                Integer.class,
                operationId);
    }

    private int movementCountForAccount(long accountId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM movements WHERE account_id = ?", Integer.class, accountId);
    }

}
