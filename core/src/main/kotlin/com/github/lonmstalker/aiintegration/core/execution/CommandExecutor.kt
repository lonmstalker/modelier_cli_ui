package com.github.lonmstalker.aiintegration.core.execution

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Serializable обертка для Duration
 */
@Serializable
data class SerializableDuration(val seconds: Long) {
    fun toDuration(): Duration = Duration.ofSeconds(seconds)
    fun toMillis(): Long = seconds * 1000
    
    companion object {
        fun from(duration: Duration): SerializableDuration = SerializableDuration(duration.seconds)
    }
}

/**
 * Платформенно-независимый исполнитель команд CLI
 * 
 * Поддерживает:
 * - Выполнение команд с таймаутами
 * - Потоковое чтение вывода
 * - Обработка ошибок и таймаутов
 * - Настройка рабочей директории
 * - Переменные окружения
 */
interface CommandExecutor {
    
    /**
     * Выполняет команду синхронно
     */
    suspend fun execute(request: ExecutionRequest): ExecutionResult
    
    /**
     * Выполняет команду асинхронно с возможностью отмены
     */
    suspend fun executeAsync(
        request: ExecutionRequest,
        onOutput: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ): ExecutionResult
    
    /**
     * Проверяет доступность команды в системе
     */
    suspend fun isCommandAvailable(command: String): Boolean
    
    /**
     * Получает версию команды
     */
    suspend fun getCommandVersion(command: String): String?
}

/**
 * Реализация CommandExecutor по умолчанию
 */
class DefaultCommandExecutor : CommandExecutor {
    
