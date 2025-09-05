package com.github.lonmstalker.aiintegration.core.models

import kotlinx.serialization.Serializable

/**
 * Ответ от AI
 */
@Serializable
data class AIResponse(
    val success: Boolean,
    val content: String,
    val metadata: ResponseMetadata = ResponseMetadata(),
    val error: AIError? = null,
    val executionTimeMs: Long,
    val tokensUsed: Int? = null,
    val requestId: String
)

/**
 * Часть потокового ответа
 */
@Serializable
data class AIResponseChunk(
    val content: String,
    val isComplete: Boolean = false,
    val requestId: String,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Метаданные ответа
 */
@Serializable
data class ResponseMetadata(
    val model: String? = null,
    val provider: String? = null,
    val confidence: Double? = null,
    val suggestions: List<String> = emptyList(),
    val codeBlocks: List<CodeBlock> = emptyList(),
    val executionSteps: List<String> = emptyList()
)

/**
 * Блок кода в ответе
 */
@Serializable
data class CodeBlock(
    val language: String,
    val code: String,
    val description: String? = null,
    val fileName: String? = null,
    val startLine: Int? = null,
    val endLine: Int? = null
)