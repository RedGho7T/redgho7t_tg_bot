package ru.redgho7t.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.redgho7t.telegrambot.entity.MessageLog;
import ru.redgho7t.telegrambot.entity.MessageLog.MessageType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository для работы с логами сообщений
 * Предоставляет методы для сохранения и получения данных из БД
 */
@Repository
public interface MessageLogRepository extends JpaRepository<MessageLog, Long> {

    /**
     * Получить все сообщения для конкретного чата
     * @param chatId ID чата
     * @return список сообщений, отсортированный по времени (новые сначала)
     */
    List<MessageLog> findByChatIdOrderByCreatedAtDesc(Long chatId);

    /**
     * Получить последние N сообщений для чата
     * @param chatId ID чата
     * @param limit количество сообщений
     * @return список сообщений
     */
    @Query("SELECT ml FROM MessageLog ml WHERE ml.chatId = :chatId " +
            "ORDER BY ml.createdAt DESC LIMIT :limit")
    List<MessageLog> findRecentMessagesByChat(@Param("chatId") Long chatId,
                                              @Param("limit") int limit);

    /**
     * Получить статистику по типам сообщений для чата
     * @param chatId ID чата
     * @return количество сообщений каждого типа
     */
    @Query("SELECT ml.messageType, COUNT(ml) FROM MessageLog ml " +
            "WHERE ml.chatId = :chatId GROUP BY ml.messageType")
    List<Object[]> getMessageTypeStatsByChat(@Param("chatId") Long chatId);

    /**
     * Получить сообщения с ошибками за последние N дней
     * @param days количество дней
     * @return список сообщений с ошибками
     */
    @Query("SELECT ml FROM MessageLog ml WHERE ml.errorMessage IS NOT NULL " +
            "AND ml.createdAt >= :fromDate ORDER BY ml.createdAt DESC")
    List<MessageLog> findErrorMessages(@Param("fromDate") LocalDateTime fromDate);

    /**
     * Получить среднее время ответа бота за последние N дней
     * @param days количество дней
     * @return среднее время ответа в миллисекундах
     */
    @Query("SELECT AVG(ml.responseTimeMs) FROM MessageLog ml " +
            "WHERE ml.responseTimeMs IS NOT NULL " +
            "AND ml.createdAt >= :fromDate")
    Double getAverageResponseTime(@Param("fromDate") LocalDateTime fromDate);

    /**
     * Подсчет сообщений за сегодня
     * @param fromDate начало дня
     * @return количество сообщений
     */
    @Query("SELECT COUNT(ml) FROM MessageLog ml WHERE ml.createdAt >= :fromDate")
    Long countMessagesToday(@Param("fromDate") LocalDateTime fromDate);

    /**
     * Получить топ активных чатов
     * @param limit количество чатов
     * @return список чатов с количеством сообщений
     */
    @Query("SELECT ml.chatId, COUNT(ml) as messageCount FROM MessageLog ml " +
            "GROUP BY ml.chatId ORDER BY messageCount DESC LIMIT :limit")
    List<Object[]> getTopActiveChats(@Param("limit") int limit);

    /**
     * Поиск сообщений по ключевому слову в тексте
     * @param chatId ID чата
     * @param keyword ключевое слово
     * @return список найденных сообщений
     */
    @Query("SELECT ml FROM MessageLog ml WHERE ml.chatId = :chatId " +
            "AND (LOWER(ml.userMessage) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(ml.botResponse) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY ml.createdAt DESC")
    List<MessageLog> searchMessagesByKeyword(@Param("chatId") Long chatId,
                                             @Param("keyword") String keyword);

    /**
     * Удалить старые логи (старше N дней) для экономии места
     * @param beforeDate дата, старше которой удалять логи
     * @return количество удаленных записей
     */
    @Query("DELETE FROM MessageLog ml WHERE ml.createdAt < :beforeDate")
    int deleteOldLogs(@Param("beforeDate") LocalDateTime beforeDate);
}