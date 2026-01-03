package com.innowise.paymentservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }

    @Component
    static class BuildInfoLogger implements CommandLineRunner {
        private static final Logger log = LoggerFactory.getLogger(BuildInfoLogger.class);

        @Value("${build.timestamp:unknown}")
        private String buildTimestamp;

        @Value("${build.version:unknown}")
        private String buildVersion;

        @Value("${build.number:unknown}")
        private String buildNumber;

        @Override
        public void run(String... args) {
            // Выводим в System.out для гарантированного отображения
            System.err.println("========================================");
            System.err.println("PAYMENT-SERVICE BUILD INFORMATION");
            System.err.println("Build Version: " + buildVersion);
            System.err.println("Build Timestamp: " + buildTimestamp);
            System.err.println("Build Number: " + buildNumber);
            System.err.println("========================================");
            // Также выводим через logger
            log.error("========================================");
            log.error("PAYMENT-SERVICE BUILD INFORMATION");
            log.error("Build Version: {}", buildVersion);
            log.error("Build Timestamp: {}", buildTimestamp);
            log.error("Build Number: {}", buildNumber);
            log.error("========================================");
        }
    }
}

