package com.github.lonmstalker.aiintegration.core.interfaces

import com.github.lonmstalker.aiintegration.core.models.*
import kotlinx.coroutines.flow.Flow

/**
 * Основной интерфейс для всех AI провайдеров
 */
interface AIProvider {
    val name: String
    val capabilities: Set<AICapability>
    val configuration: AIProviderConfiguration
    
    /**
     * Проверяет доступность провайдера
     */
    suspend fun isAvailable(): Boolean
    
    /**
     * Выполняет AI запрос
     */
    suspend fun execute(request: AIRequest): AIResponse
    
    /**
     * Потоковое выполнение запроса
     */
    suspend fun streamResponse(request: AIRequest): Flow<AIResponseChunk>
    
    /**
     * Валидирует конфигурацию провайдера
     */
    suspend fun validateConfiguration(): ValidationResult
    
    /**
     * Получает статистику использования
     */
    suspend fun getUsageStats(): UsageStats
    
    /**
     * Обновляет конфигурацию
     */
    suspend fun updateConfiguration(config: AIProviderConfiguration)
    
    /**
     * Получает информацию о доступных моделях
     */
    suspend fun getAvailableModels(): List<ModelInfo>
}

/**
 * Информация о модели
 */
data class ModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val maxTokens: Int,
    val costPerToken: Double? = null,
    val capabilities: Set<AICapability>,
    val isDefault: Boolean = false
)