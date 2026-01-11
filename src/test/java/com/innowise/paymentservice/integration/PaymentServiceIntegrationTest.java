package com.innowise.paymentservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.paymentservice.client.OrderServiceClient;
import com.innowise.paymentservice.dto.CreatePaymentRequest;
import com.innowise.paymentservice.dto.PaymentDto;
import com.innowise.paymentservice.dto.TotalSumResponse;
import com.innowise.paymentservice.model.PaymentStatus;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.MongoDBContainer;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import com.innowise.paymentservice.security.TestSecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Интеграционные тесты для payment-service с использованием Testcontainers (MongoDB, Kafka) и WireMock.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.data.mongodb.auto-index-creation=false", // Отключаем создание индексов
                "spring.liquibase.enabled=false" // Отключаем Liquibase
        })
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
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
        // Ждем, пока Kafka контейнер полностью запустится
        try {
            Thread.sleep(5000); // Даем время на инициализацию Kafka
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static WireMockServer wireMockServer;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MongoTemplate mongoTemplate; // Для очистки базы

    @MockBean
    private OrderServiceClient orderServiceClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // Mongo (добавляем имя базы, иначе Spring Boot выдаёт ошибку "Database name must not be empty")
        //registry.add("spring.data.mongodb.uri", () -> mongoContainer.getConnectionString() + "/paymentdb");
        registry.add("spring.data.mongodb.uri", () -> mongoContainer.getConnectionString() + "/testdb");
        registry.add("spring.data.mongodb.auto-index-creation", () -> "false");
        // Kafka
        registry.add("spring.kafka.bootstrap-servers", () -> kafkaContainer.getBootstrapServers());

//        // Liquibase отключаем в интеграционных тестах
//        registry.add("spring.liquibase.enabled", () -> false);

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

    @BeforeEach
    void setUp() {
        // Очищаем базу данных перед каждым тестом
        if (mongoTemplate != null) {
            mongoTemplate.getDb().drop();
        }
        
        // Настраиваем мок для OrderServiceClient, чтобы не делать реальные HTTP-запросы
        doNothing().when(orderServiceClient).updateOrderStatus(anyLong(), anyString(), anyString());
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
                baseUrl() + "/api/v1/payments",
                HttpMethod.POST,
                entity,
                PaymentDto.class
        );
        // Проверяем успешный ответ
        //assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getStatusCode().value()).isBetween(200, 299);

        PaymentDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isNotBlank();
        assertThat(body.getOrderId()).isEqualTo("it-order-1");
        assertThat(body.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        // Проверяем, что событие попало в Kafka
        // Даем время на отправку события в Kafka перед созданием consumer
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        KafkaConsumer<String, String> consumer = createTestConsumer();
        consumer.subscribe(Collections.singletonList(CREATE_PAYMENT_TOPIC));
        
        // Даем время на присоединение к consumer group
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Увеличиваем таймаут до 30 секунд для CI/CD окружения
        ConsumerRecord<String, String> record = pollSingleRecord(consumer, Duration.ofSeconds(30));

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo(body.getId());

        // Проверяем структуру события (минимально: orderId и status)
        // В Kafka событии отправляется статус заказа, а не статус платежа
        // После создания платежа статус заказа всегда становится CANCELED
        Map<?, ?> eventPayload = objectMapper.readValue(record.value(), Map.class);
        assertThat(eventPayload.get("orderId")).isEqualTo("it-order-1");
        assertThat(((String) eventPayload.get("status")).toUpperCase())
                .isEqualTo("CANCELED"); // Статус заказа после создания платежа

        consumer.close();
    }

    private ConsumerRecord<String, String> pollSingleRecord(KafkaConsumer<String, String> consumer,
                                                            Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            // Увеличиваем интервал poll до 1 секунды для более надежного опроса
            var records = consumer.poll(Duration.ofMillis(1000));
            if (!records.isEmpty()) {
                return records.iterator().next();
            }
            // Небольшая пауза между попытками для снижения нагрузки
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
    }


    @Test
    @DisplayName("Интеграционный тест: подсчет суммы за период")
    void totalSumByDatePeriod_withRealMongo() {
        // Настраиваем WireMock
        stubFor(get("/api/v1.0/random?min=1&max=100")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[24]")));

        // Создаем платежи
        for (int i = 0; i < 2; i++) {
            CreatePaymentRequest request = CreatePaymentRequest.builder()
                    .orderId("sum-order-" + i)
                    .userId("user-sum")
                    .paymentAmount(new BigDecimal("50.00"))
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<PaymentDto> response = restTemplate.exchange(
                    baseUrl() + "/api/v1/payments",
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    PaymentDto.class
            );

            assertThat(response.getStatusCode().value()).isBetween(200, 299);
        }

        Instant start = Instant.now().minus(Duration.ofHours(1));
        Instant end = Instant.now().plus(Duration.ofHours(1));

        ResponseEntity<TotalSumResponse> sumResponse = restTemplate.getForEntity(
                baseUrl() + "/api/v1/payments/total?startDate=" + start + "&endDate=" + end,
                TotalSumResponse.class
        );

        assertThat(sumResponse.getStatusCode().value()).isBetween(200, 299);
        TotalSumResponse body = sumResponse.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getTotalSum()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(body.getPaymentCount()).isEqualTo(2L);
    }


    private KafkaConsumer<String, String> createTestConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-it-consumer-" + System.currentTimeMillis());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Увеличиваем таймауты для более надежной работы в CI/CD
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 40000);
        props.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, 300000);
        return new KafkaConsumer<>(props);
    }
}
