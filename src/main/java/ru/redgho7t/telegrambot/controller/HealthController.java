package ru.redgho7t.telegrambot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.redgho7t.telegrambot.service.DatabaseService;

import java.util.HashMap;
import java.util.Map;

/**
 * REST контроллер для проверки работоспособности бота
 * Необходим для Railway и других хостинговых платформ
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @Autowired
    private DatabaseService databaseService;

    /**
     * Простая проверка работоспособности
     * GET /api/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Проверяем подключение к БД
            DatabaseService.BotStatistics stats = databaseService.getBotStatistics();

            response.put("status", "UP");
            response.put("database", "connected");
            response.put("bot", "running");
            response.put("messages_today", stats.getMessagesToday());
            response.put("avg_response_time", stats.getAverageResponseTime());
            response.put("errors_last_week", stats.getErrorsLastWeek());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("error", e.getMessage());
            response.put("database", "disconnected");

            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * Подробная информация о боте
     * GET /api/info
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> response = new HashMap<>();

        response.put("name", "Telegram AI Bot");
        response.put("version", "1.0.0");
        response.put("description", "Умный Telegram бот с AI и базой данных");
        response.put("features", new String[]{
                "Google Gemini AI integration",
                "PostgreSQL database logging",
                "Message splitting",
                "Interactive keyboards",
                "Special keyword reactions",
                "Error logging and statistics"
        });

        return ResponseEntity.ok(response);
    }

    /**
     * Корневой путь - простое приветствие
     * GET /
     */
    @GetMapping("/")
    public ResponseEntity<String> root() {
        return ResponseEntity.ok("🤖 Telegram AI Bot is running! Check /api/health for status.");
    }
}