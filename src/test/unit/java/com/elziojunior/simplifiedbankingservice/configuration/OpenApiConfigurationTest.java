package com.elziojunior.simplifiedbankingservice.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.OpenAPI;

class OpenApiConfigurationTest {

    /** Proves the generated contract carries the stable public API identity required by ADR-0032. */
    @Test
    void shouldDefinePublicApiMetadata() {
        OpenAPI openAPI = new OpenApiConfiguration().bankingOpenApi();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Simplified Banking Service API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1");
        assertThat(openAPI.getInfo().getDescription()).contains("RFC 9457 Problem Details");
    }
}
