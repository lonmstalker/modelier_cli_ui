package com.github.lonmstalker.aiintegration.jetbrains.storage

import com.github.lonmstalker.aiintegration.core.interfaces.*
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * IntelliJ-специфичная реализация хранения конфигурации
 * Использует IntelliJ Platform API для персистентности
 */
@Service(Service.Level.APP)
@State(
    name = "AIIntegrationConfiguration",
    storages = [Storage("ai-integration-config.xml")]
)
class IntellijConfigurationStorage : ConfigurationStorage, PersistentStateComponent<IntellijConfigurationStorage.State> {
    
    companion object {
        fun getInstance(): IntellijConfigurationStorage = com.intellij.openapi.components.service()
    }
    
    data class State(
        var stringValues: MutableMap<String, String> = mutableMapOf(),
        var booleanValues: MutableMap<String, Boolean> = mutableMapOf(),
        var intValues: MutableMap<String, Int> = mutableMapOf(),
        var doubleValues: MutableMap<String, Double> = mutableMapOf(),
        var stringListValues: MutableMap<String, MutableList<String>> = mutableMapOf(),
        var objectValues: MutableMap<String, String> = mutableMapOf() // JSON strings
    )
    
    private var myState = State()
    
    override fun getState(): State = myState
    
    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }
    
    override fun saveString(key: String, value: String) {
        myState.stringValues[key] = value
    }
    
    override fun getString(key: String, defaultValue: String): String {
        return myState.stringValues[key] ?: defaultValue
    }
    
    override fun saveBoolean(key: String, value: Boolean) {
        myState.booleanValues[key] = value
    }
    
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return myState.booleanValues[key] ?: defaultValue
    }
    
    override fun saveInt(key: String, value: Int) {
        myState.intValues[key] = value
    }
    
    override fun getInt(key: String, defaultValue: Int): Int {
        return myState.intValues[key] ?: defaultValue
    }
    
    override fun saveDouble(key: String, value: Double) {
        myState.doubleValues[key] = value
    }
    
    override fun getDouble(key: String, defaultValue: Double): Double {
        return myState.doubleValues[key] ?: defaultValue
    }
    
    override fun saveStringList(key: String, values: List<String>) {
        myState.stringListValues[key] = values.toMutableList()
    }
    
    override fun getStringList(key: String, defaultValue: List<String>): List<String> {
        return myState.stringListValues[key] ?: defaultValue
    }
    
    override fun <T> saveObject(key: String, obj: T, serializer: (T) -> String) {
        myState.objectValues[key] = serializer(obj)
    }
    
    override fun <T> getObject(key: String, deserializer: (String) -> T?, defaultValue: T?): T? {
        val jsonString = myState.objectValues[key] ?: return defaultValue
        return try {
            deserializer(jsonString)
        } catch (e: Exception) {
            defaultValue
        }
    }
    
    override fun hasKey(key: String): Boolean {
        return myState.stringValues.containsKey(key) ||
                myState.booleanValues.containsKey(key) ||
                myState.intValues.containsKey(key) ||
                myState.doubleValues.containsKey(key) ||
                myState.stringListValues.containsKey(key) ||
                myState.objectValues.containsKey(key)
    }
    
    override fun remove(key: String) {
        myState.stringValues.remove(key)
        myState.booleanValues.remove(key)
        myState.intValues.remove(key)
        myState.doubleValues.remove(key)
        myState.stringListValues.remove(key)
        myState.objectValues.remove(key)
    }
    
    override fun clear() {
        myState.stringValues.clear()
        myState.booleanValues.clear()
        myState.intValues.clear()
        myState.doubleValues.clear()
        myState.stringListValues.clear()
        myState.objectValues.clear()
    }
    
    override fun getAllKeys(): Set<String> {
        return (myState.stringValues.keys +
                myState.booleanValues.keys +
                myState.intValues.keys +
                myState.doubleValues.keys +
                myState.stringListValues.keys +
                myState.objectValues.keys).toSet()
    }
}

/**
 * IntelliJ-специфичная реализация безопасного хранения
 * Использует IntelliJ PasswordSafe API
 */
@Service(Service.Level.APP)
class IntellijSecureStorage : SecureStorage {
    
    companion object {
        private const val SUBSYSTEM = "AIIntegration"
        
        fun getInstance(): IntellijSecureStorage = com.intellij.openapi.components.service()
    }
    
