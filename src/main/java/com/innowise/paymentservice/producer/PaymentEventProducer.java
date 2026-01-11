package com.innowise.paymentservice.producer;

import com.innowise.paymentservice.dto.CreatePaymentEvent;
import com.innowise.paymentservice.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka Producer for sending CREATE_PAYMENT events
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventProducer {

    private static final String CREATE_PAYMENT_TOPIC = "create-payment-events";

    private final KafkaTemplate<String, CreatePaymentEvent> kafkaTemplate;

    /**
     * Send CREATE_PAYMENT event to Kafka
     */
    public void sendCreatePaymentEvent(Payment payment) {
        try {
            CreatePaymentEvent event = buildCreatePaymentEvent(payment);
            
            log.info("Sending CREATE_PAYMENT event to Kafka for paymentId: {}", payment.getId());
            
            CompletableFuture<SendResult<String, CreatePaymentEvent>> future = 
                    kafkaTemplate.send(CREATE_PAYMENT_TOPIC, payment.getId(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("CREATE_PAYMENT event sent successfully for paymentId: {}, offset: {}", 
                            payment.getId(), result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send CREATE_PAYMENT event for paymentId: {}", payment.getId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error sending CREATE_PAYMENT event for paymentId: {}", payment.getId(), e);
        }
    }

    private CreatePaymentEvent buildCreatePaymentEvent(Payment payment) {
        // Согласно требованиям: когда платеж создан (SUCCESS или FAILED), 
        // статус заказа становится CANCELED
        // Отправляем CANCELED независимо от статуса платежа
        return new CreatePaymentEvent(               
                payment.getOrderId(),              
                "CANCELED"); // Всегда CANCELED после создания платежа
    }
}

