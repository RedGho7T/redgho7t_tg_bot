package ru.redgho7t.telegrambot.service;

import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Сервис для получения анекдотов с русскоязычных API
 * Особое внимание уделено корректной обработке кодировки
 */
@Service
public class JokeService {
    private static final Logger logger = LoggerFactory.getLogger(JokeService.class);

    private static final String RSS_URL = "https://www.anekdot.ru/rss/export_j.xml";
    private static final String BACKUP_RSS_URL = "https://www.anekdot.ru/rss/random_j.xml";

    private final OkHttpClient httpClient;
    private final List<String> jokesCache;
    private long lastUpdateTime = 0;
    private static final long CACHE_DURATION = 6 * 60 * 60 * 1000; // 6 часов

    // Fallback анекдоты на случай недоступности API
    private static final List<String> FALLBACK_JOKES = Arrays.asList(
            "— Доктор, я забываю всё!\n— Когда это началось?\n— Что началось?",
            "Программист идёт в магазин. Жена говорит:\n— Купи хлеб, а если будут яйца — возьми десяток.\nПрограммист возвращается с десятью буханками хлеба:\n— Яйца были!",
            "— Чем Java отличается от JavaScript?\n— Тем же, чем Car от Carpet.",
            "— Почему программисты путают Хэллоуин и Рождество?\n— Потому что Oct 31 = Dec 25",
            "Встречаются два программиста:\n— Как дела?\n— Как в игре: то ли баг, то ли фича."
    );

