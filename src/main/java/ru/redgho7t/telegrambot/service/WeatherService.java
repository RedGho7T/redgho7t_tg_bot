package ru.redgho7t.telegrambot.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Сервис для получения прогноза погоды через OpenWeatherMap API
 * Поддерживает русскую локализацию и кэширование данных
 */
@Service
public class WeatherService {
    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

    private static final String API_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final String DEFAULT_CITY = "Moscow"; // Город по умолчанию

    @Value("${telegram.bot.weather-api-key:}")
    private String apiKey;

    private final OkHttpClient httpClient;
    private String cachedWeather;
    private long lastUpdateTime = 0;
    private static final long CACHE_DURATION = 30 * 60 * 1000; // 30 минут

    public WeatherService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        logger.info("WeatherService инициализирован");
    }

    /**
     * Получает прогноз погоды для города по умолчанию
     */
    public String getWeather() {
        return getWeather(DEFAULT_CITY);
    }

    /**
     * Получает прогноз погоды для указанного города
     */
    public String getWeather(String city) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return "⚠️ API ключ для прогноза погоды не настроен. Обратитесь к администратору.";
        }

        // Проверяем кэш только для города по умолчанию
        if (city.equals(DEFAULT_CITY) && isCacheValid()) {
            logger.debug("Возвращаем погоду из кэша");
            return cachedWeather;
        }

        try {
            String weather = fetchWeatherFromAPI(city);

            // Кэшируем только для города по умолчанию
            if (city.equals(DEFAULT_CITY)) {
                cachedWeather = weather;
                lastUpdateTime = System.currentTimeMillis();
            }

            return weather;

        } catch (Exception e) {
            logger.error("Ошибка при получении погоды для {}: {}", city, e.getMessage());
            return "❌ Не удалось получить прогноз погоды. Попробуйте позже.";
        }
    }

    /**
     * Обновляет кэш погоды
     */
    public void updateWeatherCache() {
        logger.info("Обновление кэша погоды");
        try {
            String weather = fetchWeatherFromAPI(DEFAULT_CITY);
            cachedWeather = weather;
            lastUpdateTime = System.currentTimeMillis();
            logger.info("Кэш погоды обновлён");
        } catch (Exception e) {
            logger.error("Ошибка при обновлении кэша погоды: {}", e.getMessage());
        }
    }

    /**
     * Получает данные о погоде из API
     */
    private String fetchWeatherFromAPI(String city) throws IOException {
        String url = String.format("%s?q=%s&appid=%s&units=metric&lang=ru",
                API_URL, city, apiKey);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "TelegramBot/1.0")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                if (response.code() == 404) {
                    throw new IOException("Город не найден");
                } else if (response.code() == 401) {
                    throw new IOException("Неверный API ключ");
                }
                throw new IOException("HTTP " + response.code());
            }

            if (response.body() == null) {
                throw new IOException("Пустой ответ от API");
            }

            String jsonResponse = response.body().string();
            return parseWeatherResponse(jsonResponse, city);
        }
    }

    /**
     * Парсит ответ API и форматирует для отправки пользователю
     */
    private String parseWeatherResponse(String jsonResponse, String city) {
        try {
            JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();

            // Основная информация
            JsonObject main = json.getAsJsonObject("main");
            JsonObject weather = json.getAsJsonArray("weather").get(0).getAsJsonObject();
            JsonObject wind = json.getAsJsonObject("wind");

            // Извлекаем данные
            double temp = main.get("temp").getAsDouble();
            double feelsLike = main.get("feels_like").getAsDouble();
            int humidity = main.get("humidity").getAsInt();
            int pressure = main.get("pressure").getAsInt();

            String description = weather.get("description").getAsString();
            String icon = weather.get("icon").getAsString();

            double windSpeed = wind.has("speed") ? wind.get("speed").getAsDouble() : 0;

            // Определяем эмодзи для погоды
            String weatherEmoji = getWeatherEmoji(icon);

            // Форматируем ответ
            StringBuilder result = new StringBuilder();
            result.append(String.format("🌤️ **Погода в %s**\n\n",
                    city.substring(0, 1).toUpperCase() + city.substring(1).toLowerCase()));

            result.append(String.format("%s **%s**\n", weatherEmoji,
                    description.substring(0, 1).toUpperCase() + description.substring(1)));

            result.append(String.format("🌡️ **Температура:** %.1f°C\n", temp));
            result.append(String.format("🤔 **Ощущается как:** %.1f°C\n", feelsLike));
            result.append(String.format("💧 **Влажность:** %d%%\n", humidity));
            result.append(String.format("📊 **Давление:** %d мм рт.ст.\n",
                    (int) (pressure * 0.75))); // Перевод из гПа в мм рт.ст.

            if (windSpeed > 0) {
                result.append(String.format("💨 **Ветер:** %.1f м/с\n", windSpeed));
            }

            result.append(String.format("\n📅 *Обновлено: %s*",
                    java.time.LocalDateTime.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));

            return result.toString();

        } catch (Exception e) {
            logger.error("Ошибка при парсинге ответа API: {}", e.getMessage());
            throw new RuntimeException("Ошибка при обработке данных о погоде");
        }
    }

    /**
     * Возвращает эмодзи для иконки погоды
     */
    private String getWeatherEmoji(String icon) {
        return switch (icon.substring(0, 2)) {
            case "01" -> "☀️"; // ясно
            case "02" -> "⛅"; // малооблачно
            case "03" -> "☁️"; // облачно
            case "04" -> "☁️"; // пасмурно
            case "09" -> "🌧️"; // дождь
            case "10" -> "🌦️"; // дождь с солнцем
            case "11" -> "⛈️"; // гроза
            case "13" -> "❄️"; // снег
            case "50" -> "🌫️"; // туман
            default -> "🌤️";
        };
    }

    /**
     * Проверяет, актуален ли кэш
     */
    private boolean isCacheValid() {
        return cachedWeather != null &&
                (System.currentTimeMillis() - lastUpdateTime) < CACHE_DURATION;
    }

    /**
     * Возвращает статус сервиса
     */
    public String getServiceStatus() {
        boolean hasApiKey = apiKey != null && !apiKey.trim().isEmpty();
        boolean hasCachedData = cachedWeather != null;

        return String.format("Погодный сервис: %s, API ключ: %s, Кэш: %s",
                hasApiKey ? "✅ Готов" : "❌ Не настроен",
                hasApiKey ? "✅ Есть" : "❌ Отсутствует",
                hasCachedData ? "✅ Активен" : "❌ Пуст");
    }
}