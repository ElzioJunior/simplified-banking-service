package com.elziojunior.simplifiedbankingservice.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.elziojunior.simplifiedbankingservice.metrics.ApiMetricsInterceptor;

/** Registers API request metrics before controller arguments are resolved. */
@Configuration
public class ApiMetricsConfiguration implements WebMvcConfigurer {

    private final ApiMetricsInterceptor apiMetricsInterceptor;

    public ApiMetricsConfiguration(ApiMetricsInterceptor apiMetricsInterceptor) {
        this.apiMetricsInterceptor = apiMetricsInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiMetricsInterceptor);
    }
}
