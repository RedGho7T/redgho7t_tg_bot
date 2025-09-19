package ru.redgho7t.telegrambot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Сервис для получения случайных русских анекдотов с реальными API
 */
@Service
public class JokeService {

    private static final Logger logger = LoggerFactory.getLogger(JokeService.class);
    private final Random random = new Random();

    // Локальная база русских анекдотов (fallback)
    private final List<String> localJokes = Arrays.asList(
            "— Доктор, меня постоянно мучают кошмары про футбол!\n— Принимайте эти таблетки перед сном.\n— А это поможет?\n— Не знаю, но Аршавин принимал.",

            "Встречаются два программиста:\n— Привет! Как дела? Как жена?\n— Спасибо, откатился на предыдущую версию.",

            "Объявление: \"Продам гараж в связи с покупкой квартиры. Гараж находится в спальном районе.\"",

            "— Почему у тебя жена такая нервная?\n— Да я сам не понимаю. Вчера прихожу домой в 4 утра, а она мне устраивает сцену, что я якобы пропал на три дня.",

            "Хорошо быть пессимистом. Либо ты прав, либо приятно удивлен.",

            "— Алло, это служба знакомств?\n— Да.\n— А правда, что у вас есть девушка, которая ищет мужчину с квартирой и машиной?\n— Да, правда.\n— А можете дать её телефон?\n— А у вас есть квартира и машина?\n— Нет.\n— Тогда зачем вам её телефон?\n— Хочу с ней подружиться. У нас много общего.",

            "Встретились два бизнесмена:\n— Как дела?\n— Да в последнее время хуже не придумаешь. Вчера жена выиграла в лотерею миллион долларов...\n— Ты что, с ума сошёл? Это же здорово!\n— Да, но она ещё не знает, что мы развелись два дня назад!",

            "Идёт мужик по пустыне, видит — лежит лампа. Потёр, вылезает джинн:\n— Исполню три твоих желания!\n— Хочу быть самым богатым!\n— Готово!\n— Хочу быть самым умным!\n— Готово!\n— Хочу быть самым красивым!\n— Готово!\nПревращается мужик в женщину.",

            "— Дорогой, а помнишь, ты обещал жениться на самой красивой девушке в мире?\n— Помню.\n— А почему женился на мне?\n— А ты видела хоть одну девушку красивее тебя, которая согласилась бы за меня выйти?",

            "Приходит мужик к врачу:\n— Доктор, у меня проблемы с памятью.\n— Когда это началось?\n— Что началось?",

            "Звонок в службу такси:\n— Алло, можно машину?\n— Куда?\n— На дачу.\n— А где дача?\n— В деревне.\n— А где деревня?\n— А вы что, географии не знаете?\n— Знаю.\n— Ну вот и езжайте!",

            "Разговор двух подруг:\n— Представляешь, мой муж похудел на 10 кг!\n— Как?\n— Я его выгнала.",

            "Учитель спрашивает Вовочку:\n— Если у твоего папы есть 10 яблок, и он даст тебе 3, сколько яблок у тебя будет?\n— Не знаю.\n— Как не знаешь? 10 минус 3!\n— А вы моего папу не знаете!",

            "— Милый, купи мне что-нибудь с бриллиантами!\n— Вот тебе колода карт.",

            "Пациент жалуется врачу:\n— Доктор, у меня очень плохая память!\n— И когда вы это заметили?\n— Что заметил?",

            "— Дорогая, я принёс хорошие и плохие новости.\n— Начни с хороших.\n— Airbag в машине работает отлично!",

            "Сидит программист, кодит. Жена кричит:\n— Дорогой, вынеси мусор!\n— Минуточку, допишу функцию.\nЧерез час:\n— Дорогой, ты мусор вынес?\n— Да, в функцию вынес.",

            "— Почему программисты путают Рождество и Хеллоуин?\n— Потому что 31 Oct = 25 Dec!",

            "Жена говорит мужу:\n— Дорогой, наш сын такой же, как ты!\n— Умный?\n— Нет, в шахматы играть не умеет, но всё равно переворачивает доску.",

            "— Доктор, у меня странная болезнь — я постоянно вижу спрайты и иконки!\n— Это пиксельная лихорадка. Попейте чаю в низком разрешении."
    );

