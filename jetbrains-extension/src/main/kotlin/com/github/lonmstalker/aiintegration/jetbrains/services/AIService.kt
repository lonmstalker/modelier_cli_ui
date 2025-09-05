package com.github.lonmstalker.aiintegration.jetbrains.services

import com.github.lonmstalker.aiintegration.core.interfaces.*
import com.github.lonmstalker.aiintegration.core.models.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.*

/**
 * Основной сервис для интеграции с Core AI модулем в IntelliJ IDEA
 */
@Service(Service.Level.APP)
class AIService : AutoCloseable {
    
    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName("AIService")
    )
    
    private val logger = thisLogger()
    private val providers = mutableMapOf<String, AIProvider>()
    
    companion object {
        fun getInstance(): AIService = com.intellij.openapi.components.service()
    }
    
    init {
        logger.info("AI Service initialized")
        // TODO: Initialize Core AI module
        serviceScope.launch {
            initializeProviders()
        }
    }
    
    /**
     * Регистрирует AI провайдера
     */
    fun registerProvider(provider: AIProvider) {
        providers[provider.name] = provider
        logger.info("Registered AI provider: ${provider.name}")
    }
    
    /**
     * Получает список доступных провайдеров
     */
    fun getAvailableProviders(): List<String> = providers.keys.toList()
    
    /**
     * Выполняет AI запрос
     */
    suspend fun executeRequest(request: AIRequest): AIResponse {
        return withContext(Dispatchers.IO) {
            try {
                val provider = providers[request.targetProvider] 
                    ?: providers.values.firstOrNull()
                    ?: throw IllegalStateException("No AI providers available")
                
                logger.info("Executing AI request with provider: ${provider.name}")
                provider.execute(request)
            } catch (e: Exception) {
                logger.error("Failed to execute AI request", e)
                AIResponse(
                    success = false,
                    content = "",
                    error = AIError.UnknownError(e.message ?: "Unknown error", e.toString()),
                    executionTimeMs = 0,
                    requestId = request.requestId
                )
            }
        }
    }
    
    /**
     * Выполняет запрос с несколькими моделями
     */
    suspend fun executeMultiModelRequest(
        request: AIRequest,
        providerNames: List<String>
    ): MultiModelResponse? {
        // TODO: Implement multi-model orchestration
        return null
    }
    
    /**
     * Проверяет доступность провайдеров
     */
    suspend fun checkProvidersAvailability(): Map<String, Boolean> {
        return providers.mapValues { (_, provider) ->
            try {
                provider.isAvailable()
            } catch (e: Exception) {
                logger.warn("Provider ${provider.name} availability check failed", e)
                false
            }
        }
    }
    
    private suspend fun initializeProviders() {
        // TODO: Load providers from Core module
        logger.info("Initializing AI providers...")
    }
    
    override fun close() {
        logger.info("Closing AI Service")
        serviceScope.cancel()
    }
}