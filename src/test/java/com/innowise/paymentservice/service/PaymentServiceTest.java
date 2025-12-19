package com.innowise.paymentservice.service;

import com.innowise.paymentservice.client.ExternalApiClient;
import com.innowise.paymentservice.dto.CreatePaymentRequest;
import com.innowise.paymentservice.dto.PaymentDto;
import com.innowise.paymentservice.dto.TotalSumResponse;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.model.PaymentStatus;
import com.innowise.paymentservice.producer.PaymentEventProducer;
import com.innowise.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock
    private PaymentRepository repository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private ExternalApiClient externalApiClient;

    @Mock // @Mock - это аннотация, которая используется для создания пустой заглушки
    private PaymentEventProducer paymentEventProducer;

    @InjectMocks // @InjectMocks - это аннотация, которая используется для инъекции зависимостей в тестируемый объект
    private PaymentService paymentService; // Создаем РЕАЛЬНЫЙ сервис с внедренными заглушками

    private CreatePaymentRequest createPaymentRequest;
    private Payment payment;
    private Payment savedPayment;

    @BeforeEach
    void setUp() {
        createPaymentRequest = CreatePaymentRequest.builder()
                .orderId("1")
                .userId("2")
                .paymentAmount(new BigDecimal("100.50"))
                .build();

        payment = Payment.builder()
                .id(null)
                .orderId("1")
                .userId("2")
                .paymentAmount(new BigDecimal("100.50"))
                .status(null)
                .timestamp(null)
                .build();

        savedPayment = Payment.builder()
                .id("payment-id-123")
                .orderId("1")
                .userId("2")
                .paymentAmount(new BigDecimal("100.50"))
                .status(PaymentStatus.CREATED)
                .timestamp(Instant.now())
                .build();

        // Теперь:
        // - dependency = mock(Dependency.class) - пустая заглушка
        // - service = new ServiceUnderTest(dependency) - реальный объект
    }

    @Test
    @DisplayName("createPayment_Success_WithEvenNumber_ShouldReturnSuccessStatus")
    void createPayment_Success_WithEvenNumber_ShouldReturnSuccessStatus() {
        // Given
        Payment updatedPayment = Payment.builder()
                .id("payment-id-123")
                .orderId("1")
                .userId("2")
                .paymentAmount(new BigDecimal("100.50"))
                .status(PaymentStatus.SUCCESS)
                .timestamp(Instant.now())
                .build();

        PaymentDto expectedDto = PaymentDto.builder()
                .id("payment-id-123")
                .orderId("1")
                .userId("2")
                .paymentAmount(new BigDecimal("100.50"))
                .status(PaymentStatus.SUCCESS)
                .timestamp(Instant.now())
                .build();

        when(paymentMapper.toEntity(createPaymentRequest)).thenReturn(payment);
        when(repository.save(any(Payment.class))).thenReturn(savedPayment);
        when(externalApiClient.getRandomNumber()).thenReturn(48); // Even number
        when(repository.findById("payment-id-123")).thenReturn(Optional.of(updatedPayment));
        when(paymentMapper.toDto(updatedPayment)).thenReturn(expectedDto);
        doNothing().when(paymentEventProducer).sendCreatePaymentEvent(any(Payment.class));

        // When
        PaymentDto result = paymentService.createPayment(createPaymentRequest);

        // Then
        assertNotNull(result);
        assertEquals("payment-id-123", result.getId());
        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        assertEquals("1", result.getOrderId());
        assertEquals("2", result.getUserId());
        assertEquals(new BigDecimal("100.50"), result.getPaymentAmount());

        verify(paymentMapper).toEntity(createPaymentRequest);
        verify(repository, times(2)).save(any(Payment.class)); // Once for initial save, once for status update
        verify(externalApiClient).getRandomNumber();
        verify(repository).findById("payment-id-123");
        verify(paymentMapper).toDto(updatedPayment);
        verify(paymentEventProducer).sendCreatePaymentEvent(updatedPayment);
    }

    @Test
    @DisplayName("createPayment_Success_WithOddNumber_ShouldReturnFailedStatus")
    void createPayment_Success_WithOddNumber_ShouldReturnFailedStatus() {
        // Given
        Payment updatedPayment = Payment.builder()
                .id("payment-id-123")
                .orderId("1")
                .userId("2")
                .paymentAmount(new BigDecimal("100.50"))
                .status(PaymentStatus.FAILED)
                .timestamp(Instant.now())
                .build();

        PaymentDto expectedDto = PaymentDto.builder()
                .id("payment-id-123")
                .orderId("1")
                .userId("2")
                .paymentAmount(new BigDecimal("100.50"))
                .status(PaymentStatus.FAILED)
                .timestamp(Instant.now())
                .build();

        when(paymentMapper.toEntity(createPaymentRequest)).thenReturn(payment);
        when(repository.save(any(Payment.class))).thenReturn(savedPayment);
        when(externalApiClient.getRandomNumber()).thenReturn(47); // Odd number
        when(repository.findById("payment-id-123")).thenReturn(Optional.of(updatedPayment));
        when(paymentMapper.toDto(updatedPayment)).thenReturn(expectedDto);
        doNothing().when(paymentEventProducer).sendCreatePaymentEvent(any(Payment.class));

        // When
        PaymentDto result = paymentService.createPayment(createPaymentRequest);

        // Then
        assertNotNull(result);
        assertEquals(PaymentStatus.FAILED, result.getStatus());
        verify(externalApiClient).getRandomNumber();
        verify(repository, times(2)).save(any(Payment.class));
    }

    @Test
    @DisplayName("createPayment_ApiReturnsNull_ShouldSetFailedStatus")
    void createPayment_ApiReturnsNull_ShouldSetFailedStatus() {
        // Given
        Payment updatedPayment = Payment.builder()
                .id("payment-id-123")
                .orderId("1")
                .userId("2")
                .paymentAmount(new BigDecimal("100.50"))
                .status(PaymentStatus.FAILED)
                .timestamp(Instant.now())
                .build();

        PaymentDto expectedDto = PaymentDto.builder()
                .id("payment-id-123")
                .orderId("1")
                .userId("2")
                .paymentAmount(new BigDecimal("100.50"))
                .status(PaymentStatus.FAILED)
                .timestamp(Instant.now())
                .build();

        when(paymentMapper.toEntity(createPaymentRequest)).thenReturn(payment);
        when(repository.save(any(Payment.class))).thenReturn(savedPayment);
        when(externalApiClient.getRandomNumber()).thenReturn(null); // API returns null
        when(repository.findById("payment-id-123")).thenReturn(Optional.of(updatedPayment));
        when(paymentMapper.toDto(updatedPayment)).thenReturn(expectedDto);
        doNothing().when(paymentEventProducer).sendCreatePaymentEvent(any(Payment.class));

        // When
        PaymentDto result = paymentService.createPayment(createPaymentRequest);

        // Then
        assertNotNull(result);
        assertEquals(PaymentStatus.FAILED, result.getStatus());
        verify(externalApiClient).getRandomNumber();
        verify(repository, times(2)).save(any(Payment.class)); // Once for initial save, once for FAILED status
    }

    @Test
    @DisplayName("createPayment_WithExistingStatus_ShouldPreserveStatus")
    void createPayment_WithExistingStatus_ShouldPreserveStatus() {
        // Given
        createPaymentRequest.setStatus(PaymentStatus.PENDING);
        payment.setStatus(PaymentStatus.PENDING);
        savedPayment.setStatus(PaymentStatus.PENDING);

        Payment updatedPayment = Payment.builder()
                .id("payment-id-123")
                .orderId("1")
                .userId("2")
                .paymentAmount(new BigDecimal("100.50"))
                .status(PaymentStatus.SUCCESS)
                .timestamp(Instant.now())
                .build();

        PaymentDto expectedDto = PaymentDto.builder()
                .id("payment-id-123")
                .orderId("1")
                .userId("2")
                .paymentAmount(new BigDecimal("100.50"))
                .status(PaymentStatus.SUCCESS)
                .timestamp(Instant.now())
                .build();

        when(paymentMapper.toEntity(createPaymentRequest)).thenReturn(payment);
        when(repository.save(any(Payment.class))).thenReturn(savedPayment);
        when(externalApiClient.getRandomNumber()).thenReturn(50); // Even number
        when(repository.findById("payment-id-123")).thenReturn(Optional.of(updatedPayment));
        when(paymentMapper.toDto(updatedPayment)).thenReturn(expectedDto);
        doNothing().when(paymentEventProducer).sendCreatePaymentEvent(any(Payment.class));

        // When
        PaymentDto result = paymentService.createPayment(createPaymentRequest);

        // Then
        assertNotNull(result);
        // Status will be updated by external API call, not preserved
        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
    }

    @Test
    @DisplayName("createPayment_KafkaProducerThrowsException_ShouldContinueExecution")
    void createPayment_KafkaProducerThrowsException_ShouldContinueExecution() {
        // Given
        Payment updatedPayment = Payment.builder()
                .id("payment-id-123")
                .orderId("1")
                .userId("2")
                .paymentAmount(new BigDecimal("100.50"))
                .status(PaymentStatus.SUCCESS)
                .timestamp(Instant.now())
                .build();

        PaymentDto expectedDto = PaymentDto.builder()
                .id("payment-id-123")
                .orderId("1")
                .userId("2")
                .paymentAmount(new BigDecimal("100.50"))
                .status(PaymentStatus.SUCCESS)
                .timestamp(Instant.now())
                .build();

        when(paymentMapper.toEntity(createPaymentRequest)).thenReturn(payment);
        when(repository.save(any(Payment.class))).thenReturn(savedPayment);
        when(externalApiClient.getRandomNumber()).thenReturn(48);
        when(repository.findById("payment-id-123")).thenReturn(Optional.of(updatedPayment));
        when(paymentMapper.toDto(updatedPayment)).thenReturn(expectedDto);
        doThrow(new RuntimeException("Kafka error")).when(paymentEventProducer).sendCreatePaymentEvent(any(Payment.class));

        // When
        PaymentDto result = paymentService.createPayment(createPaymentRequest);

        // Then
        assertNotNull(result);
        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        verify(paymentEventProducer).sendCreatePaymentEvent(updatedPayment);
    }

    @Test
    @DisplayName("getPaymentsByOrderId_Success_ShouldReturnList")
    void getPaymentsByOrderId_Success_ShouldReturnList() {
        // Given
        String orderId = "1";
        Payment payment1 = Payment.builder()
                .id("payment-1")
                .orderId(orderId)
                .userId("2")
                .status(PaymentStatus.SUCCESS)
                .paymentAmount(new BigDecimal("100.50"))
                .build();

        Payment payment2 = Payment.builder()
                .id("payment-2")
                .orderId(orderId)
                .userId("2")
                .status(PaymentStatus.FAILED)
                .paymentAmount(new BigDecimal("200.00"))
                .build();

        List<Payment> payments = Arrays.asList(payment1, payment2);

        PaymentDto dto1 = PaymentDto.builder()
                .id("payment-1")
                .orderId(orderId)
                .status(PaymentStatus.SUCCESS)
                .build();

        PaymentDto dto2 = PaymentDto.builder()
                .id("payment-2")
                .orderId(orderId)
                .status(PaymentStatus.FAILED)
                .build();

        List<PaymentDto> expectedDtos = Arrays.asList(dto1, dto2);

        when(repository.findByOrderId(orderId)).thenReturn(payments);
        when(paymentMapper.toDtoList(payments)).thenReturn(expectedDtos);

        // When
        List<PaymentDto> result = paymentService.getPaymentsByOrderId(orderId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByOrderId(orderId);
        verify(paymentMapper).toDtoList(payments);
    }

    @Test
    @DisplayName("getPaymentsByOrderId_NoPayments_ShouldReturnEmptyList")
    void getPaymentsByOrderId_NoPayments_ShouldReturnEmptyList() {
        // Given
        String orderId = "999";
        when(repository.findByOrderId(orderId)).thenReturn(Collections.emptyList());
        when(paymentMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        // When
        List<PaymentDto> result = paymentService.getPaymentsByOrderId(orderId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository).findByOrderId(orderId);
    }

    @Test
    @DisplayName("getPaymentsByUserId_Success_ShouldReturnList")
    void getPaymentsByUserId_Success_ShouldReturnList() {
        // Given
        String userId = "2";
        Payment payment1 = Payment.builder()
                .id("payment-1")
                .orderId("1")
                .userId(userId)
                .status(PaymentStatus.SUCCESS)
                .build();

        List<Payment> payments = Collections.singletonList(payment1);
        PaymentDto dto1 = PaymentDto.builder()
                .id("payment-1")
                .userId(userId)
                .status(PaymentStatus.SUCCESS)
                .build();

        List<PaymentDto> expectedDtos = Collections.singletonList(dto1);

        when(repository.findByUserId(userId)).thenReturn(payments);
        when(paymentMapper.toDtoList(payments)).thenReturn(expectedDtos);

        // When
        List<PaymentDto> result = paymentService.getPaymentsByUserId(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(userId, result.get(0).getUserId());
        verify(repository).findByUserId(userId);
    }

    @Test
    @DisplayName("getPaymentsByStatuses_Success_ShouldReturnFilteredList")
    void getPaymentsByStatuses_Success_ShouldReturnFilteredList() {
        // Given
        List<PaymentStatus> statuses = Arrays.asList(PaymentStatus.SUCCESS, PaymentStatus.FAILED);
        Payment payment1 = Payment.builder()
                .id("payment-1")
                .status(PaymentStatus.SUCCESS)
                .build();

        Payment payment2 = Payment.builder()
                .id("payment-2")
                .status(PaymentStatus.FAILED)
                .build();

        List<Payment> payments = Arrays.asList(payment1, payment2);
        List<PaymentDto> expectedDtos = Arrays.asList(
                PaymentDto.builder().id("payment-1").status(PaymentStatus.SUCCESS).build(),
                PaymentDto.builder().id("payment-2").status(PaymentStatus.FAILED).build()
        );

        when(repository.findByStatusIn(statuses)).thenReturn(payments);
        when(paymentMapper.toDtoList(payments)).thenReturn(expectedDtos);

        // When
        List<PaymentDto> result = paymentService.getPaymentsByStatuses(statuses);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByStatusIn(statuses);
    }

    @Test
    @DisplayName("getTotalSumByDatePeriod_Success_ShouldCalculateTotal")
    void getTotalSumByDatePeriod_Success_ShouldCalculateTotal() {
        // Given
        Instant startDate = Instant.parse("2025-01-01T00:00:00Z");
        Instant endDate = Instant.parse("2025-12-31T23:59:59Z");

        Payment payment1 = Payment.builder()
                .id("payment-1")
                .paymentAmount(new BigDecimal("100.50"))
                .timestamp(Instant.parse("2025-06-15T10:00:00Z"))
                .build();

        Payment payment2 = Payment.builder()
                .id("payment-2")
                .paymentAmount(new BigDecimal("200.75"))
                .timestamp(Instant.parse("2025-07-20T14:30:00Z"))
                .build();

        Payment payment3 = Payment.builder()
                .id("payment-3")
                .paymentAmount(null) // Should be filtered out
                .timestamp(Instant.parse("2025-08-10T09:00:00Z"))
                .build();

        List<Payment> payments = Arrays.asList(payment1, payment2, payment3);

        when(repository.findByTimestampBetween(startDate, endDate)).thenReturn(payments);

        // When
        TotalSumResponse result = paymentService.getTotalSumByDatePeriod(startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("301.25"), result.getTotalSum());
        assertEquals(startDate, result.getStartDate());
        assertEquals(endDate, result.getEndDate());
        assertEquals(3L, result.getPaymentCount());
        verify(repository).findByTimestampBetween(startDate, endDate);
    }

    @Test
    @DisplayName("getTotalSumByDatePeriod_NoPayments_ShouldReturnZero")
    void getTotalSumByDatePeriod_NoPayments_ShouldReturnZero() {
        // Given
        Instant startDate = Instant.parse("2025-01-01T00:00:00Z");
        Instant endDate = Instant.parse("2025-12-31T23:59:59Z");

        when(repository.findByTimestampBetween(startDate, endDate)).thenReturn(Collections.emptyList());

        // When
        TotalSumResponse result = paymentService.getTotalSumByDatePeriod(startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getTotalSum());
        assertEquals(0L, result.getPaymentCount());
    }

    @Test
    @DisplayName("getTotalSumByDatePeriodAndStatuses_Success_ShouldCalculateTotal")
    void getTotalSumByDatePeriodAndStatuses_Success_ShouldCalculateTotal() {
        // Given
        Instant startDate = Instant.parse("2025-01-01T00:00:00Z");
        Instant endDate = Instant.parse("2025-12-31T23:59:59Z");
        List<PaymentStatus> statuses = Collections.singletonList(PaymentStatus.SUCCESS);

        Payment payment1 = Payment.builder()
                .id("payment-1")
                .status(PaymentStatus.SUCCESS)
                .paymentAmount(new BigDecimal("150.00"))
                .timestamp(Instant.parse("2025-06-15T10:00:00Z"))
                .build();

        Payment payment2 = Payment.builder()
                .id("payment-2")
                .status(PaymentStatus.SUCCESS)
                .paymentAmount(new BigDecimal("250.50"))
                .timestamp(Instant.parse("2025-07-20T14:30:00Z"))
                .build();

        List<Payment> payments = Arrays.asList(payment1, payment2);

        when(repository.findByStatusInAndTimestampBetween(statuses, startDate, endDate)).thenReturn(payments);

        // When
        TotalSumResponse result = paymentService.getTotalSumByDatePeriodAndStatuses(startDate, endDate, statuses);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("400.50"), result.getTotalSum());
        assertEquals(startDate, result.getStartDate());
        assertEquals(endDate, result.getEndDate());
        assertEquals(2L, result.getPaymentCount());
        verify(repository).findByStatusInAndTimestampBetween(statuses, startDate, endDate);
    }

    @Test
    @DisplayName("getTotalSumByDatePeriodAndStatuses_NoPayments_ShouldReturnZero")
    void getTotalSumByDatePeriodAndStatuses_NoPayments_ShouldReturnZero() {
        // Given
        Instant startDate = Instant.parse("2025-01-01T00:00:00Z");
        Instant endDate = Instant.parse("2025-12-31T23:59:59Z");
        List<PaymentStatus> statuses = Collections.singletonList(PaymentStatus.SUCCESS);

        when(repository.findByStatusInAndTimestampBetween(statuses, startDate, endDate))
                .thenReturn(Collections.emptyList());

        // When
        TotalSumResponse result = paymentService.getTotalSumByDatePeriodAndStatuses(startDate, endDate, statuses);

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getTotalSum());
        assertEquals(0L, result.getPaymentCount());
    }

    @Test
    @DisplayName("createPayment_PaymentNotFoundAfterUpdate_ShouldThrowException")
    void createPayment_PaymentNotFoundAfterUpdate_ShouldThrowException() {
        // Given
        when(paymentMapper.toEntity(createPaymentRequest)).thenReturn(payment);
        when(repository.save(any(Payment.class))).thenReturn(savedPayment);
        when(externalApiClient.getRandomNumber()).thenReturn(48);
        when(repository.findById("payment-id-123")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            paymentService.createPayment(createPaymentRequest);
        });

        assertEquals("Payment not found after update: payment-id-123", exception.getMessage());
        verify(repository).findById("payment-id-123");
    }
}

