package com.github.lonmstalker.aiintegration.core.localization

import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Менеджер локализации для AI CLI Integration
 * 
 * Поддерживает:
 * - Динамическое переключение языков
 * - Загрузку из разных источников (файлы, сеть, плагины)
 * - Fallback на базовый язык
 * - Плюрализацию и форматирование
 * - Кэширование переводов
 */
class LocalizationManager {
    
    companion object {
        private val INSTANCE = LocalizationManager()
        
        fun getInstance(): LocalizationManager = INSTANCE
        
        const val DEFAULT_LOCALE = "en"
        const val FALLBACK_LOCALE = "en"
    }
    
    private var currentLocale: String = DEFAULT_LOCALE
    private val bundles = ConcurrentHashMap<String, LocaleBundle>()
    private val providers = mutableListOf<LocalizationProvider>()
    private val listeners = mutableListOf<LocaleChangeListener>()
    
    init {
        // Регистрируем стандартные провайдеры
        registerProvider(ResourceBundleProvider())
        registerProvider(CoreBundleProvider())
        
        // Определяем язык системы
        val systemLocale = Locale.getDefault().language
        if (isSupportedLocale(systemLocale)) {
            setLocale(systemLocale)
        }
    }
    
    /**
     * Устанавливает текущий язык
     */
    fun setLocale(locale: String) {
        if (currentLocale == locale) return
        
        val oldLocale = currentLocale
        currentLocale = locale
        
        // Уведомляем слушателей
        listeners.forEach { it.onLocaleChanged(oldLocale, locale) }
        
        // Перезагружаем бандлы
        reloadBundles()
    }
    
    /**
     * Возвращает текущий язык
     */
    fun getCurrentLocale(): String = currentLocale
    
    /**
     * Получает локализованное сообщение
     */
    fun getMessage(key: String, vararg params: Any): String {
        return getMessage(key, currentLocale, *params)
    }
    
    /**
     * Получает локализованное сообщение для конкретного языка
     */
    fun getMessage(key: String, locale: String, vararg params: Any): String {
        val bundle = getBundle(locale) ?: getBundle(FALLBACK_LOCALE)
        
        val message = bundle?.getMessage(key) ?: run {
            // Если ключ не найден, возвращаем сам ключ с префиксом
            "!$key!"
        }
        
        return if (params.isNotEmpty()) {
            formatMessage(message, *params)
        } else {
            message
        }
    }
    
    /**
     * Получает локализованное сообщение с поддержкой плюрализации
     */
    fun getPluralMessage(key: String, count: Int, vararg params: Any): String {
        val pluralKey = "$key.${getPluralForm(count, currentLocale)}"
        val message = getMessage(pluralKey, currentLocale, count, *params)
        
        // Если плюрализованный ключ не найден, пытаемся использовать базовый
        return if (message.startsWith("!") && message.endsWith("!")) {
            getMessage(key, currentLocale, count, *params)
        } else {
            message
        }
    }
    
    /**
     * Проверяет, поддерживается ли язык
     */
    fun isSupportedLocale(locale: String): Boolean {
        return getSupportedLocales().contains(locale)
    }
    
    /**
     * Возвращает список поддерживаемых языков
     */
    fun getSupportedLocales(): Set<String> {
        return providers.flatMap { it.getSupportedLocales() }.toSet()
    }
    
    /**
     * Регистрирует провайдер локализации
     */
    fun registerProvider(provider: LocalizationProvider) {
        providers.add(provider)
        reloadBundles()
    }
    
    /**
     * Добавляет слушателя изменения языка
     */
    fun addLocaleChangeListener(listener: LocaleChangeListener) {
        listeners.add(listener)
    }
    
    /**
     * Убирает слушателя изменения языка
     */
    fun removeLocaleChangeListener(listener: LocaleChangeListener) {
        listeners.remove(listener)
    }
    
    /**
     * Перезагружает все бандлы
     */
    private fun reloadBundles() {
        bundles.clear()
        getSupportedLocales().forEach { locale ->
            loadBundle(locale)
        }
    }
    
    /**
     * Загружает бандл для языка
     */
    private fun loadBundle(locale: String) {
        val messages = mutableMapOf<String, String>()
        
        // Собираем сообщения от всех провайдеров
        providers.forEach { provider ->
            try {
                val providerMessages = provider.loadMessages(locale)
                messages.putAll(providerMessages)
            } catch (e: Exception) {
                // Игнорируем ошибки провайдеров
            }
        }
        
        if (messages.isNotEmpty()) {
            bundles[locale] = LocaleBundle(locale, messages)
        }
    }
    
    /**
     * Получает бандл для языка
     */
    private fun getBundle(locale: String): LocaleBundle? {
        return bundles[locale] ?: run {
            loadBundle(locale)
            bundles[locale]
        }
    }
    
    /**
     * Форматирует сообщение с параметрами
     */
    private fun formatMessage(message: String, vararg params: Any): String {
        return try {
            String.format(message, *params)
        } catch (e: Exception) {
            // Если форматирование не удалось, возвращаем исходное сообщение
            message
        }
    }
    
