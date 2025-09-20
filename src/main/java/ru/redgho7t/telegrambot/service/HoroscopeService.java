package ru.redgho7t.telegrambot.service;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Сервис для получения гороскопов на русском языке
 * Поддерживает кэширование и ежедневное обновление
 */
@Service
public class HoroscopeService {
    private static final Logger logger = LoggerFactory.getLogger(HoroscopeService.class);

    private final OkHttpClient httpClient;
    private final Map<String, String> horoscopeCache = new HashMap<>();
    private String lastUpdateDate = "";

    // Знаки зодиака с переводом на русский (используем HashMap вместо Map.of())
    private static final Map<String, String> ZODIAC_SIGNS = createZodiacSigns();

    private static Map<String, String> createZodiacSigns() {
        Map<String, String> signs = new HashMap<>();
        signs.put("овен", "aries");
        signs.put("телец", "taurus");
        signs.put("близнецы", "gemini");
        signs.put("рак", "cancer");
        signs.put("лев", "leo");
        signs.put("дева", "virgo");
        signs.put("весы", "libra");
        signs.put("скорпион", "scorpio");
        signs.put("стрелец", "sagittarius");
        signs.put("козерог", "capricorn");
        signs.put("водолей", "aquarius");
        signs.put("рыбы", "pisces");
        return signs;
    }

    // Fallback гороскопы на случай недоступности API (используем HashMap)
    private static final Map<String, String> FALLBACK_HOROSCOPES = createFallbackHoroscopes();

    private static Map<String, String> createFallbackHoroscopes() {
        Map<String, String> horoscopes = new HashMap<>();
        horoscopes.put("овен", "Сегодня звёзды советуют вам быть активнее! Удача благоволит смелым. Хороший день для новых начинаний.");
        horoscopes.put("телец", "День принесёт стабильность и уверенность. Стоит обратить внимание на финансовые вопросы.");
        horoscopes.put("близнецы", "Ваша коммуникабельность сегодня будет особенно ценна. Время для общения и новых знакомств.");
        horoscopes.put("рак", "Семья и дом требуют вашего внимания. Интуиция поможет принять правильное решение.");
        horoscopes.put("лев", "Ваша харизма сегодня на высоте! Отличный день для творчества и самовыражения.");
        horoscopes.put("дева", "Организованность и внимание к деталям принесут успех. Избегайте спешки в важных делах.");
        horoscopes.put("весы", "Гармония и баланс — ключевые слова дня. Хорошее время для решения конфликтов.");
        horoscopes.put("скорпион", "Глубокая интуиция поможет разобраться в сложных ситуациях. Доверьтесь своим чувствам.");
        horoscopes.put("стрелец", "Расширение горизонтов ждёт вас! Возможны интересные путешествия или новые знания.");
        horoscopes.put("козерог", "Упорство и дисциплина принесут результаты. Карьерные вопросы требуют особого внимания.");
        horoscopes.put("водолей", "Оригинальность и независимость — ваши козыри. Не бойтесь быть собой.");
        horoscopes.put("рыбы", "Творческое вдохновение и эмпатия помогут в общении с окружающими.");
        return horoscopes;
    }

