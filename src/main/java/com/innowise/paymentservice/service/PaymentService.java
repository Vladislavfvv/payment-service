package com.innowise.paymentservice.service;

import com.innowise.paymentservice.dto.TotalSumResponse;
import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.model.PaymentStatus;
import com.innowise.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository repository;

    
    public Payment createPayment(Payment payment) {
        log.info("Creating payment for orderId: {}, userId: {}", payment.getOrderId(), payment.getUserId());
        
        // Если статус не указан, устанавливаем CREATED по умолчанию
        if (payment.getStatus() == null) {
            payment.setStatus(PaymentStatus.CREATED);
        }
        payment.setTimestamp(Instant.now());
        
        Payment saved = repository.save(payment);
        log.info("Payment created with id: {}", saved.getId());
        return saved;
    }


    public List<Payment> getPaymentsByOrderId(String orderId) {
        log.info("Getting payments for orderId: {}", orderId);
        return repository.findByOrderId(orderId);
    }


    public List<Payment> getPaymentsByUserId(String userId) {
        log.info("Getting payments for userId: {}", userId);
        return repository.findByUserId(userId);
    }


    public List<Payment> getPaymentsByStatuses(List<PaymentStatus> statuses) {
        log.info("Getting payments for statuses: {}", statuses);
        return repository.findByStatusIn(statuses);
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

    /**
     * Получить общую сумму платежей за период дат с фильтром по статусам
     */
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