    override fun storeSecret(key: String, value: String): Boolean {
        return try {
            val credentialAttributes = CredentialAttributes(generateServiceName(SUBSYSTEM, key))
            PasswordSafe.instance.setPassword(credentialAttributes, value)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun getSecret(key: String): String? {
        return try {
            val credentialAttributes = CredentialAttributes(generateServiceName(SUBSYSTEM, key))
            PasswordSafe.instance.getPassword(credentialAttributes)
        } catch (e: Exception) {
            null
        }
    }
    
    override fun removeSecret(key: String): Boolean {
        return try {
            val credentialAttributes = CredentialAttributes(generateServiceName(SUBSYSTEM, key))
            PasswordSafe.instance.setPassword(credentialAttributes, null)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun hasSecret(key: String): Boolean {
        return getSecret(key) != null
    }
    
    override fun getStoredKeys(): Set<String> {
        // IntelliJ PasswordSafe не предоставляет метод для получения всех ключей
        // Возвращаем пустое множество
        return emptySet()
    }
    
    override fun clearAll(): Boolean {
        // IntelliJ PasswordSafe не предоставляет метод для очистки всех секретов
        // Возвращаем false, указывая что операция не поддерживается
        return false
    }
}

/**
 * Менеджер конфигурации AI интеграции для IntelliJ
 */
@Service(Service.Level.APP)
class IntellijAIConfigurationManager : AIConfigurationManager {
    
    companion object {
        fun getInstance(): IntellijAIConfigurationManager = com.intellij.openapi.components.service()
        
        // Ключи конфигурации
        private const val PROVIDER_PREFIX = "provider."
        private const val GENERAL_PREFIX = "general."
        private const val UI_PREFIX = "ui."
        private const val ENABLED_PROVIDERS_KEY = "enabled_providers"
    }
    
    private val storage = IntellijConfigurationStorage.getInstance()
    private val secureStorage = IntellijSecureStorage.getInstance()
    
    override fun getProviderConfiguration(providerName: String): ProviderConfiguration? {
        val prefix = "$PROVIDER_PREFIX$providerName."
        
        if (!storage.hasKey("${prefix}name")) {
            return null
        }
        
        return ProviderConfiguration(
            name = storage.getString("${prefix}name", providerName),
            enabled = storage.getBoolean("${prefix}enabled", true),
            apiKey = secureStorage.getSecret("${prefix}api_key"),
            baseUrl = storage.getString("${prefix}base_url", ""),
            model = storage.getString("${prefix}model", ""),
            maxTokens = storage.getInt("${prefix}max_tokens", 0).takeIf { it > 0 },
            temperature = storage.getDouble("${prefix}temperature", 0.0).takeIf { it > 0.0 },
            timeout = storage.getInt("${prefix}timeout", 30000),
            retryCount = storage.getInt("${prefix}retry_count", 3),
            customParameters = parseCustomParameters(storage.getString("${prefix}custom_params", ""))
        )
    }
    
    override fun saveProviderConfiguration(providerName: String, config: ProviderConfiguration) {
        val prefix = "$PROVIDER_PREFIX$providerName."
        
        storage.saveString("${prefix}name", config.name)
        storage.saveBoolean("${prefix}enabled", config.enabled)
        storage.saveString("${prefix}base_url", config.baseUrl ?: "")
        storage.saveString("${prefix}model", config.model ?: "")
        storage.saveInt("${prefix}max_tokens", config.maxTokens ?: 0)
        storage.saveDouble("${prefix}temperature", config.temperature ?: 0.0)
        storage.saveInt("${prefix}timeout", config.timeout)
        storage.saveInt("${prefix}retry_count", config.retryCount)
        storage.saveString("${prefix}custom_params", serializeCustomParameters(config.customParameters))
        
        // Сохраняем API ключ в безопасное хранилище
        config.apiKey?.let { apiKey ->
            secureStorage.storeSecret("${prefix}api_key", apiKey)
        }
    }
    
    override fun getGeneralSettings(): GeneralAISettings {
        return GeneralAISettings(
            defaultProvider = storage.getString("${GENERAL_PREFIX}default_provider", ""),
            enableMultiModel = storage.getBoolean("${GENERAL_PREFIX}enable_multi_model", false),
            enableHistory = storage.getBoolean("${GENERAL_PREFIX}enable_history", true),
            maxHistorySize = storage.getInt("${GENERAL_PREFIX}max_history_size", 10000),
            enableMetrics = storage.getBoolean("${GENERAL_PREFIX}enable_metrics", true),
            autoSaveInterval = storage.getInt("${GENERAL_PREFIX}auto_save_interval", 300000),
            maxContextSize = storage.getInt("${GENERAL_PREFIX}max_context_size", 50000),
            enableValidation = storage.getBoolean("${GENERAL_PREFIX}enable_validation", true),
            enableErrorHandling = storage.getBoolean("${GENERAL_PREFIX}enable_error_handling", true)
        )
    }
    
    override fun saveGeneralSettings(settings: GeneralAISettings) {
        storage.saveString("${GENERAL_PREFIX}default_provider", settings.defaultProvider)
        storage.saveBoolean("${GENERAL_PREFIX}enable_multi_model", settings.enableMultiModel)
        storage.saveBoolean("${GENERAL_PREFIX}enable_history", settings.enableHistory)
        storage.saveInt("${GENERAL_PREFIX}max_history_size", settings.maxHistorySize)
        storage.saveBoolean("${GENERAL_PREFIX}enable_metrics", settings.enableMetrics)
        storage.saveInt("${GENERAL_PREFIX}auto_save_interval", settings.autoSaveInterval)
        storage.saveInt("${GENERAL_PREFIX}max_context_size", settings.maxContextSize)
        storage.saveBoolean("${GENERAL_PREFIX}enable_validation", settings.enableValidation)
        storage.saveBoolean("${GENERAL_PREFIX}enable_error_handling", settings.enableErrorHandling)
    }
    
    override fun getUISettings(): UISettings {
        return UISettings(
            theme = storage.getString("${UI_PREFIX}theme", "auto"),
            fontSize = storage.getInt("${UI_PREFIX}font_size", 14),
            enableSyntaxHighlighting = storage.getBoolean("${UI_PREFIX}enable_syntax_highlighting", true),
            enableAutoCompletion = storage.getBoolean("${UI_PREFIX}enable_auto_completion", true),
            showTokenCount = storage.getBoolean("${UI_PREFIX}show_token_count", true),
            showExecutionTime = storage.getBoolean("${UI_PREFIX}show_execution_time", true),
            enableNotifications = storage.getBoolean("${UI_PREFIX}enable_notifications", true),
            compactMode = storage.getBoolean("${UI_PREFIX}compact_mode", false),
            windowPosition = getWindowPosition()
        )
    }
    
    override fun saveUISettings(settings: UISettings) {
        storage.saveString("${UI_PREFIX}theme", settings.theme)
        storage.saveInt("${UI_PREFIX}font_size", settings.fontSize)
        storage.saveBoolean("${UI_PREFIX}enable_syntax_highlighting", settings.enableSyntaxHighlighting)
        storage.saveBoolean("${UI_PREFIX}enable_auto_completion", settings.enableAutoCompletion)
        storage.saveBoolean("${UI_PREFIX}show_token_count", settings.showTokenCount)
        storage.saveBoolean("${UI_PREFIX}show_execution_time", settings.showExecutionTime)
        storage.saveBoolean("${UI_PREFIX}enable_notifications", settings.enableNotifications)
        storage.saveBoolean("${UI_PREFIX}compact_mode", settings.compactMode)
        
        settings.windowPosition?.let { saveWindowPosition(it) }
    }
    
    override fun getEnabledProviders(): Set<String> {
        return storage.getStringList(ENABLED_PROVIDERS_KEY, emptyList()).toSet()
    }
    
    override fun validateConfiguration(): ConfigurationValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val missingApiKeys = mutableSetOf<String>()
        val invalidProviders = mutableSetOf<String>()
        
        val enabledProviders = getEnabledProviders()
        
        enabledProviders.forEach { providerName ->
            val config = getProviderConfiguration(providerName)
            
            if (config == null) {
                invalidProviders.add(providerName)
                errors.add("Configuration not found for provider: $providerName")
                return@forEach
            }
            
            if (config.apiKey.isNullOrBlank()) {
                missingApiKeys.add(providerName)
                warnings.add("API key not set for provider: $providerName")
            }
            
            if (config.timeout <= 0) {
                errors.add("Invalid timeout for provider $providerName: ${config.timeout}")
            }
            
            if (config.retryCount < 0) {
                errors.add("Invalid retry count for provider $providerName: ${config.retryCount}")
            }
        }
        
        val generalSettings = getGeneralSettings()
        if (generalSettings.defaultProvider.isNotEmpty() && 
            !enabledProviders.contains(generalSettings.defaultProvider)) {
            warnings.add("Default provider '${generalSettings.defaultProvider}' is not enabled")
        }
        
        return ConfigurationValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            missingApiKeys = missingApiKeys,
            invalidProviders = invalidProviders
        )
    }
    
    private fun parseCustomParameters(paramsString: String): Map<String, String> {
        if (paramsString.isBlank()) return emptyMap()
        
        return try {
            paramsString.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .associate { param ->
                    val (key, value) = param.split("=", limit = 2)
                    key.trim() to value.trim()
                }
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    private fun serializeCustomParameters(params: Map<String, String>): String {
        return params.entries.joinToString(",") { (key, value) ->
            "$key=$value"
        }
    }
    
    private fun getWindowPosition(): WindowPosition? {
        val x = storage.getInt("${UI_PREFIX}window_x", -1)
        val y = storage.getInt("${UI_PREFIX}window_y", -1)
        val width = storage.getInt("${UI_PREFIX}window_width", -1)
        val height = storage.getInt("${UI_PREFIX}window_height", -1)
        
        return if (x >= 0 && y >= 0 && width > 0 && height > 0) {
            WindowPosition(x, y, width, height)
        } else {
            null
        }
    }
    
    private fun saveWindowPosition(position: WindowPosition) {
        storage.saveInt("${UI_PREFIX}window_x", position.x)
        storage.saveInt("${UI_PREFIX}window_y", position.y)
        storage.saveInt("${UI_PREFIX}window_width", position.width)
        storage.saveInt("${UI_PREFIX}window_height", position.height)
    }
}