package com.innowise.paymentservice.model;

/**
 * Payment statuses
 */
public enum PaymentStatus {
    /**
     * Payment created but not yet processed
     */
    CREATED,
    
    /**
     * Payment is being processed
     */
    PENDING,
    
    /**
     * Payment successfully paid
     */
    PAID,
    
    /**
     * Payment failed (error during processing)
     */
    FAILED,
    
    /**
     * Payment cancelled
     */
    CANCELLED,
    
    /**
     * Payment refunded
     */
    REFUNDED
}

