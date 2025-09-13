package ru.redgho7t.telegrambot.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
* Entity для логирования всех сообщений бота
* Сохраняет входящие сообщения пользователей и ответы бота
*/
@Entity
@Table(name = "message_logs")
public class MessageLog {

@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@Column(name = "chat_id", nullable = false)
private Long chatId;

@Column(name = "user_id")
private Long userId;

@Column(name = "username")
private String username;

@Column(name = "user_message", columnDefinition = "TEXT")
private String userMessage;

@Column(name = "bot_response", columnDefinition = "TEXT")
private String botResponse;

@Column(name = "message_type")
@Enumerated(EnumType.STRING)
private MessageType messageType;

@Column(name = "created_at", nullable = false)
private LocalDateTime createdAt;

@Column(name = "response_time_ms")
private Long responseTimeMs;

@Column(name = "is_group")
private Boolean isGroup;

@Column(name = "error_message")
private String errorMessage;

// Конструкторы
public MessageLog() {
    this.createdAt = LocalDateTime.now();
}

public MessageLog(Long chatId, Long userId, String username, String userMessage,
                  String botResponse, MessageType messageType, Boolean isGroup) {
    this();
    this.chatId = chatId;
    this.userId = userId;
    this.username = username;
    this.userMessage = userMessage;
    this.botResponse = botResponse;
    this.messageType = messageType;
    this.isGroup = isGroup;
}

// Getters и Setters
public Long getId() {
    return id;
}

public void setId(Long id) {
    this.id = id;
}

public Long getChatId() {
    return chatId;
}

public void setChatId(Long chatId) {
    this.chatId = chatId;
}

public Long getUserId() {
    return userId;
}

public void setUserId(Long userId) {
    this.userId = userId;
}

public String getUsername() {
    return username;
}

public void setUsername(String username) {
    this.username = username;
}

public String getUserMessage() {
    return userMessage;
}

public void setUserMessage(String userMessage) {
    this.userMessage = userMessage;
}

public String getBotResponse() {
    return botResponse;
}

public void setBotResponse(String botResponse) {
    this.botResponse = botResponse;
}

public MessageType getMessageType() {
    return messageType;
}

public void setMessageType(MessageType messageType) {
    this.messageType = messageType;
}

public LocalDateTime getCreatedAt() {
    return createdAt;
}

public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
}

public Long getResponseTimeMs() {
    return responseTimeMs;
}

public void setResponseTimeMs(Long responseTimeMs) {
    this.responseTimeMs = responseTimeMs;
}

public Boolean getIsGroup() {
    return isGroup;
}

public void setIsGroup(Boolean isGroup) {
    this.isGroup = isGroup;
}

public String getErrorMessage() {
    return errorMessage;
}

public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
}

@Override
public String toString() {
        return "MessageLog{" +
                "id=" + id +
                ", chatId=" + chatId +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", messageType=" + messageType +
                ", createdAt=" + createdAt +
                ", responseTimeMs=" + responseTimeMs +
                ", isGroup=" + isGroup +
                '}';
}

/**
 * Enum для типов сообщений
 */
public enum MessageType {
    COMMAND,        // Команда (/start, /help, и т.д.)
    AI_REQUEST,     // Обычный вопрос к AI
    SPECIAL_KEYWORD, // Специальные слова (бот, жаби, и т.д.)
    CALLBACK,       // Нажатие на кнопку
    ERROR           // Ошибка
}
}