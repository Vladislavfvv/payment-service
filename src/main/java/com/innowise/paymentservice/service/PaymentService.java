package com.innowise.paymentservice.service;

import com.innowise.paymentservice.client.ExternalApiClient;
import com.innowise.paymentservice.dto.CreatePaymentRequest;
import com.innowise.paymentservice.dto.PaymentDto;
import com.innowise.paymentservice.dto.TotalSumResponse;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.model.PaymentStatus;
import com.innowise.paymentservice.producer.PaymentEventProducer;
import com.innowise.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository repository;
    private final PaymentMapper paymentMapper;
    private final ExternalApiClient externalApiClient;
    private final PaymentEventProducer paymentEventProducer;

    @Transactional
    public PaymentDto createPayment(CreatePaymentRequest request) {
        log.info("Creating payment for orderId: {}, userId: {}", request.getOrderId(), request.getUserId());
        
        // Convert DTO to Entity using MapStruct
        Payment payment = paymentMapper.toEntity(request);

        if (payment.getStatus() == null) {
            payment.setStatus(PaymentStatus.CREATED);
        }
        payment.setTimestamp(Instant.now());
        
        // Save entity to database (DAO layer operates with entities)
        Payment saved = repository.save(payment);
        log.info("Payment created with id: {}", saved.getId());
        
        // Call external API to generate random number and update payment status
        updatePaymentStatusFromExternalApi(saved);
        
        // Reload payment to get updated status
        Payment updatedPayment = repository.findById(saved.getId())
                .orElseThrow(() -> new RuntimeException("Payment not found after update: " + saved.getId()));
        
        // Send CREATE_PAYMENT event to Kafka
        try {
            paymentEventProducer.sendCreatePaymentEvent(updatedPayment);
        } catch (Exception e) {
            log.error("Failed to send CREATE_PAYMENT event to Kafka for paymentId: {}", updatedPayment.getId(), e);
            // Continue execution even if Kafka event fails
        }
        
        // Convert Entity back to DTO for response
        return paymentMapper.toDto(updatedPayment);
    }
    
    /**
     * Call external API to generate random number and update payment status
     * If number is even -> SUCCESS, otherwise -> FAILED
     */
    private void updatePaymentStatusFromExternalApi(Payment payment) {
        log.info("Calling external API to determine payment status for payment id: {}", payment.getId());
        
        Integer randomNumber = externalApiClient.getRandomNumber();
        
        if (randomNumber != null) {
            log.info("Received random number from external API: {} (for payment id: {})", randomNumber, payment.getId());
            
            // If number is even -> SUCCESS, otherwise -> FAILED
            boolean isEven = (randomNumber % 2 == 0);
            PaymentStatus newStatus = isEven ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
            payment.setStatus(newStatus);
            repository.save(payment);
            log.info("Payment status updated to {} based on random number: {} (number is {})", 
                    newStatus, randomNumber, isEven ? "even" : "odd");
        } else {
            // If API call failed, set status to FAILED
            payment.setStatus(PaymentStatus.FAILED);
            repository.save(payment);
            log.warn("External API call failed, payment status set to FAILED for payment id: {}", payment.getId());
        }
    }


    public List<PaymentDto> getPaymentsByOrderId(String orderId) {
        log.info("Getting payments for orderId: {}", orderId);
        
        List<Payment> payments = repository.findByOrderId(orderId);
        
        return paymentMapper.toDtoList(payments);
    }


    public List<PaymentDto> getPaymentsByUserId(String userId) {
        log.info("Getting payments for userId: {}", userId);
        
        List<Payment> payments = repository.findByUserId(userId);
       
        return paymentMapper.toDtoList(payments);
    }


    public List<PaymentDto> getPaymentsByStatuses(List<PaymentStatus> statuses) {
        log.info("Getting payments for statuses: {}", statuses);
       
        List<Payment> payments = repository.findByStatusIn(statuses);
       
        return paymentMapper.toDtoList(payments);
    }


    public TotalSumResponse getTotalSumByDatePeriod(Instant startDate, Instant endDate) {
        log.info("Calculating total sum for period: {} to {}", startDate, endDate);
        
        
        List<Payment> payments = repository.findByTimestampBetween(startDate, endDate);
        
        BigDecimal totalSum = payments.stream()
                .map(Payment::getPaymentAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add); 
        
        log.info("Total sum calculated: {} for {} payments", totalSum, payments.size());
        
        return TotalSumResponse.builder()
                .totalSum(totalSum)
                .startDate(startDate)
                .endDate(endDate)
                .paymentCount((long) payments.size())
                .build();
    }

    
    public TotalSumResponse getTotalSumByDatePeriodAndStatuses(
            Instant startDate, 
            Instant endDate, 
            List<PaymentStatus> statuses
    ) {
        log.info("Calculating total sum for period: {} to {} with statuses: {}", startDate, endDate, statuses);
               
        List<Payment> payments = repository.findByStatusInAndTimestampBetween(statuses, startDate, endDate);
        
        BigDecimal totalSum = payments.stream()
                .map(Payment::getPaymentAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.info("Total sum calculated: {} for {} payments", totalSum, payments.size());
        
        return TotalSumResponse.builder()
                .totalSum(totalSum)
                .startDate(startDate)
                .endDate(endDate)
                .paymentCount((long) payments.size())
                .build();
    }
}
