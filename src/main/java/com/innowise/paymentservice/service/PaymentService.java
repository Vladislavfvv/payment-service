package com.innowise.paymentservice.service;

import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PaymentService {
    private final PaymentRepository repo;

    public PaymentService(PaymentRepository repo) {
        this.repo = repo;
    }

    public Payment createPayment(Payment payment) {
        payment.setStatus("CREATED");
        payment.setTimestamp(Instant.now());
        return repo.save(payment);
    }
}
