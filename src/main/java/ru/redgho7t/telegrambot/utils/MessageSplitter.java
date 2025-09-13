package ru.redgho7t.telegrambot.utils;

import java.util.ArrayList;
import java.util.List;

/**
* Утилита для разбивки длинных сообщений на части
*/
public class MessageSplitter {

private static final int MAX_MESSAGE_LENGTH = 4096;
private static final String PART_INDICATOR = "\n\n📄 Часть %d из %d";

/**
 * Разбивает длинное сообщение на части с сохранением форматирования
 * @param text исходный текст
 * @return список частей сообщения
 */
public static List<String> splitMessage(String text) {
    List<String> parts = new ArrayList<>();

    // Если сообщение помещается в лимит
    if (text.length() <= MAX_MESSAGE_LENGTH) {
        parts.add(text);
        return parts;
    }

    // Разбиваем по абзацам
    String[] paragraphs = text.split("\n\n");
    StringBuilder currentPart = new StringBuilder();

    for (String paragraph : paragraphs) {
        // Если даже один абзац слишком длинный
        if (paragraph.length() > MAX_MESSAGE_LENGTH - 100) {
            // Сохраняем текущую часть
            if (currentPart.length() > 0) {
                parts.add(currentPart.toString().trim());
                currentPart = new StringBuilder();
            }
            // Разбиваем длинный абзац по предложениям
            parts.addAll(splitLongParagraph(paragraph));
            continue;
        }

        // Проверяем, поместится ли абзац в текущую часть
        if (currentPart.length() + paragraph.length() + 2 <= MAX_MESSAGE_LENGTH - 50) {
            if (currentPart.length() > 0) {
                currentPart.append("\n\n");
            }
            currentPart.append(paragraph);
        } else {
            // Сохраняем текущую часть и начинаем новую
            if (currentPart.length() > 0) {
                parts.add(currentPart.toString().trim());
            }
            currentPart = new StringBuilder(paragraph);
        }
    }

    // Добавляем последнюю часть
    if (currentPart.length() > 0) {
        parts.add(currentPart.toString().trim());
    }

    return addPartNumbers(parts);
}

/**
 * Разбивает длинный абзац по предложениям
 */
private static List<String> splitLongParagraph(String paragraph) {
    List<String> parts = new ArrayList<>();
    String[] sentences = paragraph.split("(?<=[.!?])\\s+");
    StringBuilder currentPart = new StringBuilder();

    for (String sentence : sentences) {
        if (currentPart.length() + sentence.length() + 1 <= MAX_MESSAGE_LENGTH - 50) {
            if (currentPart.length() > 0) {
                currentPart.append(" ");
            }
            currentPart.append(sentence);
        } else {
            if (currentPart.length() > 0) {
                parts.add(currentPart.toString().trim());
            }
            currentPart = new StringBuilder(sentence);
        }
    }

    if (currentPart.length() > 0) {
        parts.add(currentPart.toString().trim());
    }

    return parts;
}

/**
 * Добавляет нумерацию частей
 */
private static List<String> addPartNumbers(List<String> parts) {
    if (parts.size() <= 1) {
        return parts;
    }

    List<String> numberedParts = new ArrayList<>();
    for (int i = 0; i < parts.size(); i++) {
        String part = parts.get(i);
        String indicator = String.format(PART_INDICATOR, i + 1, parts.size());
        numberedParts.add(part + indicator);
    }

    return numberedParts;
}
}
