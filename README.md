# Payment Service

Микросервис для управления платежами.

## Быстрый старт

### Локальный запуск

1. **Запустите MongoDB и Kafka:**
   ```powershell
   cd D:\JAVA\Innowise\Projects\28-11-2025\gateway-service\gateway-service
   docker-compose up -d mongo-payment zookeeper kafka
   ```

2. **Запустите приложение:**
   ```powershell
   cd D:\JAVA\Innowise\Projects\28-11-2025\payment-service\paymentservice
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

3. **Проверьте работу:**
   - Health Check: http://localhost:8085/actuator/health
   - API: http://localhost:8085

### Запуск через Docker

```powershell
docker-compose up -d
```

## Подробные инструкции

См. [LOCAL_SETUP.md](LOCAL_SETUP.md) для детальных инструкций по локальному запуску.

## Технологии

- Spring Boot 3.5.8
- MongoDB
- Apache Kafka
- Liquibase
- MapStruct
- Lombok
