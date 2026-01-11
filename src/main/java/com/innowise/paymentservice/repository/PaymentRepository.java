package com.innowise.paymentservice.repository;

import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.model.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface PaymentRepository extends MongoRepository<Payment, String> {    
    List<Payment> findByUserId(String userId);
    
    List<Payment> findByOrderId(String orderId);

    List<Payment> findByStatus(PaymentStatus status);
    
    List<Payment> findByStatusIn(List<PaymentStatus> statuses);
    
    List<Payment> findByTimestampBetween(Instant startDate, Instant endDate);
    //Get total sum of payments for date period
    List<Payment> findByStatusInAndTimestampBetween(
        List<PaymentStatus> statuses, 
        Instant startDate, 
        Instant endDate
    );
    
    // Методы для поиска платежей по userId
    List<Payment> findByUserIdAndStatusIn(String userId, List<PaymentStatus> statuses);
    
    List<Payment> findByUserIdAndTimestampBetween(String userId, Instant startDate, Instant endDate);
    
    List<Payment> findByUserIdAndStatusInAndTimestampBetween(
        String userId,
        List<PaymentStatus> statuses, 
        Instant startDate, 
        Instant endDate
    );
}
