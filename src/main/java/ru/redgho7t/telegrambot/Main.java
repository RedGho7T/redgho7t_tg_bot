package ru.redgho7t.telegrambot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.redgho7t.telegrambot.config.BotConfig;
import ru.redgho7t.telegrambot.service.DatabaseService;

/**
 * Главный класс для запуска Telegram AI Bot с использованием Spring Boot
 *
 * Spring Boot автоматически настраивает:
 * - Подключение к базе данных
 * - Инициализацию JPA репозиториев
 * - Внедрение зависимостей
 * - Конфигурацию приложения
 *
 * @author redgho7t
 * @version 1.0
 */
@SpringBootApplication
public class Main implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Autowired
    private DatabaseService databaseService;

    /**
     * Точка входа в приложение
     * Spring Boot автоматически запускает приложение и настраивает все компоненты
     */
    public static void main(String[] args) {
        logger.info("🚀 === Запуск Telegram AI Bot с Spring Boot ===");
        SpringApplication.run(Main.class, args);
    }

    /**
     * Выполняется после полной инициализации Spring Boot контекста
     * Здесь регистрируем и запускаем Telegram бота
     */
    @Override
    public void run(String... args) throws Exception {
        logger.info("🔧 Инициализация Telegram бота...");

        try {
            // Загружаем и проверяем конфигурацию
            BotConfig config = new BotConfig();
            if (!config.isValid()) {
                logger.error("❌ Конфигурация бота не настроена. Проверьте переменные окружения.");
                logger.error("Необходимы: TELEGRAM_BOT_TOKEN, TELEGRAM_BOT_USERNAME, GOOGLE_AI_API_KEY");
                System.exit(1);
            }

            // Инициализируем TelegramBotsApi
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            // Создаём и регистрируем бота с базой данных
            TelegramAiBot bot = telegramAiBot(config, databaseService);
            botsApi.registerBot(bot);

            logger.info("✅ Бот успешно запущен и готов к работе!");
            logger.info("📱 Username: @{}", config.getBotUsername());
            logger.info("🗄️  База данных подключена и готова к использованию");

            // Показываем статистику при запуске
            showStartupStatistics();

        } catch (TelegramApiException e) {
            logger.error("❌ Ошибка при регистрации бота: {}", e.getMessage(), e);
            System.exit(1);
        } catch (Exception e) {
            logger.error("❌ Неожиданная ошибка при запуске: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Создаёт bean для Telegram бота
     * Spring автоматически внедряет все необходимые зависимости
     */
    @Bean
    public TelegramAiBot telegramAiBot(BotConfig botConfig, DatabaseService databaseService) {
        return new TelegramAiBot(botConfig, databaseService);
    }

    /**
     * Показывает статистику при запуске бота
     */
    private void showStartupStatistics() {
        try {
            DatabaseService.BotStatistics stats = databaseService.getBotStatistics();
            logger.info("📊 Статистика бота:");
            logger.info("   • Сообщений сегодня: {}", stats.getMessagesToday());
            logger.info("   • Среднее время ответа: {:.2f} мс", stats.getAverageResponseTime());
            logger.info("   • Ошибок за неделю: {}", stats.getErrorsLastWeek());
        } catch (Exception e) {
            logger.warn("⚠️  Не удалось загрузить статистику при запуске: {}", e.getMessage());
        }
    }
}