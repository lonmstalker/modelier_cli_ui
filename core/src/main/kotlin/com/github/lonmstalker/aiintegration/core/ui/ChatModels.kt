package com.github.lonmstalker.aiintegration.core.ui

import kotlinx.serialization.Serializable

/**
 * Модели данных для системы чата AI интеграции.
 * Эти модели обеспечивают единообразие данных между всеми платформами.
 * 
 * @author AI CLI Integration Team
 * @since 0.0.1
 */

/**
 * Модель сообщения чата.
 * Представляет одно сообщение в диалоге между пользователем и AI ассистентом.
 * 
 * @property id Уникальный идентификатор сообщения
 * @property sender Отправитель сообщения (пользователь, ассистент или система)
 * @property content Текстовое содержимое сообщения
 * @property timestamp Unix timestamp создания сообщения в миллисекундах
 * @property type Тип сообщения для специальной обработки UI
 * @property metadata Дополнительные метаданные сообщения
 */
@Serializable
data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType = MessageType.TEXT,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Отправитель сообщения в чате.
 */
@Serializable
enum class MessageSender {
    /** Пользователь (разработчик) */
    USER,
    /** AI ассистент */
    ASSISTANT,
    /** Системные сообщения (ошибки, уведомления) */
    SYSTEM
}

/**
 * Тип сообщения для специальной обработки в UI.
 */
@Serializable
enum class MessageType {
    /** Обычное текстовое сообщение */
    TEXT,
    /** Сообщение с кодом (требует синтаксической подсветки) */
    CODE,
    /** Сообщение об ошибке */
    ERROR,
    /** Приветственное сообщение */
    WELCOME,
    /** Индикатор печати */
    TYPING_INDICATOR
}

/**
 * Полное состояние чата.
 * Используется для сохранения и восстановления состояния между сессиями.
 * 
 * @property messages История сообщений чата
 * @property isTyping Индикатор активной печати ассистента
 * @property isEnabled Доступность интерфейса чата для ввода
 * @property currentInput Текущий текст в поле ввода
 * @property selectedProvider Выбранный AI провайдер
 */
@Serializable
data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isTyping: Boolean = false,
    val isEnabled: Boolean = true,
    val currentInput: String = "",
    val selectedProvider: String? = null
)