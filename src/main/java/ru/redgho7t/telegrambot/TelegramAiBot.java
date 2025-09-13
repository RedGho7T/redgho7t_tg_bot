package ru.redgho7t.telegrambot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.redgho7t.telegrambot.config.BotConfig;
import ru.redgho7t.telegrambot.service.GoogleAiService;
import ru.redgho7t.telegrambot.service.MessageProcessor;

/**
* Основной класс Telegram AI Bot
* <p>
* Использует GoogleAiService для работы с Gemini API.
*/
public class TelegramAiBot extends TelegramLongPollingBot {

private static final Logger logger = LoggerFactory.getLogger(TelegramAiBot.class);

private final BotConfig config;
private final MessageProcessor messageProcessor;

public TelegramAiBot(BotConfig config) {
    this.config = config;

    // Получаем Google API ключ из BotConfig
    String googleKey = config.getGoogleApiKey();
    // Инициализируем сервис для Google Gemini API
    GoogleAiService googleAiService = new GoogleAiService(googleKey);
    // Передаём сервис в MessageProcessor
    this.messageProcessor = new MessageProcessor(googleAiService);

    logger.info("TelegramAiBot инициализирован для @{}", config.getBotUsername());
}

@Override
public String getBotToken() {
    return config.getBotToken();
}

@Override
public String getBotUsername() {
    return config.getBotUsername();
}

@Override
public void onUpdateReceived(Update update) {
    try {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String userName = getUserDisplayName(update);
            String messageText = update.getMessage().getText();
            logger.debug("Получено сообщение от {} в чате {}: {}", userName, chatId, messageText);
        }

        var result = messageProcessor.processUpdate(update);
        if (result.shouldReply() && !result.getResponse().trim().isEmpty()) {
            Long chatId = getChatId(update);
            if (chatId != null) {
                sendTextMessage(chatId, result.getResponse());
            }
        }

    } catch (Exception e) {
        logger.error("Ошибка при обработке обновления: {}", e.getMessage(), e);
        try {
            Long chatId = getChatId(update);
            if (chatId != null) {
                sendTextMessage(chatId, "❌ Произошла внутренняя ошибка. Попробуйте позже.");
            }
        } catch (Exception sendError) {
            logger.error("Не удалось отправить сообщение об ошибке: {}", sendError.getMessage());
        }
    }
}

private void sendTextMessage(Long chatId, String text) {
    try {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("Markdown")
                .disableWebPagePreview(true)
                .build();
        execute(message);
        logger.debug("Отправлено сообщение в чат {}: {}", chatId,
                text.substring(0, Math.min(text.length(), 100)) + "...");
    } catch (TelegramApiException e) {
        logger.error("Ошибка при отправке сообщения в чат {}: {}", chatId, e.getMessage());
        if (e.getMessage().contains("parse")) {
            try {
                SendMessage plain = SendMessage.builder()
                        .chatId(chatId.toString())
                        .text(text)
                        .build();
                execute(plain);
                logger.debug("Сообщение отправлено без форматирования");
            } catch (TelegramApiException retry) {
                logger.error("Повторная ошибка при отправке сообщения: {}", retry.getMessage());
            }
        }
    }
}

private Long getChatId(Update update) {
    if (update.hasMessage()) {
        return update.getMessage().getChatId();
    } else if (update.hasCallbackQuery()) {
        return update.getCallbackQuery().getMessage().getChatId();
    }
    return null;
}

private String getUserDisplayName(Update update) {
    if (!update.hasMessage()) {
        return "Unknown";
    }
    var user = update.getMessage().getFrom();
    StringBuilder name = new StringBuilder();
    if (user.getFirstName() != null) {
        name.append(user.getFirstName());
    }
    if (user.getLastName() != null) {
        if (name.length() > 0) name.append(" ");
        name.append(user.getLastName());
    }
    if (name.isEmpty() && user.getUserName() != null) {
        name.append("@").append(user.getUserName());
    }
    return name.length() > 0 ? name.toString() : "User" + user.getId();
}

@Override
public void onRegister() {
    logger.info("Бот @{} успешно зарегистрирован в Telegram", getBotUsername());
}

@Override
public void clearWebhook() {
    try {
        super.clearWebhook();
        logger.info("Webhook очищен для бота @{}", getBotUsername());
    } catch (Exception e) {
        logger.error("Не удалось очистить webhook: {}", e.getMessage());
    }
}
}
