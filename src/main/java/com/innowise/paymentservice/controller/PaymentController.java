package com.innowise.paymentservice.controller;

import com.innowise.paymentservice.client.UserServiceClient;
import com.innowise.paymentservice.dto.CreatePaymentRequest;
import com.innowise.paymentservice.dto.PaymentDto;
import com.innowise.paymentservice.dto.TotalSumResponse;
import com.innowise.paymentservice.dto.UserDto;
import com.innowise.paymentservice.model.PaymentStatus;
import com.innowise.paymentservice.service.PaymentService;
import com.innowise.paymentservice.util.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@Validated
public class PaymentController {

    
    private final PaymentService paymentService;
    private final UserServiceClient userServiceClient;
    
    @Autowired
    public PaymentController(PaymentService paymentService, UserServiceClient userServiceClient) {
        this.paymentService = paymentService;
        this.userServiceClient = userServiceClient;
        log.info("========================================");
        log.debug("PaymentController CONSTRUCTOR CALLED!");
        log.info("Class: {}", this.getClass().getName());
        log.info("========================================");
    }
    
    @jakarta.annotation.PostConstruct
    public void init() {
        log.error("========================================");
        log.error("PaymentController INITIALIZED!");
        log.error("Class: {}", this.getClass().getName());
        log.error("Methods: getMyTotalSum, createPayment, getPaymentsByOrderId, getPaymentsByUserId, getPaymentsByStatuses, getTotalSumByDatePeriod");
        log.error("========================================");
    }
    
    /**
     * Получение общей суммы платежей текущего пользователя.
     * Доступно только самому пользователю (userId извлекается из JWT токена).
     * 
     * Варианты использования:
     * 1. Без параметров - общая сумма всех платежей пользователя: /api/v1/payments/my-payments
     * 2. За период: /api/v1/payments/my-payments?startDate=2025-01-01T00:00:00Z&endDate=2025-12-31T23:59:59Z
     * 3. С фильтром по статусам: /api/v1/payments/my-payments?startDate=2025-01-01T00:00:00Z&endDate=2025-12-31T23:59:59Z&statuses=SUCCESS
     * 4. Только по статусам: /api/v1/payments/my-payments?statuses=SUCCESS
     * 
     * @param startDate опциональная начальная дата периода
     * @param endDate опциональная конечная дата периода
     * @param statuses опциональный список статусов для фильтрации
     * @return общая сумма и количество платежей текущего пользователя
     */
    @GetMapping("/my-payments")
    public ResponseEntity<TotalSumResponse> getMyTotalSum(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            Instant startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            Instant endDate,
            @RequestParam(value = "statuses", required = false) 
            List<PaymentStatus> statuses
    ) {
        log.error("========================================");
        log.error("PaymentController.getMyTotalSum() CALLED!");
        log.error("Method: GET, Path: /api/v1/payments/my-payments");
        log.error("Parameters: startDate={}, endDate={}, statuses={}", startDate, endDate, statuses);
        log.error("========================================");
        
        // Получаем Authentication из SecurityContextHolder
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.error("=== AUTHENTICATION FROM REQUEST ===");
        log.error("Authentication object: {}", authentication);
        log.error("Authentication class: {}", authentication != null ? authentication.getClass().getName() : "null");
        
        // Извлекаем email из JWT токена
        String email = SecurityUtils.getEmailFromToken(authentication);
        log.error("=== EMAIL EXTRACTED FROM TOKEN ===");
        log.error("Extracted email: {}", email);
        
        // Получаем токен для передачи в user-service
        String authToken = SecurityUtils.getTokenString(authentication);
        
        // Проверяем, существует ли пользователь с таким email в user-service
        log.error("=== CALLING USER-SERVICE ===");
        log.error("Checking if user exists with email: {}", email);
        
        UserDto userDto;
        try {
            userDto = userServiceClient.getUserByEmail(email, authToken);
            log.error("User found in user-service: id={}, email={}", userDto.getId(), userDto.getEmail());
        } catch (UserServiceClient.UserServiceException e) {
            log.error("User not found in user-service for email: {}", email, e);
            return ResponseEntity.notFound().build();
        }
        
        // Используем числовой ID пользователя для поиска платежей
        String userId = String.valueOf(userDto.getId());
        log.error("=== USER ID RESOLVED ===");
        log.error("Using userId: {} (from user-service)", userId);
        log.error("=========================");
        
        // Вызываем сервис для расчета общей суммы платежей пользователя
        log.error("=== CALLING PAYMENT SERVICE ===");
        log.error("Searching for payments with userId: {}", userId);
        log.error("StartDate: {}, EndDate: {}, Statuses: {}", startDate, endDate, statuses);
        
        TotalSumResponse response = paymentService.getTotalSumByUserId(userId, startDate, endDate, statuses);
        
        log.error("=== SERVICE RESPONSE ===");
        log.error("TotalSum: {}, PaymentCount: {}", response.getTotalSum(), response.getPaymentCount());
        log.error("=========================");
        
        return ResponseEntity.ok(response);
    }
    
