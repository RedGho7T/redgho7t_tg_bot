package ru.redgho7t.telegrambot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
 * ИСПРАВЛЕНО: Добавлен @Service для работы Spring DI
 */
@Service
public class MessageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);

    private final ResponseTemplates responseTemplates;
    private final Map<String, String> keywordTemplates;
    private final GoogleAiService googleAiService;

    // ИСПРАВЛЕНО: Инъекция через конструктор вместо @Autowired полей
    private final JokeService jokeService;
    private final WeatherService weatherService;
    private final HoroscopeService horoscopeService;
    private final RouletteService rouletteService;

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
            "jabi", "JABI", "жаби", "jabbi", "JABA", "ЖАБИ"
    );

    // НОВЫЕ КЛЮЧЕВЫЕ СЛОВА для дополнительных функций
    private static final List<String> JOKE_TRIGGER_WORDS = Arrays.asList(
            "анекдот", "шутка", "прикол", "joke", "юмор", "смешное", "рассмеши"
    );

    private static final List<String> WEATHER_TRIGGER_WORDS = Arrays.asList(
            "погода", "weather", "прогноз", "температура", "дождь", "снег", "солнце"
    );

    private static final List<String> HOROSCOPE_TRIGGER_WORDS = Arrays.asList(
            "гороскоп", "horoscope", "предсказание", "зодиак", "знак", "астрология",
            "овен", "телец", "близнецы", "рак", "лев", "дева", "весы",
            "скорпион", "стрелец", "козерог", "водолей", "рыбы"
    );

    private static final List<String> ROULETTE_TRIGGER_WORDS = Arrays.asList(
            "lucky", "рулетка", "удача", "везение", "лотерея", "случайное", "число", "фортуна"
    );

    /**
     * ИСПРАВЛЕННЫЙ КОНСТРУКТОР с инъекцией всех сервисов
     */
    @Autowired
    public MessageProcessor(GoogleAiService googleAiService,
                            JokeService jokeService,
                            WeatherService weatherService,
                            HoroscopeService horoscopeService,
                            RouletteService rouletteService) {
        this.googleAiService = googleAiService;
        this.jokeService = jokeService;
        this.weatherService = weatherService;
        this.horoscopeService = horoscopeService;
        this.rouletteService = rouletteService;

        this.responseTemplates = new ResponseTemplates();
        this.keywordTemplates = initializeKeywordTemplates();

        logger.info("✅ MessageProcessor инициализирован со всеми сервисами");
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

        // 1. Проверяем команды первыми
        if (messageText.startsWith("/")) {
            return processCommand(messageText, chatId, isGroup);
        }

        // 2. НОВАЯ ЛОГИКА: Проверяем специальные функции ПЕРЕД существующими ключевыми словами
        ProcessingResult newFunctionResult = checkNewFunctions(messageText);
        if (newFunctionResult != null) {
            return newFunctionResult;
        }

        // 3. Проверяем существующие специальные ключевые слова
        ProcessingResult specialResponse = checkSpecialKeywords(messageText);
        if (specialResponse != null) {
            return specialResponse;
        }

        // 4. Для групп проверяем, обращаются ли к боту
        if (isGroup && !isMessageForBot(message)) {
            return new ProcessingResult("", false);
        }

        // 5. AI-ассистент как fallback
        return processUserMessage(messageText, userName);
    }

    /**
     * ИСПРАВЛЕННЫЙ МЕТОД: Проверяет новые функции (анекдоты, погода, гороскопы, рулетка)
     */
    private ProcessingResult checkNewFunctions(String messageText) {
        String[] tokens = messageText.toLowerCase().split("[^\\p{L}]+");

        for (String token : tokens) {
            // 1. АНЕКДОТЫ (приоритет 1)
            if (JOKE_TRIGGER_WORDS.contains(token)) {
                logger.info("🎭 Запрос анекдота: {}", token);
                try {
                    String joke = jokeService.getRandomJoke();
                    String response = responseTemplates.getJokeIntroMessage() + joke;
                    return new ProcessingResult(response, true, false);
                } catch (Exception e) {
                    logger.error("❌ Ошибка при получении анекдота: {}", e.getMessage());
                    return new ProcessingResult(responseTemplates.getJokeErrorMessage(), true, false);
                }
            }

            // 2. РУЛЕТКА (приоритет 2)
            if (ROULETTE_TRIGGER_WORDS.contains(token)) {
                logger.info("🎰 Запрос рулетки: {}", token);
                try {
                    RouletteService.RouletteResult result = rouletteService.spin();
                    String response = rouletteService.formatResult(result);
                    // Указываем специальный флаг для анимации
                    return new ProcessingResult(response, true, false, true);
                } catch (Exception e) {
                    logger.error("❌ Ошибка при работе рулетки: {}", e.getMessage());
                    return new ProcessingResult("❌ Рулетка временно не работает. Попробуйте позже!", true, false);
                }
            }

            // 3. ПОГОДА (приоритет 3)
            if (WEATHER_TRIGGER_WORDS.contains(token)) {
                logger.info("🌤️ Запрос погоды: {}", token);
                try {
                    String weather = weatherService.getWeather();
                    return new ProcessingResult(weather, true, false);
                } catch (Exception e) {
                    logger.error("❌ Ошибка при получении погоды: {}", e.getMessage());
                    return new ProcessingResult(responseTemplates.getWeatherErrorMessage(), true, false);
                }
            }

            // 4. ГОРОСКОПЫ (приоритет 4)
            if (HOROSCOPE_TRIGGER_WORDS.contains(token)) {
                logger.info("🔮 Запрос гороскопа: {}", token);
                try {
                    // Если это знак зодиака, возвращаем гороскоп для него
                    if (isZodiacSign(token)) {
                        String horoscope = horoscopeService.getHoroscope(token);
                        return new ProcessingResult(horoscope, true, false);
                    } else {
                        // Иначе возвращаем случайный гороскоп
                        String horoscope = horoscopeService.getRandomHoroscope();
                        return new ProcessingResult(horoscope, true, false);
                    }
                } catch (Exception e) {
                    logger.error("❌ Ошибка при получении гороскопа: {}", e.getMessage());
                    return new ProcessingResult("❌ Гороскоп временно недоступен. Попробуйте позже!", true, false);
                }
            }
        }

        return null; // Новые функции не обнаружены
    }

    /**
     * Проверяет, является ли слово знаком зодиака
     */
    private boolean isZodiacSign(String word) {
        List<String> zodiacSigns = Arrays.asList(
                "овен", "телец", "близнецы", "рак", "лев", "дева",
                "весы", "скорпион", "стрелец", "козерог", "водолей", "рыбы"
        );
        return zodiacSigns.contains(word.toLowerCase());
    }

    /**
     * Проверяет специальные ключевые слова и возвращает соответствующий ответ
     */
    private ProcessingResult checkSpecialKeywords(String messageText) {
        // Разбиваем на слова: все подряд non-letters как разделители
        String[] tokens = messageText.toLowerCase().split("[^\\p{L}]+");

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

    private ProcessingResult processCommand(String command, Long chatId, boolean isGroup) {
        // Убираем аргументы
        String cmdPart = command.toLowerCase().split("\\s+")[0];
        // Обрезаем суффикс вида @botusername
        String cmd = cmdPart.contains("@") ? cmdPart.substring(0, cmdPart.indexOf("@")) : cmdPart;

        return switch (cmd) {
            case "/start" -> new ProcessingResult(responseTemplates.getMainMenuMessage(), true, false);
            case "/help" -> new ProcessingResult(responseTemplates.getHelpMessage(), true, false);
            case "/about" -> new ProcessingResult(responseTemplates.getAboutMessage(), true, false);
            case "/status" -> processStatusCommand();
            case "/models" -> new ProcessingResult(responseTemplates.getModelsMessage(), true, false);

            // ИСПРАВЛЕННЫЕ НОВЫЕ КОМАНДЫ
            case "/joke", "/анекдот" -> {
                try {
                    String joke = jokeService.getRandomJoke();
                    yield new ProcessingResult(responseTemplates.getJokeIntroMessage() + joke, true, false);
                } catch (Exception e) {
                    logger.error("❌ Ошибка команды /joke: {}", e.getMessage());
                    yield new ProcessingResult(responseTemplates.getJokeErrorMessage(), true, false);
                }
            }

            case "/weather", "/погода" -> {
                try {
                    String weather = weatherService.getWeather();
                    yield new ProcessingResult(weather, true, false);
                } catch (Exception e) {
                    logger.error("❌ Ошибка команды /weather: {}", e.getMessage());
                    yield new ProcessingResult(responseTemplates.getWeatherErrorMessage(), true, false);
                }
            }

            case "/horoscope", "/гороскоп" -> {
                try {
                    String horoscope = horoscopeService.getRandomHoroscope();
                    yield new ProcessingResult(horoscope, true, false);
                } catch (Exception e) {
                    logger.error("❌ Ошибка команды /horoscope: {}", e.getMessage());
                    yield new ProcessingResult("❌ Гороскоп временно недоступен.", true, false);
                }
            }

            case "/lucky", "/рулетка" -> {
                try {
                    RouletteService.RouletteResult result = rouletteService.spin();
                    String response = rouletteService.formatResult(result);
                    yield new ProcessingResult(response, true, false, true);
                } catch (Exception e) {
                    logger.error("❌ Ошибка команды /lucky: {}", e.getMessage());
                    yield new ProcessingResult("❌ Рулетка временно не работает.", true, false);
                }
            }

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

    private ProcessingResult processStatusCommand() {
        boolean isAvailable = googleAiService.isApiAvailable();
        String status = isAvailable ? "✅ Онлайн" : "❌ Недоступен";

        // Добавляем статус новых сервисов
        StringBuilder message = new StringBuilder();
        message.append(String.format("🤖 **Статус бота:**\\n\\nAI API: %s\\n", status));

        // Статус дополнительных сервисов
        message.append("\\n**Дополнительные сервисы:**\\n");
        message.append("😄 Анекдоты: ✅ Активен\\n");
        message.append(weatherService.getServiceStatus()).append("\\n");
        message.append(horoscopeService.getServiceStatus()).append("\\n");
        message.append("🎰 Рулетка: ✅ Активна\\n");
        message.append("\\nВремя работы: активен\\nВерсия: 2.0.0");

        return new ProcessingResult(message.toString(), true, false);
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
            default -> new ProcessingResult("❓ Неизвестная команда", true, false);
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
        private final boolean needsRouletteAnimation; // НОВОЕ ПОЛЕ

        public ProcessingResult(String response, boolean shouldReply) {
            this(response, shouldReply, false, false);
        }

        public ProcessingResult(String response, boolean shouldReply, boolean showCreatorKeyboard) {
            this(response, shouldReply, showCreatorKeyboard, false);
        }

        // НОВЫЙ КОНСТРУКТОР с поддержкой анимации рулетки
        public ProcessingResult(String response, boolean shouldReply, boolean showCreatorKeyboard, boolean needsRouletteAnimation) {
            this.response = response;
            this.shouldReply = shouldReply;
            this.showCreatorKeyboard = showCreatorKeyboard;
            this.needsRouletteAnimation = needsRouletteAnimation;
        }

        public String getResponse() {
            return response;
        }

        public boolean shouldReply() {
            return shouldReply;
        }

        public boolean shouldShowCreatorKeyboard() {
            return showCreatorKeyboard;
        }

        public boolean needsRouletteAnimation() {
            return needsRouletteAnimation;
        } // НОВЫЙ ГЕТТЕР
    }
}