package com.innowise.paymentservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Objects;

/**
 * Клиент для взаимодействия с order-service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${order.service.url:http://order-service:8080}")
    private String orderServiceUrl;

    /**
     * Обновляет статус заказа в order-service
     * 
     * @param orderId ID заказа для обновления
     * @param orderStatus новый статус заказа (SUCCESS или FAILED)
     * @param authToken JWT токен для авторизации
     * @throws OrderServiceException если произошла ошибка при обновлении статуса
     */
    public void updateOrderStatus(Long orderId, String orderStatus, String authToken) {
        try {
            String baseUrl = Objects.requireNonNullElse(orderServiceUrl, "http://order-service:8080");
            WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();

            log.info("Calling order-service to update order {} status to {}", orderId, orderStatus);

            webClient.put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/orders/{id}")
                            .build(orderId))
                    .header("Authorization", buildAuthorizationHeader(authToken))
                    .header("Content-Type", "application/json")
                    .bodyValue("{\"status\":\"" + orderStatus + "\"}")
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            log.info("Order {} status successfully updated to {}", orderId, orderStatus);

        } catch (WebClientResponseException.NotFound e) {
            log.error("Order {} not found in order-service: {}", orderId, e.getMessage());
            throw new OrderServiceException("Order not found: " + orderId, e);
        } catch (WebClientResponseException e) {
            log.error("Error calling order-service to update order {} status: HTTP {} - {}", 
                    orderId, e.getStatusCode(), e.getMessage());
            throw new OrderServiceException("Failed to update order status: " + orderId, e);
        } catch (Exception e) {
            log.error("Unexpected error calling order-service to update order {} status: {}", orderId, e.getMessage(), e);
            throw new OrderServiceException("Unexpected error updating order status: " + orderId, e);
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
     * Исключение для ошибок при вызове order-service
     */
    public static class OrderServiceException extends RuntimeException {
        public OrderServiceException(String message) {
            super(message);
        }

        public OrderServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
