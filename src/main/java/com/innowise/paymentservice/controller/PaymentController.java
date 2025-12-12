package com.innowise.paymentservice.controller;

import com.innowise.paymentservice.dto.TotalSumResponse;
import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.model.PaymentStatus;
import com.innowise.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    
    @PostMapping
    public ResponseEntity<Payment> create(@RequestBody Payment payment) {
        return ResponseEntity.ok(paymentService.createPayment(payment));
    }

    
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Payment>> getPaymentsByOrderId(@PathVariable String orderId) {
        List<Payment> payments = paymentService.getPaymentsByOrderId(orderId);
        return ResponseEntity.ok(payments);
    }

   
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Payment>> getPaymentsByUserId(@PathVariable String userId) {
        List<Payment> payments = paymentService.getPaymentsByUserId(userId);
        return ResponseEntity.ok(payments);
    }

        
     //payments/status?statuses=CREATED,PAID,FAILED     
    @GetMapping("/status")
    public ResponseEntity<List<Payment>> getPaymentsByStatuses(
            @RequestParam("statuses") List<PaymentStatus> statuses
    ) {
        List<Payment> payments = paymentService.getPaymentsByStatuses(statuses);
        return ResponseEntity.ok(payments);
    }
   
    
    // Пример: /payments/total?startDate=2025-01-01T00:00:00Z&endDate=2025-12-31T23:59:59Z
    @GetMapping("/total")
    public ResponseEntity<TotalSumResponse> getTotalSumByDatePeriod(
            @RequestParam("startDate") 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            Instant startDate,
            @RequestParam("endDate") 
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
