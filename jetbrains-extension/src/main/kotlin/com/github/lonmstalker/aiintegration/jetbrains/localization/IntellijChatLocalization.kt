package com.github.lonmstalker.aiintegration.jetbrains.localization

import com.github.lonmstalker.aiintegration.core.ui.ChatLocalization
import com.github.lonmstalker.aiintegration.jetbrains.AIBundle
import java.util.*

/**
 * Реализация локализации чата для IntelliJ IDEA плагина.
 * Использует AIBundle для получения IntelliJ-специфичных строк и 
 * core ChatBundle для общих сообщений чата.
 * 
 * @author AI CLI Integration Team
 * @since 0.0.1
 */
class IntellijChatLocalization : ChatLocalization {
    
    private val coreBundle: ResourceBundle by lazy {
        try {
            ResourceBundle.getBundle("messages.ChatBundle", Locale.getDefault(), 
                javaClass.classLoader)
        } catch (e: MissingResourceException) {
            // Fallback на английский
            ResourceBundle.getBundle("messages.ChatBundle", Locale.ENGLISH,
                javaClass.classLoader)
        }
    }
    
    /**
     * Получает локализованную строку по ключу.
     * Сначала ищет в AIBundle (для IntelliJ-специфичных ключей),
     * затем в core ChatBundle (для общих ключей чата).
     * 
     * @param key ключ сообщения
     * @param params параметры для подстановки в строку
     * @return локализованная строка
     */
    override fun getMessage(key: String, vararg params: Any): String {
        return try {
            // Сначала пробуем AIBundle (IntelliJ-специфичные ключи)
            AIBundle.message(key, *params)
        } catch (e: Exception) {
            try {
                // Fallback на core ChatBundle
                val message = coreBundle.getString(key)
                if (params.isNotEmpty()) {
                    java.text.MessageFormat.format(message, *params)
                } else {
                    message
                }
            } catch (fallbackException: Exception) {
                // Последний fallback - возвращаем ключ
                "!$key!"
            }
        }
    }
    
    /**
     * Проверяет существование ключа в AIBundle или core ChatBundle.
     * 
     * @param key ключ для проверки
     * @return true если ключ существует
     */
    override fun hasKey(key: String): Boolean {
        return try {
            AIBundle.message(key)
            true
        } catch (e: Exception) {
            try {
                coreBundle.getString(key)
                true
            } catch (fallbackException: Exception) {
                false
            }
        }
    }
    
    /**
     * Получает код текущего языка из системных настроек.
     * 
     * @return код языка (например, "en", "ru")
     */
    override fun getLanguageCode(): String {
        return Locale.getDefault().language
    }
}

/**
 * Singleton объект для глобального доступа к локализации IntelliJ плагина.
 * Предоставляет единую точку доступа к локализации в рамках плагина.
 */
object IntellijChatLocalizationProvider {
    
    private val localization: ChatLocalization by lazy {
        IntellijChatLocalization()
    }
    
    /**
     * Получает экземпляр локализации.
     * 
     * @return объект локализации
     */
    fun getInstance(): ChatLocalization = localization
    
    /**
     * Удобный метод для получения сообщения.
     * 
     * @param key ключ сообщения
     * @param params параметры для подстановки
     * @return локализованная строка
     */
    fun message(key: String, vararg params: Any): String {
        return localization.getMessage(key, *params)
    }
}