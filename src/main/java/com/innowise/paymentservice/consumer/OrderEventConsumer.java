package com.innowise.paymentservice.consumer;

import com.innowise.paymentservice.dto.CreateOrderEvent;
import com.innowise.paymentservice.dto.CreatePaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import com.innowise.paymentservice.service.PaymentService;

/**
 * Kafka Consumer for handling CREATE_ORDER events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private static final String CREATE_ORDER_TOPIC = "create-order-events";
    private static final String GROUP_ID = "payment-service-group";

    private final PaymentService paymentService;

    /**
     * Handle CREATE_ORDER event from Kafka
     */
    @KafkaListener(topics = CREATE_ORDER_TOPIC, groupId = GROUP_ID)
    public void handleCreateOrderEvent(
            @Payload CreateOrderEvent event,            
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received CREATE_ORDER event: orderId={}, userId={}, offset={}", 
                    event.getOrderId(), event.getUserId(), offset);
                                 
            CreatePaymentRequest request = CreatePaymentRequest.builder()
            .orderId(String.valueOf(event.getOrderId()))
            .userId(String.valueOf(event.getUserId()))
            .paymentAmount(/* сюда можно передавать заранее посчитанную сумму, временно null */ null)
            .build();

            paymentService.createPayment(request);

            // Acknowledge message processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
            log.info("Successfully processed CREATE_ORDER event for orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Error processing CREATE_ORDER event for orderId: {}", event.getOrderId(), e);
            throw e; 
        }
    }   
}

