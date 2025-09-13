package ru.redgho7t.telegrambot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.redgho7t.telegrambot.utils.ResponseTemplates;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

/**
 * Сервис для обработки сообщений от пользователей.
 * Заменён PerplexityService на GoogleAiService для взаимодействия с Gemini API.
 */
public class MessageProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);

    private final ResponseTemplates responseTemplates;
    private final Map<String, String> keywordTemplates;
    private final GoogleAiService googleAiService;

    // Ключевые слова для разных реакций (в нижнем регистре)
    private static final List<String> BOT_TRIGGER_WORDS = Arrays.asList(
            "бот", "ботяра", "bot", "botyara", "botik", "chlenix", "bobi", "botniy"
    );

    private static final List<String> POPI_TRIGGER_WORDS = Arrays.asList(
            "попи", "popi", "пупстерс", "попикс", "попа"
    );

    private static final List<String> JAVA_TRIGGER_WORDS = Arrays.asList(
            "java", "jabi"
    );

    private static final List<String> GO_TRIGGER_WORDS = Arrays.asList(
            "go", "го", "гоу"
    );
    private static final List<String> JABI_TRIGGER_WORDS = Arrays.asList(
            "jabi", "JABI","жаби","jabbi","JABA","ЖАБИ"
    );

    /**
     * Конструктор процессора сообщений.
     *
     * @param googleAiService сервис для работы с Google Gemini AI
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

    /**
     * Обрабатывает входящее обновление (сообщение или callback).
     */
    public ProcessingResult processUpdate(Update update) {
        try {
            if (update.hasMessage()) {
                return processMessage(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                return processCallbackQuery(update.getCallbackQuery());
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

        // Проверяем команды первыми
        if (messageText.startsWith("/")) {
            return processCommand(messageText, chatId, isGroup);
        }

        // Проверяем специальные ключевые слова
        ProcessingResult specialResponse = checkSpecialKeywords(messageText);
        if (specialResponse != null) {
            return specialResponse;
        }

        // Для групп проверяем, обращаются ли к боту
        if (isGroup && !isMessageForBot(message)) {
            return new ProcessingResult("", false);
        }

        return processUserMessage(messageText, userName);
    }

    /**
     * Проверяет специальные ключевые слова и возвращает соответствующий ответ
     */
    private ProcessingResult checkSpecialKeywords(String messageText) {
        // Разбиваем на слова: все подряд non-letters как разделители
        String[] tokens = messageText
                .toLowerCase()
                .split("[^\\p{L}]+");  // \p{L} — любая буква Unicode

        for (String token : tokens) {
            // обращение к боту
            if (BOT_TRIGGER_WORDS.contains(token)) {
                return new ProcessingResult(responseTemplates.getBotResponseMessage(), true, false);
            }
            // попи
            if (POPI_TRIGGER_WORDS.contains(token)) {
                return new ProcessingResult(responseTemplates.getPopiMessage(), true, false);
            }
            for (String trigger : JABI_TRIGGER_WORDS) {
                if (Arrays.asList(tokens).contains(trigger)) {
                    logger.info("Обнаружено ключевое слово JABI: {}", trigger);
                    return new ProcessingResult(responseTemplates.getJabiMessage(), true, false);
                }
            }
            // Java
            if (JAVA_TRIGGER_WORDS.contains(token)) {
                return new ProcessingResult(responseTemplates.getJavaMessage(), true, false);
            }
            // Go
            if (GO_TRIGGER_WORDS.contains(token)) {
                return new ProcessingResult(responseTemplates.getGoMessage(), true, false);
            }
        }
        return null;
    }

    /**
     * Проверяет, содержит ли текст слово как отдельное слово (не часть другого слова)
     */
    private boolean containsWord(String text, String word) {
        // Используем регулярное выражение для поиска целого слова
        String pattern = "\\b" + word.toLowerCase() + "\\b";
        return text.matches(".*" + pattern + ".*");
    }

    private ProcessingResult processCommand(String command, Long chatId, boolean isGroup) {
        // Убираем аргументы
        String cmdPart = command.toLowerCase().split("\\s+")[0];
        // Обрезаем суффикс вида @botusername
        String cmd = cmdPart.contains("@") ? cmdPart.substring(0, cmdPart.indexOf("@")) : cmdPart;
        return switch (cmd) {
            case "/start"  -> new ProcessingResult(responseTemplates.getMainMenuMessage(), true, false);
            case "/help"   -> new ProcessingResult(responseTemplates.getHelpMessage(), true, false);
            case "/about"  -> new ProcessingResult(responseTemplates.getAboutMessage(), true, false);
            case "/status" -> processStatusCommand();
            case "/models" -> new ProcessingResult(responseTemplates.getModelsMessage(), true, false);
            default        -> {
                if (isGroup) {
                    yield new ProcessingResult("", false);
                } else {
                    yield new ProcessingResult(
                            "❓ Неизвестная команда. Используйте /help для просмотра доступных команд.", true, false
                    );
                }
            }
        };
    }

    private ProcessingResult processStatusCommand() {
        boolean isAvailable = googleAiService.isApiAvailable();
        String status = isAvailable ? "✅ Онлайн" : "❌ Недоступен";
        String message = String.format(
                "🤖 **Статус бота:**\n\nAI API: %s\nВремя работы: активен\nВерсия: 1.0.0", status
        );
        return new ProcessingResult(message, true, false);
    }

    private ProcessingResult processUserMessage(String messageText, String userName) {
        String enhanced = enhanceMessageWithTemplate(messageText);
        try {
            String aiResponse = googleAiService.sendRequest(enhanced);
            logger.debug("Ответ AI для {}: {}...", userName,
                    aiResponse.substring(0, Math.min(aiResponse.length(), 100)));
            return new ProcessingResult(aiResponse, true, false);
        } catch (Exception e) {
            logger.error("Ошибка при запросе к Google AI: {}", e.getMessage(), e);
            return new ProcessingResult("❌ Ошибка при обращении к AI. Попробуйте позже.", true, false);
        }
    }

    private String enhanceMessageWithTemplate(String messageText) {
        String lower = messageText.toLowerCase();
        for (var entry : keywordTemplates.entrySet()) {
            if (lower.contains(entry.getKey())) {
                String template = entry.getValue();
                String rest = messageText.substring(lower.indexOf(entry.getKey())
                        + entry.getKey().length()).trim();
                return rest.isEmpty() ? template + messageText : template + rest;
            }
        }
        return messageText;
    }

    private ProcessingResult processCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        logger.info("Получен callback: {}", data);

        return switch (data) {
            case "cmd_about"    -> new ProcessingResult(responseTemplates.getAboutMessage(), true, false);
            case "cmd_help"     -> new ProcessingResult(responseTemplates.getHelpMessage(), true, false);
            case "cmd_status"   -> processStatusCommand();
            case "cmd_models"   -> new ProcessingResult(responseTemplates.getModelsMessage(), true, false);
            case "info_creator" -> new ProcessingResult(responseTemplates.getCreatorInfoMessage(), true, true);
            case "back_main"    -> new ProcessingResult(responseTemplates.getBackToMainMessage(), true, false);
            default             -> new ProcessingResult("❓ Неизвестная команда", true, false);
        };
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
        String first = from.getFirstName()  != null ? from.getFirstName()  : "";
        String last  = from.getLastName()   != null ? " " + from.getLastName() : "";
        if ((first + last).isBlank() && from.getUserName() != null) {
            return "@" + from.getUserName();
        }
        return (first + last).isBlank() ? "Unknown User" : first + last;
    }

    public static class ProcessingResult {
        private final String response;
        private final boolean shouldReply;
        private final boolean showCreatorKeyboard;

        public ProcessingResult(String response, boolean shouldReply) {
            this(response, shouldReply, false);
        }

        public ProcessingResult(String response, boolean shouldReply, boolean showCreatorKeyboard) {
            this.response = response;
            this.shouldReply = shouldReply;
            this.showCreatorKeyboard = showCreatorKeyboard;
        }

        public String getResponse() { return response; }
        public boolean shouldReply() { return shouldReply; }
        public boolean shouldShowCreatorKeyboard() { return showCreatorKeyboard; }
    }
}
