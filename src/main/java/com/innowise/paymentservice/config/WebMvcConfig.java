package com.innowise.paymentservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Конфигурация для логирования всех зарегистрированных маппингов и входящих запросов.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestLoggingInterceptor());
    }
}

@Component
class RequestLoggingInterceptor implements HandlerInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.error("=== INCOMING REQUEST ===");
        log.error("Method: {}", request.getMethod());
        log.error("URI: {}", request.getRequestURI());
        log.error("Path: {}", request.getPathInfo());
        log.error("Query: {}", request.getQueryString());
        log.error("Handler: {}", handler);
        log.error("Handler class: {}", handler != null ? handler.getClass().getName() : "null");
        log.error("========================");
        return true;
    }
}

@Component
class RequestMappingLogger implements ApplicationListener<ContextRefreshedEvent> {
    
    private static final Logger log = LoggerFactory.getLogger(RequestMappingLogger.class);
    
    private final RequestMappingHandlerMapping handlerMapping;
    
    public RequestMappingLogger(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }
    
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        System.err.println("========================================");
        System.err.println("REQUEST MAPPING LOGGER CALLED");
        System.err.println("========================================");
        log.error("========================================");
        log.error("REQUEST MAPPING LOGGER CALLED");
        log.error("========================================");
        
        if (handlerMapping != null) {
            System.err.println("=== REGISTERED REQUEST MAPPINGS ===");
            log.error("=== REGISTERED REQUEST MAPPINGS ===");
            int count = 0;
            for (var entry : handlerMapping.getHandlerMethods().entrySet()) {
                String mappingStr = entry.getKey().toString();
                String methodStr = entry.getValue().toString();
                System.err.println("Mapping " + count + ": " + mappingStr + " -> " + methodStr);
                log.error("Mapping {}: {} -> {}", count, mappingStr, methodStr);
                count++;
                
                // Особое внимание к payment endpoints
                if (mappingStr.contains("payments") || methodStr.contains("PaymentController")) {
                    System.err.println(">>> PAYMENT MAPPING FOUND: " + mappingStr);
                    log.error(">>> PAYMENT MAPPING FOUND: {}", mappingStr);
                }
            }
            System.err.println("=== TOTAL MAPPINGS: " + count + " ===");
            log.error("=== TOTAL MAPPINGS: {} ===", count);
            System.err.println("=== END OF MAPPINGS ===");
            log.error("=== END OF MAPPINGS ===");
        } else {
            System.err.println("ERROR: RequestMappingHandlerMapping is NULL!");
            log.error("ERROR: RequestMappingHandlerMapping is NULL!");
        }
        System.err.println("========================================");
        log.error("========================================");
    }
}

