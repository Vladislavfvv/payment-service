package com.innowise.paymentservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.paymentservice.dto.CreatePaymentRequest;
import com.innowise.paymentservice.dto.PaymentDto;
import com.innowise.paymentservice.dto.TotalSumResponse;
import com.innowise.paymentservice.model.PaymentStatus;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.MongoDBContainer;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для payment-service с использованием Testcontainers (MongoDB, Kafka) и WireMock.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaymentServiceIntegrationTest {

    private static final String CREATE_PAYMENT_TOPIC = "create-payment-events";

    private static final MongoDBContainer mongoContainer =
            new MongoDBContainer("mongo:7.0");

    private static final KafkaContainer kafkaContainer =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    static {
        mongoContainer.start();
        kafkaContainer.start();
    }

    private static WireMockServer wireMockServer;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // Mongo (добавляем имя базы, иначе Spring Boot выдаёт ошибку "Database name must not be empty")
        registry.add("spring.data.mongodb.uri", () -> mongoContainer.getConnectionString() + "/paymentdb");

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", () -> kafkaContainer.getBootstrapServers());

        // Liquibase отключаем в интеграционных тестах
        registry.add("spring.liquibase.enabled", () -> false);

        // Поднимаем WireMock и настраиваем URL внешнего API
        registry.add("external.api.random-number.url", () -> {
            if (wireMockServer == null) {
                wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
                wireMockServer.start();
            }
            configureFor("localhost", wireMockServer.port());
            return String.format("http://localhost:%d/api/v1.0/random?min=1&max=100", wireMockServer.port());
        });
    }

    @BeforeAll
    void setUpWireMock() {
        // Инициализация происходит в overrideProperties через DynamicPropertySource
    }

    @AfterAll
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    @DisplayName("Интеграционный тест: создание платежа с успешным ответом внешнего API и отправкой события в Kafka")
    void createPayment_success_flow_withKafkaEvent() throws Exception {
        // Настраиваем WireMock: внешний API возвращает четное число -> SUCCESS
        stubFor(get("/api/v1.0/random?min=1&max=100")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[42]")));

        // Формируем запрос на создание платежа
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .orderId("it-order-1")
                .userId("user-1")
                .paymentAmount(new BigDecimal("10.50"))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<CreatePaymentRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<PaymentDto> response = restTemplate.exchange(
                baseUrl() + "/payments",
                HttpMethod.POST,
                entity,
                PaymentDto.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        PaymentDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isNotBlank();
        assertThat(body.getOrderId()).isEqualTo("it-order-1");
        assertThat(body.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        // Проверяем, что событие попало в Kafka
        KafkaConsumer<String, String> consumer = createTestConsumer();
        consumer.subscribe(Collections.singletonList(CREATE_PAYMENT_TOPIC));

        ConsumerRecord<String, String> record = pollSingleRecord(consumer, Duration.ofSeconds(10));

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo(body.getId());

        // Проверяем структуру события (минимально: orderId и status)
        Map<?, ?> eventPayload = objectMapper.readValue(record.value(), Map.class);
        assertThat(eventPayload.get("orderId")).isEqualTo("it-order-1");
        assertThat(((String) eventPayload.get("status")).toUpperCase())
                .isEqualTo(PaymentStatus.SUCCESS.name());

        consumer.close();
    }

    private ConsumerRecord<String, String> pollSingleRecord(KafkaConsumer<String, String> consumer,
                                                            Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            var records = consumer.poll(Duration.ofMillis(500));
            if (!records.isEmpty()) {
                return records.iterator().next();
            }
        }
        return null;
    }

    @Test
    @DisplayName("Интеграционный тест: подсчет суммы за период на реальной MongoDB")
    void totalSumByDatePeriod_withRealMongo() {
        // Для простоты: создаем несколько платежей через REST, чтобы они попали в Mongo
        stubFor(get("/api/v1.0/random?min=1&max=100")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[24]")));

        for (int i = 0; i < 2; i++) {
            CreatePaymentRequest request = CreatePaymentRequest.builder()
                    .orderId("sum-order-" + i)
                    .userId("user-sum")
                    .paymentAmount(new BigDecimal("50.00"))
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.exchange(
                    baseUrl() + "/payments",
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    PaymentDto.class
            );
        }

        Instant start = Instant.now().minus(Duration.ofHours(1));
        Instant end = Instant.now().plus(Duration.ofHours(1));

        ResponseEntity<TotalSumResponse> sumResponse = restTemplate.getForEntity(
                baseUrl() + "/payments/total?startDate=" + start + "&endDate=" + end,
                TotalSumResponse.class
        );

        assertThat(sumResponse.getStatusCode().is2xxSuccessful()).isTrue();
        TotalSumResponse body = sumResponse.getBody();
        assertThat(body).isNotNull();
        // В сумме учитываются платежи из обоих тестов: 10.50 + 50.00 + 50.00 = 110.50
        assertThat(body.getTotalSum()).isEqualByComparingTo(new BigDecimal("110.50"));
        assertThat(body.getPaymentCount()).isGreaterThanOrEqualTo(3L);
    }

    private KafkaConsumer<String, String> createTestConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-it-consumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(props);
    }
}