    /**
     * Получает случайный анекдот из всех доступных источников
     * @return текст анекдота
     */
    public String getRandomJoke() {
        // Сначала пытаемся получить из внешних API
        try {
            String externalJoke = getExternalJoke();
            if (externalJoke != null && !externalJoke.trim().isEmpty() && externalJoke.length() > 10) {
                logger.info("Получен анекдот из внешнего API");
                return externalJoke;
            }
        } catch (Exception e) {
            logger.warn("Ошибка при получении анекдота из внешнего источника: {}", e.getMessage());
        }

        // Fallback на локальную базу
        String localJoke = getLocalJoke();
        logger.debug("Использована локальная база анекдотов");
        return localJoke;
    }

    /**
     * Получает анекдот из локальной базы
     * @return случайный анекдот из локальной коллекции
     */
    private String getLocalJoke() {
        return localJokes.get(random.nextInt(localJokes.size()));
    }

    /**
     * Пытается получить анекдот из внешних источников
     * @return текст анекдота или null в случае ошибки
     */
    private String getExternalJoke() {
        // Список русскоязычных API (в порядке приоритета)
        String[] apiUrls = {
                "http://rzhunemogu.ru/RandJSON.aspx?CType=1"  // RzhuneMogu.ru API
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
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (TelegramBot/1.0)");
            connection.setConnectTimeout(3000); // 3 секунды
            connection.setReadTimeout(3000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Читаем с правильной кодировкой для русского текста
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

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
     * @param response ответ от API
     * @param apiUrl URL источника
     * @return распарсенный текст анекдота
     */
    private String parseJokeResponse(String response, String apiUrl) {
        try {
            if (apiUrl.contains("rzhunemogu.ru")) {
                // Парсим RzhuneMogu: {"content":"текст анекдота"}
                String content = extractJsonValue(response, "content");
                if (content != null && content.length() > 10) {
                    // Очищаем HTML теги и лишние символы
                    return cleanJokeText(content);
                }
            }
        } catch (Exception e) {
            logger.warn("Ошибка парсинга ответа от {}: {}", apiUrl, e.getMessage());
        }

        return null;
    }

    /**
     * Очищает текст анекдота от HTML тегов и нормализует
     * @param text исходный текст
     * @return очищенный текст
     */
    private String cleanJokeText(String text) {
        if (text == null) return null;

        return text
                .replaceAll("<[^>]*>", "") // Удаляем HTML теги
                .replaceAll("&quot;", "\"") // Декодируем кавычки
                .replaceAll("&amp;", "&")   // Декодируем амперсанд
                .replaceAll("&lt;", "<")    // Декодируем <
                .replaceAll("&gt;", ">")    // Декодируем >
                .replaceAll("\\\\n", "\n")  // Нормализуем переводы строк
                .replaceAll("\\\\r", "")    // Удаляем \\r
                .replaceAll("\\\\t", " ")   // Заменяем табы на пробелы
                .replaceAll("\\s+", " ")    // Убираем лишние пробелы
                .trim();
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

            // Обрабатываем escaped кавычки
            while (endIndex > 0 && json.charAt(endIndex - 1) == '\\') {
                endIndex = json.indexOf("\"", endIndex + 1);
            }

            if (endIndex == -1) {
                return null;
            }

            return json.substring(startIndex, endIndex);
        } catch (Exception e) {
            logger.warn("Ошибка парсинга JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Проверяет доступность сервиса анекдотов
     * @return true если сервис работает
     */
    public boolean isServiceAvailable() {
        return true; // Локальная база всегда доступна
    }

    /**
     * Получает статистику анекдотов
     * @return информация о доступных анекдотах
     */
    public String getJokeStats() {
        int localCount = localJokes.size();
        boolean apiAvailable = testExternalApi();

        if (apiAvailable) {
            return String.format("📊 Доступно %d локальных анекдотов + тысячи из внешних API", localCount);
        } else {
            return String.format("📊 Доступно %d русских анекдотов в локальной базе", localCount);
        }
    }

    /**
     * Тестирует доступность внешних API
     * @return true если хотя бы один API доступен
     */
    private boolean testExternalApi() {
        try {
            String testJoke = getExternalJoke();
            return testJoke != null && !testJoke.trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}