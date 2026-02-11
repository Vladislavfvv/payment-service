package com.innowise.paymentservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Configuration for WebClient to call external APIs
 */
@Configuration
@Slf4j
public class WebClientConfig {
    
    /**
     * Builder with logging filters for all WebClient calls.
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                // Логируем исходящие HTTP-запросы
                .filter(ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
                    log.info("WebClient request: {} {}", clientRequest.method(), clientRequest.url());
                    clientRequest.headers().forEach((name, values) ->
                            values.forEach(value ->
                                    log.debug("WebClient request header: {}={}", name, value)));
                    return Mono.just(clientRequest);
                }))
                // Логируем ответы от внешних сервисов (без тела)
                .filter(ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
                    log.info("WebClient response: HTTP {}", clientResponse.statusCode());
                    clientResponse.headers().asHttpHeaders().forEach((name, values) ->
                            values.forEach(value ->
                                    log.debug("WebClient response header: {}={}", name, value)));
                    return Mono.just(clientResponse);
                }));
    }
    
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }
}
