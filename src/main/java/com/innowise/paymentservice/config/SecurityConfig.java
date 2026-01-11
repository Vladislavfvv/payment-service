package com.innowise.paymentservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;

/**
 * Р С™Р С•Р Р…РЎвЂћР С‘Р С–РЎС“РЎР‚Р В°РЎвЂ Р С‘РЎРЏ Р В±Р ВµР В·Р С•Р С—Р В°РЎРѓР Р…Р С•РЎРѓРЎвЂљР С‘ Р Т‘Р В»РЎРЏ payment-service.
 * Р СћРЎР‚Р ВµР В±РЎС“Р ВµРЎвЂљ JWT РЎвЂљР С•Р С”Р ВµР Р… Р Т‘Р В»РЎРЏ Р Р†РЎРѓР ВµРЎвЂ¦ РЎРЊР Р…Р Т‘Р С—Р С•Р С‘Р Р…РЎвЂљР С•Р Р†, Р С”РЎР‚Р С•Р СР Вµ actuator health/info.
 * Р ВРЎРѓР С—Р С•Р В»РЎРЉР В·РЎС“Р ВµРЎвЂљРЎРѓРЎРЏ РЎвЂљР С•Р В»РЎРЉР С”Р С• Р Р† production Р С•Р С”РЎР‚РЎС“Р В¶Р ВµР Р…Р С‘Р С‘ (Р Р…Р Вµ Р Р† РЎвЂљР ВµРЎРѓРЎвЂљР В°РЎвЂ¦).
 */
@Configuration
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${jwt.secret:mySecretKeyForJWTGenerationInAuthenticationService2025}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("========================================");
        log.info("SecurityConfig.securityFilterChain() CALLED!");
        log.info("========================================");
        
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder()))
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.error("========================================");
                            log.error("=== JWT AUTHENTICATION ERROR ===");
                            log.error("Request URI: {}", request.getRequestURI());
                            log.error("Error: {}", authException.getMessage(), authException);
                            log.error("========================================");
                            response.setStatus(401);
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + authException.getMessage() + "\"}");
                        })
                )
                .addFilterBefore(new RequestLoggingFilter(), BearerTokenAuthenticationFilter.class);

        log.info("SecurityFilterChain configured successfully");
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        log.info("========================================");
        log.info("SecurityConfig.jwtDecoder() CALLED!");
        log.info("JWT Secret length: {}", jwtSecret.length());
        log.info("JWT Secret (first 20 chars): {}", jwtSecret.substring(0, Math.min(20, jwtSecret.length())));
        
        try {
            SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey).build();
            log.info("JwtDecoder created successfully");
            return decoder;
        } catch (Exception e) {
            log.error("ERROR creating JwtDecoder: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Custom filter for logging incoming requests and authentication status.
     */
    private static class RequestLoggingFilter implements Filter {
        private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
                throws IOException, ServletException {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String uri = httpRequest.getRequestURI();
            String method = httpRequest.getMethod();
            
            // Log request before authentication (only for non-actuator endpoints to reduce noise)
            if (!uri.startsWith("/actuator/")) {
                String authHeader = httpRequest.getHeader("Authorization");
                log.debug("========================================");
                log.debug("=== SECURITY FILTER: INCOMING REQUEST ===");
                log.debug("URI: {}, Method: {}", uri, method);
                log.debug("Authorization header present: {}", authHeader != null);
                if (authHeader != null) {
                    log.debug("Authorization header (first 50 chars): {}", authHeader.substring(0, Math.min(50, authHeader.length())));
                }
            }
            
            // Continue filter chain
            chain.doFilter(request, response);
            
            // Log authentication status after filter chain (only for non-actuator endpoints)
            if (!uri.startsWith("/actuator/")) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                log.debug("=== AFTER AUTHENTICATION FILTER ===");
                log.debug("Authentication: {}", authentication != null ? "PRESENT" : "NULL");
                log.debug("Authenticated: {}", authentication != null && authentication.isAuthenticated());
                log.debug("Authentication class: {}", authentication != null ? authentication.getClass().getName() : "null");
                log.debug("Authentication name: {}", authentication != null ? authentication.getName() : "null");
                log.debug("========================================");
            }
        }
    }
}

