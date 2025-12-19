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

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Validated
public class PaymentController {

    private final PaymentService paymentService;
    
    /**
     * Создание нового платежа.
     * 
     * @param request данные для создания платежа
     * @return созданный платеж
     */
    @PostMapping
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
     * Получение общей суммы платежей за период.
     * Пример: /api/v1/payments/total?startDate=2025-01-01T00:00:00Z&endDate=2025-12-31T23:59:59Z
     * Пример с фильтром по статусам: /api/v1/payments/total?startDate=2025-01-01T00:00:00Z&endDate=2025-12-31T23:59:59Z&statuses=SUCCESS
     * 
     * @param startDate начальная дата периода
     * @param endDate конечная дата периода
     * @param statuses опциональный список статусов для фильтрации
     * @return общая сумма и количество платежей за период
     */
    @GetMapping("/total")
    public ResponseEntity<TotalSumResponse> getTotalSumByDatePeriod(
            @RequestParam("startDate") 
            @NotNull(message = "Start date is required")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            Instant startDate,
            @RequestParam("endDate") 
            @NotNull(message = "End date is required")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            Instant endDate,
            @RequestParam(value = "statuses", required = false) 
            List<PaymentStatus> statuses
    ) {
        TotalSumResponse response;
        
        if (statuses != null && !statuses.isEmpty()) {
            response = paymentService.getTotalSumByDatePeriodAndStatuses(startDate, endDate, statuses);
        } else {
            response = paymentService.getTotalSumByDatePeriod(startDate, endDate);
        }
        
        return ResponseEntity.ok(response);
    }
}
