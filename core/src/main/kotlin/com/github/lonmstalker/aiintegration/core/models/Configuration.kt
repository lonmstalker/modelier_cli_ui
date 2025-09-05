package com.github.lonmstalker.aiintegration.core.models

import kotlinx.serialization.Serializable

/**
 * Конфигурация провайдера AI
 */
@Serializable
data class AIProviderConfiguration(
    val executablePath: String? = null,
    val apiKey: String? = null,
    val apiUrl: String? = null,
    val customParameters: Map<String, String> = emptyMap(),
    val timeoutSeconds: Long = 30,
    val maxTokens: Int? = null,
    val temperature: Double? = null,
    val model: String? = null,
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 1000
)

/**
 * Результат валидации
 */
@Serializable
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError> = emptyList(),
    val warnings: List<String> = emptyList(),
    val recommendations: List<String> = emptyList()
)

/**
 * Ошибка валидации
 */
@Serializable
data class ValidationError(
    val field: String,
    val message: String,
    val severity: ValidationSeverity = ValidationSeverity.ERROR
)

/**
 * Серьезность ошибки валидации
 */
@Serializable
enum class ValidationSeverity {
    WARNING, ERROR, CRITICAL
}

/**
 * Статистика использования
 */
@Serializable
data class UsageStats(
    val totalRequests: Long,
    val successfulRequests: Long,
    val failedRequests: Long,
    val averageResponseTimeMs: Double,
    val totalTokensUsed: Long,
    val estimatedCost: Double? = null,
    val lastUsed: Long? = null,
    val topCommands: Map<String, Long> = emptyMap()
)

/**
 * Настройки Core модуля
 */
@Serializable
data class CoreConfiguration(
    val providers: Map<String, AIProviderConfiguration> = emptyMap(),
    val defaultProvider: String = "claude-code",
    val enableLogging: Boolean = true,
    val logLevel: LogLevel = LogLevel.INFO,
    val maxContextTokens: Int = 50000,
    val enableMultiModel: Boolean = false,
    val cacheEnabled: Boolean = true,
    val cacheExpirationHours: Int = 24,
    val vectorDbPath: String = ".ai-cache/vectordb",
    val serverPort: Int = 8080,
    val serverEnabled: Boolean = false
)

/**
 * Уровень логирования
 */
@Serializable
enum class LogLevel {
    TRACE, DEBUG, INFO, WARN, ERROR
}