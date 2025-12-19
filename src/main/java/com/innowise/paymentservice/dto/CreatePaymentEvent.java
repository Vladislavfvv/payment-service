package com.innowise.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event DTO for CREATE_PAYMENT event sent to Kafka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatePaymentEvent {   
    private String orderId;    
    private String status;// SUCCESS / FAILED   
}

