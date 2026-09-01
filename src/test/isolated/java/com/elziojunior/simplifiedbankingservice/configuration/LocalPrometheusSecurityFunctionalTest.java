package com.elziojunior.simplifiedbankingservice.configuration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.elziojunior.simplifiedbankingservice.metrics.ApiMetrics;

@WebMvcTest(
        controllers = LocalPrometheusSecurityFunctionalTest.ScrapeController.class,
        properties = "observability.prometheus.public-scrape-enabled=true")
@Import({ SecurityConfiguration.class, LocalPrometheusSecurityFunctionalTest.ScrapeController.class })
class LocalPrometheusSecurityFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApiMetrics apiMetrics;

    /** Proves the explicit local setting opens only the scrape chain needed by the internal Prometheus container. */
    @Test
    void shouldAllowPrometheusScrapeWithoutCredentials() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string("metrics"));
    }

    @RestController
    static class ScrapeController {

        @GetMapping("/actuator/prometheus")
        String scrape() {
            return "metrics";
        }
    }
}
