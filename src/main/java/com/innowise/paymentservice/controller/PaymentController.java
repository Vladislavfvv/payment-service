package com.innowise.paymentservice.controller;

import com.innowise.paymentservice.dto.CreatePaymentRequest;
import com.innowise.paymentservice.dto.PaymentDto;
import com.innowise.paymentservice.dto.TotalSumResponse;
import com.innowise.paymentservice.model.PaymentStatus;
import com.innowise.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Validated
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;
    
    /**
     * Получение всех платежей.
     * 
     * @return список всех платежей
     */
    @GetMapping("")
    public ResponseEntity<List<PaymentDto>> getAllPayments() {
        log.error("=== PaymentController.getAllPayments() CALLED ===");
        log.error("Method: GET, Path: /api/v1/payments");
        List<PaymentDto> payments = paymentService.getAllPayments();
        log.error("Returning {} payments", payments.size());
        return ResponseEntity.ok(payments);
    }
    
    /**
     * Получение всех платежей (альтернативный эндпоинт для тестирования).
     * 
     * @return список всех платежей
     */
    @GetMapping("/all")
    public ResponseEntity<List<PaymentDto>> getAllPaymentsAlternative() {
        log.error("=== PaymentController.getAllPaymentsAlternative() CALLED ===");
        log.error("Method: GET, Path: /api/v1/payments/all");
        List<PaymentDto> payments = paymentService.getAllPayments();
        log.error("Returning {} payments", payments.size());
        return ResponseEntity.ok(payments);
    }
    
    /**
     * Создание нового платежа.
     * 
     * @param request данные для создания платежа
     * @return созданный платеж
     */
    @PostMapping("")
    public ResponseEntity<PaymentDto> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentDto paymentDto = paymentService.createPayment(request);
        return ResponseEntity.ok(paymentDto);
    }

    /**
     * Получение платежей по ID заказа.
     * 
     * @param orderId ID заказа
     * @return список платежей для указанного заказа
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentDto>> getPaymentsByOrderId(
            @PathVariable 
            @NotBlank(message = "Order ID cannot be blank")
            @Size(min = 1, max = 50, message = "Order ID must be between 1 and 50 characters")
            String orderId) {
        List<PaymentDto> payments = paymentService.getPaymentsByOrderId(orderId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Получение платежей по ID пользователя.
     * 
     * @param userId ID пользователя
     * @return список платежей для указанного пользователя
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentDto>> getPaymentsByUserId(
            @PathVariable 
            @NotBlank(message = "User ID cannot be blank")
            @Size(min = 1, max = 50, message = "User ID must be between 1 and 50 characters")
            String userId) {
        List<PaymentDto> payments = paymentService.getPaymentsByUserId(userId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Получение платежей по статусам.
     * Пример: /api/v1/payments/statuses?statuses=CREATED,SUCCESS,FAILED
     * 
     * @param statuses список статусов для фильтрации
     * @return список платежей с указанными статусами
     */
    @GetMapping("/statuses")
    public ResponseEntity<List<PaymentDto>> getPaymentsByStatuses(
            @RequestParam("statuses") 
            @NotNull(message = "Statuses list cannot be null")
            List<PaymentStatus> statuses
    ) {
        List<PaymentDto> payments = paymentService.getPaymentsByStatuses(statuses);
        return ResponseEntity.ok(payments);
    }
   
    /**
     * Получение общей суммы платежей.
     * 
     * Варианты использования:
     * 1. Без параметров - общая сумма всех платежей: /api/v1/payments/total
     * 2. За период: /api/v1/payments/total?startDate=2025-01-01T00:00:00Z&endDate=2025-12-31T23:59:59Z
     * 3. С фильтром по статусам: /api/v1/payments/total?startDate=2025-01-01T00:00:00Z&endDate=2025-12-31T23:59:59Z&statuses=SUCCESS
     * 4. Только по статусам: /api/v1/payments/total?statuses=SUCCESS
     * 
     * @param startDate опциональная начальная дата периода
     * @param endDate опциональная конечная дата периода
     * @param statuses опциональный список статусов для фильтрации
     * @return общая сумма и количество платежей
     */
    @GetMapping("/total")
    public ResponseEntity<TotalSumResponse> getTotalSum(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            Instant startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            Instant endDate,
            @RequestParam(value = "statuses", required = false) 
            List<PaymentStatus> statuses
    ) {
        TotalSumResponse response;
        
        // Если даты не указаны, возвращаем общую сумму всех платежей
        if (startDate == null && endDate == null) {
            if (statuses != null && !statuses.isEmpty()) {
                response = paymentService.getTotalSumByStatuses(statuses);
            } else {
                response = paymentService.getTotalSum();
            }
        } else if (startDate != null && endDate != null) {
            // Если указаны обе даты, используем период
            if (statuses != null && !statuses.isEmpty()) {
                response = paymentService.getTotalSumByDatePeriodAndStatuses(startDate, endDate, statuses);
            } else {
                response = paymentService.getTotalSumByDatePeriod(startDate, endDate);
            }
        } else {
            // Если указана только одна дата - ошибка
            throw new IllegalArgumentException("Both startDate and endDate must be provided, or neither");
        }
        
        return ResponseEntity.ok(response);
    }
}
