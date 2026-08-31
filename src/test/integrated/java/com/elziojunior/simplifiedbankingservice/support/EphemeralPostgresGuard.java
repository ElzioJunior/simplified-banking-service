package com.elziojunior.simplifiedbankingservice.support;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.testcontainers.containers.PostgreSQLContainer;

/** Prevents integrated tests from using a datasource outside their disposable PostgreSQL container. */
public final class EphemeralPostgresGuard {

    private EphemeralPostgresGuard() {
    }

    /**
     * Verifies that Spring resolved the exact running Testcontainer datasource
     * so test writes can never reach the application's transactional database.
     */
    public static void verify(DataSource dataSource, PostgreSQLContainer<?> container) {
        if (!container.isRunning()) {
            throw new IllegalStateException("The integrated-test PostgreSQL container is not running.");
        }

        try (Connection connection = dataSource.getConnection()) {
            String actualUrl = connection.getMetaData().getURL();
            String expectedUrl = container.getJdbcUrl();
            boolean expectedConnection = actualUrl.equals(expectedUrl) || actualUrl.startsWith(expectedUrl + "?");
            if (!expectedConnection || !container.getDatabaseName().equals(connection.getCatalog())) {
                throw new IllegalStateException(
                        "Integrated tests refused a datasource outside their disposable PostgreSQL container.");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Integrated-test datasource isolation could not be verified.", exception);
        }
    }
}
