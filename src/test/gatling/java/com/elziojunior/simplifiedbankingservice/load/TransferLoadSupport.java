package com.elziojunior.simplifiedbankingservice.load;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.math.BigDecimal;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Public-API fixture support shared by opt-in transfer load simulations. */
final class TransferLoadSupport {

    static final String BASE_URL = setting("TRANSFER_LOAD_BASE_URL", "transfer.load.base-url", "http://localhost:8080");
    static final int RATE = Integer.parseInt(setting("TRANSFER_LOAD_RATE", "transfer.load.rate", "10"));
    static final int DURATION_SECONDS = Integer.parseInt(
            setting("TRANSFER_LOAD_DURATION_SECONDS", "transfer.load.duration-seconds", "30"));
    static final int DESTINATION_COUNT = Integer.parseInt(
            setting("TRANSFER_LOAD_DESTINATIONS", "transfer.load.destinations", "20"));
    private static final String ENVIRONMENT = required("TRANSFER_LOAD_ENVIRONMENT", "transfer.load.environment");
    private static final String DATABASE_URL = required("TRANSFER_LOAD_DATABASE_URL", "transfer.load.database-url");
    private static final String DATABASE_USERNAME = required(
            "TRANSFER_LOAD_DATABASE_USERNAME", "transfer.load.database-username");
    private static final String DATABASE_PASSWORD = required(
            "TRANSFER_LOAD_DATABASE_PASSWORD", "transfer.load.database-password");

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper JSON = new ObjectMapper();

    private TransferLoadSupport() {
    }

    static long createAccount(String name, String balance) {
        String response = post("/api/v1/accounts", """
                {"name":"%s","initialBalance":%s}
                """.formatted(name, balance), null, 201);
        return json(response).path("id").asLong();
    }

    static UUID issueToken() {
        return UUID.fromString(json(post("/api/v1/transfer-tokens", "", null, 201)).path("token").asText());
    }

    static void validateDedicatedTarget() {
        if (!"dedicated-load-test".equals(ENVIRONMENT)) {
            throw new IllegalStateException("TRANSFER_LOAD_ENVIRONMENT must identify a dedicated-load-test target");
        }
        String target = BASE_URL.toLowerCase();
        if (target.contains("prod") || target.contains("production")) {
            throw new IllegalStateException("Production-like load targets are prohibited");
        }
    }

    static BigDecimal totalBalance() {
        try (var connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
                var statement = connection.prepareStatement("SELECT COALESCE(sum(balance), 0) FROM accounts");
                var result = statement.executeQuery()) {
            result.next();
            return result.getBigDecimal(1);
        } catch (SQLException exception) {
            throw new IllegalStateException("Post-run consistency access is unavailable", exception);
        }
    }

    static void assertMoneyConserved(BigDecimal expectedTotal) {
        BigDecimal actualTotal = totalBalance();
        if (actualTotal.compareTo(expectedTotal) != 0) {
            throw new IllegalStateException("Load run did not conserve total account money");
        }
    }

    private static String post(String path, String body, UUID token, int expectedStatus) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (token != null) {
            request.header("Idempotency-Key", token.toString());
        }
        try {
            HttpResponse<String> response = CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != expectedStatus) {
                throw new IllegalStateException("Fixture API returned status " + response.statusCode());
            }
            return response.body();
        } catch (IOException exception) {
            throw new IllegalStateException("Fixture API is unavailable", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Fixture API call was interrupted", exception);
        }
    }

    private static JsonNode json(String value) {
        try {
            return JSON.readTree(value);
        } catch (IOException exception) {
            throw new IllegalStateException("Fixture API returned invalid JSON", exception);
        }
    }

    private static String setting(String environmentName, String propertyName, String defaultValue) {
        return System.getProperty(propertyName, System.getenv().getOrDefault(environmentName, defaultValue));
    }

    private static String required(String environmentName, String propertyName) {
        String value = System.getProperty(propertyName, System.getenv(environmentName));
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(environmentName + " is required for authorized load execution");
        }
        return value;
    }
}
