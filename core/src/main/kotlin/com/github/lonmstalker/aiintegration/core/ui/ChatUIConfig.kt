package com.github.lonmstalker.aiintegration.core.ui

import kotlinx.serialization.Serializable

/**
 * Конфигурация и настройки пользовательского интерфейса чата.
 * Обеспечивает единообразный внешний вид на всех платформах.
 * 
 * @author AI CLI Integration Team
 * @since 0.0.1
 */

/**
 * Основная конфигурация UI чата.
 * Все размеры указаны в логических пикселях для кроссплатформенности.
 * 
 * @property theme Цветовая схема интерфейса
 * @property fontSize Размер шрифта в пикселях
 * @property messageSpacing Расстояние между сообщениями в пикселях
 * @property inputHeight Высота поля ввода в пикселях
 * @property showTimestamps Показывать временные метки сообщений
 * @property enableMarkdown Поддержка разметки Markdown в сообщениях
 * @property maxMessages Максимальное количество сообщений в истории
 * @property autoScroll Автоматическая прокрутка к новым сообщениям
 */
@Serializable
data class ChatUIConfig(
    val theme: ChatTheme = ChatTheme.AUTO,
    val fontSize: Int = 12,
    val messageSpacing: Int = 8,
    val inputHeight: Int = 80,
    val showTimestamps: Boolean = true,
    val enableMarkdown: Boolean = true,
    val maxMessages: Int = 1000,
    val autoScroll: Boolean = true
) {
    companion object {
        /**
         * Создает конфигурацию по умолчанию для темной темы.
         */
        fun createDarkTheme(): ChatUIConfig = ChatUIConfig(
            theme = ChatTheme.DARK,
            fontSize = 13
        )
        
        /**
         * Создает конфигурацию по умолчанию для светлой темы.
         */
        fun createLightTheme(): ChatUIConfig = ChatUIConfig(
            theme = ChatTheme.LIGHT,
            fontSize = 12
        )
        
        /**
         * Создает компактную конфигурацию для небольших экранов.
         */
        fun createCompactConfig(): ChatUIConfig = ChatUIConfig(
            fontSize = 11,
            messageSpacing = 6,
            inputHeight = 60,
            showTimestamps = false
        )
    }
    
    /**
     * Проверяет валидность конфигурации.
     * @return true если все параметры в допустимых пределах
     */
    fun isValid(): Boolean {
        return fontSize in 8..24 &&
               messageSpacing in 0..20 &&
               inputHeight in 40..200 &&
               maxMessages in 100..10000
    }
    
    /**
     * Применяет ограничения к значениям конфигурации.
     * @return корректную конфигурацию с ограниченными значениями
     */
    fun constrain(): ChatUIConfig {
        return copy(
            fontSize = fontSize.coerceIn(8, 24),
            messageSpacing = messageSpacing.coerceIn(0, 20),
            inputHeight = inputHeight.coerceIn(40, 200),
            maxMessages = maxMessages.coerceIn(100, 10000)
        )
    }
}

/**
 * Цветовая схема интерфейса чата.
 */
@Serializable
enum class ChatTheme {
    /** Светлая тема */
    LIGHT,
    /** Темная тема */
    DARK,
    /** Автоматическая тема (следует системной) */
    AUTO
}

/**
 * Предопределенные цветовые палитры для разных тем.
 */
object ChatColorPalette {
    
    /**
     * Цвета для светлой темы.
     */
    object Light {
        const val BACKGROUND = "#FFFFFF"
        const val SURFACE = "#F5F5F5"
        const val PRIMARY = "#2196F3"
        const val SECONDARY = "#757575"
        const val TEXT_PRIMARY = "#212121"
        const val TEXT_SECONDARY = "#757575"
        const val USER_MESSAGE_BG = "#E3F2FD"
        const val ASSISTANT_MESSAGE_BG = "#F5F5F5"
        const val ERROR_COLOR = "#F44336"
        const val BORDER_COLOR = "#E0E0E0"
    }
    
    /**
     * Цвета для темной темы.
     */
    object Dark {
        const val BACKGROUND = "#1E1E1E"
        const val SURFACE = "#2D2D2D"
        const val PRIMARY = "#64B5F6"
        const val SECONDARY = "#BDBDBD"
        const val TEXT_PRIMARY = "#FFFFFF"
        const val TEXT_SECONDARY = "#BDBDBD"
        const val USER_MESSAGE_BG = "#1565C0"
        const val ASSISTANT_MESSAGE_BG = "#424242"
        const val ERROR_COLOR = "#EF5350"
        const val BORDER_COLOR = "#424242"
    }
    
    /**
     * Получает палитру цветов для указанной темы.
     * @param theme тема интерфейса
     * @return объект с константами цветов
     */
    fun getColorsForTheme(theme: ChatTheme): Any {
        return when (theme) {
            ChatTheme.LIGHT -> Light
            ChatTheme.DARK -> Dark
            ChatTheme.AUTO -> {
                // В реальной реализации здесь должна быть проверка системной темы
                Dark // По умолчанию темная
            }
        }
    }
}