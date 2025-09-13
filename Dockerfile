# Используем официальный образ OpenJDK 17
FROM openjdk:17-jdk-slim

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем Maven wrapper и pom.xml для кеширования зависимостей
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Делаем mvnw исполняемым
RUN chmod +x mvnw

# Загружаем зависимости (этот слой будет кеширован)
RUN ./mvnw dependency:go-offline -B

# Копируем исходный код
COPY src ./src

# Собираем приложение
RUN ./mvnw clean package -DskipTests

# Создаем финальный образ
FROM openjdk:17-jdk-slim

# Устанавливаем необходимые пакеты
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Создаем пользователя для безопасности
RUN groupadd -r botuser && useradd -r -g botuser botuser

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем собранный JAR файл из предыдущего этапа
COPY --from=0 /app/target/*.jar app.jar

# Изменяем владельца файлов
RUN chown -R botuser:botuser /app

# Переключаемся на пользователя
USER botuser

# Настройки JVM для контейнера
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseStringDeduplication"

# Открываем порт для веб-сервера (Railway требует HTTP endpoint)
EXPOSE 8080

# Проверка здоровья приложения
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Запускаем приложение
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

