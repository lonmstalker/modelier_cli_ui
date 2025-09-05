package com.github.lonmstalker.aiintegration.core.models

import kotlinx.serialization.Serializable

/**
 * Запрос к AI
 */
@Serializable
data class AIRequest(
    val command: AICommand,
    val context: ProjectContext,
    val parameters: Map<String, String> = emptyMap(),
    val targetProvider: String? = null,
    val priority: RequestPriority = RequestPriority.NORMAL,
    val requestId: String = generateRequestId()
)

/**
 * Приоритет запроса
 */
@Serializable
enum class RequestPriority {
    LOW, NORMAL, HIGH, URGENT
}

private fun generateRequestId(): String = java.util.UUID.randomUUID().toString()