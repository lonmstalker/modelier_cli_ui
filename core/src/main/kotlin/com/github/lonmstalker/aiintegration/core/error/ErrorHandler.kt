package com.github.lonmstalker.aiintegration.core.error

import com.github.lonmstalker.aiintegration.core.models.AIError
import com.github.lonmstalker.aiintegration.core.models.AIResponse

/**
 * Обработчик ошибок AI операций
 */
interface ErrorHandler {
    
    /**
     * Обрабатывает исключение и возвращает AIResponse с ошибкой
     */
    fun handleException(
        exception: Throwable,
        requestId: String,
        context: String = ""
    ): AIResponse
    
    /**
     * Проверяет ответ на наличие ошибок
     */
    fun validateResponse(response: AIResponse): AIResponse
    
    /**
     * Обрабатывает специфичные ошибки провайдера
     */
    fun handleProviderError(
        error: String,
        providerName: String,
        requestId: String
    ): AIResponse
}

/**
 * Реализация обработчика ошибок по умолчанию
 */
class DefaultErrorHandler : ErrorHandler {
    
    override fun handleException(
        exception: Throwable,
        requestId: String,
        context: String
    ): AIResponse {
        val error = when (exception) {
            is CommandNotFoundException -> AIError.CommandNotFound(exception.message ?: "Command not found")
            is TimeoutException -> AIError.Timeout(exception.message ?: "Operation timed out")
            is PermissionDeniedException -> AIError.PermissionDenied(exception.message ?: "Permission denied")
            is NetworkException -> AIError.NetworkError(exception.message ?: "Network error")
            is ValidationException -> AIError.ValidationError(exception.message ?: "Validation failed")
            is RateLimitException -> AIError.RateLimitExceeded(exception.message ?: "Rate limit exceeded")
            else -> AIError.UnknownError(
                exception.message ?: "Unknown error",
                buildString {
                    if (context.isNotEmpty()) {
                        append("Context: $context\n")
                    }
                    append("Exception: ${exception::class.simpleName}\n")
                    append("Stack trace: ${exception.stackTraceToString()}")
                }
            )
        }
        
        return AIResponse(
            success = false,
            content = "",
            error = error,
            executionTimeMs = 0,
            requestId = requestId
        )
    }
    
    override fun validateResponse(response: AIResponse): AIResponse {
        if (!response.success) {
            return response
        }
        
        // Проверяем базовые требования к успешному ответу
        val validationErrors = mutableListOf<String>()
        
        if (response.content.isBlank()) {
            validationErrors.add("Response content is empty")
        }
        
        if (response.content.length > 100_000) {
            validationErrors.add("Response content is too large (${response.content.length} chars)")
        }
        
        // Проверяем на потенциально опасный контент
        val dangerousPatterns = listOf(
            "rm -rf /", "format c:", "dd if=/dev/zero", ":(){ :|:& };:",
            "sudo rm", "del /f /s /q", "rmdir /s", "DROP DATABASE"
        )
        
        val containsDangerousContent = dangerousPatterns.any { 
            response.content.contains(it, ignoreCase = true) 
        }
        
        if (containsDangerousContent) {
            validationErrors.add("Response contains potentially dangerous content")
        }
        
        return if (validationErrors.isNotEmpty()) {
            response.copy(
                success = false,
                error = AIError.ValidationError("Response validation failed: ${validationErrors.joinToString(", ")}")
            )
        } else {
            response
        }
    }
    
    override fun handleProviderError(
        error: String,
        providerName: String,
        requestId: String
    ): AIResponse {
        val aiError = when {
            error.contains("authentication", ignoreCase = true) ||
            error.contains("unauthorized", ignoreCase = true) ||
            error.contains("invalid api key", ignoreCase = true) -> {
                AIError.AuthenticationError("Authentication failed for provider $providerName")
            }
            
            error.contains("rate limit", ignoreCase = true) ||
            error.contains("quota", ignoreCase = true) -> {
                AIError.RateLimitExceeded("Rate limit exceeded for provider $providerName")
            }
            
            error.contains("network", ignoreCase = true) ||
            error.contains("connection", ignoreCase = true) ||
            error.contains("timeout", ignoreCase = true) -> {
                AIError.NetworkError("Network error for provider $providerName: $error")
            }
            
            error.contains("not found", ignoreCase = true) ||
            error.contains("404", ignoreCase = true) -> {
                AIError.ResourceNotFound("Resource not found for provider $providerName")
            }
            
            else -> AIError.ProviderError(providerName, error)
        }
        
        return AIResponse(
            success = false,
            content = "",
            error = aiError,
            executionTimeMs = 0,
            requestId = requestId
        )
    }
}

/**
 * Операционный обработчик AI действий с встроенной обработкой ошибок
 */
class AIOperationHandler(
    private val errorHandler: ErrorHandler = DefaultErrorHandler()
) {
    
    /**
     * Выполняет AI операцию с обработкой ошибок
     */
    suspend fun <T> executeWithErrorHandling(
        requestId: String,
        context: String = "",
        operation: suspend () -> T
    ): Result<T> {
        return try {
            val result = operation()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Выполняет операцию и возвращает AIResponse
     */
    suspend fun executeOperation(
        requestId: String,
        context: String = "",
        operation: suspend () -> AIResponse
    ): AIResponse {
        return try {
            val response = operation()
            errorHandler.validateResponse(response)
        } catch (e: Exception) {
            errorHandler.handleException(e, requestId, context)
        }
    }
}

/**
 * Исключения для AI операций
 */
sealed class AIException(message: String, cause: Throwable? = null) : Exception(message, cause)

class CommandNotFoundException(command: String) : AIException("Command not found: $command")
class TimeoutException(timeout: String) : AIException("Operation timed out after $timeout")
class PermissionDeniedException(resource: String) : AIException("Permission denied for: $resource")
class NetworkException(details: String) : AIException("Network error: $details")
class ValidationException(details: String) : AIException("Validation error: $details")
class RateLimitException(provider: String) : AIException("Rate limit exceeded for provider: $provider")
class ConfigurationException(details: String) : AIException("Configuration error: $details")

/**
 * Фабрика для создания обработчиков ошибок
 */
object ErrorHandlerFactory {
    
    private var defaultHandler: ErrorHandler = DefaultErrorHandler()
    
    fun getDefault(): ErrorHandler = defaultHandler
    
    fun setDefault(handler: ErrorHandler) {
        defaultHandler = handler
    }
    
    fun create(): ErrorHandler = DefaultErrorHandler()
}