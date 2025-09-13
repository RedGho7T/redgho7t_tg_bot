package ru.redgho7t.telegrambot.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

/**
* Фабрика для создания клавиатур бота
*/
public class KeyboardFactory {

/**
 * Главное меню с основными командами
 */
public static InlineKeyboardMarkup getMainMenuKeyboard() {
    InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rows = new ArrayList<>();

    // Первая строка - команды
    List<InlineKeyboardButton> row1 = new ArrayList<>();
    row1.add(createButton("ℹ️ О боте", "cmd_about"));
    row1.add(createButton("❓ Помощь", "cmd_help"));
    rows.add(row1);

    // Вторая строка - статус и модели
    List<InlineKeyboardButton> row2 = new ArrayList<>();
    row2.add(createButton("📊 Статус", "cmd_status"));
    row2.add(createButton("🧠 Модели", "cmd_models"));
    rows.add(row2);

    // Третья строка - информация о создателе
    List<InlineKeyboardButton> row3 = new ArrayList<>();
    row3.add(createButton("👨‍💻 О создателе", "info_creator"));
    rows.add(row3);

    // Четвертая строка - ссылка на профиль (замените на ваш username)
    List<InlineKeyboardButton> row4 = new ArrayList<>();
    row4.add(createUrlButton("📱 Связаться с создателем", "https://t.me/redgho7t"));
    rows.add(row4);

    keyboard.setKeyboard(rows);
    return keyboard;
}

/**
 * Клавиатура с информацией о создателе
 */
public static InlineKeyboardMarkup getCreatorInfoKeyboard() {
    InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rows = new ArrayList<>();

    // Ссылки на профили
    List<InlineKeyboardButton> row1 = new ArrayList<>();
    row1.add(createUrlButton("📱 Telegram", "https://t.me/redgho7t"));
    row1.add(createUrlButton("💻 GitHub", "https://github.com/RedGho7T"));
    rows.add(row1);

    // Кнопка назад
    List<InlineKeyboardButton> row2 = new ArrayList<>();
    row2.add(createButton("◀️ Назад", "back_main"));
    rows.add(row2);

    keyboard.setKeyboard(rows);
    return keyboard;
}

/**
 * Создает обычную кнопку с callback данными
 */
private static InlineKeyboardButton createButton(String text, String callbackData) {
    InlineKeyboardButton button = new InlineKeyboardButton();
    button.setText(text);
    button.setCallbackData(callbackData);
    return button;
}

/**
 * Создает кнопку-ссылку
 */
private static InlineKeyboardButton createUrlButton(String text, String url) {
    InlineKeyboardButton button = new InlineKeyboardButton();
    button.setText(text);
    button.setUrl(url);
    return button;
}
}
