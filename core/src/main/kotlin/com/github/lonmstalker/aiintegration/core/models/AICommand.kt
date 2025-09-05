package com.github.lonmstalker.aiintegration.core.models

import kotlinx.serialization.Serializable

/**
 * Типы AI команд
 */
@Serializable
sealed class AICommand {
    @Serializable
    data class ExplainCode(val code: String, val language: String? = null) : AICommand()
    
    @Serializable
    data class GenerateCode(val description: String, val language: String, val context: String? = null) : AICommand()
    
    @Serializable
    data class RefactorCode(val code: String, val instructions: String, val language: String? = null) : AICommand()
    
    @Serializable
    data class FixBug(val code: String, val error: String, val language: String? = null) : AICommand()
    
    @Serializable
    data class GenerateTests(val code: String, val framework: String, val language: String? = null) : AICommand()
    
    @Serializable
    data class ReviewCode(val code: String, val reviewType: ReviewType, val language: String? = null) : AICommand()
    
    @Serializable
    data class AnalyzeArchitecture(val projectStructure: String) : AICommand()
    
    @Serializable
    data class GenerateDocumentation(val code: String, val format: DocumentationFormat, val language: String? = null) : AICommand()
    
    @Serializable
    data class OptimizePerformance(val code: String, val language: String? = null) : AICommand()
    
    @Serializable
    data class CustomPrompt(val prompt: String, val context: String? = null) : AICommand()
}

/**
 * Типы code review
 */
@Serializable
enum class ReviewType {
    GENERAL,
    SECURITY,
    PERFORMANCE,
    BEST_PRACTICES,
    STYLE,
    BUGS
}

/**
 * Форматы документации
 */
@Serializable
enum class DocumentationFormat {
    MARKDOWN,
    JAVADOC,
    KDOC,
    SPHINX,
    DOXYGEN
}