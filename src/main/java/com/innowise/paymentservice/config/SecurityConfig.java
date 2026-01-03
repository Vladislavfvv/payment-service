package com.innowise.paymentservice.config;

import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.jsonwebtoken.security.Keys;

/**
 * Конфигурация Spring Security для payment-service.
 * Настраивает OAuth2 Resource Server для валидации JWT токенов от authentication-service.
 * 
 * ВАЖНО: Gateway-service уже проверяет JWT токены, но payment-service также должен
 * проверять токены для дополнительной защиты (defense in depth).
 */
@Configuration
@EnableWebSecurity
@org.springframework.context.annotation.Profile("!test") // Не загружается в тестах
public class SecurityConfig {

    @Value("${jwt.secret:mySecretKeyForJWTGenerationInAuthenticationService2025}")
    private String jwtSecret;

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    
    /**
     * Настраивает Security Filter Chain для работы с JWT токенами.
     * 
     * Правила доступа:
     * - Все эндпоинты требуют аутентификации (JWT токен)
     * - Публичные эндпоинты: /actuator/health, /actuator/info (для мониторинга)
     * - JWT токен проверяется через JwtDecoder
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new RequestLoggingFilter(), AuthorizationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Публичные эндпоинты - доступны без аутентификации для мониторинга
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        // Все остальные эндпоинты требуют аутентификации
                        .anyRequest().authenticated()
                )
                // Для запросов, требующих аутентификации, проверяем JWT токен из заголовка Authorization: Bearer <token>
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder()))
                );

        return http.build();
    }
    
    /**
     * Фильтр для логирования всех входящих запросов ДО Security фильтров.
     */
    private static class RequestLoggingFilter extends OncePerRequestFilter {
        private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
            log.error("=== SECURITY FILTER: INCOMING REQUEST ===");
            log.error("Method: {}", request.getMethod());
            log.error("URI: {}", request.getRequestURI());
            log.error("Path: {}", request.getPathInfo());
            log.error("Query: {}", request.getQueryString());
            log.error("==========================================");
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Создает JWT Decoder для проверки JWT токенов.
     * Использует симметричный ключ (HS256) для проверки подлинности токенов.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }
}

