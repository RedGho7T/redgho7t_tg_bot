package ru.redgho7t.telegrambot.config;

public class EnvConfig {
public static String getGoogleApiKey() {
    String key = System.getenv("GOOGLE_AI_API_KEY");
    if (key == null || key.trim().isEmpty()) {
        throw new IllegalStateException("Переменная окружения GOOGLE_AI_API_KEY не найдена или пуста");
    }
    return key.trim();
}

public static String getTelegramBotToken() {
    return System.getenv("TELEGRAM_BOT_TOKEN");
}

public static String getTelegramBotUsername() {
    return System.getenv("TELEGRAM_BOT_USERNAME");
}
}