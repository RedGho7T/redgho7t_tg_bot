# Telegram AI Bot

**Telegram bot** that responds to messages using **Google Gemini AI** (Google AI Studio).

## Description

This bot is written in Java:
- Receives messages from users via the Telegram Bots API.
- Sends requests to the Gemini model via the Google AI Studio (REST API).
- Processes the responses and sends them back to the chat.
- Supports keyword templates (greetings, requests, comparisons, etc.).
- Logs the processing process using SLF4J + Logback.

## Features

- `/start`, `/help`, `/about`, `/status`, `/models` 
- Automatic switching between private chats and groups. 
- Dynamic keyword template substitution. 
- Customizable logging (console, file, levels).
