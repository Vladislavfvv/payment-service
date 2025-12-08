package com.innowise.paymentservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.domain.Sort;

import com.innowise.paymentservice.model.Payment;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MongoConfig {

    private final MongoTemplate mongoTemplate;

    @PostConstruct
    public void createIndexes() {
        log.info("Creating indexes for MongoDB collections...");
        
        try {
            IndexOperations indexOps = mongoTemplate.indexOps(Payment.class);
            
            // Создаем индексы вручную
            indexOps.ensureIndex(new Index().on("orderId", Sort.Direction.ASC).named("idx_order_id"));
            indexOps.ensureIndex(new Index().on("userId", Sort.Direction.ASC).named("idx_user_id"));
            indexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC).named("idx_status"));
            indexOps.ensureIndex(new Index().on("timestamp", Sort.Direction.DESC).named("idx_timestamp"));
            
            log.info("Indexes created successfully for Payment collection");
        } catch (Exception e) {
            log.error("Error creating indexes", e);
        }
    }
}

