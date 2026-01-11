package com.innowise.paymentservice.consumer;

import com.innowise.paymentservice.dto.CreateOrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

/**
 * Kafka Consumer for handling CREATE_ORDER events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private static final String CREATE_ORDER_TOPIC = "create-order-events";
    private static final String GROUP_ID = "payment-service-group";

    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("OrderEventConsumer initialized");
        log.info("Listening to topic: {}", CREATE_ORDER_TOPIC);
        log.info("Consumer group: {}", GROUP_ID);
        log.info("========================================");
    }

    /**
     * Handle CREATE_ORDER event from Kafka
     */
    @KafkaListener(topics = CREATE_ORDER_TOPIC, groupId = GROUP_ID, containerFactory = "orderEventKafkaListenerContainerFactory")
    public void handleCreateOrderEvent(
            @Payload CreateOrderEvent event,            
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            // ErrorHandlingDeserializer может вернуть null при ошибке десериализации
            if (event == null) {
                log.warn("Received null CREATE_ORDER event at offset {}, skipping (likely deserialization error from old message format)", offset);
                // Acknowledge чтобы пропустить проблемное сообщение
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
                return;
            }
            
            log.info("========================================");
            log.info("=== KAFKA: Received CREATE_ORDER event ===");
            log.info("Order ID: {}", event.getOrderId());
            log.info("User ID: {}", event.getUserId());
            log.info("Offset: {}", offset);
            log.info("========================================");
            
            // Платеж НЕ создается автоматически при создании заказа
            // Платеж будет создан только когда пользователь нажмет кнопку "Оплатить" на фронтенде
            log.info("CREATE_ORDER event received for orderId: {}. Payment will be created only when user clicks 'Pay' button.", event.getOrderId());

            // Acknowledge message processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
            log.info("========================================");
            log.info("=== KAFKA: Successfully processed CREATE_ORDER event ===");
            log.info("Order ID: {}", event.getOrderId());
            log.info("========================================");
        } catch (Exception e) {
            log.error("========================================");
            log.error("=== KAFKA: ERROR processing CREATE_ORDER event ===");
            log.error("Order ID: {}", event != null ? event.getOrderId() : "null");
            log.error("Error: {}", e.getMessage(), e);
            log.error("========================================");
            throw e; 
        }
    }   
}

