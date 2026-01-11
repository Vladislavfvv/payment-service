package com.innowise.paymentservice.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Утилитный класс для работы с Spring Security и извлечения данных из JWT токена.
 */
@Slf4j
public class SecurityUtils {

    /**
     * Извлекает userId пользователя из JWT токена.
     * 
     * ВАЖНО: JWT токен, генерируемый authentication-service, содержит только:
     * - "sub" (subject) - email пользователя
     * - "role" - роль пользователя
     * 
     * НЕТ claim "userId" в токене!
     * 
     * Поэтому используем email из "sub" как userId.
     * Это соответствует тому, как работают user-service и order-service.
     * 
     * Выбрасывает IllegalStateException, если токен не содержит email (sub).
     */
    public static String getUserIdFromToken(Authentication authentication) {
        log.error("========================================");
        log.error("SecurityUtils.getUserIdFromToken() CALLED");
        log.error("========================================");
        
        log.error("Authentication object: {}", authentication);
        log.error("Authentication class: {}", authentication != null ? authentication.getClass().getName() : "null");

        if (authentication == null) {
            log.error("ERROR: Authentication is null!");
            throw new IllegalStateException("Authentication is required");
        }

        Jwt jwt = extractJwt(authentication);
        log.error("Extracted JWT: {}", jwt != null ? "SUCCESS" : "FAILED");
        
        if (jwt == null) {
            log.error("ERROR: JWT token is null!");
            throw new IllegalStateException("JWT token is required");
        }

        // Логируем все claims токена для диагностики
        log.error("=== JWT TOKEN CLAIMS ===");
        log.error("JWT Subject (sub): {}", jwt.getSubject());
        log.error("JWT Claims: {}", jwt.getClaims());

        // Пытаемся получить userId из claim "userId" (на случай, если в будущем его добавят)
        String userId = jwt.getClaimAsString("userId");
        log.error("Claim 'userId': {}", userId);
        
        if (userId != null && !userId.isBlank()) {
            log.error(">>> Using userId from claim 'userId': {}", userId);
            return userId;
        }

        // JWT токен НЕ содержит userId, используем email из "sub" как userId
        // Это соответствует реализации в user-service и order-service
        String email = jwt.getSubject();
        log.error("Claim 'sub' (email): {}", email);
        
        if (email != null && !email.isBlank()) {
            log.error(">>> Using email from claim 'sub' as userId: {}", email);
            return email;
        }

        log.error("ERROR: 'sub' claim (email) not found in JWT token!");
        throw new IllegalStateException("Email (sub claim) not found in JWT token");
    }

    /**
     * Извлекает email пользователя из JWT токена (claim "sub").
     * Выбрасывает IllegalStateException, если токен не содержит email.
     */
    public static String getEmailFromToken(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("Authentication is required");
        }

        Jwt jwt = extractJwt(authentication);
        if (jwt == null) {
            throw new IllegalStateException("JWT token is required");
        }

        String email = jwt.getSubject();
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Email (sub claim) not found in JWT token");
        }

        return email;
    }

    /**
     * Извлекает строку токена из Authentication объекта для передачи в другие сервисы.
     * Возвращает токен в формате "Bearer {token}".
     * Выбрасывает IllegalStateException, если токен не найден.
     */
    public static String getTokenString(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("Authentication is required");
        }

        Jwt jwt = extractJwt(authentication);
        if (jwt == null) {
            throw new IllegalStateException("JWT token is required");
        }

        return "Bearer " + jwt.getTokenValue();
    }

    /**
     * Извлекает JWT из Authentication объекта.
     * Возвращает JWT токен или null, если не удалось извлечь.
     */
    private static Jwt extractJwt(Authentication authentication) {
        System.err.println("=== EXTRACTING JWT FROM AUTHENTICATION ===");
        System.err.println("Authentication: " + authentication);
        System.err.println("Authentication type: " + (authentication != null ? authentication.getClass().getName() : "null"));
        log.error("=== EXTRACTING JWT FROM AUTHENTICATION ===");
        log.error("Authentication: {}", authentication);
        log.error("Authentication type: {}", authentication != null ? authentication.getClass().getName() : "null");
        
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            System.err.println(">>> Found JwtAuthenticationToken");
            System.err.println("Token: " + jwtAuthenticationToken.getToken());
            log.error(">>> Found JwtAuthenticationToken");
            log.error("Token: {}", jwtAuthenticationToken.getToken());
            return jwtAuthenticationToken.getToken();
        }
        
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            System.err.println(">>> Found JWT in principal");
            System.err.println("JWT: " + jwt);
            log.error(">>> Found JWT in principal");
            log.error("JWT: {}", jwt);
            return jwt;
        }
        
        System.err.println(">>> WARNING: Could not extract JWT from Authentication!");
        System.err.println("Principal type: " + (authentication != null && authentication.getPrincipal() != null ? authentication.getPrincipal().getClass().getName() : "null"));
        log.error(">>> WARNING: Could not extract JWT from Authentication!");
        log.error("Principal type: {}", authentication != null && authentication.getPrincipal() != null ? authentication.getPrincipal().getClass().getName() : "null");
        return null;
    }
}

