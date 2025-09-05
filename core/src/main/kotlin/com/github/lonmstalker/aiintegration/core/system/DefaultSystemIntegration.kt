package com.github.lonmstalker.aiintegration.core.system

import com.github.lonmstalker.aiintegration.core.interfaces.*
import com.github.lonmstalker.aiintegration.core.execution.CommandExecutor
import com.github.lonmstalker.aiintegration.core.execution.CommandExecutorFactory
import com.github.lonmstalker.aiintegration.core.execution.ExecutionRequest
import com.github.lonmstalker.aiintegration.core.execution.SerializableDuration
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.time.Duration
import kotlin.io.path.exists
import kotlin.io.path.isExecutable
import kotlin.io.path.isReadable
import kotlin.io.path.isWritable

/**
 * Реализация системной интеграции по умолчанию
 */
class DefaultSystemIntegration : SystemIntegration {
    
    private val commandExecutor: CommandExecutor = CommandExecutorFactory.getDefault()
    
    override fun detectPlatform(): Platform {
        return Platform.current()
    }
    
    override suspend fun findExecutable(
        name: String,
        searchPaths: List<String>
    ): ExecutableInfo? {
        val platform = detectPlatform()
        val executableName = getExecutableName(name, platform)
        
        // Сначала проверяем searchPaths
        searchPaths.forEach { searchPath ->
            val execPath = File(searchPath, executableName)
            if (execPath.exists() && execPath.canExecute()) {
                return createExecutableInfo(execPath, platform)
            }
        }
        
        // Затем проверяем PATH
        val pathVar = getEnvironmentVariable("PATH") ?: ""
        val pathSeparator = if (platform == Platform.WINDOWS) ";" else ":"
        
        pathVar.split(pathSeparator).forEach { pathDir ->
            if (pathDir.isNotBlank()) {
                val execPath = File(pathDir.trim(), executableName)
                if (execPath.exists() && execPath.canExecute()) {
                    return createExecutableInfo(execPath, platform)
                }
            }
        }
        
        // Проверяем стандартные пути
        getStandardPaths(platform).forEach { standardPath ->
            val execPath = File(standardPath, executableName)
            if (execPath.exists() && execPath.canExecute()) {
                return createExecutableInfo(execPath, platform)
            }
        }
        
        return null
    }
    
    override fun getEnvironmentVariables(): Map<String, String> {
        return System.getenv()
    }
    
    override fun getEnvironmentVariable(name: String): String? {
        return System.getenv(name)
    }
    
    override suspend fun executeCommand(command: SystemCommand): CommandResult {
        val commandList = listOf(command.executable) + command.arguments
        
        val request = ExecutionRequest(
            command = commandList,
            workingDirectory = command.workingDirectory,
            environment = command.environment,
            timeout = SerializableDuration(command.timeoutSeconds),
            input = command.inputData
        )
        
        val startTime = System.currentTimeMillis()
        
        return try {
            val result = commandExecutor.execute(request)
            val executionTime = System.currentTimeMillis() - startTime
            
            CommandResult(
                success = result.success,
                exitCode = result.exitCode,
                stdout = result.output,
                stderr = result.errorOutput,
                executionTimeMs = executionTime,
                timedOut = result.timeout,
                command = command
            )
        } catch (e: Exception) {
            CommandResult(
                success = false,
                exitCode = -1,
                stdout = "",
                stderr = e.message ?: "Unknown error",
                executionTimeMs = System.currentTimeMillis() - startTime,
                timedOut = false,
                command = command
            )
        }
    }
    
    override suspend fun checkFilePermissions(path: String): Set<FilePermission> {
        val permissions = mutableSetOf<FilePermission>()
        
        try {
            val filePath = Path.of(path)
            
            if (!filePath.exists()) {
                return emptySet()
            }
            
            if (filePath.isReadable()) {
                permissions.add(FilePermission.READ)
            }
            
            if (filePath.isWritable()) {
                permissions.add(FilePermission.WRITE)
            }
            
            if (filePath.isExecutable()) {
                permissions.add(FilePermission.EXECUTE)
            }
            
        } catch (e: Exception) {
            // Если не можем проверить права, возвращаем пустое множество
        }
        
        return permissions
    }
    
    override fun getSystemInfo(): SystemInfo {
        val runtime = Runtime.getRuntime()
        
        return SystemInfo(
            platform = detectPlatform(),
            osName = System.getProperty("os.name") ?: "Unknown",
            osVersion = System.getProperty("os.version") ?: "Unknown",
            architecture = System.getProperty("os.arch") ?: "Unknown",
            javaVersion = System.getProperty("java.version") ?: "Unknown",
            availableProcessors = runtime.availableProcessors(),
            totalMemory = runtime.totalMemory(),
            freeMemory = runtime.freeMemory()
        )
    }
    
