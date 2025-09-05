package com.github.lonmstalker.aiintegration.core.ui

/**
 * События пользовательского интерфейса чата.
 * Используется для связи между UI компонентами и бизнес-логикой.
 * 
 * @author AI CLI Integration Team
 * @since 0.0.1
 */

/**
 * Базовый класс для всех событий чата.
 * Используется паттерн sealed class для type-safe обработки событий.
 */
sealed class ChatEvent {
    
    /**
     * Пользователь отправил сообщение.
     * @property message текст сообщения от пользователя
     * @property context дополнительный контекст для обработки сообщения
     */
    data class MessageSent(
        val message: String,
        val context: Map<String, Any> = emptyMap()
    ) : ChatEvent()
    
    /**
     * Получено новое сообщение для отображения.
     * @property message объект сообщения для добавления в чат
     */
    data class MessageReceived(
        val message: ChatMessage
    ) : ChatEvent()
    
    /**
     * Запрос на очистку истории чата.
     */
    object ClearChat : ChatEvent()
    
    /**
     * Начало индикации печати ассистентом.
     */
    object StartTyping : ChatEvent()
    
    /**
     * Остановка индикации печати ассистентом.
     */
    object StopTyping : ChatEvent()
    
    /**
     * Изменение конфигурации UI.
     * @property config новая конфигурация интерфейса
     */
    data class ConfigChanged(
        val config: ChatUIConfig
    ) : ChatEvent()
    
    /**
     * Произошла ошибка в процессе работы чата.
     * @property error основное сообщение об ошибке
     * @property details дополнительные детали ошибки (опционально)
     * @property code код ошибки для программной обработки
     */
    data class ErrorOccurred(
        val error: String,
        val details: String? = null,
        val code: String? = null
    ) : ChatEvent()
    
    /**
     * Запрос на изменение выбранного AI провайдера.
     * @property providerId идентификатор нового провайдера
     */
    data class ProviderChanged(
        val providerId: String
    ) : ChatEvent()
    
    /**
     * Пользователь запросил повтор последнего сообщения.
     */
    object RetryLastMessage : ChatEvent()
    
    /**
     * Запрос на экспорт истории чата.
     * @property format формат экспорта (json, markdown, txt)
     */
    data class ExportChat(
        val format: String = "markdown"
    ) : ChatEvent()
    
    /**
     * Изменение фокуса на поле ввода.
     * @property hasFocus true если поле ввода получило фокус
     */
    data class InputFocusChanged(
        val hasFocus: Boolean
    ) : ChatEvent()
    
    /**
     * Изменение текста в поле ввода.
     * @property text текущий текст в поле ввода
     */
    data class InputTextChanged(
        val text: String
    ) : ChatEvent()
}

/**
 * Типы событий для фильтрации и группировки.
 */
enum class ChatEventType {
    /** События, связанные с сообщениями */
    MESSAGE,
    /** События пользовательского интерфейса */
    UI,
    /** События конфигурации */
    CONFIG,
    /** События ошибок */
    ERROR,
    /** События системы */
    SYSTEM
}

/**
 * Расширения для определения типа события.
 */
val ChatEvent.type: ChatEventType
    get() = when (this) {
        is ChatEvent.MessageSent,
        is ChatEvent.MessageReceived,
        is ChatEvent.RetryLastMessage -> ChatEventType.MESSAGE
        
        is ChatEvent.StartTyping,
        is ChatEvent.StopTyping,
        is ChatEvent.InputFocusChanged,
        is ChatEvent.InputTextChanged -> ChatEventType.UI
        
        is ChatEvent.ConfigChanged,
        is ChatEvent.ProviderChanged -> ChatEventType.CONFIG
        
        is ChatEvent.ErrorOccurred -> ChatEventType.ERROR
        
        is ChatEvent.ClearChat,
        is ChatEvent.ExportChat -> ChatEventType.SYSTEM
    }

/**
 * Интерфейс для обработки событий чата.
 */
fun interface ChatEventHandler {
    /**
     * Обрабатывает событие чата.
     * @param event событие для обработки
     */
    fun handle(event: ChatEvent)
}

/**
 * Композитный обработчик для управления несколькими обработчиками.
 */
class CompositeChatEventHandler : ChatEventHandler {
    private val handlers = mutableListOf<ChatEventHandler>()
    
    /**
     * Добавляет новый обработчик.
     */
    fun addHandler(handler: ChatEventHandler) {
        handlers.add(handler)
    }
    
    /**
     * Удаляет обработчик.
     */
    fun removeHandler(handler: ChatEventHandler) {
        handlers.remove(handler)
    }
    
    /**
     * Обрабатывает событие всеми зарегистрированными обработчиками.
     */
    override fun handle(event: ChatEvent) {
        handlers.forEach { handler ->
            try {
                handler.handle(event)
            } catch (e: Exception) {
                // Логирование ошибок обработки (в реальной реализации)
                println("Error handling event: ${e.message}")
            }
        }
    }
}