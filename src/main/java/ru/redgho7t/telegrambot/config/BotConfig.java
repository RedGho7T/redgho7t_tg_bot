package ru.redgho7t.telegrambot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "telegram.bot")
public class BotConfig {
    private static final Logger logger = LoggerFactory.getLogger(BotConfig.class);

    private String token;
    private String username;
    private String googleApiKey;

    // НОВЫЕ ПОЛЯ для дополнительных API
    private String weatherApiKey;
    private String weatherApiUrl = "https://api.openweathermap.org/data/2.5";
    private String jokeApiUrl = "https://www.anekdot.ru/rss/export_j.xml";
    private String horoscopeApiUrl = "";

    // Spring автоматически заполнит поля из application.properties
    public String getBotToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getBotUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getGoogleApiKey() { return googleApiKey; }
    public void setGoogleApiKey(String googleApiKey) { this.googleApiKey = googleApiKey; }

    // НОВЫЕ ГЕТТЕРЫ И СЕТТЕРЫ
    public String getWeatherApiKey() { return weatherApiKey; }
    public void setWeatherApiKey(String weatherApiKey) { this.weatherApiKey = weatherApiKey; }

    public String getWeatherApiUrl() { return weatherApiUrl; }
    public void setWeatherApiUrl(String weatherApiUrl) { this.weatherApiUrl = weatherApiUrl; }

    public String getJokeApiUrl() { return jokeApiUrl; }
    public void setJokeApiUrl(String jokeApiUrl) { this.jokeApiUrl = jokeApiUrl; }

    public String getHoroscopeApiUrl() { return horoscopeApiUrl; }
    public void setHoroscopeApiUrl(String horoscopeApiUrl) { this.horoscopeApiUrl = horoscopeApiUrl; }

    public boolean isValid() {
        boolean basicValid = token != null && !token.isBlank()
                && username != null && !username.isBlank()
                && googleApiKey != null && !googleApiKey.isBlank();

        if (!basicValid) {
            logger.error("Основная конфигурация бота некорректна");
            return false;
        }

        // Проверяем дополнительные API (не критичны для работы)
        if (weatherApiKey == null || weatherApiKey.isBlank() || weatherApiKey.contains("YOUR_")) {
            logger.warn("Weather API ключ не настроен - функция погоды будет недоступна");
        }

        return true;
    }

    public void logConfiguration() {
        logger.info("=== КОНФИГУРАЦИЯ БОТА ===");
        logger.info("Bot Username: @{}", username);
        logger.info("Google AI API: {}", googleApiKey != null && !googleApiKey.isBlank() ? "✅ Настроен" : "❌ Не настроен");
        logger.info("Weather API: {}", weatherApiKey != null && !weatherApiKey.isBlank() && !weatherApiKey.contains("YOUR_") ? "✅ Настроен" : "❌ Не настроен");
        logger.info("Jokes API URL: {}", jokeApiUrl);
        logger.info("========================");
    }
}