    public JokeService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        this.jokesCache = new ArrayList<>();
        logger.info("JokeService инициализирован");
    }

    /**
     * Получает случайный анекдот
     */
    public String getRandomJoke() {
        // Проверяем кэш и обновляем при необходимости
        refreshCacheIfNeeded();

        // Если кэш пуст, используем fallback
        if (jokesCache.isEmpty()) {
            logger.warn("Кэш анекдотов пуст, используем fallback");
            return getRandomFallbackJoke();
        }

        // Возвращаем случайный анекдот из кэша
        Random random = new Random();
        String joke = jokesCache.get(random.nextInt(jokesCache.size()));

        logger.debug("Возвращен анекдот длиной {} символов", joke.length());
        return joke;
    }

    /**
     * Принудительно обновляет кэш анекдотов
     */
    public void refreshJokeCache() {
        logger.info("Принудительное обновление кэша анекдотов");
        loadJokesFromAPI();
    }

    /**
     * Проверяет и обновляет кэш при необходимости
     */
    private void refreshCacheIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (jokesCache.isEmpty() || (currentTime - lastUpdateTime) > CACHE_DURATION) {
            logger.info("Требуется обновление кэша анекдотов");
            loadJokesFromAPI();
        }
    }

    /**
     * Загружает анекдоты из API
     */
    private void loadJokesFromAPI() {
        try {
            List<String> newJokes = fetchJokesFromRSS(RSS_URL);

            // Если основной источник не дал результатов, пробуем резервный
            if (newJokes.isEmpty()) {
                logger.warn("Основной RSS не дал результатов, пробуем резервный");
                newJokes = fetchJokesFromRSS(BACKUP_RSS_URL);
            }

            if (!newJokes.isEmpty()) {
                synchronized (jokesCache) {
                    jokesCache.clear();
                    jokesCache.addAll(newJokes);
                    lastUpdateTime = System.currentTimeMillis();
                }
                logger.info("Загружено {} анекдотов в кэш", newJokes.size());
            } else {
                logger.error("Не удалось загрузить анекдоты ни из одного источника");
            }

        } catch (Exception e) {
            logger.error("Ошибка при загрузке анекдотов: {}", e.getMessage(), e);
        }
    }

    /**
     * Получает анекдоты из RSS с правильной обработкой кодировки
     */
    private List<String> fetchJokesFromRSS(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept-Charset", "UTF-8")
                .addHeader("User-Agent", "Mozilla/5.0 (compatible; TelegramBot/1.0)")
                .addHeader("Accept", "application/rss+xml, application/xml, text/xml")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + response.message());
            }

            if (response.body() == null) {
                throw new IOException("Пустой ответ от сервера");
            }

            // Получаем данные как байты для правильной обработки кодировки
            byte[] bytes = response.body().bytes();
            String xmlContent = new String(bytes, StandardCharsets.UTF_8);

            // Если кодировка некорректная, пробуем Windows-1251
            if (containsEncodingIssues(xmlContent)) {
                xmlContent = new String(bytes, "Windows-1251");
                logger.debug("Использована кодировка Windows-1251");
            }

            return parseJokesFromXML(xmlContent);
        }
    }

    /**
     * Парсит анекдоты из XML контента
     */
    private List<String> parseJokesFromXML(String xmlContent) {
        List<String> jokes = new ArrayList<>();

        try {
            // Очищаем XML от недопустимых символов
            xmlContent = cleanXmlContent(xmlContent);

            // Парсим XML с помощью JSoup
            Document doc = Jsoup.parse(xmlContent, "", org.jsoup.parser.Parser.xmlParser());
            Elements items = doc.select("item");

            for (Element item : items) {
                Element description = item.selectFirst("description");
                if (description != null) {
                    String joke = description.text();
                    joke = cleanJokeText(joke);

                    if (isValidJoke(joke)) {
                        jokes.add(joke);
                    }
                }
            }

            logger.debug("Распарсено {} анекдотов из XML", jokes.size());

        } catch (Exception e) {
            logger.error("Ошибка при парсинге XML: {}", e.getMessage(), e);
        }

        return jokes;
    }

    /**
     * Очищает XML от недопустимых символов
     */
    private String cleanXmlContent(String xmlContent) {
        // Удаляем недопустимые XML символы
        xmlContent = xmlContent.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");

        // Заменяем HTML entities
        xmlContent = xmlContent.replace("&nbsp;", " ");
        xmlContent = xmlContent.replace("&amp;", "&");
        xmlContent = xmlContent.replace("&lt;", "<");
        xmlContent = xmlContent.replace("&gt;", ">");
        xmlContent = xmlContent.replace("&quot;", "\"");

        return xmlContent;
    }

    /**
     * Очищает текст анекдота от HTML тегов и лишних символов
     */
    private String cleanJokeText(String joke) {
        // Удаляем HTML теги
        joke = Jsoup.parse(joke).text();

        // Заменяем multiple пробелы на один
        joke = joke.replaceAll("\\s+", " ");

        // Убираем лишние переносы строк
        joke = joke.replaceAll("\\n\\s*\\n", "\n");

        // Обрезаем пробелы по краям
        joke = joke.trim();

        return joke;
    }

    /**
     * Проверяет, является ли текст валидным анекдотом
     */
    private boolean isValidJoke(String joke) {
        if (joke == null || joke.trim().isEmpty()) {
            return false;
        }

        // Проверяем минимальную длину
        if (joke.length() < 10) {
            return false;
        }

        // Проверяем максимальную длину (Telegram ограничение)
        if (joke.length() > 4000) {
            return false;
        }

        // Проверяем, что это не системное сообщение
        String lowerJoke = joke.toLowerCase();
        if (lowerJoke.contains("error") ||
                lowerJoke.contains("ошибка") ||
                lowerJoke.contains("404") ||
                lowerJoke.contains("не найден")) {
            return false;
        }

        return true;
    }

    /**
     * Проверяет, есть ли проблемы с кодировкой
     */
    private boolean containsEncodingIssues(String text) {
        // Ищем характерные признаки неправильной кодировки
        return text.contains("Ð") ||
                text.contains("Ñ") ||
                text.contains("ÐŸ") ||
                text.contains("â€") ||
                text.contains("Â");
    }

    /**
     * Возвращает случайный fallback анекдот
     */
    private String getRandomFallbackJoke() {
        Random random = new Random();
        return FALLBACK_JOKES.get(random.nextInt(FALLBACK_JOKES.size()));
    }

    /**
     * Возвращает статистику кэша
     */
    public String getCacheStats() {
        return String.format("Кэш анекдотов: %d элементов, последнее обновление: %s",
                jokesCache.size(),
                lastUpdateTime > 0 ? new Date(lastUpdateTime).toString() : "никогда");
    }
}