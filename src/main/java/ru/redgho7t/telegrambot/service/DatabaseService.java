package ru.redgho7t.telegrambot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.redgho7t.telegrambot.entity.MessageLog;
import ru.redgho7t.telegrambot.entity.MessageLog.MessageType;
import ru.redgho7t.telegrambot.repository.MessageLogRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для работы с базой данных
 * Обеспечивает логирование всех сообщений и запросов к боту
 */
@Service
@Transactional
public class DatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    private final MessageLogRepository messageLogRepository;

    @Autowired
    public DatabaseService(MessageLogRepository messageLogRepository) {
        this.messageLogRepository = messageLogRepository;
        logger.info("DatabaseService инициализирован");
    }

    /**
     * Логирует сообщение пользователя и ответ бота
     * @param chatId ID чата
     * @param userId ID пользователя
     * @param username имя пользователя
     * @param userMessage сообщение пользователя
     * @param botResponse ответ бота
     * @param messageType тип сообщения
     * @param isGroup групповой чат или нет
     * @param responseTimeMs время ответа в миллисекундах
     * @return сохраненный объект MessageLog
     */
    public MessageLog logMessage(Long chatId, Long userId, String username,
                                 String userMessage, String botResponse,
                                 MessageType messageType, Boolean isGroup,
                                 Long responseTimeMs) {
        try {
            MessageLog messageLog = new MessageLog();
            messageLog.setChatId(chatId);
            messageLog.setUserId(userId);
            messageLog.setUsername(username);
            messageLog.setUserMessage(userMessage);
            messageLog.setBotResponse(botResponse);
            messageLog.setMessageType(messageType);
            messageLog.setIsGroup(isGroup);
            messageLog.setResponseTimeMs(responseTimeMs);
            messageLog.setCreatedAt(LocalDateTime.now());

            MessageLog saved = messageLogRepository.save(messageLog);
            logger.debug("Сохранено сообщение в БД: chatId={}, messageType={}, responseTime={}ms",
                    chatId, messageType, responseTimeMs);
            return saved;

        } catch (Exception e) {
            logger.error("Ошибка при сохранении сообщения в БД: chatId={}, error={}",
                    chatId, e.getMessage(), e);
            throw new RuntimeException("Ошибка сохранения в БД", e);
        }
    }

    /**
     * Логирует ошибку обработки сообщения
     * @param chatId ID чата
     * @param userId ID пользователя
     * @param username имя пользователя
     * @param userMessage сообщение пользователя
     * @param errorMessage сообщение об ошибке
     * @param isGroup групповой чат или нет
     * @return сохраненный объект MessageLog
     */
    public MessageLog logError(Long chatId, Long userId, String username,
                               String userMessage, String errorMessage, Boolean isGroup) {
        try {
            MessageLog messageLog = new MessageLog();
            messageLog.setChatId(chatId);
            messageLog.setUserId(userId);
            messageLog.setUsername(username);
            messageLog.setUserMessage(userMessage);
            messageLog.setMessageType(MessageType.ERROR);
            messageLog.setErrorMessage(errorMessage);
            messageLog.setIsGroup(isGroup);
            messageLog.setCreatedAt(LocalDateTime.now());

            MessageLog saved = messageLogRepository.save(messageLog);
            logger.warn("Сохранена ошибка в БД: chatId={}, error={}", chatId, errorMessage);
            return saved;

        } catch (Exception e) {
            logger.error("Критическая ошибка при сохранении ошибки в БД: {}", e.getMessage(), e);
            // Не бросаем исключение, чтобы не нарушить работу бота
            return null;
        }
    }

    /**
     * Получает историю сообщений для чата
     * @param chatId ID чата
     * @param limit максимальное количество сообщений
     * @return список сообщений
     */
    public List<MessageLog> getChatHistory(Long chatId, int limit) {
        try {
            return messageLogRepository.findRecentMessagesByChat(chatId, limit);
        } catch (Exception e) {
            logger.error("Ошибка при получении истории чата {}: {}", chatId, e.getMessage(), e);
            return List.of(); // возвращаем пустой список
        }
    }

    /**
     * Получает статистику по боту
     * @return объект со статистикой
     */
    public BotStatistics getBotStatistics() {
        try {
            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            LocalDateTime weekStart = LocalDateTime.now().minusDays(7);

            Long messagesToday = messageLogRepository.countMessagesToday(todayStart);
            Double avgResponseTime = messageLogRepository.getAverageResponseTime(weekStart);
            List<MessageLog> recentErrors = messageLogRepository.findErrorMessages(weekStart);

            return new BotStatistics(messagesToday, avgResponseTime, recentErrors.size());

        } catch (Exception e) {
            logger.error("Ошибка при получении статистики: {}", e.getMessage(), e);
            return new BotStatistics(0L, 0.0, 0);
        }
    }

    /**
     * Очищает старые логи для экономии места в БД
     * @param daysToKeep количество дней для хранения логов
     * @return количество удаленных записей
     */
    public int cleanupOldLogs(int daysToKeep) {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
            int deletedCount = messageLogRepository.deleteOldLogs(cutoffDate);
            logger.info("Удалено старых логов: {} записей (старше {} дней)", deletedCount, daysToKeep);
            return deletedCount;
        } catch (Exception e) {
            logger.error("Ошибка при очистке старых логов: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Класс для хранения статистики бота
     */
    public static class BotStatistics {
        private final Long messagesToday;
        private final Double averageResponseTime;
        private final Integer errorsLastWeek;

        public BotStatistics(Long messagesToday, Double averageResponseTime, Integer errorsLastWeek) {
            this.messagesToday = messagesToday != null ? messagesToday : 0L;
            this.averageResponseTime = averageResponseTime != null ? averageResponseTime : 0.0;
            this.errorsLastWeek = errorsLastWeek != null ? errorsLastWeek : 0;
        }

        // Getters
        public Long getMessagesToday() { return messagesToday; }
        public Double getAverageResponseTime() { return averageResponseTime; }
        public Integer getErrorsLastWeek() { return errorsLastWeek; }

        @Override
        public String toString() {
            return String.format("📊 **Статистика бота:**\n\n" +
                            "• Сообщений сегодня: %d\n" +
                            "• Среднее время ответа: %.2f мс\n" +
                            "• Ошибок за неделю: %d",
                    messagesToday, averageResponseTime, errorsLastWeek);
        }
    }
}