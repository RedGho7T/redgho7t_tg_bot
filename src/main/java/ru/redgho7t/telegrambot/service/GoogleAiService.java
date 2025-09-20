package ru.redgho7t.telegrambot.service;

import okhttp3.*;
import com.google.gson.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Сервис для взаимодействия с Google Gemini API через Google AI Studio.
 * ИСПРАВЛЕНО: Добавлен @Service для Spring DI
 */
@Service
public class GoogleAiService {
    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String MODEL = "gemini-2.5-flash";

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String apiKey;

    public GoogleAiService(@Value("${telegram.bot.google-api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    /**
     * Отправляет запрос к модели Gemini и возвращает текст ответа.
     */
    public String sendRequest(String userMessage) throws IOException {
        String url = BASE_URL + MODEL + ":generateContent?key=" + apiKey;
        JsonObject requestBody = createRequestBody(userMessage);

        RequestBody body = RequestBody.create(
                gson.toJson(requestBody),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("x-goog-api-key", apiKey)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                throw new IOException("HTTP " + response.code() + ": " + errorBody);
            }

            return parseResponse(response.body().string());
        }
    }

    /**
     * Создаёт JSON-тело запроса для Gemini.
     */
    private JsonObject createRequestBody(String userMessage) {
        JsonObject part = new JsonObject();
        part.addProperty("text", userMessage);

        JsonArray parts = new JsonArray();
        parts.add(part);

        JsonObject content = new JsonObject();
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject requestBody = new JsonObject();
        requestBody.add("contents", contents);

        return requestBody;
    }

    /**
     * Парсит ответ от Gemini API и возвращает содержимое первого фрагмента.
     */
    private String parseResponse(String responseBody) throws IOException {
        try {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            JsonArray candidates = json.getAsJsonArray("candidates");

            if (candidates != null && candidates.size() > 0) {
                JsonObject first = candidates.get(0).getAsJsonObject();
                JsonObject content = first.getAsJsonObject("content");
                JsonArray parts = content.getAsJsonArray("parts");

                if (parts != null && parts.size() > 0) {
                    return parts.get(0).getAsJsonObject().get("text").getAsString();
                }
            }

            throw new IOException("Неожиданный формат ответа: " + responseBody);
        } catch (JsonSyntaxException e) {
            throw new IOException("Ошибка парсинга JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Проверяет доступность Gemini API (здесь простой заглушечный вызов).
     */
    public boolean isApiAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}