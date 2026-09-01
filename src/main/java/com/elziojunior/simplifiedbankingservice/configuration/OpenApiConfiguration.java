package com.elziojunior.simplifiedbankingservice.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/** Defines the public OpenAPI metadata governed by ADR-0032. */
@Configuration
public class OpenApiConfiguration {

    /** Builds the discoverable API identity shown by Swagger UI and exported specifications. */
    @Bean
    OpenAPI bankingOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Simplified Banking Service API")
                .version("v1")
                .description(
                        "Public account, transfer, and movement operations. Error responses use RFC 9457 Problem Details."));
    }
}
