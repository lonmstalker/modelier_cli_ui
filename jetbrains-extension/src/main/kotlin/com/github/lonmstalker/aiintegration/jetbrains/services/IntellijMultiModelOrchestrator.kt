package com.github.lonmstalker.aiintegration.jetbrains.services

import com.github.lonmstalker.aiintegration.core.interfaces.*
import com.github.lonmstalker.aiintegration.core.models.*
import com.github.lonmstalker.aiintegration.core.orchestration.DefaultMultiModelOrchestrator
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger

/**
 * IntelliJ-специфичный wrapper для мульти-модельного оркестратора
 * 
 * Использует DefaultMultiModelOrchestrator из core и добавляет:
 * - IntelliJ-специфичное логирование
 * - Интеграцию с IntelliJ сервисами
 */
@Service(Service.Level.APP)
class IntellijMultiModelOrchestrator : MultiModelOrchestrator {
    
    private val logger = thisLogger()
    private val coreOrchestrator = DefaultMultiModelOrchestrator()
    
    override suspend fun executeParallel(
        request: AIRequest,
        providers: List<AIProvider>
    ): MultiModelResponse {
        logger.info("Starting parallel execution with ${providers.size} providers")
        
        return try {
            val result = coreOrchestrator.executeParallel(request, providers)
            logger.info("Multi-model execution completed in ${result.executionStats.totalExecutionTimeMs}ms")
            result
        } catch (e: Exception) {
            logger.error("Multi-model execution failed", e)
            throw e
        }
    }
    
    override suspend fun compareResponses(
        responses: Map<AIProvider, AIResponse>,
        originalQuery: String,
        criteria: ComparisonCriteria?
    ): ComparisonResult {
        logger.info("Comparing ${responses.size} responses")
        
        return try {
            val result = coreOrchestrator.compareResponses(responses, originalQuery, criteria)
            logger.info("Response comparison completed, winner: ${result.winner.name}")
            result
        } catch (e: Exception) {
            logger.error("Response comparison failed", e)
            throw e
        }
    }
    
    override suspend fun selectBestResponse(
        responses: Map<AIProvider, AIResponse>,
        comparison: ComparisonResult?
    ): AIProvider {
        return coreOrchestrator.selectBestResponse(responses, comparison)
    }
    
    override suspend fun createConsensusResponse(
        responses: Map<AIProvider, AIResponse>,
        comparison: ComparisonResult
    ): AIResponse {
        logger.info("Creating consensus response from ${responses.size} providers")
        return coreOrchestrator.createConsensusResponse(responses, comparison)
    }
}

/**
 * Companion object для создания экземпляра оркестратора
 */
object MultiModelOrchestratorFactory {
    fun getInstance(): MultiModelOrchestrator {
        return com.intellij.openapi.components.service<IntellijMultiModelOrchestrator>()
    }
}