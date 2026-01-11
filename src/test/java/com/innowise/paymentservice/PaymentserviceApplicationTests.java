package com.innowise.paymentservice;

import com.innowise.paymentservice.security.TestSecurityConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Testcontainers
class PaymentServiceApplicationTests {

    private static final MongoDBContainer mongoContainer = new MongoDBContainer("mongo:7.0");

    static {
        mongoContainer.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // Используем Testcontainers для MongoDB
        registry.add("spring.data.mongodb.uri", () -> mongoContainer.getConnectionString() + "/testdb");
        registry.add("spring.data.mongodb.auto-index-creation", () -> "false");
        registry.add("spring.liquibase.enabled", () -> "false");
    }

    @Test
    void contextLoads() {
        // Проверяем, что контекст загружается с MongoDB
        Assertions.assertTrue(mongoContainer.isRunning(), "MongoDB container should be running");
    }

    @Test
    void testMongoConnection(@Autowired MongoTemplate mongoTemplate) {
        Assertions.assertNotNull(mongoTemplate);
        // Простая проверка работы MongoDB
        mongoTemplate.createCollection("test");
        Assertions.assertTrue(mongoTemplate.collectionExists("test"));
    }

}

