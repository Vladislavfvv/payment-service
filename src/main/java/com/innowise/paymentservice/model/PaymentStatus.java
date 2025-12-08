package com.innowise.paymentservice.model;

/**
 * Статусы платежа
 */
public enum PaymentStatus {
    /**
     * Платеж создан, но еще не обработан
     */
    CREATED,
    
    /**
     * Платеж находится в процессе обработки
     */
    PENDING,
    
    /**
     * Платеж успешно оплачен
     */
    PAID,
    
    /**
     * Платеж не прошел (ошибка при обработке)
     */
    FAILED,
    
    /**
     * Платеж отменен
     */
    CANCELLED,
    
    /**
     * Платеж возвращен (refund)
     */
    REFUNDED
}

