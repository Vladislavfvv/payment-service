package com.innowise.paymentservice.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Класс для чтения информации о сборке из build-info.properties
 */
@Slf4j
@Component
@Getter
public class BuildInfo {
    
    private String appVersion;
    private String appName;
    private String buildTime;
    private String buildJavaVersion;
    private String buildSpringBootVersion;
    
    @PostConstruct
    public void loadBuildInfo() {
        try {
            ClassPathResource resource = new ClassPathResource("build-info.properties");
            Properties props = new Properties();
            
            try (InputStream inputStream = resource.getInputStream()) {
                props.load(inputStream);
            }
            
            this.appVersion = props.getProperty("app.version", "unknown");
            this.appName = props.getProperty("app.name", "payment-service");
            this.buildTime = props.getProperty("app.build.time", "unknown");
            this.buildJavaVersion = props.getProperty("app.build.java.version", "unknown");
            this.buildSpringBootVersion = props.getProperty("app.build.spring.boot.version", "unknown");
            
            log.info("Build info loaded: version={}, buildTime={}", appVersion, buildTime);
        } catch (IOException e) {
            log.warn("Failed to load build-info.properties: {}", e.getMessage());
            // Устанавливаем значения по умолчанию
            this.appVersion = "unknown";
            this.appName = "payment-service";
            this.buildTime = "unknown";
            this.buildJavaVersion = "unknown";
            this.buildSpringBootVersion = "unknown";
        }
    }
    
    public String getFormattedBuildInfo() {
        return String.format(
            "Application: %s | Version: %s | Build Time: %s | Java: %s | Spring Boot: %s",
            appName, appVersion, buildTime, buildJavaVersion, buildSpringBootVersion
        );
    }
}

