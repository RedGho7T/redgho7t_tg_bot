package ru.redgho7t.telegrambot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.redgho7t.telegrambot.utils.ResponseTemplates;
import ru.redgho7t.telegrambot.service.GoogleAiService;

import java.util.HashMap;
import java.util.Map;

/**
* Сервис для обработки сообщений от пользователей
* <p>
* Заменён PerplexityService на GoogleAiService для взаимодействия с Gemini API.
*/
public class MessageProcessor {

private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);

private final ResponseTemplates responseTemplates;
private final Map<String, String> keywordTemplates;
private final GoogleAiService googleAiService;

/**
 * Конструктор процессора сообщений
 *
 * @param googleAiService сервис для работы с Google Gemini API
 */
public MessageProcessor(GoogleAiService googleAiService) {
    this.googleAiService = googleAiService;
    this.responseTemplates = new ResponseTemplates();
    this.keywordTemplates = initializeKeywordTemplates();

    logger.info("MessageProcessor инициализирован с GoogleAiService");
}

private Map<String, String> initializeKeywordTemplates() {
    Map<String, String> templates = new HashMap<>();

    templates.put("привет", "Привет! Меня зовут AI Bot. Расскажи мне: ");
    templates.put("hello", "Hello! I'm AI Bot. Tell me: ");
    templates.put("помощь", "Как я могу помочь с вопросом: ");
    templates.put("help", "How can I help you with: ");
    templates.put("найди", "Найти информацию о: ");
    templates.put("search", "Search for information about: ");
    templates.put("что такое", "Что такое ");
    templates.put("what is", "What is ");
    templates.put("объясни", "Объясни ");
    templates.put("explain", "Explain ");
    templates.put("расскажи", "Расскажи о ");
    templates.put("tell me", "Tell me about ");
    templates.put("сравни", "Сравни ");
    templates.put("compare", "Compare ");
    templates.put("разница", "В чем разница между ");
    templates.put("difference", "What's the difference between ");

    return templates;
}

public ProcessingResult processUpdate(Update update) {
    try {
        if (update.hasMessage()) {
            return processMessage(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            return processCallbackQuery(update);
        }
        return new ProcessingResult("", false);
    } catch (Exception e) {
        logger.error("Ошибка при обработке обновления: {}", e.getMessage(), e);
        return new ProcessingResult("❌ Произошла ошибка при обработке сообщения.", true);
    }
}

private ProcessingResult processMessage(Message message) {
    String messageText = message.getText();
    if (messageText == null || messageText.trim().isEmpty()) {
        return new ProcessingResult("", false);
    }

    Long chatId = message.getChatId();
    String userName = getUserName(message);
    boolean isGroup = message.getChat().isGroupChat() || message.getChat().isSuperGroupChat();

    logger.info("Получено сообщение от {}: {}", userName, messageText);

    if (messageText.startsWith("/")) {
        return processCommand(messageText, chatId, isGroup);
    }
    if (isGroup && !isMessageForBot(message)) {
        return new ProcessingResult("", false);
    }
    return processUserMessage(messageText, userName);
}

private ProcessingResult processCommand(String command, Long chatId, boolean isGroup) {
    String cmd = command.toLowerCase().split("\\s+")[0];
    return switch (cmd) {
        case "/start" -> new ProcessingResult(responseTemplates.getStartMessage(), true);
        case "/help" -> new ProcessingResult(responseTemplates.getHelpMessage(), true);
        case "/about" -> new ProcessingResult(responseTemplates.getAboutMessage(), true);
        case "/status" -> processStatusCommand();
        case "/models" -> new ProcessingResult(responseTemplates.getModelsMessage(), true);
        default -> {
            if (isGroup) {
                yield new ProcessingResult("", false);
            } else {
                yield new ProcessingResult(
                        "❓ Неизвестная команда. Используйте /help для просмотра доступных команд.", true
                );
            }
        }
    };
}

private ProcessingResult processStatusCommand() {
    boolean isAvailable = googleAiService.isApiAvailable();
    String status = isAvailable ? "✅ Онлайн" : "❌ Недоступен";
    String message = String.format(
            "🤖 **Статус бота:**\n\nAI API: %s\nВремя работы: активен\nВерсия: 1.0.0",
            status
    );
    return new ProcessingResult(message, true);
}

private ProcessingResult processUserMessage(String messageText, String userName) {
    String enhanced = enhanceMessageWithTemplate(messageText);
    try {
        String aiResponse = googleAiService.sendRequest(enhanced);
        logger.debug(
                "Ответ AI для {}: {}...",
                userName,
                aiResponse.substring(0, Math.min(aiResponse.length(), 100))
        );
        return new ProcessingResult(aiResponse, true);
    } catch (Exception e) {
        logger.error("Ошибка при запросе к Google AI: {}", e.getMessage(), e);
        return new ProcessingResult(
                "❌ Ошибка при обращении к AI. Попробуйте позже.", true
        );
    }
}

private String enhanceMessageWithTemplate(String messageText) {
    String lower = messageText.toLowerCase();
    for (var entry : keywordTemplates.entrySet()) {
        if (lower.contains(entry.getKey())) {
            String template = entry.getValue();
            String rest = messageText.substring(lower.indexOf(entry.getKey()) + entry.getKey().length()).trim();
            return rest.isEmpty() ? template + messageText : template + rest;
        }
    }
    return messageText;
}

private ProcessingResult processCallbackQuery(Update update) {
    return new ProcessingResult("Callback query обработан", false);
}

private boolean isMessageForBot(Message message) {
    String text = message.getText();
    if (text != null && text.contains("@")) {
        return true;
    }
    var reply = message.getReplyToMessage();
    return reply != null && reply.getFrom().getIsBot();
}

private String getUserName(Message message) {
    var from = message.getFrom();
    String first = from.getFirstName() != null ? from.getFirstName() : "";
    String last = from.getLastName() != null ? " " + from.getLastName() : "";
    if ((first + last).isBlank() && from.getUserName() != null) {
        return "@" + from.getUserName();
    }
    return (first + last).isBlank() ? "Unknown User" : first + last;
}

public static class ProcessingResult {
    private final String response;
    private final boolean shouldReply;

    public ProcessingResult(String response, boolean shouldReply) {
        this.response = response;
        this.shouldReply = shouldReply;
    }

    public String getResponse() {
        return response;
    }

    public boolean shouldReply() {
        return shouldReply;
    }
}
}