    /**
     * Определяет форму множественного числа для языка
     */
    private fun getPluralForm(count: Int, locale: String): String {
        return when (locale) {
            "ru" -> when {
                count % 10 == 1 && count % 100 != 11 -> "one"
                count % 10 in 2..4 && count % 100 !in 12..14 -> "few"
                else -> "many"
            }
            "en" -> if (count == 1) "one" else "other"
            else -> "other"
        }
    }
}

/**
 * Провайдер локализации
 */
interface LocalizationProvider {
    /**
     * Загружает сообщения для языка
     */
    fun loadMessages(locale: String): Map<String, String>
    
    /**
     * Возвращает поддерживаемые языки
     */
    fun getSupportedLocales(): Set<String>
}

/**
 * Слушатель изменения языка
 */
fun interface LocaleChangeListener {
    fun onLocaleChanged(oldLocale: String, newLocale: String)
}

/**
 * Бандл сообщений для языка
 */
data class LocaleBundle(
    val locale: String,
    val messages: Map<String, String>
) {
    fun getMessage(key: String): String? = messages[key]
    
    fun hasMessage(key: String): Boolean = messages.containsKey(key)
    
    fun getKeys(): Set<String> = messages.keys
}

/**
 * Провайдер на основе ResourceBundle
 */
class ResourceBundleProvider : LocalizationProvider {
    
    private val bundleNames = listOf(
        "messages.ChatBundle",
        "messages.AIBundle",
        "messages.CoreBundle"
    )
    
    override fun loadMessages(locale: String): Map<String, String> {
        val messages = mutableMapOf<String, String>()
        
        bundleNames.forEach { bundleName ->
            try {
                val bundle = ResourceBundle.getBundle(bundleName, Locale(locale))
                bundle.keys.forEach { key ->
                    messages[key] = bundle.getString(key)
                }
            } catch (e: Exception) {
                // Пропускаем недоступные бандлы
            }
        }
        
        return messages
    }
    
    override fun getSupportedLocales(): Set<String> {
        // Определяем поддерживаемые языки по наличию файлов
        return setOf("en", "ru")
    }
}

/**
 * Провайдер для Core модуля
 */
class CoreBundleProvider : LocalizationProvider {
    
    override fun loadMessages(locale: String): Map<String, String> {
        // Встроенные сообщения для критичных компонентов
        return when (locale) {
            "ru" -> mapOf(
                "orchestrator.analyzing" to "Анализирую ответы от {0} провайдеров...",
                "orchestrator.comparison.winner" to "Лучший результат: {0} (оценка: {1})",
                "orchestrator.error.no_providers" to "Нет доступных AI провайдеров",
                "orchestrator.error.all_failed" to "Все провайдеры вернули ошибку",
                "orchestrator.consensus" to "Консенсус-ответ на основе {0} провайдеров",
                "localization.unsupported" to "Язык {0} не поддерживается",
                "localization.changed" to "Язык интерфейса изменен на {0}",
                "validation.required" to "Обязательное поле",
                "validation.invalid_format" to "Неверный формат",
                "validation.too_long" to "Слишком длинное значение (макс. {0})"
            )
            "en" -> mapOf(
                "orchestrator.analyzing" to "Analyzing responses from {0} providers...",
                "orchestrator.comparison.winner" to "Best result: {0} (score: {1})",
                "orchestrator.error.no_providers" to "No AI providers available",
                "orchestrator.error.all_failed" to "All providers returned errors",
                "orchestrator.consensus" to "Consensus response based on {0} providers",
                "localization.unsupported" to "Locale {0} is not supported",
                "localization.changed" to "Interface language changed to {0}",
                "validation.required" to "Required field",
                "validation.invalid_format" to "Invalid format",
                "validation.too_long" to "Value too long (max {0})"
            )
            else -> emptyMap()
        }
    }
    
    override fun getSupportedLocales(): Set<String> = setOf("en", "ru")
}

/**
 * Расширения для удобного использования
 */
fun String.localized(vararg params: Any): String {
    return LocalizationManager.getInstance().getMessage(this, *params)
}

fun String.localizedPlural(count: Int, vararg params: Any): String {
    return LocalizationManager.getInstance().getPluralMessage(this, count, *params)
}

/**
 * Предустановленные ключи локализации
 */
object LocalizationKeys {
    // Orchestrator
    const val ORCHESTRATOR_ANALYZING = "orchestrator.analyzing"
    const val ORCHESTRATOR_COMPARISON_WINNER = "orchestrator.comparison.winner"
    const val ORCHESTRATOR_ERROR_NO_PROVIDERS = "orchestrator.error.no_providers"
    const val ORCHESTRATOR_ERROR_ALL_FAILED = "orchestrator.error.all_failed"
    const val ORCHESTRATOR_CONSENSUS = "orchestrator.consensus"
    
    // Localization
    const val LOCALIZATION_UNSUPPORTED = "localization.unsupported"
    const val LOCALIZATION_CHANGED = "localization.changed"
    
    // Validation
    const val VALIDATION_REQUIRED = "validation.required"
    const val VALIDATION_INVALID_FORMAT = "validation.invalid_format"
    const val VALIDATION_TOO_LONG = "validation.too_long"
}