package com.github.lonmstalker.aiintegration.core.ui

/**
 * Интерфейс локализации для чата AI ассистента.
 * Определяет контракт для получения локализованных строк.
 * Реализации должны быть созданы в каждом плагине отдельно.
 * 
 * @author AI CLI Integration Team
 * @since 0.0.1
 */
interface ChatLocalization {
    
    /**
     * Получает локализованную строку по ключу.
     * 
     * @param key ключ сообщения
     * @param params параметры для подстановки в строку
     * @return локализованная строка
     */
    fun getMessage(key: String, vararg params: Any): String
    
    /**
     * Проверяет, существует ли ключ в текущей локализации.
     * 
     * @param key ключ для проверки
     * @return true если ключ существует
     */
    fun hasKey(key: String): Boolean
    
    /**
     * Получает код текущего языка.
     * 
     * @return код языка (например, "en", "ru")
     */
    fun getLanguageCode(): String
}

/**
 * Константы ключей локализации для чата.
 * Используются всеми платформами для обеспечения консистентности.
 */
object ChatLocalizationKeys {
    
    // Приветственные сообщения
    const val WELCOME_TITLE = "chat.welcome.title"
    const val WELCOME_CAPABILITIES = "chat.welcome.capabilities"
    const val WELCOME_EXPLAIN = "chat.welcome.explain"
    const val WELCOME_GENERATE = "chat.welcome.generate"
    const val WELCOME_REFACTOR = "chat.welcome.refactor"
    const val WELCOME_REVIEW = "chat.welcome.review"
    const val WELCOME_INSTRUCTION = "chat.welcome.instruction"
    
    // Элементы интерфейса
    const val BUTTON_SEND = "chat.button.send"
    const val BUTTON_CLEAR = "chat.button.clear"
    const val BUTTON_EXPORT = "chat.button.export"
    const val PLACEHOLDER_INPUT = "chat.placeholder.input"
    const val TYPING_INDICATOR = "chat.typing.indicator"
    
    // Отправители сообщений
    const val SENDER_YOU = "chat.sender.you"
    const val SENDER_ASSISTANT = "chat.sender.assistant"
    const val SENDER_SYSTEM = "chat.sender.system"
    
    // Сообщения об ошибках
    const val ERROR_GENERAL = "chat.error.general"
    const val ERROR_CONNECTION = "chat.error.connection"
    const val ERROR_TIMEOUT = "chat.error.timeout"
    const val ERROR_DEFAULT = "chat.error.default"
    const val ERROR_TOKEN_LIMIT = "chat.error.tokenLimit"
    const val ERROR_SERVICE_UNAVAILABLE = "chat.error.serviceUnavailable"
    
    // Действия и команды
    const val ACTION_RETRY = "chat.action.retry"
    const val ACTION_COPY = "chat.action.copy"
    const val CONFIRM_CLEAR_CHAT = "chat.confirm.clearChat"
    const val CONFIRM_EXPORT = "chat.confirm.export"
    
    // Статусы и индикаторы
    const val STATUS_CONNECTED = "chat.status.connected"
    const val STATUS_DISCONNECTED = "chat.status.disconnected"
    const val STATUS_PROCESSING = "chat.status.processing"
    const val STATUS_LOADING = "chat.status.loading"
    
    // Настройки и конфигурация
    const val SETTINGS_TITLE = "chat.settings.title"
    const val SETTINGS_THEME = "chat.settings.theme"
    const val SETTINGS_FONT_SIZE = "chat.settings.fontSize"
    const val SETTINGS_AUTO_SCROLL = "chat.settings.autoScroll"
    const val SETTINGS_SHOW_TIMESTAMPS = "chat.settings.showTimestamps"
    
    // Форматы экспорта
    const val FORMAT_MARKDOWN = "chat.format.markdown"
    const val FORMAT_JSON = "chat.format.json"
    const val FORMAT_TEXT = "chat.format.text"
    
    // Темы оформления
    const val THEME_LIGHT = "chat.theme.light"
    const val THEME_DARK = "chat.theme.dark"
    const val THEME_AUTO = "chat.theme.auto"
    
    // Временные форматы
    const val TIME_FORMAT_SHORT = "chat.time.format.short"
    const val TIME_FORMAT_FULL = "chat.time.format.full"
}

/**
 * Расширения для удобства работы с локализацией.
 * Предоставляют готовые методы для часто используемых сообщений.
 */

/**
 * Получает полное приветственное сообщение.
 */
fun ChatLocalization.getWelcomeMessage(): String {
    return buildString {
        appendLine(getMessage(ChatLocalizationKeys.WELCOME_TITLE))
        appendLine()
        appendLine(getMessage(ChatLocalizationKeys.WELCOME_CAPABILITIES))
        appendLine(getMessage(ChatLocalizationKeys.WELCOME_EXPLAIN))
        appendLine(getMessage(ChatLocalizationKeys.WELCOME_GENERATE))
        appendLine(getMessage(ChatLocalizationKeys.WELCOME_REFACTOR))
        appendLine(getMessage(ChatLocalizationKeys.WELCOME_REVIEW))
        appendLine()
        append(getMessage(ChatLocalizationKeys.WELCOME_INSTRUCTION))
    }
}

/**
 * Получает сообщение об ошибке с форматированием.
 */
fun ChatLocalization.getErrorMessage(error: String): String {
    return getMessage(ChatLocalizationKeys.ERROR_GENERAL, error)
}

/**
 * Получает сообщение по умолчанию с параметром.
 */
fun ChatLocalization.getDefaultMessage(userMessage: String): String {
    return getMessage(ChatLocalizationKeys.ERROR_DEFAULT, userMessage)
}