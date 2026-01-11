package com.innowise.paymentservice.client;

import com.innowise.paymentservice.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Objects;

/**
 * Клиент для взаимодействия с user-service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${user.service.url:http://user-service:8080}")
    private String userServiceUrl;

    /**
     * Получает пользователя по email из user-service
     * 
     * @param email email пользователя
     * @param authToken JWT токен для авторизации
     * @return UserDto с информацией о пользователе
     * @throws UserServiceException если пользователь не найден или произошла ошибка
     */
    public UserDto getUserByEmail(String email, String authToken) {
        try {
            String baseUrl = Objects.requireNonNullElse(userServiceUrl, "http://user-service:8080");
            WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();

            log.info("Calling user-service to get user by email: {}", email);

            UserDto userDto = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/users/email")
                            .queryParam("email", email)
                            .build())
                    .header("Authorization", buildAuthorizationHeader(authToken))
                    .retrieve()
                    .bodyToMono(UserDto.class)
                    .block();

            if (userDto == null) {
                log.error("User not found in user-service for email: {}", email);
                throw new UserServiceException("User not found for email: " + email);
            }

            log.info("User found in user-service: id={}, email={}", userDto.getId(), userDto.getEmail());
            return userDto;

        } catch (WebClientResponseException.NotFound e) {
            log.error("User not found in user-service for email {}: {}", email, e.getMessage());
            throw new UserServiceException("User not found for email: " + email, e);
        } catch (WebClientResponseException e) {
            log.error("Error calling user-service for email {}: HTTP {} - {}", 
                    email, e.getStatusCode(), e.getMessage());
            throw new UserServiceException("Failed to get user by email: " + email, e);
        } catch (Exception e) {
            log.error("Unexpected error calling user-service for email {}: {}", email, e.getMessage(), e);
            throw new UserServiceException("Unexpected error getting user by email: " + email, e);
        }
    }

    /**
     * Формирует корректный заголовок Authorization
     */
    private String buildAuthorizationHeader(String authToken) {
        if (authToken == null || authToken.isBlank()) {
            return "";
        }
        return authToken.startsWith("Bearer") ? authToken : "Bearer " + authToken;
    }

    /**
     * Исключение для ошибок при вызове user-service
     */
    public static class UserServiceException extends RuntimeException {
        public UserServiceException(String message) {
            super(message);
        }

        public UserServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

