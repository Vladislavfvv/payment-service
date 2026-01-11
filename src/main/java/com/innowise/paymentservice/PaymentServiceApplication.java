package com.innowise.paymentservice;

import com.innowise.paymentservice.config.BuildInfo;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Slf4j
@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentServiceApplication.class);
        log.info("========================================");
        log.info("PAYMENT SERVICE MAIN METHOD STARTED");
        log.info("Spring Boot Version: {}", SpringBootVersion.getVersion());
        log.info("========================================");
        SpringApplication.run(PaymentServiceApplication.class, args);
        log.info("========================================");
        log.info("PAYMENT SERVICE MAIN METHOD COMPLETED");
        log.info("========================================");
    }
    
    @Bean
    public CommandLineRunner printBuildInfo(BuildInfo buildInfo) {
        return args -> {
            log.info("========================================");
            log.info("=== BUILD INFORMATION ===");
            log.info("Application Name: {}", buildInfo.getAppName());
            log.info("Application Version: {}", buildInfo.getAppVersion());
            log.info("Build Time: {}", buildInfo.getBuildTime());
            log.info("Java Version: {}", buildInfo.getBuildJavaVersion());
            log.info("Spring Boot Version: {}", buildInfo.getBuildSpringBootVersion());
            log.info("========================================");
            System.out.println("========================================");
            System.out.println("=== BUILD INFORMATION ===");
            System.out.println(buildInfo.getFormattedBuildInfo());
            System.out.println("========================================");
        };
    }
    
    @Bean
    public CommandLineRunner logMappings(ApplicationContext context, BuildInfo buildInfo) {
        return args -> {
            // Force output to System.out and System.err (always visible in Kubernetes logs)
            System.out.println("========================================");
            System.out.println("=== PAYMENT SERVICE STARTUP DIAGNOSTICS ===");
            System.out.println("Spring Boot Version: " + SpringBootVersion.getVersion());
            System.out.println("Build Info: " + buildInfo.getFormattedBuildInfo());
            System.out.println("========================================");
            System.err.println("========================================");
            System.err.println("=== PAYMENT SERVICE STARTUP DIAGNOSTICS (stderr) ===");
            System.err.println("Spring Boot Version: " + SpringBootVersion.getVersion());
            System.err.println("Build Info: " + buildInfo.getFormattedBuildInfo());
            System.err.println("========================================");
            
            // Use log.info for informational messages
            log.info("========================================");
            log.info("=== PAYMENT SERVICE STARTUP DIAGNOSTICS ===");
            log.info("Spring Boot Version: {}", SpringBootVersion.getVersion());
            log.info("Build Info: {}", buildInfo.getFormattedBuildInfo());
            log.info("========================================");
            
            // Also use log.info to ensure visibility
            log.info("========================================");
            log.info("=== PAYMENT SERVICE STARTUP DIAGNOSTICS ===");
            log.info("Spring Boot Version: {}", SpringBootVersion.getVersion());
            log.info("========================================");
            
            // Check if PaymentController bean exists
            try {
                Object controller = context.getBean("paymentController");
                log.error(">>> PaymentController bean found: {}", controller.getClass().getName());
            } catch (Exception e) {
                log.error(">>> ERROR: PaymentController bean NOT FOUND!");
                log.error("    Exception: {}", e.getMessage(), e);
            }
            
            // List all controller beans
            try {
                String[] controllerBeans = context.getBeanNamesForAnnotation(org.springframework.web.bind.annotation.RestController.class);
                log.error(">>> Found {} @RestController beans:", controllerBeans.length);
                for (String beanName : controllerBeans) {
                    log.error("    - {}", beanName);
                }
            } catch (Exception e) {
                log.error(">>> Error listing controllers: {}", e.getMessage(), e);
            }
            
            // Check request mappings
            log.error("========================================");
            log.error("Checking registered request mappings...");
            log.error("========================================");
            
            try {
                RequestMappingHandlerMapping mapping = context.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
                final int[] count = {0};
                final boolean[] foundMy = {false};
                
                log.error("Total mappings: {}", mapping.getHandlerMethods().size());
                
                mapping.getHandlerMethods().forEach((mappingInfo, handlerMethod) -> {
                    String mappingStr = mappingInfo.toString();
                    String methodStr = handlerMethod.toString();
                    log.error("Mapping {}: {}", count[0]++, mappingStr);
                    log.error("    -> {}", methodStr);
                    
                    if (mappingStr.contains("/my-payments") || mappingStr.contains("/my") || methodStr.contains("getMyTotalSum")) {
                        log.error(">>> *** FOUND /my-payments MAPPING! ***");
                        log.error("    Full mapping: {}", mappingStr);
                        log.error("    Handler method: {}", methodStr);
                        foundMy[0] = true;
                    }
                });
                
                if (!foundMy[0]) {
                    log.error(">>> *** WARNING: /my-payments mapping NOT FOUND in registered mappings! ***");
                }
                
            } catch (Exception e) {
                log.error(">>> ERROR getting mappings: {}", e.getMessage(), e);
            }
            
            log.error("========================================");
            log.error("=== END DIAGNOSTICS ===");
            log.error("========================================");
        };
    }
    
    @Bean
    public ApplicationListener<ApplicationReadyEvent> applicationReadyListener(BuildInfo buildInfo) {
        return event -> {
            log.info("========================================");
            log.info("=== PAYMENT SERVICE APPLICATION READY ===");
            log.info("Spring Boot Version: {}", SpringBootVersion.getVersion());
            log.info("Build Info: {}", buildInfo.getFormattedBuildInfo());
            log.info("========================================");
            log.info("========================================");
            log.info("=== PAYMENT SERVICE APPLICATION READY ===");
            log.info("Spring Boot Version: {}", SpringBootVersion.getVersion());
            log.info("Build Info: {}", buildInfo.getFormattedBuildInfo());
            log.info("========================================");
            System.out.println("========================================");
            System.out.println("=== PAYMENT SERVICE APPLICATION READY ===");
            System.out.println("Spring Boot Version: " + SpringBootVersion.getVersion());
            System.out.println("Build Info: " + buildInfo.getFormattedBuildInfo());
            System.out.println("========================================");
        };
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

