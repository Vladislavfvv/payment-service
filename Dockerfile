# === Этап 1: Сборка приложения ===
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Копирование pom.xml для кэширования зависимостей
# ВАЖНО: Если pom.xml изменился, этот слой будет пересобран
COPY pom.xml .

# Полная очистка кэша Maven для Spring Boot перед загрузкой зависимостей
# Это гарантирует, что при изменении версий в pom.xml будут использоваться новые версии
# Удаляем весь кэш Spring Boot, чтобы гарантировать использование версии из pom.xml
RUN rm -rf /root/.m2/repository/org/springframework/boot || true && \
    rm -rf /root/.m2/repository/org/springframework/boot-starter-parent || true

# Загрузка зависимостей с принудительным обновлением (кэшируется отдельно от кода)
# Флаг -U гарантирует обновление зависимостей до актуальных версий
# Флаг --update-snapshots обновляет snapshot-версии
# Это ускоряет пересборку при изменении только исходного кода
RUN mvn -B dependency:go-offline -U --update-snapshots --no-transfer-progress || true

# Копирование исходного кода
# ВАЖНО: Этот слой пересобирается при любом изменении кода
COPY src ./src

# Сборка проекта (пропускаем тесты для ускорения сборки образа)
# Используем --no-transfer-progress для более чистого вывода
# --no-transfer-progress убирает лишний вывод о загрузке зависимостей
# Добавляем -U для обновления зависимостей (гарантирует использование актуальных версий)
# Добавляем --update-snapshots для обновления snapshot-версий
RUN mvn -B clean package -DskipTests -U --update-snapshots --no-transfer-progress

# === Этап 2: Запуск приложения ===
FROM eclipse-temurin:21-jre
WORKDIR /app

# Копирование JAR файла из этапа сборки
COPY --from=build /app/target/*.jar app.jar

# Порт для Payment Service
# Внутри контейнера приложение слушает на 8085
# docker-compose маппит внешний 8085 -> внутренний 8085
EXPOSE 8085

# Переменная окружения для профиля
ENV SPRING_PROFILES_ACTIVE=docker

# Запуск приложения
ENTRYPOINT ["java", "-jar", "app.jar"]
