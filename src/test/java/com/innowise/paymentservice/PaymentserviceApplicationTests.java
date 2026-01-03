package com.innowise.paymentservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class PaymentServiceApplicationTests {

    static MongoDBContainer mongoContainer;

    static {
        mongoContainer = new MongoDBContainer("mongo:7.0")
                .withStartupTimeout(java.time.Duration.ofSeconds(120));
        mongoContainer.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Используем MongoDB из Testcontainers
        registry.add("spring.data.mongodb.uri", () -> mongoContainer.getConnectionString() + "/paymentdb");
        // Отключаем Liquibase для теста контекста
        registry.add("spring.liquibase.enabled", () -> false);
    }

    @Test
    void contextLoads() {
        // Тест проверяет, что Spring контекст загружается успешно
    }

}

