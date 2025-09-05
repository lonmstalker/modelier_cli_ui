package com.github.lonmstalker.aiintegration.core.interfaces

import com.github.lonmstalker.aiintegration.core.models.*
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс для извлечения и управления контекстом проекта
 */
interface ProjectContextProvider {
    /**
     * Извлекает контекст проекта для AI запроса
     */
    suspend fun extractContext(query: ContextQuery): ProjectContext
    
    /**
     * Строит умный контекст на основе семантического поиска
     */
    suspend fun buildSmartContext(query: String, projectPath: String, maxTokens: Int): SmartContext
    
    /**
     * Обновляет контекст после изменений в файлах
     */
    suspend fun updateContext(projectPath: String, changes: List<FileChange>)
    
    /**
     * Получает релевантные файлы для запроса
     */
    suspend fun getRelevantFiles(query: String, projectPath: String, limit: Int = 10): List<FileInfo>
    
    /**
     * Поток обновлений контекста
     */
    fun watchContextChanges(projectPath: String): Flow<ContextUpdateEvent>
}

/**
 * Запрос контекста
 */
data class ContextQuery(
    val query: String,
    val projectPath: String,
    val currentFile: String? = null,
    val selectedText: String? = null,
    val cursorPosition: CursorPosition? = null,
    val includeGitInfo: Boolean = true,
    val includeDependencies: Boolean = true,
    val maxTokens: Int = 50000
)

/**
 * Умный контекст с семантической информацией
 */
data class SmartContext(
    val primaryElements: List<CodeElement>,
    val relatedElements: List<CodeElement>,
    val dependencies: List<CodeElement>,
    val examples: List<CodeElement>,
    val documentation: List<DocumentationElement>,
    val relevanceScore: Double,
    val tokenCount: Int,
    val buildStrategy: ContextBuildStrategy
)

/**
 * Стратегия построения контекста
 */
enum class ContextBuildStrategy {
    SEMANTIC_SEARCH,
    DEPENDENCY_ANALYSIS,
    FILE_PROXIMITY,
    USAGE_PATTERNS,
    HYBRID
}

/**
 * Элемент документации
 */
data class DocumentationElement(
    val type: DocumentationType,
    val content: String,
    val source: String,
    val relevanceScore: Double = 0.0
)

/**
 * Тип документации
 */
enum class DocumentationType {
    README, API_DOC, INLINE_COMMENT, CHANGELOG, WIKI
}

/**
 * Изменение файла
 */
data class FileChange(
    val filePath: String,
    val changeType: FileChangeType,
    val content: String? = null,
    val oldContent: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Тип изменения файла
 */
enum class FileChangeType {
    CREATED, MODIFIED, DELETED, RENAMED, MOVED
}

/**
 * События обновления контекста
 */
sealed class ContextUpdateEvent {
    data class FileChanged(val change: FileChange) : ContextUpdateEvent()
    data class IndexUpdated(val affectedFiles: List<String>) : ContextUpdateEvent()
    data class DependenciesChanged(val projectPath: String) : ContextUpdateEvent()
}