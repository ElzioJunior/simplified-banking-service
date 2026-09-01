package com.elziojunior.simplifiedbankingservice.configuration;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;

/** Defines the deliberately narrow temporary API and documentation security exceptions. */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfiguration {

    /**
     * Allows only the internal Compose Prometheus scraper to read its dedicated
     * endpoint when ADR-0033's explicit local setting is active.
     */
    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "observability.prometheus.public-scrape-enabled", havingValue = "true")
    SecurityFilterChain localPrometheusSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/actuator/prometheus")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }

    /**
     * Leaves versioned API and discoverability routes unauthenticated for the
     * initial scope while retaining protection for operational routes.
     */
    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // TODO(ADR-0027): require bearer tokens in the Authorization header when
        // the authentication model and token validation mechanism are approved.
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/**"))
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/api/v1/**", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
