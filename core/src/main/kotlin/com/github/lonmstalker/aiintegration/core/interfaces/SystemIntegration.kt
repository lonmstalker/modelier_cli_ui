package com.github.lonmstalker.aiintegration.core.interfaces

import com.github.lonmstalker.aiintegration.core.models.*

/**
 * Интерфейс для взаимодействия с операционной системой
 */
interface SystemIntegration {
    /**
     * Определяет текущую платформу
     */
    fun detectPlatform(): Platform
    
    /**
     * Ищет исполняемый файл в системе
     */
    suspend fun findExecutable(name: String, searchPaths: List<String> = emptyList()): ExecutableInfo?
    
    /**
     * Получает переменные окружения
     */
    fun getEnvironmentVariables(): Map<String, String>
    
    /**
     * Получает переменную окружения
     */
    fun getEnvironmentVariable(name: String): String?
    
    /**
     * Выполняет системную команду
     */
    suspend fun executeCommand(command: SystemCommand): CommandResult
    
    /**
     * Проверяет права доступа к файлу
     */
    suspend fun checkFilePermissions(path: String): Set<FilePermission>
    
    /**
     * Получает информацию о системе
     */
    fun getSystemInfo(): SystemInfo
}

/**
 * Платформы
 */
enum class Platform {
    WINDOWS, MACOS, LINUX, UNKNOWN;
    
    companion object {
        fun current(): Platform {
            val osName = System.getProperty("os.name").lowercase()
            return when {
                osName.contains("win") -> WINDOWS
                osName.contains("mac") -> MACOS
                osName.contains("nix") || osName.contains("nux") || osName.contains("aix") -> LINUX
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Информация об исполняемом файле
 */
data class ExecutableInfo(
    val path: String,
    val version: String? = null,
    val isValid: Boolean,
    val permissions: Set<FilePermission>,
    val platform: Platform,
    val size: Long = 0L,
    val lastModified: Long = 0L
)

/**
 * Права доступа к файлам
 */
enum class FilePermission {
    READ, WRITE, EXECUTE
}

/**
 * Системная команда
 */
data class SystemCommand(
    val executable: String,
    val arguments: List<String> = emptyList(),
    val workingDirectory: String? = null,
    val environment: Map<String, String> = emptyMap(),
    val timeoutSeconds: Long = 30,
    val inputData: String? = null
)

/**
 * Результат выполнения команды
 */
data class CommandResult(
    val success: Boolean,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val executionTimeMs: Long,
    val timedOut: Boolean = false,
    val command: SystemCommand
)

/**
 * Информация о системе
 */
data class SystemInfo(
    val platform: Platform,
    val osName: String,
    val osVersion: String,
    val architecture: String,
    val javaVersion: String,
    val availableProcessors: Int,
    val totalMemory: Long,
    val freeMemory: Long
)