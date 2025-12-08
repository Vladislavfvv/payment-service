package com.innowise.paymentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "payments")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Payment {
    @Id
    private String id;

    @Indexed
    private String orderId;
    
    @Indexed
    private String userId;

    @Indexed
    private PaymentStatus status;

    @Indexed
    private Instant timestamp;

    private BigDecimal paymentAmount;
}