    override suspend fun execute(request: ExecutionRequest): ExecutionResult = withContext(Dispatchers.IO) {
        try {
            val processBuilder = createProcessBuilder(request)
            val process = processBuilder.start()
            
            val output = StringBuilder()
            val errorOutput = StringBuilder()
            
            // Читаем stdout и stderr параллельно
            val outputJob = async {
                BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
                    lines.forEach { line ->
                        output.appendLine(line)
                    }
                }
            }
            
            val errorJob = async {
                BufferedReader(InputStreamReader(process.errorStream)).useLines { lines ->
                    lines.forEach { line ->
                        errorOutput.appendLine(line)
                    }
                }
            }
            
            // Ждем завершения процесса с таймаутом
            val completed = withTimeoutOrNull(request.timeout.toMillis()) {
                process.waitFor()
                outputJob.await()
                errorJob.await()
                true
            } ?: false
            
            if (!completed) {
                process.destroyForcibly()
                return@withContext ExecutionResult(
                    success = false,
                    output = output.toString(),
                    errorOutput = errorOutput.toString(),
                    exitCode = -1,
                    timeout = true,
                    executionTimeMs = request.timeout.toMillis()
                )
            }
            
            val exitCode = process.exitValue()
            
            ExecutionResult(
                success = exitCode == 0,
                output = output.toString(),
                errorOutput = errorOutput.toString(),
                exitCode = exitCode,
                timeout = false,
                executionTimeMs = System.currentTimeMillis() // TODO: track actual time
            )
            
        } catch (e: Exception) {
            ExecutionResult(
                success = false,
                output = "",
                errorOutput = e.message ?: "Unknown error",
                exitCode = -1,
                timeout = false,
                executionTimeMs = 0,
                exception = e
            )
        }
    }
    
    override suspend fun executeAsync(
        request: ExecutionRequest,
        onOutput: (String) -> Unit,
        onError: (String) -> Unit
    ): ExecutionResult = withContext(Dispatchers.IO) {
        try {
            val processBuilder = createProcessBuilder(request)
            val process = processBuilder.start()
            
            val output = StringBuilder()
            val errorOutput = StringBuilder()
            
            // Потоковое чтение с коллбеками
            val outputJob = async {
                BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
                    lines.forEach { line ->
                        output.appendLine(line)
                        onOutput(line)
                    }
                }
            }
            
            val errorJob = async {
                BufferedReader(InputStreamReader(process.errorStream)).useLines { lines ->
                    lines.forEach { line ->
                        errorOutput.appendLine(line)
                        onError(line)
                    }
                }
            }
            
            val completed = withTimeoutOrNull(request.timeout.toMillis()) {
                process.waitFor()
                outputJob.await()
                errorJob.await()
                true
            } ?: false
            
            if (!completed) {
                process.destroyForcibly()
                return@withContext ExecutionResult(
                    success = false,
                    output = output.toString(),
                    errorOutput = errorOutput.toString(),
                    exitCode = -1,
                    timeout = true,
                    executionTimeMs = request.timeout.toMillis()
                )
            }
            
            val exitCode = process.exitValue()
            
            ExecutionResult(
                success = exitCode == 0,
                output = output.toString(),
                errorOutput = errorOutput.toString(),
                exitCode = exitCode,
                timeout = false,
                executionTimeMs = System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            ExecutionResult(
                success = false,
                output = "",
                errorOutput = e.message ?: "Unknown error",
                exitCode = -1,
                timeout = false,
                executionTimeMs = 0,
                exception = e
            )
        }
    }
    
    override suspend fun isCommandAvailable(command: String): Boolean {
        return try {
            val request = ExecutionRequest(
                command = listOf(command, "--version"),
                timeout = Duration.ofSeconds(5)
            )
            val result = execute(request)
            result.success || result.exitCode in 0..2 // Некоторые команды возвращают 1-2 для --version
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun getCommandVersion(command: String): String? {
        return try {
            val request = ExecutionRequest(
                command = listOf(command, "--version"),
                timeout = Duration.ofSeconds(5)
            )
            val result = execute(request)
            if (result.success || result.output.isNotBlank()) {
                result.output.trim().lines().firstOrNull()
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun createProcessBuilder(request: ExecutionRequest): ProcessBuilder {
        val processBuilder = ProcessBuilder(request.command)
        
        // Настройка рабочей директории
        request.workingDirectory?.let { workDir ->
            processBuilder.directory(File(workDir))
        }
        
        // Настройка переменных окружения
        if (request.environment.isNotEmpty()) {
            val env = processBuilder.environment()
            request.environment.forEach { (key, value) ->
                env[key] = value
            }
        }
        
        // Настройка перенаправления
        if (request.redirectErrorStream) {
            processBuilder.redirectErrorStream(true)
        }
        
        return processBuilder
    }
}

/**
 * Запрос на выполнение команды
 */
@Serializable
data class ExecutionRequest(
    val command: List<String>,
    val workingDirectory: String? = null,
    val environment: Map<String, String> = emptyMap(),
    val timeout: SerializableDuration = SerializableDuration(300), // 5 минут по умолчанию
    val redirectErrorStream: Boolean = false,
    val input: String? = null
) {
    companion object {
        /**
         * Создает запрос из строки команды
         */
        fun fromString(
            commandLine: String,
            workingDirectory: String? = null,
            timeout: SerializableDuration = SerializableDuration(300)
        ): ExecutionRequest {
            val command = commandLine.split("\\s+".toRegex())
            return ExecutionRequest(command, workingDirectory, emptyMap(), timeout)
        }
    }
}

/**
 * Результат выполнения команды
 */
@Serializable
data class ExecutionResult(
    val success: Boolean,
    val output: String,
    val errorOutput: String = "",
    val exitCode: Int,
    val timeout: Boolean = false,
    val executionTimeMs: Long,
    val exception: Throwable? = null
) {
    /**
     * Объединенный вывод (stdout + stderr)
     */
    val combinedOutput: String
        get() = if (errorOutput.isBlank()) output else "$output\n$errorOutput"
    
    /**
     * Короткое описание результата
     */
    val summary: String
        get() = when {
            timeout -> "Command timed out"
            success -> "Success (exit code: $exitCode)"
            else -> "Failed (exit code: $exitCode)"
        }
}

/**
 * Исключения выполнения команд
 */
sealed class ExecutionException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class CommandNotFoundException(command: String) : ExecutionException("Command not found: $command")
    class TimeoutException(timeout: Duration) : ExecutionException("Command timed out after $timeout")
    class PermissionDeniedException(command: String) : ExecutionException("Permission denied for command: $command")
    class WorkingDirectoryNotFoundException(directory: String) : ExecutionException("Working directory not found: $directory")
}

/**
 * Утилиты для работы с командами
 */
object CommandUtils {
    
    /**
     * Экранирует аргумент команды для безопасного использования
     */
    fun escapeArgument(argument: String): String {
        return when {
            argument.contains(" ") -> "\"$argument\""
            else -> argument
        }
    }
    
    /**
     * Строит команду из отдельных частей
     */
    fun buildCommand(executable: String, vararg arguments: String): List<String> {
        return listOf(executable) + arguments.toList()
    }
    
    /**
     * Определяет является ли команда системной
     */
    fun isSystemCommand(command: String): Boolean {
        val systemCommands = setOf("ls", "dir", "cat", "type", "echo", "cd", "pwd", "mkdir", "rm", "del")
        return systemCommands.contains(command.lowercase())
    }
    
    /**
     * Получает платформенно-специфичный путь к исполняемому файлу
     */
    fun getExecutablePath(command: String): String {
        val os = System.getProperty("os.name").lowercase()
        return if (os.contains("win")) {
            if (!command.endsWith(".exe")) "$command.exe" else command
        } else {
            command
        }
    }
}

/**
 * Фабрика для создания CommandExecutor
 */
object CommandExecutorFactory {
    
    private var defaultExecutor: CommandExecutor = DefaultCommandExecutor()
    
    /**
     * Возвращает экземпляр CommandExecutor по умолчанию
     */
    fun getDefault(): CommandExecutor = defaultExecutor
    
    /**
     * Устанавливает кастомный CommandExecutor по умолчанию
     */
    fun setDefault(executor: CommandExecutor) {
        defaultExecutor = executor
    }
    
    /**
     * Создает новый экземпляр CommandExecutor
     */
    fun create(): CommandExecutor = DefaultCommandExecutor()
}