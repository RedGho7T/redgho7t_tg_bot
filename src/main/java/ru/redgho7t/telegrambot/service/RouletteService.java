package ru.redgho7t.telegrambot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Сервис рулетки для генерации случайных чисел от 1 до 777
 * Поддерживает анимацию через Telegram Dice API
 */
@Service
public class RouletteService {
    private static final Logger logger = LoggerFactory.getLogger(RouletteService.class);

    private static final int MIN_NUMBER = 1;
    private static final int MAX_NUMBER = 777;

    private final Random random;
    private final List<RouletteResult> history;

    public RouletteService() {
        this.random = new Random();
        this.history = new ArrayList<>();
        logger.info("RouletteService инициализирован");
    }

    /**
     * Генерирует случайное число от 1 до 777
     */
    public RouletteResult spin() {
        int number = random.nextInt(MAX_NUMBER) + MIN_NUMBER;
        RouletteResult result = new RouletteResult(number);

        // Сохраняем в историю (последние 100 результатов)
        synchronized (history) {
            history.add(result);
            if (history.size() > 100) {
                history.remove(0);
            }
        }

        logger.info("Рулетка: выпало число {}", number);
        return result;
    }

    /**
     * Возвращает отформатированное сообщение с результатом
     */
    public String formatResult(RouletteResult result) {
        int number = result.getNumber();

        StringBuilder message = new StringBuilder();
        message.append("🎰 **Рулетка удачи!**\n\n");
        message.append(String.format("🔢 **Ваше число: %d**\n\n", number));

        // Добавляем специальное сообщение в зависимости от числа
        if (number == 777) {
            message.append("🎉 **ДЖЕКПОТ!** 🎉\n");
            message.append("Поздравляем! Вы выиграли максимальный приз!\n");
            message.append("💎 Невероятная удача! ✨");
        } else if (number >= 700) {
            message.append("⭐ **Отличный результат!** ⭐\n");
            message.append("Вам очень повезло!\n");
            message.append("🍀 Удача определенно на вашей стороне!");
        } else if (number >= 500) {
            message.append("👍 **Хороший результат!** 👍\n");
            message.append("Неплохая удача!\n");
            message.append("🎲 Попробуйте ещё раз!");
        } else if (number >= 300) {
            message.append("🎯 **Средний результат**\n");
            message.append("Не расстраивайтесь!\n");
            message.append("🔄 Следующий раз повезёт больше!");
        } else if (number >= 100) {
            message.append("🌟 **Начальная удача**\n");
            message.append("Это только начало!\n");
            message.append("💪 Продолжайте играть!");
        } else {
            message.append("🍀 **Попробуйте ещё раз!** 🍀\n");
            message.append("Удача любит настойчивых!\n");
            message.append("🎰 Крутите рулетку снова!");
        }

        return message.toString();
    }

    /**
     * Возвращает статистику игрока
     */
    public String getStatistics() {
        if (history.isEmpty()) {
            return "📊 **Статистика рулетки**\n\nПока нет результатов. Начните игру командой \"lucky\"!";
        }

        int total = history.size();
        int jackpots = 0;
        int excellent = 0; // 700+
        int good = 0; // 500+
        int sum = 0;
        int max = 0;
        int min = MAX_NUMBER;

        for (RouletteResult result : history) {
            int number = result.getNumber();
            sum += number;

            if (number > max) max = number;
            if (number < min) min = number;

            if (number == 777) {
                jackpots++;
            } else if (number >= 700) {
                excellent++;
            } else if (number >= 500) {
                good++;
            }
        }

        double average = (double) sum / total;

        StringBuilder stats = new StringBuilder();
        stats.append("📊 **Статистика рулетки**\n\n");
        stats.append(String.format("🎲 **Всего игр:** %d\n", total));
        stats.append(String.format("📈 **Среднее число:** %.1f\n", average));
        stats.append(String.format("🔺 **Максимум:** %d\n", max));
        stats.append(String.format("🔻 **Минимум:** %d\n", min));
        stats.append("\n**Распределение результатов:**\n");

        if (jackpots > 0) {
            stats.append(String.format("🎉 **Джекпоты (777):** %d\n", jackpots));
        }
        stats.append(String.format("⭐ **Отличные (700+):** %d\n", excellent));
        stats.append(String.format("👍 **Хорошие (500+):** %d\n", good));
        stats.append(String.format("🎯 **Остальные:** %d\n", total - jackpots - excellent - good));

        if (jackpots > 0) {
            double jackpotChance = (double) jackpots / total * 100;
            stats.append(String.format("\n🎰 **Шанс джекпота:** %.2f%%", jackpotChance));
        }

        return stats.toString();
    }

    /**
     * Очищает историю результатов
     */
    public void clearHistory() {
        synchronized (history) {
            history.clear();
        }
        logger.info("История рулетки очищена");
    }

    /**
     * Возвращает последние результаты
     */
    public String getRecentResults(int count) {
        if (history.isEmpty()) {
            return "🎰 Нет результатов для показа";
        }

        int size = Math.min(count, history.size());
        List<RouletteResult> recent = history.subList(history.size() - size, history.size());

        StringBuilder results = new StringBuilder();
        results.append(String.format("🎲 **Последние %d результатов:**\n\n", size));

        for (int i = recent.size() - 1; i >= 0; i--) {
            RouletteResult result = recent.get(i);
            String emoji = getNumberEmoji(result.getNumber());
            results.append(String.format("%s %d\n", emoji, result.getNumber()));
        }

        return results.toString();
    }

    /**
     * Возвращает эмодзи для числа
     */
    private String getNumberEmoji(int number) {
        if (number == 777) {
            return "🎉";
        } else if (number >= 700) {
            return "⭐";
        } else if (number >= 500) {
            return "👍";
        } else if (number >= 300) {
            return "🎯";
        } else {
            return "🎲";
        }
    }

    /**
     * Класс для хранения результата рулетки
     */
    public static class RouletteResult {
        private final int number;
        private final long timestamp;

        public RouletteResult(int number) {
            this.number = number;
            this.timestamp = System.currentTimeMillis();
        }

        public int getNumber() {
            return number;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public boolean isJackpot() {
            return number == 777;
        }

        public boolean isExcellent() {
            return number >= 700;
        }

        public boolean isGood() {
            return number >= 500;
        }
    }
}