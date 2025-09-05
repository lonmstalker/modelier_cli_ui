package com.github.lonmstalker.aiintegration.core.ui

import java.text.SimpleDateFormat
import java.util.*

/**
 * Утилиты для работы с сообщениями чата.
 * Содержит фабричные методы и вспомогательные функции для создания
 * и обработки сообщений чата.
 * 
 * @author AI CLI Integration Team
 * @since 0.0.1
 */
object ChatMessageUtils {
    
    private val defaultTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val random = Random()
    
    /**
     * Создает приветственное сообщение от AI ассистента.
     * Сообщение содержит информацию о возможностях ассистента.
     * 
     * @param localization интерфейс локализации для получения переведенных строк
     * @return приветственное сообщение
     */
    fun createWelcomeMessage(localization: ChatLocalization): ChatMessage {
        val welcomeContent = localization.getWelcomeMessage()
        
        return ChatMessage(
            id = generateMessageId(),
            sender = MessageSender.ASSISTANT,
            content = welcomeContent,
            type = MessageType.WELCOME
        )
    }
    
    /**
     * Создает сообщение пользователя.
     * 
     * @param content текст сообщения
     * @param metadata дополнительные метаданные (опционально)
     * @return сообщение пользователя
     */
    fun createUserMessage(
        content: String,
        metadata: Map<String, String> = emptyMap()
    ): ChatMessage {
        return ChatMessage(
            id = generateMessageId(),
            sender = MessageSender.USER,
            content = content.trim(),
            type = MessageType.TEXT,
            metadata = metadata
        )
    }
    
    /**
     * Создает сообщение от AI ассистента.
     * 
     * @param content текст ответа ассистента
     * @param type тип сообщения (по умолчанию TEXT)
     * @param metadata дополнительные метаданные (опционально)
     * @return сообщение ассистента
     */
    fun createAssistantMessage(
        content: String,
        type: MessageType = MessageType.TEXT,
        metadata: Map<String, String> = emptyMap()
    ): ChatMessage {
        return ChatMessage(
            id = generateMessageId(),
            sender = MessageSender.ASSISTANT,
            content = content.trim(),
            type = type,
            metadata = metadata
        )
    }
    
    /**
     * Создает сообщение об ошибке.
     * 
     * @param error текст ошибки
     * @param localization интерфейс локализации
     * @param details дополнительные детали ошибки (опционально)
     * @return системное сообщение об ошибке
     */
    fun createErrorMessage(
        error: String,
        localization: ChatLocalization,
        details: String? = null
    ): ChatMessage {
        val base = localization.getErrorMessage(error)
        val errorContent = if (details != null) {
            "$base\n\nDetails: $details"
        } else {
            base
        }
        
        return ChatMessage(
            id = generateMessageId(),
            sender = MessageSender.SYSTEM,
            content = errorContent,
            type = MessageType.ERROR
        )
    }
    
    /**
     * Создает индикатор печати ассистента.
     * Используется для показа что ассистент обрабатывает запрос.
     * 
     * @param localization интерфейс локализации
     * @return сообщение-индикатор печати
     */
    fun createTypingIndicator(localization: ChatLocalization): ChatMessage {
        return ChatMessage(
            id = TYPING_INDICATOR_ID,
            sender = MessageSender.ASSISTANT,
            content = localization.getMessage(ChatLocalizationKeys.TYPING_INDICATOR),
            type = MessageType.TYPING_INDICATOR
        )
    }
    
    /**
     * Создает сообщение с кодом.
     * Автоматически определяет тип как CODE для правильного отображения.
     * 
     * @param code текст кода
     * @param language язык программирования (опционально)
     * @param explanation объяснение кода (опционально)
     * @return сообщение с кодом
     */
    fun createCodeMessage(
        code: String,
        language: String? = null,
        explanation: String? = null
    ): ChatMessage {
        val content = buildString {
            if (explanation != null) {
                appendLine(explanation)
                appendLine()
            }
            if (language != null) {
                appendLine("```$language")
            } else {
                appendLine("```")
            }
            appendLine(code)
            append("```")
        }
        
        val metadata = mutableMapOf<String, String>()
        if (language != null) {
            metadata["language"] = language
        }
        
        return ChatMessage(
            id = generateMessageId(),
            sender = MessageSender.ASSISTANT,
            content = content,
            type = MessageType.CODE,
            metadata = metadata
        )
    }
    
    /**
     * Генерирует уникальный идентификатор сообщения.
     * Формат: timestamp_randomNumber
     * 
     * @return уникальный ID сообщения
     */
    fun generateMessageId(): String {
        return "${System.currentTimeMillis()}_${random.nextInt(1000, 9999)}"
    }
    
    /**
     * Форматирует временную метку сообщения.
     * 
     * @param timestamp Unix timestamp в миллисекундах
     * @param pattern паттерн форматирования (по умолчанию "HH:mm:ss")
     * @return отформатированная строка времени
     */
    fun formatTimestamp(timestamp: Long, pattern: String = "HH:mm:ss"): String {
        return try {
            val format = if (pattern == "HH:mm:ss") {
                defaultTimeFormat
            } else {
                SimpleDateFormat(pattern, Locale.getDefault())
            }
            format.format(Date(timestamp))
        } catch (e: Exception) {
            // Возвращаем пустую строку в случае ошибки форматирования
            ""
        }
    }
    
    /**
     * Определяет, является ли сообщение пустым или содержащим только пробелы.
     * 
     * @param message сообщение для проверки
     * @return true если сообщение пустое
     */
    fun isEmptyMessage(message: ChatMessage): Boolean {
        return message.content.trim().isEmpty()
    }
    
    /**
     * Определяет, является ли сообщение индикатором печати.
     * 
     * @param message сообщение для проверки
     * @return true если это индикатор печати
     */
    fun isTypingIndicator(message: ChatMessage): Boolean {
        return message.id == TYPING_INDICATOR_ID || 
               message.type == MessageType.TYPING_INDICATOR
    }
    
    /**
     * Группирует сообщения по дням для удобного отображения.
     * 
     * @param messages список сообщений
     * @return карта где ключ - дата, значение - список сообщений за этот день
     */
    fun groupMessagesByDay(messages: List<ChatMessage>): Map<String, List<ChatMessage>> {
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return messages.groupBy { message ->
            dayFormat.format(Date(message.timestamp))
        }
    }
    
    /**
     * Фильтрует сообщения по типу отправителя.
     * 
     * @param messages список сообщений
     * @param sender тип отправителя для фильтрации
     * @return отфильтрованный список сообщений
     */
    fun filterMessagesBySender(
        messages: List<ChatMessage>,
        sender: MessageSender
    ): List<ChatMessage> {
        return messages.filter { it.sender == sender }
    }
    
    /**
     * Возвращает последние N сообщений из списка.
     * 
     * @param messages список всех сообщений
     * @param count количество последних сообщений
     * @return список последних сообщений
     */
    fun getLastMessages(messages: List<ChatMessage>, count: Int): List<ChatMessage> {
        return if (messages.size <= count) {
            messages
        } else {
            messages.takeLast(count)
        }
    }
    
    /**
     * Константы для специальных сообщений.
     */

    /** Специальный ID для индикатора печати */
    const val TYPING_INDICATOR_ID = "__typing_indicator__"
    
    /** Максимальная длина отображаемого сообщения */
    const val MAX_MESSAGE_DISPLAY_LENGTH = 10000
    
    /** Максимальная длина сообщения для экспорта */
    const val MAX_MESSAGE_EXPORT_LENGTH = 50000
}
