package com.elziojunior.simplifiedbankingservice.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.elziojunior.simplifiedbankingservice.configuration.ApiMetricsConfiguration;
import com.elziojunior.simplifiedbankingservice.configuration.OpenApiConfiguration;
import com.elziojunior.simplifiedbankingservice.configuration.SecurityConfiguration;
import com.elziojunior.simplifiedbankingservice.metrics.ApiMetrics;
import com.elziojunior.simplifiedbankingservice.metrics.ApiMetricsInterceptor;
import com.elziojunior.simplifiedbankingservice.model.mapper.AccountMapperImpl;
import com.elziojunior.simplifiedbankingservice.model.mapper.AccountMovementMapperImpl;
import com.elziojunior.simplifiedbankingservice.model.mapper.TransferMapperImpl;
import com.elziojunior.simplifiedbankingservice.model.mapper.TransferTokenMapperImpl;
import com.elziojunior.simplifiedbankingservice.service.AccountService;
import com.elziojunior.simplifiedbankingservice.service.TransferService;
import com.elziojunior.simplifiedbankingservice.service.TransferTokenService;
import com.elziojunior.simplifiedbankingservice.service.AccountMovementService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springdoc.webmvc.ui.SwaggerConfig;

@WebMvcTest({
        AccountController.class,
        AccountMovementController.class,
        TransferController.class,
        TransferTokenController.class
})
@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocWebMvcConfiguration.class, SwaggerConfig.class})
@EnableConfigurationProperties({
        SpringDocConfigProperties.class,
        SwaggerUiConfigProperties.class,
        SwaggerUiOAuthProperties.class
})
@Import({
        AccountMapperImpl.class,
        AccountMovementMapperImpl.class,
        TransferMapperImpl.class,
        TransferTokenMapperImpl.class,
        ApiExceptionHandler.class,
        ApiMetricsConfiguration.class,
        ApiMetricsInterceptor.class,
        OpenApiConfiguration.class,
        SecurityConfiguration.class
})
class OpenApiDocumentationFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private AccountMovementService accountMovementService;

    @MockitoBean
    private TransferService transferService;

    @MockitoBean
    private TransferTokenService transferTokenService;

    @MockitoBean
    private ApiMetrics apiMetrics;

    /** Proves every public endpoint and its principal success and validation examples are exported together. */
    @Test
    void shouldPublishEveryPublicOperationAndPrincipalExamples() throws Exception {
        String contract = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode paths = objectMapper.readTree(contract).path("paths");

        JsonNode account = paths.path("/api/v1/accounts").path("post");
        assertThat(account.path("requestBody").toString())
                .contains("positiveOpeningBalance", "zeroOpeningBalance", "blankNameValidation",
                        "missingBalanceValidation", "unsupportedBalanceValidation");
        assertThat(account.path("responses").toString())
                .contains("createdAccount", "constraintValidation", "malformedJson", "unsupportedMonetaryRange");

        JsonNode movements = paths.path("/api/v1/accounts/{accountId}/movements").path("get");
        assertThat(movements.path("parameters").toString())
                .contains("accountId", "page", "period", "type", "invalidNegativePage", "oneDay", "oneWeek",
                        "oneMonth", "invalidPeriod", "invalidType")
                .doesNotContain("\"start\"", "\"end\"");
        assertThat(movements.path("responses").toString())
                .contains(
                        "filteredMovementPage", "emptyMovementPage", "invalidPeriod", "unknownAccount",
                        "persistenceUnavailable");

        JsonNode token = paths.path("/api/v1/transfer-tokens").path("post");
        assertThat(token.path("responses").toString()).contains("issuedTransferToken", "persistenceUnavailable");

        JsonNode transfer = paths.path("/api/v1/transfers").path("post");
        assertThat(transfer.path("parameters").toString()).contains("Idempotency-Key");
        assertThat(transfer.path("requestBody").toString())
                .contains("successfulTransfer", "idempotentReplay", "zeroAmountValidation", "sameAccountConflict",
                        "unsupportedAmountValidation", "insufficientFundsConflict", "unknownAccount",
                        "tokenPayloadMismatch");
        assertThat(transfer.path("responses").toString())
                .contains("completedTransfer", "missingIdempotencyKey", "malformedIdempotencyKey",
                        "unknownTransferAccount", "invalidToken", "expiredToken", "tokenPayloadMismatch",
                        "persistenceUnavailable");

        assertThat(paths.fieldNames()).toIterable().containsExactlyInAnyOrder(
                "/api/v1/accounts",
                "/api/v1/accounts/{accountId}/movements",
                "/api/v1/transfer-tokens",
                "/api/v1/transfers");
    }

    /** Proves developers can reach Swagger UI without credentials while operational authentication remains separate. */
    @Test
    void shouldExposeSwaggerUiWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/swagger-ui/index.html"));
    }
}
