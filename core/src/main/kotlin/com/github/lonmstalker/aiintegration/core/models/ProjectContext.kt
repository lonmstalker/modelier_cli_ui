package com.github.lonmstalker.aiintegration.core.models

import kotlinx.serialization.Serializable

/**
 * Контекст проекта для AI
 */
@Serializable
data class ProjectContext(
    val projectPath: String,
    val projectName: String,
    val selectedFiles: List<FileInfo> = emptyList(),
    val currentFile: FileInfo? = null,
    val selectedText: String? = null,
    val cursorPosition: CursorPosition? = null,
    val gitStatus: GitStatus? = null,
    val projectStructure: ProjectStructure? = null,
    val relevantDependencies: List<Dependency> = emptyList(),
    val semanticContext: List<CodeElement> = emptyList(),
    val language: String? = null,
    val framework: String? = null
)

/**
 * Информация о файле
 */
@Serializable
data class FileInfo(
    val path: String,
    val relativePath: String,
    val name: String,
    val extension: String,
    val size: Long,
    val lastModified: Long,
    val content: String? = null,
    val language: String? = null
)

/**
 * Позиция курсора
 */
@Serializable
data class CursorPosition(
    val line: Int,
    val column: Int,
    val offset: Int
)

/**
 * Git статус
 */
@Serializable
data class GitStatus(
    val currentBranch: String?,
    val hasUncommittedChanges: Boolean,
    val modifiedFiles: List<String> = emptyList(),
    val addedFiles: List<String> = emptyList(),
    val deletedFiles: List<String> = emptyList(),
    val recentCommits: List<CommitInfo> = emptyList()
)

/**
 * Информация о коммите
 */
@Serializable
data class CommitInfo(
    val hash: String,
    val message: String,
    val author: String,
    val timestamp: Long,
    val changedFiles: List<String> = emptyList()
)

/**
 * Структура проекта
 */
@Serializable
data class ProjectStructure(
    val rootDirectory: String,
    val sourceDirectories: List<String> = emptyList(),
    val testDirectories: List<String> = emptyList(),
    val buildFiles: List<String> = emptyList(),
    val configFiles: List<String> = emptyList(),
    val packageStructure: List<PackageInfo> = emptyList(),
    val buildTool: BuildTool? = null
)

/**
 * Информация о пакете
 */
@Serializable
data class PackageInfo(
    val name: String,
    val path: String,
    val classes: List<String> = emptyList(),
    val subPackages: List<String> = emptyList()
)

/**
 * Инструмент сборки
 */
@Serializable
enum class BuildTool {
    GRADLE, MAVEN, SBT, BAZEL, NPM, YARN, PIP, CARGO, GO_MOD
}

/**
 * Зависимость проекта
 */
@Serializable
data class Dependency(
    val name: String,
    val version: String,
    val group: String? = null,
    val scope: String? = null,
    val type: DependencyType = DependencyType.EXTERNAL
)

/**
 * Тип зависимости
 */
@Serializable
enum class DependencyType {
    EXTERNAL, INTERNAL, SYSTEM
}