    // Логирование при создании контроллера
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
        // Логируем сразу в конструкторе
        System.err.println("========================================");
        System.err.println("PaymentController CONSTRUCTOR CALLED!");
        System.err.println("Class: " + this.getClass().getName());
        System.err.println("========================================");
        log.error("========================================");
        log.error("PaymentController CONSTRUCTOR CALLED!");
        log.error("Class: {}", this.getClass().getName());
        log.error("========================================");
    }
    
    @PostConstruct
    public void init() {
        System.err.println("========================================");
        System.err.println("PaymentController INITIALIZED!");
        System.err.println("Class: " + this.getClass().getName());
        System.err.println("Methods: getAllPayments, getAllPaymentsAlternative");
        System.err.println("========================================");
        log.error("========================================");
        log.error("PaymentController INITIALIZED!");
        log.error("Class: {}", this.getClass().getName());
        log.error("========================================");
    }
    
    /**
     * Получение всех платежей.
     * 
     * @return список всех платежей
     */
    @GetMapping("")
    public ResponseEntity<List<PaymentDto>> getAllPayments() {
        log.error("=== PaymentController.getAllPayments() CALLED ===");
        log.error("Method: GET, Path: /api/v1/payments");
        List<PaymentDto> payments = paymentService.getAllPayments();
        log.error("Returning {} payments", payments.size());
        return ResponseEntity.ok(payments);
    }
    
    /**
     * Получение всех платежей (альтернативный эндпоинт для тестирования).
     * ВАЖНО: Этот метод должен быть ПЕРЕД методами с переменными пути, чтобы Spring правильно его разрешал.
     * 
     * @return список всех платежей
     */
    @GetMapping("/all")
    public ResponseEntity<List<PaymentDto>> getAllPaymentsAlternative() {
        log.error("=== PaymentController.getAllPaymentsAlternative() CALLED ===");
        log.error("Method: GET, Path: /api/v1/payments/all");
        List<PaymentDto> payments = paymentService.getAllPayments();
        log.error("Returning {} payments", payments.size());
        return ResponseEntity.ok(payments);
    }
    
    /**
     * Получение платежей по статусам.
     * Пример: /api/v1/payments/statuses?statuses=CREATED,SUCCESS,FAILED
     * 
     * @param statuses список статусов для фильтрации
     * @return список платежей с указанными статусами
     */
    @GetMapping("/statuses")
    public ResponseEntity<List<PaymentDto>> getPaymentsByStatuses(
            @RequestParam("statuses") 
            @NotNull(message = "Statuses list cannot be null")
            List<PaymentStatus> statuses
    ) {
        List<PaymentDto> payments = paymentService.getPaymentsByStatuses(statuses);
        return ResponseEntity.ok(payments);
    }
    
    /**
     * Создание нового платежа.
     * 
     * @param request данные для создания платежа
     * @return созданный платеж
     */
    @PostMapping("")
    public ResponseEntity<PaymentDto> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        log.error("========================================");
        log.error("PaymentController.createPayment() CALLED!");
        log.error("Method: POST, Path: /api/v1/payments");
        log.error("Request body: orderId={}, userId={}, paymentAmount={}", 
                request.getOrderId(), request.getUserId(), request.getPaymentAmount());
        log.error("========================================");
        
        try {
            // Получаем токен для передачи в order-service
            // ВАЖНО: В продакшене все запросы требуют аутентификации (SecurityConfig.anyRequest().authenticated())
            // Анонимные пользователи НЕ должны попадать в этот контроллер в продакшене.
            // Обработка null токена нужна только для:
            // 1. Интеграционных тестов (где используется TestSecurityConfig с permitAll())
            // 2. Внутренних вызовов между микросервисами (если они настроены)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String authToken = null;
            try {
                authToken = SecurityUtils.getTokenString(authentication);
            } catch (IllegalStateException e) {
                // ВАЖНО: В продакшене (SecurityConfig с @Profile("!test")) все запросы требуют аутентификации
                // (.anyRequest().authenticated()). Spring Security блокирует анонимных пользователей ДО контроллера.
                // Анонимные пользователи (AnonymousAuthenticationToken) НЕ авторизованы и не должны попадать сюда в продакшене.
                // 
                // Обработка null токена нужна только для:
                // 1. Интеграционных тестов (TestSecurityConfig с permitAll() и профилем "test")
                // 2. Внутренних вызовов между микросервисами (если они настроены без JWT)
                //
                // Если анонимный пользователь все же попал сюда в продакшене - это ошибка конфигурации SecurityConfig,
                // но мы не блокируем здесь, так как Spring Security уже должен был это сделать.
                log.warn("Could not extract JWT token from authentication (may be anonymous in test or internal call): {}", e.getMessage());
                authToken = null;
            }
            
            PaymentDto paymentDto = paymentService.createPayment(request, authToken);
            log.error("=== PAYMENT CREATED SUCCESSFULLY ===");
            log.error("Payment ID: {}, Order ID: {}, User ID: {}, Amount: {}, Status: {}", 
                    paymentDto.getId(), paymentDto.getOrderId(), paymentDto.getUserId(), 
                    paymentDto.getPaymentAmount(), paymentDto.getStatus());
            log.error("========================================");
            return ResponseEntity.ok(paymentDto);
        } catch (Exception e) {
            log.error("=== ERROR CREATING PAYMENT ===");
            log.error("Error: {}", e.getMessage(), e);
            log.error("========================================");
            throw e;
        }
    }

    /**
     * Получение платежей по ID заказа.
     * 
     * @param orderId ID заказа
     * @return список платежей для указанного заказа
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentDto>> getPaymentsByOrderId(
            @PathVariable 
            @NotBlank(message = "Order ID cannot be blank")
            @Size(min = 1, max = 50, message = "Order ID must be between 1 and 50 characters")
            String orderId) {
        log.error("========================================");
        log.error("PaymentController.getPaymentsByOrderId() CALLED!");
        log.error("Method: GET, Path: /api/v1/payments/order/{}", orderId);
        log.error("========================================");
        List<PaymentDto> payments = paymentService.getPaymentsByOrderId(orderId);
        log.error("Found {} payments for orderId: {}", payments.size(), orderId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Получение платежей по ID пользователя.
     * 
     * @param userId ID пользователя
     * @return список платежей для указанного пользователя
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentDto>> getPaymentsByUserId(
            @PathVariable 
            @NotBlank(message = "User ID cannot be blank")
            @Size(min = 1, max = 50, message = "User ID must be between 1 and 50 characters")
            String userId) {
        log.error("========================================");
        log.error("PaymentController.getPaymentsByUserId() CALLED!");
        log.error("Method: GET, Path: /api/v1/payments/user/{}", userId);
        log.error("========================================");
        List<PaymentDto> payments = paymentService.getPaymentsByUserId(userId);
        log.error("Found {} payments for userId: {}", payments.size(), userId);
        return ResponseEntity.ok(payments);
    }
    
    /**
     * Получение общей суммы платежей.
     * 
     * @param statuses список статусов для фильтрации
     * @return список платежей с указанными статусами
     */
    @GetMapping("/statuses")
    public ResponseEntity<List<PaymentDto>> getPaymentsByStatuses(
            @RequestParam("statuses") 
            @NotNull(message = "Statuses list cannot be null")
            List<PaymentStatus> statuses
    ) {
        log.error("========================================");
        log.error("PaymentController.getPaymentsByStatuses() CALLED!");
        log.error("Method: GET, Path: /api/v1/payments/statuses");
        log.error("Statuses: {}", statuses);
        log.error("========================================");
        List<PaymentDto> payments = paymentService.getPaymentsByStatuses(statuses);
        log.error("Found {} payments with statuses: {}", payments.size(), statuses);
        return ResponseEntity.ok(payments);
    }
   
    /**
     * Получение общей суммы платежей за период.
     * Пример: /api/v1/payments/total?startDate=2025-01-01T00:00:00Z&endDate=2025-12-31T23:59:59Z
     * Пример с фильтром по статусам: /api/v1/payments/total?startDate=2025-01-01T00:00:00Z&endDate=2025-12-31T23:59:59Z&statuses=SUCCESS
     * 
     * @param startDate опциональная начальная дата периода
     * @param endDate опциональная конечная дата периода
     * @param statuses опциональный список статусов для фильтрации
     * @return общая сумма и количество платежей
     */
    @GetMapping("/total")
    public ResponseEntity<TotalSumResponse> getTotalSum(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            Instant startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            Instant endDate,
            @RequestParam(value = "statuses", required = false) 
            List<PaymentStatus> statuses
    ) {
        log.error("========================================");
        log.error("PaymentController.getTotalSumByDatePeriod() CALLED!");
        log.error("Method: GET, Path: /api/v1/payments/total");
        log.error("Parameters: startDate={}, endDate={}, statuses={}", startDate, endDate, statuses);
        log.error("========================================");
        TotalSumResponse response;
        
        // Если даты не указаны, возвращаем общую сумму всех платежей
        if (startDate == null && endDate == null) {
            if (statuses != null && !statuses.isEmpty()) {
                response = paymentService.getTotalSumByStatuses(statuses);
            } else {
                response = paymentService.getTotalSum();
            }
        } else if (startDate != null && endDate != null) {
            // Если указаны обе даты, используем период
            if (statuses != null && !statuses.isEmpty()) {
                response = paymentService.getTotalSumByDatePeriodAndStatuses(startDate, endDate, statuses);
            } else {
                response = paymentService.getTotalSumByDatePeriod(startDate, endDate);
            }
        } else {
            // Если указана только одна дата - ошибка
            throw new IllegalArgumentException("Both startDate and endDate must be provided, or neither");
        }
        
        log.error("Total sum: {}, Payment count: {}", response.getTotalSum(), response.getPaymentCount());
        return ResponseEntity.ok(response);
    }
}
