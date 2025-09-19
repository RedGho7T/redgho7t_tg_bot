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
 * Добавлена поддержка анекдотов через JokeService.
 */
public class MessageProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);

    private final ResponseTemplates responseTemplates;
    private final Map<String, String> keywordTemplates;
    private final GoogleAiService googleAiService;
    private final JokeService jokeService; // Сервис анекдотов

    // Ключевые слова для разных реакций (в нижнем регистре)
    private static final List<String> BOT_TRIGGER_WORDS = Arrays.asList(
            "бот", "ботяра", "bot", "botyara", "попи", "popi",
            "java", "жаби", "jabi", "го", "go", "айди", "ai"
    );

    // Специальные ключевые слова и их ответы
    private static final Map<String, String> SPECIAL_RESPONSES = new HashMap<>();
    static {
        SPECIAL_RESPONSES.put("попи", "🐸 Попи жив и здоров!");
        SPECIAL_RESPONSES.put("popi", "🐸 Popi is alive and well!");
        SPECIAL_RESPONSES.put("java", "☕ Java - лучший язык программирования!");
        SPECIAL_RESPONSES.put("жаби", "☕ Жаба рулит!");
        SPECIAL_RESPONSES.put("jabi", "☕ Java rocks!");
        SPECIAL_RESPONSES.put("го", "🚀 Поехали!");
        SPECIAL_RESPONSES.put("go", "🚀 Let's go!");
        SPECIAL_RESPONSES.put("айди", "🤖 AI Bot к вашим услугам!");
        SPECIAL_RESPONSES.put("ai", "🤖 AI Bot at your service!");
        SPECIAL_RESPONSES.put("ботяра", "🤖 Ботяра на связи!");
        SPECIAL_RESPONSES.put("botyara", "🤖 Botyara is online!");
    }

    /**
     * Конструктор процессора сообщений.
     *
     * @param googleAiService сервис для работы с Google Gemini API
     * @param jokeService сервис для получения анекдотов
     */
    public MessageProcessor(GoogleAiService googleAiService, JokeService jokeService) {
        this.googleAiService = googleAiService;
        this.jokeService = jokeService;
        this.responseTemplates = new ResponseTemplates();
        this.keywordTemplates = initializeKeywordTemplates();
        logger.info("MessageProcessor инициализирован с GoogleAiService и JokeService");
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

        // Шаблоны для анекдотов
        templates.put("анекдот", "🤣 Анекдот для поднятия настроения:");
        templates.put("шутка", "😄 Вот забавная шутка:");
        templates.put("расскажи анекдот", "🎭 Держи анекдот:");
        templates.put("joke", "😂 Here's a joke for you:");
        templates.put("tell me a joke", "🤣 Here's a funny one:");

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

        // 1. Проверяем команды первыми (работают везде)
        if (messageText.startsWith("/")) {
            return processCommand(messageText, chatId, isGroup);
        }

        // 2. Проверяем специальные ключевые слова (работают везде)
        ProcessingResult specialResponse = checkSpecialKeywords(messageText);
        if (specialResponse != null) {
            return specialResponse;
        }

        // 3. Проверяем запросы на анекдоты (работают везде)
        if (isJokeRequest(messageText)) {
            return processJokeRequest(messageText);
        }

        // 4. ТОЛЬКО для AI-запросов проверяем, обращаются ли к боту в группах
        if (isGroup && !isMessageForBot(message)) {
            return new ProcessingResult("", false);
        }

        return processUserMessage(messageText, userName);
    }

    /**
     * Проверяет специальные ключевые слова и возвращает соответствующий ответ
     */
    private ProcessingResult checkSpecialKeywords(String messageText) {
        String lowerText = messageText.toLowerCase();

        for (String keyword : BOT_TRIGGER_WORDS) {
            if (lowerText.contains(keyword)) {
                String response = SPECIAL_RESPONSES.get(keyword);
                if (response != null) {
                    logger.info("Найдено специальное ключевое слово: {}", keyword);
                    return new ProcessingResult(response, true, false);
                }

                // Если нет специального ответа, возвращаем общий ответ
                return new ProcessingResult("🤖 " + keyword + " - я тебя слышу!", true, false);
            }
        }

        return null; // Ключевые слова не найдены
    }

    /**
     * Проверяет, является ли сообщение запросом на анекдот
     */
    private boolean isJokeRequest(String messageText) {
        String lowerText = messageText.toLowerCase().trim();

        // Точные совпадения
        if (lowerText.equals("анекдот") || lowerText.equals("шутка") ||
                lowerText.equals("joke") || lowerText.equals("анекдотик")) {
            return true;
        }

        // Фразы с запросом анекдота
        String[] jokePatterns = {
                "расскажи анекдот", "расскажи шутку", "давай анекдот",
                "хочу анекдот", "дай анекдот", "покажи анекдот",
                "tell me a joke", "tell a joke", "give me a joke",
                "анекдот плз", "анекдот пожалуйста", "шутку пожалуйста"
        };

        for (String pattern : jokePatterns) {
            if (lowerText.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Обрабатывает запрос на анекдот
     */
    private ProcessingResult processJokeRequest(String messageText) {
        try {
            String joke = jokeService.getRandomJoke();
            String template = getJokeTemplate(messageText.toLowerCase());
            String response = template + "\n\n" + joke + "\n\n😊 Хотите ещё? Просто напишите \"анекдот\"!";

            logger.info("Отправлен анекдот по ключевому слову");
            return new ProcessingResult(response, true, false);
        } catch (Exception e) {
            logger.error("Ошибка при получении анекдота по ключевому слову: {}", e.getMessage(), e);
            return new ProcessingResult("❌ Извините, не удалось получить анекдот. Попробуйте позже.", true, false);
        }
    }

    /**
     * Получает подходящий шаблон для анекдота в зависимости от запроса
     */
    private String getJokeTemplate(String messageText) {
        if (messageText.contains("joke")) {
            return "😂 Here's a joke for you:";
        } else if (messageText.contains("шутк")) {
            return "😄 Вот забавная шутка:";
        } else {
            return "🎭 Держи анекдот:";
        }
    }

    private ProcessingResult processCommand(String command, Long chatId, boolean isGroup) {
        String cmd = command.toLowerCase().split("\\s+")[0];
        return switch (cmd) {
            case "/start" -> new ProcessingResult(responseTemplates.getMainMenuMessage(), true, false);
            case "/help" -> new ProcessingResult(responseTemplates.getHelpMessage(), true, false);
            case "/about" -> new ProcessingResult(responseTemplates.getAboutMessage(), true, false);
            case "/status" -> processStatusCommand();
            case "/models" -> new ProcessingResult(responseTemplates.getModelsMessage(), true, false);
            case "/joke", "/анекдот" -> processJokeCommand(); // Команды для анекдотов
            default -> {
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

    /**
     * Обрабатывает команду получения анекдота
     */
    private ProcessingResult processJokeCommand() {
        try {
            String joke = jokeService.getRandomJoke();
            String response = "🎭 **Анекдот дня:**\n\n" + joke + "\n\n😄 Надеюсь, поднял настроение!";
            logger.info("Отправлен анекдот по команде");
            return new ProcessingResult(response, true, false);
        } catch (Exception e) {
            logger.error("Ошибка при получении анекдота: {}", e.getMessage(), e);
            return new ProcessingResult("❌ Извините, не удалось получить анекдот. Попробуйте позже.", true, false);
        }
    }

    private ProcessingResult processStatusCommand() {
        boolean isAvailable = googleAiService.isApiAvailable();
        boolean jokeServiceAvailable = jokeService.isServiceAvailable();
        String status = isAvailable ? "✅ Онлайн" : "❌ Недоступен";
        String jokeStatus = jokeServiceAvailable ? "✅ Онлайн" : "❌ Недоступен";

        String message = String.format(
                "🤖 **Статус бота:**\n\nAI API: %s\nСервис анекдотов: %s\nВремя работы: активен\nВерсия: 1.0.0",
                status, jokeStatus
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
            case "cmd_about" -> new ProcessingResult(responseTemplates.getAboutMessage(), true, false);
            case "cmd_help" -> new ProcessingResult(responseTemplates.getHelpMessage(), true, false);
            case "cmd_status" -> processStatusCommand();
            case "cmd_models" -> new ProcessingResult(responseTemplates.getModelsMessage(), true, false);
            case "info_creator" -> new ProcessingResult(responseTemplates.getCreatorInfoMessage(), true, true);
            case "back_main" -> new ProcessingResult(responseTemplates.getBackToMainMessage(), true, false);
            case "cmd_joke" -> processJokeCommand(); // Callback для анекдотов
            default -> new ProcessingResult("❓ Неизвестная команда", true, false);
        };
    }

    private boolean isMessageForBot(Message message) {
        String text = message.getText();
        // Проверяем упоминание бота через @username
        if (text != null && text.contains("@")) {
            return true;
        }

        // Проверяем, является ли это ответом на сообщение бота
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