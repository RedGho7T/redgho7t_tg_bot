package ru.redgho7t.telegrambot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Сервис для ежедневного автоматического обновления данных
 * Обновляет гороскопы, погоду и анекдоты по расписанию
 */
@Service
@EnableScheduling
public class DailyUpdateService {
    private static final Logger logger = LoggerFactory.getLogger(DailyUpdateService.class);

    @Autowired
    private HoroscopeService horoscopeService;

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private JokeService jokeService;

    /**
     * Ежедневное обновление в полночь
     * Обновляет гороскопы и кэш погоды
     */
    @Scheduled(cron = "0 0 0 * * *") // каждый день в 00:00
    public void dailyUpdate() {
        logger.info("🕛 Начинаем ежедневное обновление данных");

        try {
            // Обновляем гороскопы на новый день
            horoscopeService.updateAllHoroscopes();
            logger.info("✅ Гороскопы обновлены");

            // Обновляем кэш погоды
            weatherService.updateWeatherCache();
            logger.info("✅ Кэш погоды обновлён");

            logger.info("🎉 Ежедневное обновление завершено успешно");

        } catch (Exception e) {
            logger.error("❌ Ошибка при ежедневном обновлении: {}", e.getMessage(), e);
        }
    }

    /**
     * Обновление анекдотов каждые 6 часов
     */
    @Scheduled(cron = "0 0 */6 * * *") // каждые 6 часов в 00, 06, 12, 18
    public void updateJokes() {
        logger.info("😄 Обновление кэша анекдотов");

        try {
            jokeService.refreshJokeCache();
            logger.info("✅ Кэш анекдотов обновлён");
        } catch (Exception e) {
            logger.error("❌ Ошибка при обновлении анекдотов: {}", e.getMessage(), e);
        }
    }

    /**
     * Обновление кэша погоды каждые 30 минут
     */
    @Scheduled(cron = "0 */30 * * * *") // каждые 30 минут
    public void updateWeatherCache() {
        logger.debug("🌤️ Плановое обновление кэша погоды");

        try {
            weatherService.updateWeatherCache();
            logger.debug("✅ Кэш погоды обновлён");
        } catch (Exception e) {
            logger.warn("⚠️ Ошибка при обновлении кэша погоды: {}", e.getMessage());
        }
    }

    /**
     * Еженедельная очистка старых данных
     */
    @Scheduled(cron = "0 0 2 * * SUN") // каждое воскресенье в 02:00
    public void weeklyCleanup() {
        logger.info("🧹 Еженедельная очистка данных");

        try {
            // Можно добавить очистку логов, статистики и т.д.
            logger.info("✅ Еженедельная очистка завершена");
        } catch (Exception e) {
            logger.error("❌ Ошибка при еженедельной очистке: {}", e.getMessage(), e);
        }
    }
}