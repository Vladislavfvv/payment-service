package com.innowise.paymentservice.repository;

import com.innowise.paymentservice.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    List<Payment> findByUserId(String userId);

    List<Payment> findByOrderId(String orderId);

    List<Payment> findByStatus(String status);
}
