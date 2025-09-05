package com.github.lonmstalker.aiintegration.core.models

import kotlinx.serialization.Serializable

/**
 * Ошибки AI операций
 */
@Serializable
sealed class AIError(val message: String, val cause: String? = null) {
    @Serializable
    data class ConfigurationError(
        val msg: String, 
        val details: String? = null,
        val missingConfig: List<String> = emptyList()
    ) : AIError(msg, details)
    
    @Serializable
    data class NetworkError(
        val msg: String, 
        val details: String? = null,
        val statusCode: Int? = null
    ) : AIError(msg, details)
    
    @Serializable
    data class AuthenticationError(
        val msg: String, 
        val details: String? = null,
        val isApiKeyInvalid: Boolean = false
    ) : AIError(msg, details)
    
    @Serializable
    data class RateLimitError(
        val msg: String, 
        val retryAfterSeconds: Long? = null,
        val remainingRequests: Int? = null
    ) : AIError(msg)
    
    @Serializable
    data class ModelNotAvailableError(
        val msg: String,
        val availableModels: List<String> = emptyList()
    ) : AIError(msg)
    
    @Serializable
    data class InvalidRequestError(
        val msg: String,
        val validationErrors: List<String> = emptyList()
    ) : AIError(msg)
    
    @Serializable
    data class TimeoutError(
        val msg: String,
        val timeoutSeconds: Long
    ) : AIError(msg)
    
    @Serializable
    data class QuotaExceededError(
        val msg: String,
        val quotaType: String,
        val resetTime: String? = null
    ) : AIError(msg)
    
    @Serializable
    data class UnknownError(
        val msg: String, 
        val details: String? = null,
        val errorCode: String? = null
    ) : AIError(msg, details)
}