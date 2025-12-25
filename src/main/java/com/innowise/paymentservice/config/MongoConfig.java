package com.innowise.paymentservice.config;

import com.innowise.paymentservice.model.Payment;
import com.mongodb.client.model.Indexes;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MongoConfig {

    private final MongoTemplate mongoTemplate;

    @PostConstruct
    public void createIndexes() {
        log.info("Creating indexes for MongoDB collections...");
        
        try {
            String collectionName = mongoTemplate.getCollectionName(Payment.class);
            
            // Создаем индексы используя MongoDB Java Driver API
            // Индексы будут созданы автоматически, если их еще нет
            //класс служит дополнительной гарантией создания индексов при старте приложения
            mongoTemplate.getCollection(collectionName).createIndex(Indexes.ascending("orderId"));
            mongoTemplate.getCollection(collectionName).createIndex(Indexes.ascending("userId"));
            mongoTemplate.getCollection(collectionName).createIndex(Indexes.ascending("status"));
            mongoTemplate.getCollection(collectionName).createIndex(Indexes.descending("timestamp"));
            
            log.info("Indexes created successfully for Payment collection");
        } catch (Exception e) {
            log.error("Error creating indexes", e);
        }
    }
}

