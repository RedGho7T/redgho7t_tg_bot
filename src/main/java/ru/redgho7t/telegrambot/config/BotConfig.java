package ru.redgho7t.telegrambot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Конфигурационный класс для Telegram AI Bot
* <p>
* Загружает настройки из переменных окружения:
* - TELEGRAM_BOT_TOKEN
* - TELEGRAM_BOT_USERNAME
* - GOOGLE_AI_API_KEY
*/
public class BotConfig {

private static final Logger logger = LoggerFactory.getLogger(BotConfig.class);

// Токен бота от @BotFather
private final String botToken;

// Имя пользователя бота (без @)
private final String botUsername;

// API-ключ Google AI Studio (Gemini)
private final String googleApiKey;

/**
 * Конструктор загружает настройки из переменных окружения
 */
public BotConfig() {
    this.botToken = getEnv("TELEGRAM_BOT_TOKEN");
    this.botUsername = getEnv("TELEGRAM_BOT_USERNAME");
    this.googleApiKey = getEnv("GOOGLE_AI_API_KEY");

    logger.info("Конфигурация загружена:");
    logger.debug("BotUsername: {}", botUsername);
    logger.debug("Using Google AI API Key: {}", googleApiKey != null ? "[SET]" : "[NOT SET]");
}

private String getEnv(String name) {
    String value = System.getenv(name);
    if (value == null || value.trim().isEmpty()) {
        logger.error("Переменная окружения '{}' не задана или пуста", name);
    }
    return value;
}

/**
 * Проверяет, что все обязательные переменные заданы
 */
public boolean isValid() {
    return botToken != null && !botToken.isBlank()
            && botUsername != null && !botUsername.isBlank()
            && googleApiKey != null && !googleApiKey.isBlank();
}

public String getBotToken() {
    return botToken;
}

public String getBotUsername() {
    return botUsername;
}

public String getGoogleApiKey() {
    return googleApiKey;
}
}
