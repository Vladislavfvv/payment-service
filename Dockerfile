# === Этап 1: Сборка приложения ===
# Используем локально собранный JAR для гарантии актуальности кода
FROM eclipse-temurin:21-jre
WORKDIR /app

# Копирование локально собранного JAR файла
# ВАЖНО: JAR должен быть собран локально перед сборкой образа: mvn clean package -DskipTests
COPY target/*.jar app.jar

# Порт для Payment Service (8085 согласно application.properties)
EXPOSE 8085

# Переменная окружения для профиля
ENV SPRING_PROFILES_ACTIVE=docker

# Запуск приложения
ENTRYPOINT ["java", "-jar", "app.jar"]
