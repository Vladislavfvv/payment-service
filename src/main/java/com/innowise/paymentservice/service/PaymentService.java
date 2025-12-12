package com.innowise.paymentservice.service;

import com.innowise.paymentservice.dto.CreatePaymentRequest;
import com.innowise.paymentservice.dto.PaymentDto;
import com.innowise.paymentservice.dto.TotalSumResponse;
import com.innowise.paymentservice.mapper.PaymentMapper;
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
    private final PaymentMapper paymentMapper;

    
    public PaymentDto createPayment(CreatePaymentRequest request) {
        log.info("Creating payment for orderId: {}, userId: {}", request.getOrderId(), request.getUserId());
        
       
        Payment payment = paymentMapper.toEntity(request);

        if (payment.getStatus() == null) {
            payment.setStatus(PaymentStatus.CREATED);
        }
        payment.setTimestamp(Instant.now());
        
        
        Payment saved = repository.save(payment);
        log.info("Payment created with id: {}", saved.getId());
        
        
        return paymentMapper.toDto(saved);
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
