package com.elziojunior.simplifiedbankingservice.configuration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import com.elziojunior.simplifiedbankingservice.metrics.ApiMetricsInterceptor;

class ApiMetricsConfigurationTest {

    /** Proves the request metrics interceptor is registered with Spring MVC. */
    @Test
    void shouldRegisterApiMetricsInterceptor() {
        ApiMetricsInterceptor interceptor = mock(ApiMetricsInterceptor.class);
        InterceptorRegistry registry = mock(InterceptorRegistry.class);

        new ApiMetricsConfiguration(interceptor).addInterceptors(registry);

        verify(registry).addInterceptor(interceptor);
    }
}
