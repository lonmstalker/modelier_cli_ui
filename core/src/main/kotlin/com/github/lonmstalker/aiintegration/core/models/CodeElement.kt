package com.github.lonmstalker.aiintegration.core.models

import kotlinx.serialization.Serializable

/**
 * Семантический элемент кода
 */
@Serializable
data class CodeElement(
    val id: String,
    val type: ElementType,
    val name: String,
    val file: FileInfo,
    val range: TextRange,
    val signature: String? = null,
    val documentation: String? = null,
    val embedding: List<Float> = emptyList(), // Serializable vector
    val dependencies: List<String> = emptyList(),
    val usages: List<CodeReference> = emptyList(),
    val relevanceScore: Double = 0.0,
    val complexity: Int? = null,
    val annotations: List<String> = emptyList()
)

/**
 * Тип элемента кода
 */
@Serializable
enum class ElementType {
    CLASS, 
    INTERFACE, 
    FUNCTION, 
    METHOD, 
    VARIABLE, 
    CONSTANT, 
    FIELD,
    PROPERTY,
    ENUM, 
    ANNOTATION, 
    PACKAGE, 
    MODULE, 
    COMMENT, 
    TEST,
    CONSTRUCTOR,
    LAMBDA
}

/**
 * Диапазон текста
 */
@Serializable
data class TextRange(
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
    val startOffset: Int,
    val endOffset: Int
)

/**
 * Ссылка на код
 */
@Serializable
data class CodeReference(
    val file: String,
    val range: TextRange,
    val context: String? = null,
    val referenceType: ReferenceType = ReferenceType.USAGE
)

/**
 * Тип ссылки
 */
@Serializable
enum class ReferenceType {
    USAGE, 
    DEFINITION, 
    DECLARATION, 
    IMPLEMENTATION, 
    INHERITANCE, 
    IMPORT, 
    CALL
}