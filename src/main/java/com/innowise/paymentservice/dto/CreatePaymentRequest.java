package com.innowise.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.innowise.paymentservice.model.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new payment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatePaymentRequest {
    @NotBlank(message = "Order ID is required")
    @Size(min = 1, max = 50, message = "Order ID must be between 1 and 50 characters")
    private String orderId;
    
    @NotBlank(message = "User ID is required")
    @Size(min = 1, max = 50, message = "User ID must be between 1 and 50 characters")
    private String userId;
    
    @NotNull(message = "Payment amount is required")
    @Positive(message = "Payment amount must be positive")
    @DecimalMin(value = "0.01", message = "Payment amount must be at least 0.01")
    private BigDecimal paymentAmount;
    
    private PaymentStatus status;
}

