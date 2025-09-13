package ru.redgho7t.telegrambot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "telegram.bot")
public class BotConfig {
    private static final Logger logger = LoggerFactory.getLogger(BotConfig.class);

    private String token;
    private String username;
    private String googleApiKey;

    // Spring автоматически заполнит поля из application.properties

    public String getBotToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getBotUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getGoogleApiKey() { return googleApiKey; }
    public void setGoogleApiKey(String googleApiKey) { this.googleApiKey = googleApiKey; }

    public boolean isValid() {
        return token != null && !token.isBlank()
                && username != null && !username.isBlank()
                && googleApiKey != null && !googleApiKey.isBlank();
    }
}
