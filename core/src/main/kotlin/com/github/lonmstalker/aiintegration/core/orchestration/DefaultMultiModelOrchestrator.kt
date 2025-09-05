package com.github.lonmstalker.aiintegration.core.orchestration

import com.github.lonmstalker.aiintegration.core.interfaces.*
import com.github.lonmstalker.aiintegration.core.models.*
import kotlinx.coroutines.*

/**
 * Реализация мульти-модельного оркестратора по умолчанию
 * 
 * Координирует работу с несколькими AI провайдерами:
 * - Параллельное выполнение запросов
 * - Сравнение и валидация ответов
 * - Выбор лучшего результата
 * - Создание консенсус-ответа
 */
class DefaultMultiModelOrchestrator : MultiModelOrchestrator {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineName("MultiModelOrchestrator"))
    
    override suspend fun executeParallel(
        request: AIRequest,
        providers: List<AIProvider>
    ): MultiModelResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        // Параллельное выполнение запросов
        val responses = mutableMapOf<AIProvider, AIResponse>()
        val jobs = providers.map { provider ->
            async {
                try {
                    val response = provider.execute(request)
                    synchronized(responses) {
                        responses[provider] = response
                    }
                } catch (e: Exception) {
                    synchronized(responses) {
                        responses[provider] = AIResponse(
                            success = false,
                            content = "",
                            error = AIError.UnknownError(e.message ?: "Unknown error", e.toString()),
                            executionTimeMs = 0,
                            requestId = request.requestId
                        )
                    }
                }
            }
        }
        
        // Ждем завершения всех запросов
        jobs.awaitAll()
        val parallelExecutionTime = System.currentTimeMillis() - startTime
        
        // Фильтруем успешные ответы
        val successfulResponses = responses.filter { it.value.success }
        
        if (successfulResponses.isEmpty()) {
            throw IllegalStateException("All AI providers failed to execute the request")
        }
        
        // Сравниваем ответы
        val comparisonStart = System.currentTimeMillis()
        val comparison = compareResponses(successfulResponses, request.toString())
        val comparisonTime = System.currentTimeMillis() - comparisonStart
        
        // Выбираем лучший ответ
        val bestProvider = selectBestResponse(successfulResponses, comparison)
        val bestResponse = successfulResponses[bestProvider]!!
        
        // Создаем консенсус-ответ если есть несколько успешных ответов
        val consensusResponse = if (successfulResponses.size > 1) {
            try {
                createConsensusResponse(successfulResponses, comparison)
            } catch (e: Exception) {
                null
            }
        } else null
        
        val totalTime = System.currentTimeMillis() - startTime
        val executionStats = ExecutionStats(
            totalExecutionTimeMs = totalTime,
            parallelExecutionTimeMs = parallelExecutionTime,
            comparisonTimeMs = comparisonTime,
            providersUsed = providers.size,
            successfulProviders = successfulResponses.size,
            failedProviders = providers.size - successfulResponses.size,
            averageTokensUsed = successfulResponses.values.mapNotNull { it.tokensUsed }.average(),
            totalCost = null // TODO: calculate total cost
        )
        
        MultiModelResponse(
            responses = responses,
            comparison = comparison,
            recommendedProvider = bestProvider,
            recommendedResponse = bestResponse,
            consensusResponse = consensusResponse,
            executionStats = executionStats
        )
    }
    
    override suspend fun compareResponses(
        responses: Map<AIProvider, AIResponse>,
        originalQuery: String,
        criteria: ComparisonCriteria?
    ): ComparisonResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        
        val usedCriteria = criteria ?: ComparisonCriteria()
        val scores = mutableMapOf<AIProvider, ComparisonScore>()
        
        // Анализируем каждый ответ
        for ((provider, response) in responses) {
            val score = analyzeResponse(response, originalQuery, usedCriteria)
            scores[provider] = score
        }
        
        // Находим победителя
        val winner = scores.maxByOrNull { it.value.overall }?.key
            ?: throw IllegalStateException("No valid responses to compare")
        
        val winnerScore = scores[winner]!!
        val reasoning = buildString {
            append("Winner: ${winner.name} (score: ${String.format("%.2f", winnerScore.overall)})\n")
            append("Breakdown:\n")
            append("- Accuracy: ${String.format("%.2f", winnerScore.accuracy)}\n")
            append("- Completeness: ${String.format("%.2f", winnerScore.completeness)}\n")
            append("- Code Quality: ${String.format("%.2f", winnerScore.codeQuality)}\n")
            append("- Explanation: ${String.format("%.2f", winnerScore.explanation)}\n")
            append("- Relevance: ${String.format("%.2f", winnerScore.relevance)}\n")
            append("- Safety: ${String.format("%.2f", winnerScore.safety)}")
        }
        
        // Вычисляем уверенность на основе разницы в скорах
        val allScores = scores.values.map { it.overall }.sorted().reversed()
        val confidence = if (allScores.size > 1) {
            val gap = allScores[0] - allScores[1]
            (gap * 2).coerceIn(0.0, 1.0)
        } else 1.0
        
        val recommendations = generateRecommendations(scores, responses)
        
        val comparisonTime = System.currentTimeMillis() - startTime
        
        ComparisonResult(
            scores = scores,
            winner = winner,
            reasoning = reasoning,
            confidence = confidence,
            criteria = usedCriteria,
            recommendations = recommendations
        )
    }
    
    override suspend fun selectBestResponse(
        responses: Map<AIProvider, AIResponse>,
        comparison: ComparisonResult?
    ): AIProvider {
        return comparison?.winner ?: responses.keys.first()
    }
    
    override suspend fun createConsensusResponse(
        responses: Map<AIProvider, AIResponse>,
        comparison: ComparisonResult
    ): AIResponse = withContext(Dispatchers.Default) {
        
        val bestResponse = responses[comparison.winner]!!
        val allContents = responses.values.filter { it.success }.map { it.content }
        
        // Простая стратегия консенсуса: берем лучший ответ и добавляем альтернативы
        val consensusContent = buildString {
            append("🎯 **Рекомендованный ответ** (${comparison.winner.name}):\n\n")
            append(bestResponse.content)
            
            if (responses.size > 1) {
                append("\n\n" + "─".repeat(50))
                append("\n🔍 **Альтернативные предложения**:\n\n")
                
                responses.forEach { (provider, response) ->
                    if (provider != comparison.winner && response.success) {
                        val score = comparison.scores[provider]?.overall
                        append("**${provider.name}** ${if (score != null) "(оценка: ${String.format("%.1f", score * 10)}/10)" else ""}:\n")
                        append("${response.content.take(200)}${if (response.content.length > 200) "..." else ""}\n\n")
                    }
                }
                
                append("💡 **Рекомендации**:\n")
                comparison.recommendations.forEach { recommendation ->
                    append("• $recommendation\n")
                }
            }
        }
        
        AIResponse(
            success = true,
            content = consensusContent,
            metadata = ResponseMetadata(
                model = "consensus",
                provider = "Multi-Model Orchestrator",
                confidence = comparison.confidence,
                suggestions = comparison.recommendations
            ),
            error = null,
            executionTimeMs = bestResponse.executionTimeMs,
            tokensUsed = responses.values.sumOf { it.tokensUsed ?: 0 },
            requestId = bestResponse.requestId
        )
    }
    
    /**
     * Анализирует качество ответа по различным критериям
     */
    private fun analyzeResponse(
        response: AIResponse,
        originalQuery: String,
        criteria: ComparisonCriteria
    ): ComparisonScore {
        // Базовая оценка на основе доступных метрик
        val accuracy = evaluateAccuracy(response, originalQuery)
        val completeness = evaluateCompleteness(response, originalQuery)
        val codeQuality = evaluateCodeQuality(response)
        val explanation = evaluateExplanation(response)
        val relevance = evaluateRelevance(response, originalQuery)
        val safety = evaluateSafety(response)
        
        return ComparisonScore.calculate(
            accuracy = accuracy,
            completeness = completeness,
            codeQuality = codeQuality,
            explanation = explanation,
            relevance = relevance,
            safety = safety,
            weights = criteria.weights
        )
    }
    
    private fun evaluateAccuracy(response: AIResponse, query: String): Double {
        // Простая эвристика: более длинные ответы считаются более точными
        // В реальной реализации можно использовать NLP модели
        val contentLength = response.content.length
        return when {
            contentLength < 50 -> 0.3
            contentLength < 200 -> 0.6
            contentLength < 500 -> 0.8
            else -> 0.9
        }.coerceIn(0.0, 1.0)
    }
    
    private fun evaluateCompleteness(response: AIResponse, query: String): Double {
        val hasCodeBlocks = response.content.contains("```")
        val hasExplanation = response.content.length > 100
        val hasExamples = response.content.contains("example", ignoreCase = true)
        
        return listOf(hasCodeBlocks, hasExplanation, hasExamples).count { it } / 3.0
    }
    
    private fun evaluateCodeQuality(response: AIResponse): Double {
        val codeBlocks = extractCodeBlocks(response.content)
        if (codeBlocks.isEmpty()) return 0.7 // Нет кода - средняя оценка
        
        return codeBlocks.map { code ->
            val hasBestPractices = code.contains("fun ") && code.contains("{") && code.contains("}")
            val hasComments = code.contains("//") || code.contains("/*")
            val isReadable = code.lines().size > 1 && code.contains(" ")
            
            listOf(hasBestPractices, hasComments, isReadable).count { it } / 3.0
        }.average()
    }
    
    private fun evaluateExplanation(response: AIResponse): Double {
        val content = response.content
        val hasStructure = content.contains("*") || content.contains("-") || content.contains("1.")
        val hasReasonablLength = content.length in 100..2000
        val hasKeywords = listOf("because", "since", "therefore", "потому что", "так как", "поэтому")
            .any { content.contains(it, ignoreCase = true) }
        
        return listOf(hasStructure, hasReasonablLength, hasKeywords).count { it } / 3.0
    }
    
    private fun evaluateRelevance(response: AIResponse, query: String): Double {
        val queryWords = query.lowercase().split("\\s+".toRegex())
        val responseWords = response.content.lowercase().split("\\s+".toRegex())
        
        val relevantWords = queryWords.intersect(responseWords.toSet())
        return (relevantWords.size.toDouble() / queryWords.size).coerceIn(0.3, 1.0)
    }
    
    private fun evaluateSafety(response: AIResponse): Double {
        val content = response.content.lowercase()
        val dangerousPatterns = listOf(
            "rm -rf", "delete", "drop table", "system(", "exec(",
            "удалить", "стереть", "форматировать"
        )
        
        val hasDangerousContent = dangerousPatterns.any { content.contains(it) }
        return if (hasDangerousContent) 0.3 else 0.9
    }
    
    private fun extractCodeBlocks(content: String): List<String> {
        val codeBlockRegex = "```[\\w]*\\n?([\\s\\S]*?)```".toRegex()
        return codeBlockRegex.findAll(content).map { it.groupValues[1] }.toList()
    }
    
    private fun generateRecommendations(
        scores: Map<AIProvider, ComparisonScore>,
        responses: Map<AIProvider, AIResponse>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Анализируем сильные стороны разных провайдеров
        val bestAccuracy = scores.maxByOrNull { it.value.accuracy }
        val bestCompleteness = scores.maxByOrNull { it.value.completeness }
        val bestCodeQuality = scores.maxByOrNull { it.value.codeQuality }
        
        if (bestAccuracy?.key != scores.maxByOrNull { it.value.overall }?.key) {
            recommendations.add("${bestAccuracy?.key?.name} показал лучшую точность")
        }
        
        if (bestCompleteness?.key != scores.maxByOrNull { it.value.overall }?.key) {
            recommendations.add("${bestCompleteness?.key?.name} дал более полный ответ")
        }
        
        if (bestCodeQuality?.key != scores.maxByOrNull { it.value.overall }?.key) {
            recommendations.add("${bestCodeQuality?.key?.name} предложил код лучшего качества")
        }
        
        // Общие рекомендации
        val avgOverallScore = scores.values.map { it.overall }.average()
        if (avgOverallScore < 0.7) {
            recommendations.add("Рассмотрите возможность переформулировать запрос для получения лучших результатов")
        }
        
        return recommendations
    }
}

/**
 * Фабрика для создания мульти-модельного оркестратора
 */
object MultiModelOrchestratorFactory {
    
    private var defaultOrchestrator: MultiModelOrchestrator = DefaultMultiModelOrchestrator()
    
    /**
     * Возвращает экземпляр оркестратора по умолчанию
     */
    fun getDefault(): MultiModelOrchestrator = defaultOrchestrator
    
    /**
     * Устанавливает кастомный оркестратор по умолчанию
     */
    fun setDefault(orchestrator: MultiModelOrchestrator) {
        defaultOrchestrator = orchestrator
    }
    
    /**
     * Создает новый экземпляр оркестратора
     */
    fun create(): MultiModelOrchestrator = DefaultMultiModelOrchestrator()
}