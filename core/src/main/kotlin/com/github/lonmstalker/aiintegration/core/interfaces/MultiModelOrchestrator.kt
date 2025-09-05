package com.github.lonmstalker.aiintegration.core.interfaces

import com.github.lonmstalker.aiintegration.core.models.*

/**
 * Интерфейс для координации работы с несколькими AI моделями
 */
interface MultiModelOrchestrator {
    /**
     * Выполняет запрос параллельно в нескольких провайдерах
     */
    suspend fun executeParallel(
        request: AIRequest,
        providers: List<AIProvider>
    ): MultiModelResponse
    
    /**
     * Сравнивает ответы от разных провайдеров
     */
    suspend fun compareResponses(
        responses: Map<AIProvider, AIResponse>,
        originalQuery: String,
        criteria: ComparisonCriteria? = null
    ): ComparisonResult
    
    /**
     * Выбирает лучший ответ на основе сравнения
     */
    suspend fun selectBestResponse(
        responses: Map<AIProvider, AIResponse>,
        comparison: ComparisonResult? = null
    ): AIProvider
    
    /**
     * Выполняет консенсус-запрос (объединяет лучшие части ответов)
     */
    suspend fun createConsensusResponse(
        responses: Map<AIProvider, AIResponse>,
        comparison: ComparisonResult
    ): AIResponse
}

/**
 * Результат мульти-модельного запроса
 */
data class MultiModelResponse(
    val responses: Map<AIProvider, AIResponse>,
    val comparison: ComparisonResult?,
    val recommendedProvider: AIProvider,
    val recommendedResponse: AIResponse,
    val consensusResponse: AIResponse? = null,
    val executionStats: ExecutionStats
)

/**
 * Результат сравнения ответов
 */
data class ComparisonResult(
    val scores: Map<AIProvider, ComparisonScore>,
    val winner: AIProvider,
    val reasoning: String,
    val confidence: Double,
    val criteria: ComparisonCriteria,
    val recommendations: List<String> = emptyList()
)

/**
 * Оценка сравнения для одного провайдера
 */
data class ComparisonScore(
    val accuracy: Double,
    val completeness: Double,
    val codeQuality: Double,
    val explanation: Double,
    val relevance: Double,
    val safety: Double,
    val overall: Double
) {
    companion object {
        fun calculate(
            accuracy: Double,
            completeness: Double,
            codeQuality: Double,
            explanation: Double,
            relevance: Double,
            safety: Double,
            weights: ComparisonWeights = ComparisonWeights.default()
        ): ComparisonScore {
            val overall = (accuracy * weights.accuracy +
                    completeness * weights.completeness +
                    codeQuality * weights.codeQuality +
                    explanation * weights.explanation +
                    relevance * weights.relevance +
                    safety * weights.safety) / weights.total()
            
            return ComparisonScore(accuracy, completeness, codeQuality, explanation, relevance, safety, overall)
        }
    }
}

/**
 * Критерии сравнения
 */
data class ComparisonCriteria(
    val weights: ComparisonWeights = ComparisonWeights.default(),
    val taskType: TaskType? = null,
    val customCriteria: List<String> = emptyList()
)

/**
 * Веса для критериев сравнения
 */
data class ComparisonWeights(
    val accuracy: Double = 0.25,
    val completeness: Double = 0.20,
    val codeQuality: Double = 0.20,
    val explanation: Double = 0.15,
    val relevance: Double = 0.15,
    val safety: Double = 0.05
) {
    fun total(): Double = accuracy + completeness + codeQuality + explanation + relevance + safety
    
    companion object {
        fun default() = ComparisonWeights()
        
        fun forCodeGeneration() = ComparisonWeights(
            accuracy = 0.30,
            completeness = 0.25,
            codeQuality = 0.25,
            explanation = 0.10,
            relevance = 0.05,
            safety = 0.05
        )
        
        fun forCodeReview() = ComparisonWeights(
            accuracy = 0.20,
            completeness = 0.30,
            codeQuality = 0.15,
            explanation = 0.25,
            relevance = 0.05,
            safety = 0.05
        )
    }
}

/**
 * Тип задачи для оптимизации сравнения
 */
enum class TaskType {
    CODE_GENERATION,
    CODE_EXPLANATION,
    CODE_REVIEW,
    BUG_FIXING,
    REFACTORING,
    TESTING,
    DOCUMENTATION
}

/**
 * Статистика выполнения
 */
data class ExecutionStats(
    val totalExecutionTimeMs: Long,
    val parallelExecutionTimeMs: Long,
    val comparisonTimeMs: Long,
    val providersUsed: Int,
    val successfulProviders: Int,
    val failedProviders: Int,
    val averageTokensUsed: Double,
    val totalCost: Double? = null
)