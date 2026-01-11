package com.innowise.paymentservice.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Тестовая конфигурация безопасности для интеграционных тестов.
 * Отключает проверку безопасности, чтобы упростить тестирование REST API.
 * ВАЖНО: Это используется только для интеграционных тестов контроллера,
 * чтобы избежать проблем с мокированием JwtDecoder в Spring Security Test.
 * 
 * Используем @Primary и @Order(1) для обеспечения приоритета над SecurityConfig.
 * @ConditionalOnMissingBean гарантирует, что этот bean создается только если нет другого SecurityFilterChain.
 */
@Configuration
@EnableWebSecurity
public class TestSecurityConfig {
    @Bean
    @Primary
    @Order(1)
    @ConditionalOnMissingBean(name = "securityFilterChain")
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
