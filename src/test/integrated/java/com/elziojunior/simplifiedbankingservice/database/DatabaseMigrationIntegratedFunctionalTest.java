package com.elziojunior.simplifiedbankingservice.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.elziojunior.simplifiedbankingservice.support.EphemeralPostgresGuard;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DatabaseMigrationIntegratedFunctionalTest {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRESQL = new PostgreSQLContainer<>("postgres:17.6-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    /** Proves every scenario is connected only to its disposable PostgreSQL Testcontainer. */
    @BeforeEach
    void verifyEphemeralDatabase() {
        EphemeralPostgresGuard.verify(dataSource, POSTGRESQL);
    }

    /**
     * Proves that Flyway creates the documented PostgreSQL types, constraints,
     * and indexes because later persistence mappings depend on this exact schema.
     */
    @Test
    void createsDocumentedSchemaStructure() {
        assertColumn("accounts", "id", "bigint", null, null, "YES");
        assertColumn("accounts", "name", "character varying", 255, null, "NO");
        assertColumn("accounts", "balance", "numeric", null, 2, "NO");
        assertColumn("accounts", "created_at", "timestamp with time zone", null, null, "NO");
        assertColumn("movements", "id", "bigint", null, null, "YES");
        assertColumn("movements", "account_id", "bigint", null, null, "NO");
        assertColumn("movements", "operation_id", "uuid", null, null, "NO");
        assertColumn("movements", "type", "character varying", 6, null, "NO");
        assertColumn("movements", "amount", "numeric", null, 2, "NO");
        assertColumn("movements", "created_at", "timestamp with time zone", null, null, "NO");

        List<String> accountConstraints = constraintNames("accounts");
        assertThat(accountConstraints).containsExactlyInAnyOrder(
                "pk_accounts",
                "chk_accounts_name_not_blank",
                "chk_accounts_balance_nonnegative");

        List<String> movementConstraints = constraintNames("movements");
        assertThat(movementConstraints).containsExactlyInAnyOrder(
                "pk_movements",
                "fk_movements_account",
                "chk_movements_type",
                "chk_movements_amount_positive",
                "uq_movements_operation_type");

        List<String> movementIndexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'public' AND tablename = 'movements'",
                String.class);
        assertThat(movementIndexes).containsExactlyInAnyOrder(
                "pk_movements",
                "uq_movements_operation_type",
                "idx_movements_account_created_at",
                "idx_movements_account_type_created_at");

        String deleteRule = jdbcTemplate.queryForObject(
                """
                SELECT rc.delete_rule
                FROM information_schema.referential_constraints rc
                WHERE rc.constraint_schema = 'public'
                  AND rc.constraint_name = 'fk_movements_account'
                """,
                String.class);
        assertThat(deleteRule).isEqualTo("RESTRICT");
    }

    /**
     * Proves that valid accounts and paired movements can be persisted because
     * the migration must support the documented successful transfer shape.
     */
    @Test
    void acceptsValidAccountsAndPairedMovements() {
        long sourceAccountId = insertAccount("Source account", new BigDecimal("100.00"));
        long destinationAccountId = insertAccount("Destination account", BigDecimal.ZERO);
        UUID operationId = UUID.randomUUID();

        insertMovement(sourceAccountId, operationId, "DEBIT", new BigDecimal("25.00"));
        insertMovement(destinationAccountId, operationId, "CREDIT", new BigDecimal("25.00"));

        Integer movementCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM movements WHERE operation_id = ?",
                Integer.class,
                operationId);
        assertThat(movementCount).isEqualTo(2);
    }

    /**
     * Proves that invalid names and negative balances are rejected at the
     * database boundary so malformed account rows cannot bypass application validation.
     */
    @Test
    void rejectsInvalidAccountRows() {
        assertThatThrownBy(() -> insertAccount("   ", BigDecimal.ZERO))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertAccount("Invalid balance", new BigDecimal("-0.01")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertAccount("a".repeat(256), BigDecimal.ZERO))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * Proves movement amount, type, account, and per-operation uniqueness
     * constraints because invalid financial-history rows must fail independently.
     */
    @Test
    void rejectsInvalidOrDuplicateMovementRows() {
        long accountId = insertAccount("Movement account", new BigDecimal("10.00"));
        UUID operationId = UUID.randomUUID();

        assertThatThrownBy(() -> insertMovement(accountId, UUID.randomUUID(), "DEBIT", BigDecimal.ZERO))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertMovement(accountId, UUID.randomUUID(), "OTHER", BigDecimal.ONE))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertMovement(Long.MAX_VALUE, UUID.randomUUID(), "CREDIT", BigDecimal.ONE))
                .isInstanceOf(DataIntegrityViolationException.class);

        insertMovement(accountId, operationId, "DEBIT", BigDecimal.ONE);
        assertThatThrownBy(() -> insertMovement(accountId, operationId, "DEBIT", BigDecimal.ONE))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * Proves that movement history prevents account deletion because financial
     * traceability requires retained movements and their referenced accounts.
     */
    @Test
    void preventsDeletingAnAccountReferencedByMovementHistory() {
        long accountId = insertAccount("Retained account", new BigDecimal("10.00"));
        insertMovement(accountId, UUID.randomUUID(), "DEBIT", BigDecimal.ONE);

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM accounts WHERE id = ?", accountId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * Proves that running Flyway again performs no work because an applied
     * versioned migration must remain deterministic and idempotent.
     */
    @Test
    void doesNotReapplyAnAlreadyAppliedMigration() {
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("3");
    }

    /**
     * Proves V3 retains token constraints while removing the superseded outbox table.
     */
    @Test
    void retainsTransferTokenSchemaAndRemovesOutbox() {
        assertColumn("transfer_idempotency_tokens", "token", "uuid", null, null, "NO");
        assertNullableNumericColumn("transfer_idempotency_tokens", "amount", 19, 2);

        assertThat(constraintNames("transfer_idempotency_tokens")).contains(
                "pk_transfer_idempotency_tokens",
                "uq_transfer_idempotency_tokens_operation",
                "chk_transfer_tokens_association");

        assertThat(indexNames("transfer_idempotency_tokens"))
                .contains("idx_transfer_tokens_unused_expiration");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.transfer_notification_outbox')",
                String.class)).isNull();
    }

    /**
     * Verifies one column's PostgreSQL metadata so the schema test can express
     * type, length, scale, nullability, and identity expectations consistently.
     */
    private void assertColumn(
            String table,
            String column,
            String expectedType,
            Integer expectedLength,
            Integer expectedScale,
            String expectedIdentity) {
        var metadata = jdbcTemplate.queryForMap(
                """
                SELECT data_type, character_maximum_length, numeric_precision, numeric_scale, is_nullable, is_identity
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                """,
                table,
                column);

        assertThat(metadata.get("data_type")).isEqualTo(expectedType);
        if (expectedLength != null) {
            assertThat(metadata.get("character_maximum_length")).isEqualTo(expectedLength);
        }
        if (expectedType.equals("numeric")) {
            assertThat(metadata.get("numeric_precision")).isEqualTo(19);
            assertThat(metadata.get("numeric_scale")).isEqualTo(expectedScale);
        }
        assertThat(metadata.get("is_nullable")).isEqualTo("NO");
        assertThat(metadata.get("is_identity")).isEqualTo(expectedIdentity);
    }

    /** Verifies nullable monetary metadata used before a token is associated. */
    private void assertNullableNumericColumn(String table, String column, int precision, int scale) {
        var metadata = jdbcTemplate.queryForMap(
                """
                SELECT data_type, numeric_precision, numeric_scale, is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                """,
                table,
                column);
        assertThat(metadata.get("data_type")).isEqualTo("numeric");
        assertThat(metadata.get("numeric_precision")).isEqualTo(precision);
        assertThat(metadata.get("numeric_scale")).isEqualTo(scale);
        assertThat(metadata.get("is_nullable")).isEqualTo("YES");
    }

    /**
     * Retrieves named table constraints so the migration test can detect a
     * missing or accidentally renamed database-enforced invariant.
     */
    private List<String> constraintNames(String table) {
        return jdbcTemplate.queryForList(
                "SELECT conname FROM pg_constraint WHERE conrelid = CAST(? AS regclass)",
                String.class,
                table);
    }

    /** Retrieves table index names so performance-critical pending work remains discoverable. */
    private List<String> indexNames(String table) {
        return jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'public' AND tablename = ?",
                String.class,
                table);
    }

    /**
     * Inserts an account and returns its generated identity so movement
     * scenarios use the same database behavior expected by account creation.
     */
    private long insertAccount(String name, BigDecimal balance) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO accounts (name, balance, created_at) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                name,
                balance,
                OffsetDateTime.now());
        return id;
    }

    /**
     * Inserts one financial movement so scenarios exercise the migration's
     * relational, value, and operation-level constraints through real JDBC.
     */
    private void insertMovement(long accountId, UUID operationId, String type, BigDecimal amount) {
        jdbcTemplate.update(
                """
                INSERT INTO movements (account_id, operation_id, type, amount, created_at)
                VALUES (?, ?, ?, ?, ?)
                """,
                accountId,
                operationId,
                type,
                amount,
                OffsetDateTime.now());
    }
}
