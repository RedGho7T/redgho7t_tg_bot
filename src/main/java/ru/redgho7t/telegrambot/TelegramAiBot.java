package ru.redgho7t.telegrambot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.redgho7t.telegrambot.config.BotConfig;
import ru.redgho7t.telegrambot.entity.MessageLog.MessageType;
import ru.redgho7t.telegrambot.service.DatabaseService;
import ru.redgho7t.telegrambot.service.GoogleAiService;
import ru.redgho7t.telegrambot.service.JokeService;
import ru.redgho7t.telegrambot.service.MessageProcessor;
import ru.redgho7t.telegrambot.utils.KeyboardFactory;
import ru.redgho7t.telegrambot.utils.MessageSplitter;

import java.util.List;

/**
 * Основной класс Telegram AI Bot с поддержкой базы данных и анекдотов
 *
 * Новые возможности:
 * - Автоматическое логирование всех сообщений в БД
 * - Сохранение времени ответа
 * - Логирование ошибок
 * - Статистика использования
 * - Функция анекдотов по ключевому слову
 */
@Component
public class TelegramAiBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramAiBot.class);
    private static final int MESSAGE_DELAY = 200; // Задержка между сообщениями в мс

    private final BotConfig config;
    private final MessageProcessor messageProcessor;
    private final DatabaseService databaseService;

    @Autowired
    public TelegramAiBot(BotConfig config, DatabaseService databaseService, JokeService jokeService) {
        this.config = config;
        this.databaseService = databaseService;

        // Получаем Google API ключ из BotConfig
        String googleKey = config.getGoogleApiKey();

        // Инициализируем сервис для Google Gemini API
        GoogleAiService googleAiService = new GoogleAiService(googleKey);

        // Передаём сервисы в MessageProcessor
        this.messageProcessor = new MessageProcessor(googleAiService, jokeService);

        logger.info("🤖 TelegramAiBot инициализирован для @{} с поддержкой БД и анекдотов", config.getBotUsername());
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
        // Засекаем время начала обработки
        long startTime = System.currentTimeMillis();
        try {
            // Обрабатываем callback запросы (нажатия на кнопки)
            if (update.hasCallbackQuery()) {
                handleCallbackQuery(update, startTime);
                return;
            }

            // Обрабатываем обычные сообщения
            if (update.hasMessage() && update.getMessage().hasText()) {
                handleTextMessage(update, startTime);
            }

        } catch (Exception e) {
            logger.error("❌ Ошибка при обработке обновления: {}", e.getMessage(), e);
            try {
                Long chatId = getChatId(update);
                if (chatId != null) {
                    // Логируем ошибку в БД
                    logError(update, e, startTime);
                    sendSimpleMessage(chatId, "❌ Произошла внутренняя ошибка. Попробуйте позже.");
                }
            } catch (Exception sendError) {
                logger.error("❌ Не удалось отправить сообщение об ошибке: {}", sendError.getMessage());
            }
        }
    }

    /**
     * Обрабатывает callback запросы (нажатия на кнопки)
     */
    private void handleCallbackQuery(Update update, long startTime) {
        var callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        Long userId = callbackQuery.getFrom().getId();
        String userName = getUserDisplayName(update);
        String callbackData = callbackQuery.getData();

        logger.info("🔘 Callback от {} в чате {}: {}", userName, chatId, callbackData);

        // Отправляем подтверждение нажатия
        try {
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(callbackQuery.getId());
            execute(answer);
        } catch (TelegramApiException e) {
            logger.warn("⚠️ Не удалось ответить на callback: {}", e.getMessage());
        }

        // Обрабатываем callback через MessageProcessor
        var result = messageProcessor.processUpdate(update);
        if (result.shouldReply() && !result.getResponse().trim().isEmpty()) {
            InlineKeyboardMarkup keyboard = null;

            // Определяем нужную клавиатуру
            if (callbackData.equals("cmd_start") || callbackData.equals("back_main")) {
                keyboard = KeyboardFactory.getMainMenuKeyboard();
            } else if (result.shouldShowCreatorKeyboard()) {
                keyboard = KeyboardFactory.getCreatorInfoKeyboard();
            }

            sendMessageWithKeyboard(chatId, result.getResponse(), keyboard);

            // Логируем в БД
            long responseTime = System.currentTimeMillis() - startTime;
            databaseService.logMessage(
                    chatId, userId, userName,
                    "Callback: " + callbackData, result.getResponse(),
                    MessageType.CALLBACK, false, responseTime
            );
        }
    }

    /**
     * Обрабатывает текстовые сообщения
     */
    private void handleTextMessage(Update update, long startTime) {
        Long chatId = update.getMessage().getChatId();
        Long userId = update.getMessage().getFrom().getId();
        String userName = getUserDisplayName(update);
        String messageText = update.getMessage().getText();
        boolean isGroup = update.getMessage().getChat().isGroupChat() ||
                update.getMessage().getChat().isSuperGroupChat();

        logger.info("💬 Сообщение от {} в чате {}: {}", userName, chatId,
                messageText.length() > 50 ? messageText.substring(0, 50) + "..." : messageText);

        var result = messageProcessor.processUpdate(update);

        if (result.shouldReply() && !result.getResponse().trim().isEmpty()) {
            InlineKeyboardMarkup keyboard = null;

            // Для команды /start показываем главное меню
            if (messageText.equals("/start") || messageText.startsWith("/start@")) {
                keyboard = KeyboardFactory.getMainMenuKeyboard();
            }

            sendMessageWithKeyboard(chatId, result.getResponse(), keyboard);

            // Определяем тип сообщения для логирования
            MessageType messageType;
            if (messageText.startsWith("/")) {
                messageType = MessageType.COMMAND;
            } else if (containsSpecialKeyword(messageText)) {
                messageType = MessageType.SPECIAL_KEYWORD;
            } else {
                messageType = MessageType.AI_REQUEST;
            }

            // Логируем в БД
            long responseTime = System.currentTimeMillis() - startTime;
            databaseService.logMessage(
                    chatId, userId, userName,
                    messageText, result.getResponse(),
                    messageType, isGroup, responseTime
            );
        }
    }

    /**
     * Проверяет, содержит ли сообщение специальные ключевые слова
     * Добавляем проверку на анекдоты
     */
    private boolean containsSpecialKeyword(String messageText) {
        String lowerText = messageText.toLowerCase();
        return lowerText.contains("бот") || lowerText.contains("ботяра") ||
                lowerText.contains("попи") || lowerText.contains("java") ||
                lowerText.contains("жаби") || lowerText.contains("jabi") ||
                lowerText.contains("го") || lowerText.contains("go") ||
                lowerText.contains("анекдот") || lowerText.contains("шутка") ||
                lowerText.contains("joke");
    }

    /**
     * Логирует ошибку в базу данных
     */
    private void logError(Update update, Exception error, long startTime) {
        try {
            Long chatId = getChatId(update);
            Long userId = null;
            String userName = "Unknown";
            String messageText = "";
            boolean isGroup = false;

            if (update.hasMessage()) {
                userId = update.getMessage().getFrom().getId();
                userName = getUserDisplayName(update);
                messageText = update.getMessage().getText();
                isGroup = update.getMessage().getChat().isGroupChat() ||
                        update.getMessage().getChat().isSuperGroupChat();
            } else if (update.hasCallbackQuery()) {
                userId = update.getCallbackQuery().getFrom().getId();
                userName = getUserDisplayName(update);
                messageText = "Callback: " + update.getCallbackQuery().getData();
            }

            databaseService.logError(chatId, userId, userName, messageText,
                    error.getMessage(), isGroup);
        } catch (Exception e) {
            logger.error("❌ Критическая ошибка при логировании ошибки: {}", e.getMessage());
        }
    }

    /**
     * Отправляет сообщение с автоматическим разбиением на части
     */
    private void sendMessageWithKeyboard(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        List<String> messageParts = MessageSplitter.splitMessage(text);

        for (int i = 0; i < messageParts.size(); i++) {
            String part = messageParts.get(i);
            // Клавиатуру прикрепляем только к последнему сообщению
            InlineKeyboardMarkup currentKeyboard = (i == messageParts.size() - 1) ? keyboard : null;

            sendTextMessage(chatId, part, currentKeyboard);

            // Добавляем задержку между сообщениями (кроме последнего)
            if (i < messageParts.size() - 1) {
                try {
                    Thread.sleep(MESSAGE_DELAY);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("⚠️ Прервана отправка сообщений");
                    break;
                }
            }
        }

        if (messageParts.size() > 1) {
            logger.info("📤 Отправлено {} частей сообщения в чат {}", messageParts.size(), chatId);
        }
    }

    /**
     * Отправляет одно текстовое сообщение
     */
    private void sendTextMessage(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        try {
            SendMessage message = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .parseMode("Markdown")
                    .disableWebPagePreview(true)
                    .replyMarkup(keyboard)
                    .build();

            execute(message);
            logger.debug("✅ Отправлено сообщение в чат {}: {} символов", chatId, text.length());

        } catch (TelegramApiException e) {
            logger.error("❌ Ошибка при отправке сообщения в чат {}: {}", chatId, e.getMessage());

            // Если ошибка форматирования, пробуем без Markdown
            if (e.getMessage().contains("parse")) {
                try {
                    SendMessage plainMessage = SendMessage.builder()
                            .chatId(chatId.toString())
                            .text(text)
                            .replyMarkup(keyboard)
                            .build();

                    execute(plainMessage);
                    logger.debug("✅ Сообщение отправлено без форматирования");
                } catch (TelegramApiException retry) {
                    logger.error("❌ Повторная ошибка при отправке сообщения: {}", retry.getMessage());
                }
            }
        }
    }

    /**
     * Отправляет простое сообщение без клавиатуры
     */
    private void sendSimpleMessage(Long chatId, String text) {
        sendTextMessage(chatId, text, null);
    }

    /**
     * Получает ID чата из обновления
     */
    private Long getChatId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        }

        return null;
    }

    /**
     * Получает отображаемое имя пользователя
     */
    private String getUserDisplayName(Update update) {
        var user = update.hasMessage()
                ? update.getMessage().getFrom()
                : update.hasCallbackQuery()
                ? update.getCallbackQuery().getFrom()
                : null;

        if (user == null) return "Unknown";

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
        logger.info("🚀 Бот @{} успешно зарегистрирован в Telegram", getBotUsername());
    }

    @Override
    public void clearWebhook() {
        try {
            super.clearWebhook();
            logger.info("🧹 Webhook очищен для бота @{}", getBotUsername());
        } catch (Exception e) {
            logger.error("❌ Не удалось очистить webhook: {}", e.getMessage());
        }
    }
}