package ru.redgho7t.telegrambot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class Main implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Autowired
    private TelegramAiBot bot;

    public static void main(String[] args) {
        logger.info("🚀 === Запуск Telegram AI Bot с Spring Boot ===");
        SpringApplication.run(Main.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("🔧 Регистрация Telegram бота...");
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);
        logger.info("✅ Бот зарегистрирован: @{}", bot.getBotUsername());
    }
}
