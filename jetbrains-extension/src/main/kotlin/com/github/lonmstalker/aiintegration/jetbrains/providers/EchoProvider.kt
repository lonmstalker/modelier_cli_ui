package com.github.lonmstalker.aiintegration.jetbrains.providers

import com.github.lonmstalker.aiintegration.core.interfaces.AIProvider
import com.github.lonmstalker.aiintegration.core.interfaces.ModelInfo
import com.github.lonmstalker.aiintegration.core.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.delay

/**
 * Simple echo provider for testing and demonstration.
 * Returns echo responses to show that the chat interface is working.
 */
class EchoProvider : AIProvider {
    
    override val name: String = "Echo Provider"
    
    override val capabilities: Set<AICapability> = setOf(
        AICapability.CODE_EXPLANATION,
        AICapability.CODE_GENERATION,
        AICapability.CODE_REFACTORING,
        AICapability.CODE_REVIEW
    )
    
    override val configuration: AIProviderConfiguration = AIProviderConfiguration(
        apiKey = null,
        apiUrl = null,
        model = "echo-v1",
        maxTokens = 4000,
        temperature = 0.7,
        customParameters = mapOf("type" to "echo")
    )
    
    override suspend fun isAvailable(): Boolean = true
    
    override suspend fun execute(request: AIRequest): AIResponse {
        // Simulate processing delay
        delay(500)
        
        val userMessage = when (val command = request.command) {
            is AICommand.CustomPrompt -> command.prompt
            is AICommand.ExplainCode -> "Explain: ${command.code}"
            is AICommand.GenerateCode -> "Generate: ${command.description}"
            is AICommand.RefactorCode -> "Refactor: ${command.code}"
            is AICommand.ReviewCode -> "Review: ${command.code}"
            is AICommand.FixBug -> "Fix bug in: ${command.code}"
            is AICommand.GenerateTests -> "Generate tests for: ${command.code}"
            is AICommand.AnalyzeArchitecture -> "Analyze: ${command.projectStructure}"
            is AICommand.GenerateDocumentation -> "Document: ${command.code}"
            is AICommand.OptimizePerformance -> "Optimize: ${command.code}"
        }
        
        val echoResponse = "Echo: $userMessage\n\n" +
                "This is a test response from the Echo Provider. " +
                "Your AI CLI Integration plugin is working correctly!"
        
        return AIResponse(
            success = true,
            content = echoResponse,
            metadata = ResponseMetadata(
                model = "echo-v1",
                provider = "Echo Provider"
            ),
            error = null,
            executionTimeMs = 500,
            tokensUsed = userMessage.length,
            requestId = request.requestId
        )
    }
    
    override suspend fun streamResponse(request: AIRequest): Flow<AIResponseChunk> {
        val response = execute(request)
        return flowOf(
            AIResponseChunk(
                content = response.content,
                isComplete = true,
                requestId = request.requestId
            )
        )
    }
    
    override suspend fun validateConfiguration(): ValidationResult {
        return ValidationResult(
            isValid = true,
            errors = emptyList(),
            warnings = emptyList(),
            recommendations = listOf("Echo provider is always available for testing")
        )
    }
    
    override suspend fun getUsageStats(): UsageStats {
        return UsageStats(
            totalRequests = 0,
            successfulRequests = 0,
            failedRequests = 0,
            averageResponseTimeMs = 500.0,
            totalTokensUsed = 0,
            estimatedCost = 0.0,
            lastUsed = null,
            topCommands = emptyMap()
        )
    }
    
    override suspend fun updateConfiguration(config: AIProviderConfiguration) {
        // Echo provider doesn't need configuration updates
    }
    
    override suspend fun getAvailableModels(): List<ModelInfo> {
        return listOf(
            ModelInfo(
                id = "echo-v1",
                name = "Echo Model v1",
                description = "Simple echo model for testing",
                maxTokens = 4000,
                costPerToken = 0.0,
                capabilities = capabilities,
                isDefault = true
            )
        )
    }
}