package com.innowise.paymentservice.service;

import com.innowise.paymentservice.client.ExternalApiClient;
import com.innowise.paymentservice.client.OrderServiceClient;
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
    private final OrderServiceClient orderServiceClient;

    @Transactional
    public PaymentDto createPayment(CreatePaymentRequest request, String authToken) {
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
        
        // Согласно требованиям: при создании платежа статус заказа становится PROCESSING
        // (используется если есть задержка в получении ответа)
        try {
            Long orderId = Long.parseLong(request.getOrderId());
            orderServiceClient.updateOrderStatus(orderId, "PROCESSING", authToken);
            log.info("Order {} status updated to PROCESSING after payment creation", orderId);
        } catch (Exception e) {
            log.error("Failed to update order status to PROCESSING for orderId: {}", request.getOrderId(), e);
            // Continue execution even if order status update fails
        }
        
        // Call external API to generate random number and update payment status
        updatePaymentStatusFromExternalApi(saved);
        
        // Reload payment to get updated status
        Payment updatedPayment = repository.findById(saved.getId())
                .orElseThrow(() -> new RuntimeException("Payment not found after update: " + saved.getId()));
        
        // Update order status based on payment status
        // Согласно требованиям: когда платеж создан (SUCCESS или FAILED), статус заказа становится CANCELED
        try {
            Long orderId = Long.parseLong(request.getOrderId());
            // После создания платежа (независимо от результата SUCCESS/FAILED) статус заказа = CANCELED
            String orderStatus = "CANCELED";
            orderServiceClient.updateOrderStatus(orderId, orderStatus, authToken);
            log.info("Order {} status updated to CANCELED after payment processing (payment status: {})", 
                    orderId, updatedPayment.getStatus());
        } catch (Exception e) {
            log.error("Failed to update order status to CANCELED for orderId: {}", request.getOrderId(), e);
            // Continue execution even if order status update fails
        }
        
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
     * 
     * Согласно требованиям: при создании платежа может быть задержка в получении ответа,
     * в этом случае статус заказа должен быть PROCESSING
     */
    private void updatePaymentStatusFromExternalApi(Payment payment) {
        log.info("Calling external API to determine payment status for payment id: {}", payment.getId());
        
        // TODO: Если есть задержка в получении ответа, можно установить статус заказа в PROCESSING
        // через orderServiceClient.updateOrderStatus(orderId, "PROCESSING", authToken)
        // Но для этого нужен доступ к orderId и authToken, которые сейчас недоступны в этом методе
        
        Integer randomNumber = externalApiClient.getRandomNumber();
        
        log.info("========================================");
        log.info("=== EXTERNAL API RESULT ===");
        log.info("Payment ID: {}", payment.getId());
        log.info("Random number received: {}", randomNumber);
        log.info("Random number is null: {}", randomNumber == null);
        
        if (randomNumber != null) {
            log.info("Received random number from external API: {} (for payment id: {})", randomNumber, payment.getId());
            
            // If number is even -> SUCCESS, otherwise -> FAILED
            boolean isEven = (randomNumber % 2 == 0);
            int remainder = randomNumber % 2;
            log.info("Random number: {}, Remainder (mod 2): {}, Is even: {}", randomNumber, remainder, isEven);
            
            PaymentStatus newStatus = isEven ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
            payment.setStatus(newStatus);
            repository.save(payment);
            log.info("Payment status updated to {} based on random number: {} (number is {})", 
                    newStatus, randomNumber, isEven ? "even" : "odd");
            log.info("=== RESULT: Payment status = {} ===", newStatus);
        } else {
            // If API call failed, set status to FAILED
            payment.setStatus(PaymentStatus.FAILED);
            repository.save(payment);
            log.warn("External API call failed or returned null, payment status set to FAILED for payment id: {}", payment.getId());
            log.info("=== RESULT: Payment status = FAILED (API returned null) ===");
        }
        log.info("========================================");
    }


    /**
     * Получение всех платежей.
     * 
     * @return список всех платежей
     */
    public List<PaymentDto> getAllPayments() {
        log.info("Getting all payments");
        
        List<Payment> payments = repository.findAll();
        log.info("Found {} payments in database", payments.size());
        
        return paymentMapper.toDtoList(payments);
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

    /**
     * Получение общей суммы всех платежей.
     * 
     * @return общая сумма и количество всех платежей
     */
    public TotalSumResponse getTotalSum() {
        log.info("Calculating total sum for all payments");
        
        List<Payment> payments = repository.findAll();
        
        BigDecimal totalSum = payments.stream()
                .map(Payment::getPaymentAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.info("Total sum calculated: {} for {} payments", totalSum, payments.size());
        
        return TotalSumResponse.builder()
                .totalSum(totalSum)
                .paymentCount((long) payments.size())
                .build();
    }

    /**
     * Получение общей суммы платежей по статусам.
     * 
     * @param statuses список статусов для фильтрации
     * @return общая сумма и количество платежей с указанными статусами
     */
    public TotalSumResponse getTotalSumByStatuses(List<PaymentStatus> statuses) {
        log.info("Calculating total sum for statuses: {}", statuses);
        
        List<Payment> payments = repository.findByStatusIn(statuses);
        
        BigDecimal totalSum = payments.stream()
                .map(Payment::getPaymentAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.info("Total sum calculated: {} for {} payments", totalSum, payments.size());
        
        return TotalSumResponse.builder()
                .totalSum(totalSum)
                .paymentCount((long) payments.size())
                .build();
    }

    /**
     * Получение общей суммы платежей для конкретного пользователя.
     * 
     * @param userId ID пользователя
     * @param startDate опциональная начальная дата периода
     * @param endDate опциональная конечная дата периода
     * @param statuses опциональный список статусов для фильтрации
     * @return общая сумма и количество платежей пользователя
     */
    public TotalSumResponse getTotalSumByUserId(
            String userId,
            Instant startDate,
            Instant endDate,
            List<PaymentStatus> statuses
    ) {
        System.err.println("========================================");
        System.err.println("PaymentService.getTotalSumByUserId() CALLED");
        System.err.println("========================================");
        System.err.println("=== SEARCHING FOR PAYMENTS ===");
        System.err.println("UserId: " + userId);
        System.err.println("StartDate: " + startDate);
        System.err.println("EndDate: " + endDate);
        System.err.println("Statuses: " + statuses);
        log.error("========================================");
        log.error("PaymentService.getTotalSumByUserId() CALLED");
        log.error("=== SEARCHING FOR PAYMENTS ===");
        log.error("UserId: {}", userId);
        log.error("StartDate: {}, EndDate: {}, Statuses: {}", startDate, endDate, statuses);
        
        List<Payment> payments;
        
        // Если даты не указаны
        if (startDate == null && endDate == null) {
            System.err.println(">>> No date range specified, searching all payments for userId: " + userId);
            log.error(">>> No date range specified, searching all payments for userId: {}", userId);
            
            if (statuses != null && !statuses.isEmpty()) {
                // Только по статусам
                System.err.println(">>> Filtering by statuses: " + statuses);
                log.error(">>> Filtering by statuses: {}", statuses);
                payments = repository.findByUserIdAndStatusIn(userId, statuses);
            } else {
                // Все платежи пользователя
                System.err.println(">>> No status filter, getting all payments for userId: " + userId);
                log.error(">>> No status filter, getting all payments for userId: {}", userId);
                payments = repository.findByUserId(userId);
            }
        } else if (startDate != null && endDate != null) {
            // Если указаны обе даты
            System.err.println(">>> Date range specified: " + startDate + " to " + endDate);
            log.error(">>> Date range specified: {} to {}", startDate, endDate);
            
            if (statuses != null && !statuses.isEmpty()) {
                // По датам и статусам
                System.err.println(">>> Filtering by date range and statuses: " + statuses);
                log.error(">>> Filtering by date range and statuses: {}", statuses);
                payments = repository.findByUserIdAndStatusInAndTimestampBetween(userId, statuses, startDate, endDate);
            } else {
                // Только по датам
                System.err.println(">>> Filtering by date range only");
                log.error(">>> Filtering by date range only");
                payments = repository.findByUserIdAndTimestampBetween(userId, startDate, endDate);
            }
        } else {
            throw new IllegalArgumentException("Both startDate and endDate must be provided, or neither");
        }
        
        System.err.println("=== PAYMENTS FOUND ===");
        System.err.println("Found " + payments.size() + " payments for userId: " + userId);
        log.error("=== PAYMENTS FOUND ===");
        log.error("Found {} payments for userId: {}", payments.size(), userId);
        
        BigDecimal totalSum = payments.stream()
                .map(Payment::getPaymentAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        System.err.println("=== CALCULATED TOTAL ===");
        System.err.println("TotalSum: " + totalSum);
        System.err.println("PaymentCount: " + payments.size());
        System.err.println("========================================");
        log.error("=== CALCULATED TOTAL ===");
        log.error("TotalSum: {}, PaymentCount: {}", totalSum, payments.size());
        log.error("========================================");
        
        return TotalSumResponse.builder()
                .totalSum(totalSum)
                .startDate(startDate)
                .endDate(endDate)
                .paymentCount((long) payments.size())
                .build();
    }
}