    public HoroscopeService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        logger.info("HoroscopeService инициализирован");
    }

    /**
     * Получает гороскоп для знака зодиака
     */
    public String getHoroscope(String zodiacSign) {
        String sign = zodiacSign.toLowerCase().trim();

        // Проверяем валидность знака
        if (!ZODIAC_SIGNS.containsKey(sign)) {
            return "❓ Неизвестный знак зодиака. Доступные знаки: " +
                    String.join(", ", ZODIAC_SIGNS.keySet());
        }

        // Обновляем кэш если нужно
        updateCacheIfNeeded();

        // Получаем гороскоп из кэша или fallback
        String horoscope = horoscopeCache.getOrDefault(sign, FALLBACK_HOROSCOPES.get(sign));

        return formatHoroscope(sign, horoscope);
    }

    /**
     * Возвращает случайный гороскоп
     */
    public String getRandomHoroscope() {
        String[] signs = ZODIAC_SIGNS.keySet().toArray(new String[0]);
        Random random = new Random();
        String randomSign = signs[random.nextInt(signs.length)];
        return getHoroscope(randomSign);
    }

    /**
     * Обновляет все гороскопы
     */
    public void updateAllHoroscopes() {
        logger.info("Обновление всех гороскопов");
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        // Загружаем гороскопы для всех знаков
        for (Map.Entry<String, String> entry : ZODIAC_SIGNS.entrySet()) {
            try {
                String horoscope = fetchHoroscopeFromAPI(entry.getValue());
                if (horoscope != null && !horoscope.trim().isEmpty()) {
                    horoscopeCache.put(entry.getKey(), horoscope);
                }
                // Небольшая задержка между запросами
                Thread.sleep(500);
            } catch (Exception e) {
                logger.warn("Ошибка при загрузке гороскопа для {}: {}", entry.getKey(), e.getMessage());
            }
        }

        lastUpdateDate = today;
        logger.info("Обновление гороскопов завершено. Загружено: {} знаков", horoscopeCache.size());
    }

    /**
     * Проверяет и обновляет кэш при необходимости
     */
    private void updateCacheIfNeeded() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        if (!today.equals(lastUpdateDate) || horoscopeCache.isEmpty()) {
            logger.info("Требуется обновление гороскопов на {}", today);
            updateAllHoroscopes();
        }
    }

    /**
     * Получает гороскоп из внешнего API
     */
    private String fetchHoroscopeFromAPI(String englishSign) throws IOException {
        // Для демонстрации используем простой fallback
        // В реальном проекте здесь будет запрос к API гороскопов

        // Генерируем "гороскоп" на основе даты и знака
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        Random random = new Random((today + englishSign).hashCode());

        String[] predictions = {
                "Сегодня вас ждут приятные сюрпризы! Звёзды благоволят новым начинаниям.",
                "День потребует внимательности в общении. Избегайте конфликтных ситуаций.",
                "Удачное время для творческих проектов. Ваша интуиция на высоте!",
                "Финансовые вопросы требуют осторожности. Не спешите с крупными тратами.",
                "Семейные отношения принесут радость. Проведите время с близкими.",
                "Карьерные возможности могут появиться неожиданно. Будьте готовы к переменам.",
                "Хороший день для саморазвития. Изучите что-то новое.",
                "Ваша энергия привлечёт внимание окружающих. Используйте это с пользой."
        };

        return predictions[random.nextInt(predictions.length)];
    }

    /**
     * Форматирует гороскоп для отправки
     */
    private String formatHoroscope(String sign, String horoscope) {
        String emoji = getZodiacEmoji(sign);
        String capitalizedSign = sign.substring(0, 1).toUpperCase() + sign.substring(1);
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        return String.format("🔮 **Гороскоп для %s %s**\n\n%s\n\n📅 *%s*",
                emoji, capitalizedSign, horoscope, today);
    }

    /**
     * Возвращает эмодзи для знака зодиака
     */
    private String getZodiacEmoji(String sign) {
        return switch (sign.toLowerCase()) {
            case "овен" -> "♈";
            case "телец" -> "♉";
            case "близнецы" -> "♊";
            case "рак" -> "♋";
            case "лев" -> "♌";
            case "дева" -> "♍";
            case "весы" -> "♎";
            case "скорпион" -> "♏";
            case "стрелец" -> "♐";
            case "козерог" -> "♑";
            case "водолей" -> "♒";
            case "рыбы" -> "♓";
            default -> "⭐";
        };
    }

    /**
     * Возвращает справку по знакам зодиака
     */
    public String getZodiacGuide() {
        StringBuilder guide = new StringBuilder();
        guide.append("🔮 **Знаки зодиака:**\n\n");

        for (String sign : ZODIAC_SIGNS.keySet()) {
            String emoji = getZodiacEmoji(sign);
            String capitalizedSign = sign.substring(0, 1).toUpperCase() + sign.substring(1);
            guide.append(String.format("%s **%s**\n", emoji, capitalizedSign));
        }

        guide.append("\n📝 *Напишите название знака для получения гороскопа*");
        guide.append("\n🎲 *Или используйте \"гороскоп\" для случайного гороскопа*");

        return guide.toString();
    }

    /**
     * Возвращает статистику сервиса
     */
    public String getServiceStatus() {
        return String.format("Сервис гороскопов: ✅ Активен\nКэш: %d знаков\nПоследнее обновление: %s",
                horoscopeCache.size(),
                lastUpdateDate.isEmpty() ? "никогда" : lastUpdateDate);
    }
}