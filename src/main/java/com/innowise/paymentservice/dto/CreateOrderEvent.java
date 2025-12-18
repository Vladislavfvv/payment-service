package com.innowise.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event DTO for CREATE_ORDER event received from Kafka
 * Это структура события, которое Order Service отправляет в Kafka в топик create-order-events.
 * Используется в:
    OrderEventProducer (в order-service) – как тип KafkaTemplate<String, CreateOrderEvent> и payload сообщения.
    OrderEventConsumer (в payment-service) – как тип объекта, который десериализуется из JSON.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateOrderEvent {
    private Long orderId;
    private Long userId;   
}