    private fun getExecutableName(name: String, platform: Platform): String {
        return when (platform) {
            Platform.WINDOWS -> if (name.endsWith(".exe")) name else "$name.exe"
            else -> name
        }
    }
    
    private fun createExecutableInfo(execFile: File, platform: Platform): ExecutableInfo {
        val permissions = mutableSetOf<FilePermission>()
        
        if (execFile.canRead()) permissions.add(FilePermission.READ)
        if (execFile.canWrite()) permissions.add(FilePermission.WRITE)
        if (execFile.canExecute()) permissions.add(FilePermission.EXECUTE)
        
        return ExecutableInfo(
            path = execFile.absolutePath,
            version = getExecutableVersion(execFile),
            isValid = execFile.exists() && execFile.canExecute(),
            permissions = permissions,
            platform = platform,
            size = execFile.length(),
            lastModified = execFile.lastModified()
        )
    }
    
    private fun getExecutableVersion(execFile: File): String? {
        return try {
            // Пытаемся получить версию через --version
            val versionCommand = SystemCommand(
                executable = execFile.absolutePath,
                arguments = listOf("--version"),
                timeoutSeconds = 5
            )
            
            val result = java.util.concurrent.CompletableFuture.supplyAsync {
                try {
                    val process = ProcessBuilder(versionCommand.executable, *versionCommand.arguments.toTypedArray())
                        .redirectErrorStream(true)
                        .start()
                    
                    val output = process.inputStream.bufferedReader().readText()
                    process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
                    
                    if (process.exitValue() == 0) {
                        output.lines().firstOrNull()?.trim()
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }.get(6, java.util.concurrent.TimeUnit.SECONDS)
            
            result
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getStandardPaths(platform: Platform): List<String> {
        return when (platform) {
            Platform.WINDOWS -> listOf(
                "C:\\Windows\\System32",
                "C:\\Windows",
                "C:\\Program Files",
                "C:\\Program Files (x86)"
            )
            Platform.MACOS -> listOf(
                "/usr/bin",
                "/usr/local/bin",
                "/opt/homebrew/bin",
                "/Applications",
                "/System/Applications"
            )
            Platform.LINUX -> listOf(
                "/usr/bin",
                "/usr/local/bin",
                "/bin",
                "/sbin",
                "/usr/sbin",
                "/snap/bin"
            )
            Platform.UNKNOWN -> emptyList()
        }
    }
}

/**
 * Утилиты для работы с системой
 */
object SystemUtils {
    
    /**
     * Проверяет доступность команды в системе
     */
    suspend fun isCommandAvailable(command: String): Boolean {
        val systemIntegration = DefaultSystemIntegration()
        return systemIntegration.findExecutable(command) != null
    }
    
    /**
     * Получает информацию о доступных AI CLI инструментах
     */
    suspend fun detectAvailableAITools(): List<AIToolInfo> {
        val systemIntegration = DefaultSystemIntegration()
        val commonTools = listOf(
            "claude-code", "claude", "aider", "cursor",
            "copilot", "github-copilot", "codex",
            "chatgpt", "gpt", "ollama", "llama"
        )
        
        val detectedTools = mutableListOf<AIToolInfo>()
        
        commonTools.forEach { toolName ->
            val executable = systemIntegration.findExecutable(toolName)
            if (executable != null) {
                detectedTools.add(
                    AIToolInfo(
                        name = toolName,
                        executablePath = executable.path,
                        version = executable.version,
                        isAvailable = true,
                        type = detectToolType(toolName)
                    )
                )
            }
        }
        
        return detectedTools
    }
    
    /**
     * Создает безопасное окружение для выполнения команд
     */
    fun createSafeEnvironment(baseEnv: Map<String, String> = System.getenv()): Map<String, String> {
        val safeEnv = baseEnv.toMutableMap()
        
        // Удаляем потенциально опасные переменные
        val dangerousVars = setOf(
            "LD_PRELOAD", "DYLD_INSERT_LIBRARIES", "DYLD_LIBRARY_PATH",
            "PYTHONPATH", "RUBYLIB", "PERL5LIB"
        )
        
        dangerousVars.forEach { safeEnv.remove(it) }
        
        // Добавляем безопасные значения по умолчанию
        safeEnv.putIfAbsent("LANG", "en_US.UTF-8")
        safeEnv.putIfAbsent("LC_ALL", "en_US.UTF-8")
        
        return safeEnv
    }
    
    private fun detectToolType(toolName: String): AIToolType {
        return when (toolName.lowercase()) {
            "claude-code", "claude" -> AIToolType.CLAUDE
            "copilot", "github-copilot" -> AIToolType.GITHUB_COPILOT
            "cursor" -> AIToolType.CURSOR
            "aider" -> AIToolType.AIDER
            "codex" -> AIToolType.CODEX
            "chatgpt", "gpt" -> AIToolType.OPENAI
            "ollama" -> AIToolType.OLLAMA
            "llama" -> AIToolType.LLAMA
            else -> AIToolType.UNKNOWN
        }
    }
}

/**
 * Информация о AI инструменте
 */
data class AIToolInfo(
    val name: String,
    val executablePath: String,
    val version: String?,
    val isAvailable: Boolean,
    val type: AIToolType
)

/**
 * Типы AI инструментов
 */
enum class AIToolType {
    CLAUDE,
    GITHUB_COPILOT,
    CURSOR,
    AIDER,
    CODEX,
    OPENAI,
    OLLAMA,
    LLAMA,
    UNKNOWN
}

/**
 * Менеджер системных ресурсов
 */
class SystemResourceManager {
    
    private val systemIntegration = DefaultSystemIntegration()
    
    /**
     * Мониторит использование системных ресурсов
     */
    fun getResourceUsage(): ResourceUsage {
        val runtime = Runtime.getRuntime()
        val systemInfo = systemIntegration.getSystemInfo()
        
        return ResourceUsage(
            totalMemory = runtime.totalMemory(),
            freeMemory = runtime.freeMemory(),
            usedMemory = runtime.totalMemory() - runtime.freeMemory(),
            maxMemory = runtime.maxMemory(),
            availableProcessors = systemInfo.availableProcessors,
            systemLoadAverage = getSystemLoadAverage()
        )
    }
    
    /**
     * Проверяет доступность системных ресурсов
     */
    fun checkResourceAvailability(): ResourceAvailability {
        val usage = getResourceUsage()
        val memoryUsagePercent = (usage.usedMemory.toDouble() / usage.totalMemory) * 100
        
        return ResourceAvailability(
            hasEnoughMemory = memoryUsagePercent < 90,
            hasEnoughDisk = checkDiskSpace(),
            systemLoad = usage.systemLoadAverage,
            recommendations = generateRecommendations(usage)
        )
    }
    
    private fun getSystemLoadAverage(): Double {
        return try {
            val osBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean()
            if (osBean is com.sun.management.OperatingSystemMXBean) {
                osBean.systemLoadAverage
            } else {
                -1.0 // Недоступно на этой платформе
            }
        } catch (e: Exception) {
            -1.0
        }
    }
    
    private fun checkDiskSpace(): Boolean {
        return try {
            val currentDir = File(System.getProperty("user.dir"))
            val freeSpace = currentDir.freeSpace
            val totalSpace = currentDir.totalSpace
            
            if (totalSpace > 0) {
                val freeSpacePercent = (freeSpace.toDouble() / totalSpace) * 100
                freeSpacePercent > 10 // Есть больше 10% свободного места
            } else {
                true // Не можем определить, предполагаем что места достаточно
            }
        } catch (e: Exception) {
            true
        }
    }
    
    private fun generateRecommendations(usage: ResourceUsage): List<String> {
        val recommendations = mutableListOf<String>()
        
        val memoryUsagePercent = (usage.usedMemory.toDouble() / usage.totalMemory) * 100
        
        if (memoryUsagePercent > 80) {
            recommendations.add("Высокое использование памяти (${String.format("%.1f", memoryUsagePercent)}%). Рекомендуется перезапустить приложение.")
        }
        
        if (usage.systemLoadAverage > usage.availableProcessors * 0.8) {
            recommendations.add("Высокая нагрузка на процессор. Рекомендуется уменьшить количество параллельных задач.")
        }
        
        return recommendations
    }
}

/**
 * Использование ресурсов
 */
data class ResourceUsage(
    val totalMemory: Long,
    val freeMemory: Long,
    val usedMemory: Long,
    val maxMemory: Long,
    val availableProcessors: Int,
    val systemLoadAverage: Double
)

/**
 * Доступность ресурсов
 */
data class ResourceAvailability(
    val hasEnoughMemory: Boolean,
    val hasEnoughDisk: Boolean,
    val systemLoad: Double,
    val recommendations: List<String>
)