package com.github.lonmstalker.aiintegration.core.interfaces

/**
 * Интерфейс для хранения конфигурации приложения
 * Реализации должны обеспечивать персистентность настроек
 */
interface ConfigurationStorage {
    
    /**
     * Сохраняет строковое значение
     */
    fun saveString(key: String, value: String)
    
    /**
     * Получает строковое значение
     */
    fun getString(key: String, defaultValue: String = ""): String
    
    /**
     * Сохраняет булево значение
     */
    fun saveBoolean(key: String, value: Boolean)
    
    /**
     * Получает булево значение
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    
    /**
     * Сохраняет целое число
     */
    fun saveInt(key: String, value: Int)
    
    /**
     * Получает целое число
     */
    fun getInt(key: String, defaultValue: Int = 0): Int
    
    /**
     * Сохраняет число с плавающей точкой
     */
    fun saveDouble(key: String, value: Double)
    
    /**
     * Получает число с плавающей точкой
     */
    fun getDouble(key: String, defaultValue: Double = 0.0): Double
    
    /**
     * Сохраняет список строк
     */
    fun saveStringList(key: String, values: List<String>)
    
    /**
     * Получает список строк
     */
    fun getStringList(key: String, defaultValue: List<String> = emptyList()): List<String>
    
    /**
     * Сохраняет объект как JSON
     */
    fun <T> saveObject(key: String, obj: T, serializer: (T) -> String)
    
    /**
     * Получает объект из JSON
     */
    fun <T> getObject(key: String, deserializer: (String) -> T?, defaultValue: T? = null): T?
    
    /**
     * Проверяет существование ключа
     */
    fun hasKey(key: String): Boolean
    
    /**
     * Удаляет значение по ключу
     */
    fun remove(key: String)
    
    /**
     * Очищает все настройки
     */
    fun clear()
    
    /**
     * Получает все ключи
     */
    fun getAllKeys(): Set<String>
}

/**
 * Интерфейс для безопасного хранения конфиденциальных данных
 */
interface SecureStorage {
    
    /**
     * Сохраняет секретное значение (пароль, API ключ и т.д.)
     */
    fun storeSecret(key: String, value: String): Boolean
    
    /**
     * Получает секретное значение
     */
    fun getSecret(key: String): String?
    
    /**
     * Удаляет секретное значение
     */
    fun removeSecret(key: String): Boolean
    
    /**
     * Проверяет существование секретного значения
     */
    fun hasSecret(key: String): Boolean
    
    /**
     * Получает список всех сохраненных ключей (без значений)
     */
    fun getStoredKeys(): Set<String>
    
    /**
     * Очищает все сохраненные секреты
     */
    fun clearAll(): Boolean
}

/**
 * Менеджер конфигурации AI интеграции
 */
interface AIConfigurationManager {
    
    /**
     * Получает конфигурацию провайдера
     */
    fun getProviderConfiguration(providerName: String): ProviderConfiguration?
    
    /**
     * Сохраняет конфигурацию провайдера
     */
    fun saveProviderConfiguration(providerName: String, config: ProviderConfiguration)
    
    /**
     * Получает общие настройки AI
     */
    fun getGeneralSettings(): GeneralAISettings
    
    /**
     * Сохраняет общие настройки AI
     */
    fun saveGeneralSettings(settings: GeneralAISettings)
    
    /**
     * Получает настройки интерфейса
     */
    fun getUISettings(): UISettings
    
    /**
     * Сохраняет настройки интерфейса
     */
    fun saveUISettings(settings: UISettings)
    
    /**
     * Получает список включенных провайдеров
     */
    fun getEnabledProviders(): Set<String>
    
    /**
     * Валидирует конфигурацию
     */
    fun validateConfiguration(): ConfigurationValidationResult
}

/**
 * Конфигурация AI провайдера
 */
data class ProviderConfiguration(
    val name: String,
    val enabled: Boolean = true,
    val apiKey: String? = null,
    val baseUrl: String? = null,
    val model: String? = null,
    val maxTokens: Int? = null,
    val temperature: Double? = null,
    val timeout: Int = 30000, // ms
    val retryCount: Int = 3,
    val customParameters: Map<String, String> = emptyMap()
)

/**
 * Общие настройки AI
 */
data class GeneralAISettings(
    val defaultProvider: String = "",
    val enableMultiModel: Boolean = false,
    val enableHistory: Boolean = true,
    val maxHistorySize: Int = 10000,
    val enableMetrics: Boolean = true,
    val autoSaveInterval: Int = 300000, // ms (5 minutes)
    val maxContextSize: Int = 50000,
    val enableValidation: Boolean = true,
    val enableErrorHandling: Boolean = true
)

/**
 * Настройки интерфейса
 */
data class UISettings(
    val theme: String = "auto",
    val fontSize: Int = 14,
    val enableSyntaxHighlighting: Boolean = true,
    val enableAutoCompletion: Boolean = true,
    val showTokenCount: Boolean = true,
    val showExecutionTime: Boolean = true,
    val enableNotifications: Boolean = true,
    val compactMode: Boolean = false,
    val windowPosition: WindowPosition? = null
)

/**
 * Позиция и размер окна
 */
data class WindowPosition(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

/**
 * Результат валидации конфигурации
 */
data class ConfigurationValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val missingApiKeys: Set<String> = emptySet(),
    val invalidProviders: Set<String> = emptySet()
)