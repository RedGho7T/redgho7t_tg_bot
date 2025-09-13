package ru.redgho7t.telegrambot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.redgho7t.telegrambot.config.BotConfig;

/**
* Главный класс для запуска Telegram AI Bot
* <p>
* Этот класс отвечает за:
* - Инициализацию API Telegram Bots
* - Регистрацию бота
* - Обработку ошибок запуска
*
* @author redgho7t
* @version 1.0
*/
public class Main {

private static final Logger logger = LoggerFactory.getLogger(Main.class);

/**
 * Точка входа в приложение
 *
 * @param args аргументы командной строки (не используются)
 */
public static void main(String[] args) {
    logger.info("=== Запуск Telegram AI Bot ===");

    try {
        // Загружаем и проверяем конфигурацию
        BotConfig config = new BotConfig();
        if (!config.isValid()) {
            logger.error("Конфигурация бота не настроена. Проверьте переменные окружения.");
            System.exit(1);
        }

        // Инициализируем TelegramBotsApi
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

        // Создаём и регистрируем бота
        TelegramAiBot bot = new TelegramAiBot(config);
        botsApi.registerBot(bot);

        logger.info("Бот успешно запущен и готов к работе!");
        logger.info("Username: @{}", config.getBotUsername());

    } catch (TelegramApiException e) {
        logger.error("Ошибка при регистрации бота: {}", e.getMessage(), e);
        System.exit(1);
    } catch (Exception e) {
        logger.error("Неожиданная ошибка при запуске: {}", e.getMessage(), e);
        System.exit(1);
    }
}
}
