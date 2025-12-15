package com.innowise.paymentservice.client;

import com.innowise.paymentservice.dto.RandomNumberResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Client for external API to generate random numbers using WebClient
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalApiClient {
    
    private final WebClient webClient;
    
    @Value("${external.api.random-number.url:http://www.randomnumberapi.com/api/v1.0/random?min=1&max=100}")
    private String randomNumberApiUrl;
    
    /**
     * Get random number from external API
     * @return random number, or null if API call fails
     */
    public Integer getRandomNumber() {
        try {
            log.info("Calling external API for random number: {}", randomNumberApiUrl);
            
            // Используем WebClient для асинхронного вызова, затем блокируем для синхронного результата
            RandomNumberResponse[] response = webClient.get()
                    .uri(randomNumberApiUrl)
                    .retrieve()
                    .bodyToMono(RandomNumberResponse[].class)
                    .timeout(Duration.ofSeconds(10))
                    // Обработка HTTP ошибок от внешнего API (4xx, 5xx)
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        HttpStatusCode statusCode = ex.getStatusCode();
                        int statusCodeValue = statusCode.value();
                        String errorMessage = ex.getMessage();
                        
                        // Логируем разные типы ошибок с разным уровнем детализации
                        if (statusCodeValue >= 400 && statusCodeValue < 500) {
                            // Клиентские ошибки (400-499)
                            log.error("Client error calling external API: HTTP {} - {}", 
                                    statusCodeValue, errorMessage);
                        } else if (statusCodeValue >= 500 && statusCodeValue < 600) {
                            // Серверные ошибки (500-599)
                            log.error("Server error calling external API: HTTP {} - {}", 
                                    statusCodeValue, errorMessage);
                        } else {
                            // Другие HTTP ошибки
                            log.error("HTTP error calling external API: HTTP {} - {}", statusCodeValue, errorMessage);
                        }
                        
                        return Mono.empty(); // Возвращаем пустой Mono, чтобы продолжить обработку
                    })
                    // Обработка таймаутов
                    .onErrorResume(java.util.concurrent.TimeoutException.class, ex -> {
                        log.error("Timeout calling external API after 10 seconds: {}", ex.getMessage());
                        return Mono.empty();
                    })
                    // Обработка сетевых ошибок и других исключений
                    .onErrorResume(Exception.class, ex -> {
                        log.error("Unexpected error calling external API: {} - {}", 
                                ex.getClass().getSimpleName(), ex.getMessage(), ex);
                        return Mono.empty();
                    })
                    .block(); // Блокируем для синхронного результата (совместимость с текущим кодом)
            
            if (response != null && response.length > 0 && response[0].getRandom() != null) {
                Integer randomNumber = response[0].getRandom();
                log.info("Received random number from external API: {}", randomNumber);
                return randomNumber;
            } else {
                log.warn("External API returned empty or invalid response");
                return null;
            }
        } catch (Exception e) {
            log.error("Error calling external API for random number", e);
            return null;
        }
    }
}

