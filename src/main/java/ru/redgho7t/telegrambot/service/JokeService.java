package ru.redgho7t.telegrambot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Сервис для получения случайных анекдотов из различных источников
 */
@Service
public class JokeService {

    private static final Logger logger = LoggerFactory.getLogger(JokeService.class);
    private final Random random = new Random();

    // Локальная база анекдотов (fallback на случай проблем с внешними API)
    private final List<String> localJokes = Arrays.asList(
            "Программист приходит домой:\n— Дорогая, я купил хлеб!\n— А где?\n— В переменной bread.",

            "— Доктор, я боюсь операции под наркозом.\n— Не волнуйтесь, я тоже в первый раз.",

            "Встречаются два программиста:\n— Как дела?\n— Да вот, переменную ищу уже два дня.\n— А как называется?\n— Не помню...",

            "Жена программиста просит:\n— Дорогой, сходи в магазин, купи хлеб, а если будут яйца — возьми десяток.\nВозвращается он с 10 батонами хлеба:\n— Яйца были.",

            "— Папа, что такое рекурсия?\n— Подожди, спрошу у дедушки. Дедушка, что такое рекурсия?\n— Подожди, спрошу у прадедушки...",

            "Тестировщик заходит в бар. Заказывает пиво. Заказывает 2 пива. Заказывает 0 пив. Заказывает -1 пиво. Заказывает слона. Бар работает отлично.\nЗаходит реальный пользователь и спрашивает, где туалет. Бар загорается.",

            "— Сколько программистов нужно, чтобы закрутить лампочку?\n— Ни одного. Это аппаратная проблема.",

            "Система работает? Не трогай!\nСистема не работает? Перезагрузи!\nНе помогает? Переустанови!\nВсё равно не работает? Скажи, что так и должно быть.",

            "— Почему у программистов путаются Хеллоуин и Рождество?\n— Потому что Oct 31 == Dec 25!",

            "Комментарий в коде: 'Этот код работает, не знаю как и почему, но работает. НЕ ТРОГАТЬ!'"
    );

    /**
     * Получает случайный анекдот из всех доступных источников
     * @return текст анекдота
     */
    public String getRandomJoke() {
        try {
            // Пытаемся получить анекдот из внешних источников
            String externalJoke = getExternalJoke();
            if (externalJoke != null && !externalJoke.trim().isEmpty()) {
                return externalJoke;
            }
        } catch (Exception e) {
            logger.warn("Ошибка при получении анекдота из внешнего источника: {}", e.getMessage());
        }

        // Если внешние источники недоступны, используем локальную базу
        return getLocalJoke();
    }

    /**
     * Получает анекдот из локальной базы
     * @return случайный анекдот из локальной коллекции
     */
    private String getLocalJoke() {
        logger.debug("Используем локальную базу анекдотов");
        return localJokes.get(random.nextInt(localJokes.size()));
    }

    /**
     * Пытается получить анекдот из внешних источников
     * @return текст анекдота или null в случае ошибки
     */
    private String getExternalJoke() {
        // Список API для получения анекдотов (в порядке приоритета)
        String[] apiUrls = {
                "https://official-joke-api.appspot.com/random_joke", // English jokes
                "https://v2.jokeapi.dev/joke/Programming?lang=en&type=single", // Programming jokes
                "https://icanhazdadjoke.com/" // Dad jokes
        };

        for (String apiUrl : apiUrls) {
            try {
                String joke = fetchJokeFromApi(apiUrl);
                if (joke != null && !joke.trim().isEmpty()) {
                    logger.debug("Получен анекдот из внешнего API: {}", apiUrl);
                    return joke;
                }
            } catch (Exception e) {
                logger.debug("Ошибка при обращении к API {}: {}", apiUrl, e.getMessage());
            }
        }

        return null;
    }

    /**
     * Выполняет HTTP-запрос к API для получения анекдота
     * @param apiUrl URL API
     * @return текст анекдота или null
     */
    private String fetchJokeFromApi(String apiUrl) {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "TelegramBot/1.0");
            connection.setConnectTimeout(5000); // 5 секунд
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {

                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    return parseJokeResponse(response.toString(), apiUrl);
                }
            } else {
                logger.warn("HTTP {} от API: {}", responseCode, apiUrl);
            }
        } catch (Exception e) {
            logger.warn("Ошибка при запросе к API {}: {}", apiUrl, e.getMessage());
        }

        return null;
    }

    /**
     * Парсит ответ от API в зависимости от его формата
     * @param jsonResponse JSON-ответ от API
     * @param apiUrl URL источника (для определения формата)
     * @return распарсенный текст анекдота
     */
    private String parseJokeResponse(String jsonResponse, String apiUrl) {
        try {
            if (apiUrl.contains("official-joke-api")) {
                // Парсим формат: {"setup":"...", "punchline":"..."}
                String setup = extractJsonValue(jsonResponse, "setup");
                String punchline = extractJsonValue(jsonResponse, "punchline");
                if (setup != null && punchline != null) {
                    return setup + "\n" + punchline;
                }
            } else if (apiUrl.contains("jokeapi.dev")) {
                // Парсим формат: {"joke":"..."} или {"setup":"...", "delivery":"..."}
                String joke = extractJsonValue(jsonResponse, "joke");
                if (joke != null) {
                    return joke;
                }

                String setup = extractJsonValue(jsonResponse, "setup");
                String delivery = extractJsonValue(jsonResponse, "delivery");
                if (setup != null && delivery != null) {
                    return setup + "\n" + delivery;
                }
            } else if (apiUrl.contains("icanhazdadjoke")) {
                // Парсим формат: {"joke":"..."}
                String joke = extractJsonValue(jsonResponse, "joke");
                if (joke != null) {
                    return joke;
                }
            }
        } catch (Exception e) {
            logger.warn("Ошибка парсинга ответа от {}: {}", apiUrl, e.getMessage());
        }

        return null;
    }

    /**
     * Простой парсер для извлечения значений из JSON
     * @param json JSON строка
     * @param key ключ для поиска
     * @return значение или null
     */
    private String extractJsonValue(String json, String key) {
        try {
            String searchPattern = "\"" + key + "\":\"";
            int startIndex = json.indexOf(searchPattern);
            if (startIndex == -1) {
                return null;
            }

            startIndex += searchPattern.length();
            int endIndex = json.indexOf("\"", startIndex);

            if (endIndex == -1) {
                return null;
            }

            String value = json.substring(startIndex, endIndex);
            // Декодируем escape-последовательности
            return value.replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\\\", "\\");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Проверяет доступность сервиса анекдотов
     * @return true если сервис работает
     */
    public boolean isServiceAvailable() {
        // Локальная база всегда доступна
        return true;
    }
}