package com.github.lonmstalker.aiintegration.core.history

import com.github.lonmstalker.aiintegration.core.models.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.*

/**
 * Сервис для управления историей AI взаимодействий
 */
interface HistoryService {
    
    /**
     * Сохраняет запрос и ответ в истории
     */
    suspend fun saveInteraction(request: AIRequest, response: AIResponse): HistoryEntry
    
    /**
     * Получает историю взаимодействий
     */
    suspend fun getHistory(
        limit: Int = 100,
        offset: Int = 0,
        filter: HistoryFilter? = null
    ): List<HistoryEntry>
    
    /**
     * Получает количество записей в истории
     */
    suspend fun getHistoryCount(filter: HistoryFilter? = null): Long
    
    /**
     * Ищет в истории по текстовому запросу
     */
    suspend fun searchHistory(
        query: String,
        limit: Int = 50
    ): List<HistoryEntry>
    
    /**
     * Получает запись истории по ID
     */
    suspend fun getHistoryEntry(id: String): HistoryEntry?
    
    /**
     * Удаляет запись из истории
     */
    suspend fun deleteHistoryEntry(id: String): Boolean
    
    /**
     * Очищает всю историю
     */
    suspend fun clearHistory(): Boolean
    
    /**
     * Получает поток обновлений истории
     */
    fun getHistoryUpdates(): Flow<HistoryUpdate>
    
    /**
     * Экспортирует историю в различных форматах
     */
    suspend fun exportHistory(
        format: ExportFormat,
        filter: HistoryFilter? = null
    ): String
    
    /**
     * Импортирует историю из файла
     */
    suspend fun importHistory(data: String, format: ExportFormat): ImportResult
    
    /**
     * Получает статистику использования
     */
    suspend fun getUsageStatistics(period: StatisticsPeriod): UsageStatistics
}

/**
 * Базовая реализация сервиса истории
 */
abstract class BaseHistoryService : HistoryService {
    
    protected val entries = mutableMapOf<String, HistoryEntry>()
    protected var maxHistorySize: Int = 10000
    
    override suspend fun saveInteraction(request: AIRequest, response: AIResponse): HistoryEntry {
        val entry = HistoryEntry(
            id = UUID.randomUUID().toString(),
            timestamp = Instant.now(),
            request = request,
            response = response,
            provider = response.metadata.provider ?: "unknown",
            success = response.success,
            executionTimeMs = response.executionTimeMs,
            tokensUsed = response.tokensUsed?.toLong() ?: 0L
        )
        
        entries[entry.id] = entry
        
        // Проверяем лимит размера истории
        if (entries.size > maxHistorySize) {
            val oldestEntry = entries.values.minByOrNull { it.timestamp }
            oldestEntry?.let { entries.remove(it.id) }
        }
        
        notifyHistoryUpdate(HistoryUpdate.Added(entry))
        return entry
    }
    
    override suspend fun getHistory(
        limit: Int,
        offset: Int,
        filter: HistoryFilter?
    ): List<HistoryEntry> {
        var filteredEntries = entries.values.toList()
        
        // Применяем фильтр
        filter?.let { f ->
            filteredEntries = filteredEntries.filter { entry ->
                var matches = true
                
                f.provider?.let { provider ->
                    matches = entry.provider.equals(provider, ignoreCase = true)
                }
                
                f.success?.let { success ->
                    matches = matches && entry.success == success
                }
                
                f.dateFrom?.let { dateFrom ->
                    matches = matches && entry.timestamp >= dateFrom
                }
                
                f.dateTo?.let { dateTo ->
                    matches = matches && entry.timestamp <= dateTo
                }
                
                f.commandType?.let { commandType ->
                    matches = matches && entry.request.command.toString()
                        .contains(commandType, ignoreCase = true)
                }
                
                matches
            }
        }
        
        // Сортируем по времени (новые первыми)
        filteredEntries = filteredEntries.sortedByDescending { it.timestamp }
        
        // Применяем пагинацию
        return filteredEntries.drop(offset).take(limit)
    }
    
    override suspend fun getHistoryCount(filter: HistoryFilter?): Long {
        return getHistory(Int.MAX_VALUE, 0, filter).size.toLong()
    }
    
    override suspend fun searchHistory(query: String, limit: Int): List<HistoryEntry> {
        return entries.values
            .filter { entry ->
                entry.request.command.toString().contains(query, ignoreCase = true) ||
                entry.response.content.contains(query, ignoreCase = true) ||
                entry.provider.contains(query, ignoreCase = true)
            }
            .sortedByDescending { it.timestamp }
            .take(limit)
    }
    
    override suspend fun getHistoryEntry(id: String): HistoryEntry? {
        return entries[id]
    }
    
    override suspend fun deleteHistoryEntry(id: String): Boolean {
        val entry = entries.remove(id)
        if (entry != null) {
            notifyHistoryUpdate(HistoryUpdate.Deleted(entry))
            return true
        }
        return false
    }
    
    override suspend fun clearHistory(): Boolean {
        val count = entries.size
        entries.clear()
        notifyHistoryUpdate(HistoryUpdate.Cleared(count))
        return true
    }
    
    override suspend fun exportHistory(
        format: ExportFormat,
        filter: HistoryFilter?
    ): String {
        val entriesToExport = getHistory(Int.MAX_VALUE, 0, filter)
        
        return when (format) {
            ExportFormat.JSON -> exportToJson(entriesToExport)
            ExportFormat.CSV -> exportToCsv(entriesToExport)
            ExportFormat.MARKDOWN -> exportToMarkdown(entriesToExport)
        }
    }
    
    override suspend fun importHistory(data: String, format: ExportFormat): ImportResult {
        return try {
            val importedEntries = when (format) {
                ExportFormat.JSON -> importFromJson(data)
                ExportFormat.CSV -> importFromCsv(data)
                ExportFormat.MARKDOWN -> importFromMarkdown(data)
            }
            
            var successCount = 0
            val errors = mutableListOf<String>()
            
            importedEntries.forEach { entry ->
                try {
                    entries[entry.id] = entry
                    successCount++
                } catch (e: Exception) {
                    errors.add("Failed to import entry ${entry.id}: ${e.message}")
                }
            }
            
            ImportResult(successCount, errors)
        } catch (e: Exception) {
            ImportResult(0, listOf("Import failed: ${e.message}"))
        }
    }
    
    override suspend fun getUsageStatistics(period: StatisticsPeriod): UsageStatistics {
        val periodStart = when (period) {
            StatisticsPeriod.DAY -> Instant.now().minusSeconds(24 * 3600)
            StatisticsPeriod.WEEK -> Instant.now().minusSeconds(7 * 24 * 3600)
            StatisticsPeriod.MONTH -> Instant.now().minusSeconds(30 * 24 * 3600)
            StatisticsPeriod.YEAR -> Instant.now().minusSeconds(365 * 24 * 3600)
            StatisticsPeriod.ALL -> Instant.EPOCH
        }
        
        val periodEntries = entries.values.filter { it.timestamp >= periodStart }
        
        val totalRequests = periodEntries.size
        val successfulRequests = periodEntries.count { it.success }
        val failedRequests = totalRequests - successfulRequests
        val averageExecutionTime = periodEntries.map { it.executionTimeMs }.average()
        val totalTokensUsed = periodEntries.sumOf { it.tokensUsed ?: 0 }
        
        val providerStats = periodEntries.groupBy { it.provider }
            .mapValues { (_, entries) ->
                ProviderStatistics(
                    requestCount = entries.size,
                    successRate = entries.count { it.success }.toDouble() / entries.size,
                    averageExecutionTime = entries.map { it.executionTimeMs }.average(),
                    totalTokensUsed = entries.sumOf { it.tokensUsed ?: 0 }
                )
            }
        
        val commandStats = periodEntries.groupBy { it.request.command::class.simpleName }
            .mapValues { (_, entries) ->
                CommandStatistics(
                    count = entries.size,
                    successRate = entries.count { it.success }.toDouble() / entries.size
                )
            }
        
        return UsageStatistics(
            period = period,
            totalRequests = totalRequests,
            successfulRequests = successfulRequests,
            failedRequests = failedRequests,
            successRate = if (totalRequests > 0) successfulRequests.toDouble() / totalRequests else 0.0,
            averageExecutionTime = averageExecutionTime,
            totalTokensUsed = totalTokensUsed,
            providerStats = providerStats,
            commandStats = commandStats
        )
    }
    
    protected abstract fun notifyHistoryUpdate(update: HistoryUpdate)
    
    private fun exportToJson(entries: List<HistoryEntry>): String {
        // Простая JSON реализация - в реальности лучше использовать kotlinx.serialization
        return entries.joinToString(
            prefix = "[\n",
            postfix = "\n]",
            separator = ",\n"
        ) { entry ->
            """  {
    "id": "${entry.id}",
    "timestamp": "${entry.timestamp}",
    "provider": "${entry.provider}",
    "success": ${entry.success},
    "executionTimeMs": ${entry.executionTimeMs},
    "tokensUsed": ${entry.tokensUsed},
    "request": "${entry.request.command}",
    "response": "${entry.response.content.take(100)}..."
  }"""
        }
    }
    
    private fun exportToCsv(entries: List<HistoryEntry>): String {
        val header = "ID,Timestamp,Provider,Success,ExecutionTime,TokensUsed,Request,Response\n"
        val rows = entries.joinToString("\n") { entry ->
            "${entry.id},${entry.timestamp},${entry.provider},${entry.success}," +
            "${entry.executionTimeMs},${entry.tokensUsed ?: 0}," +
            "\"${entry.request.command}\",\"${entry.response.content.replace("\"", "\"\"").take(100)}...\""
        }
        return header + rows
    }
    
    private fun exportToMarkdown(entries: List<HistoryEntry>): String {
        return buildString {
            appendLine("# AI History Export")
            appendLine()
            
            entries.forEach { entry ->
                appendLine("## ${entry.timestamp}")
                appendLine("**Provider:** ${entry.provider}")
                appendLine("**Success:** ${entry.success}")
                appendLine("**Execution Time:** ${entry.executionTimeMs}ms")
                if (entry.tokensUsed != null) {
                    appendLine("**Tokens Used:** ${entry.tokensUsed}")
                }
                appendLine()
                appendLine("### Request")
                appendLine("```")
                appendLine(entry.request.command.toString())
                appendLine("```")
                appendLine()
                appendLine("### Response")
                appendLine(entry.response.content)
                appendLine()
                appendLine("---")
                appendLine()
            }
        }
    }
    
    private fun importFromJson(data: String): List<HistoryEntry> {
        // Упрощенная реализация - в реальности нужен полный JSON парсер
        return emptyList() // TODO: implement JSON import
    }
    
    private fun importFromCsv(data: String): List<HistoryEntry> {
        // Упрощенная реализация
        return emptyList() // TODO: implement CSV import
    }
    
    private fun importFromMarkdown(data: String): List<HistoryEntry> {
        // Упрощенная реализация
        return emptyList() // TODO: implement Markdown import
    }
}

/**
 * Запись в истории
 */
data class HistoryEntry(
    val id: String,
    val timestamp: Instant,
    val request: AIRequest,
    val response: AIResponse,
    val provider: String,
    val success: Boolean,
    val executionTimeMs: Long,
    val tokensUsed: Long? = null,
    val tags: Set<String> = emptySet(),
    val note: String? = null
)

/**
 * Фильтр для истории
 */
data class HistoryFilter(
    val provider: String? = null,
    val success: Boolean? = null,
    val dateFrom: Instant? = null,
    val dateTo: Instant? = null,
    val commandType: String? = null,
    val tags: Set<String> = emptySet()
)

/**
 * Обновление истории
 */
sealed class HistoryUpdate {
    data class Added(val entry: HistoryEntry) : HistoryUpdate()
    data class Updated(val entry: HistoryEntry) : HistoryUpdate()
    data class Deleted(val entry: HistoryEntry) : HistoryUpdate()
    data class Cleared(val count: Int) : HistoryUpdate()
}

/**
 * Формат экспорта
 */
enum class ExportFormat {
    JSON, CSV, MARKDOWN
}

/**
 * Результат импорта
 */
data class ImportResult(
    val successCount: Int,
    val errors: List<String>
)

/**
 * Период для статистики
 */
enum class StatisticsPeriod {
    DAY, WEEK, MONTH, YEAR, ALL
}

/**
 * Статистика использования
 */
data class UsageStatistics(
    val period: StatisticsPeriod,
    val totalRequests: Int,
    val successfulRequests: Int,
    val failedRequests: Int,
    val successRate: Double,
    val averageExecutionTime: Double,
    val totalTokensUsed: Long,
    val providerStats: Map<String, ProviderStatistics>,
    val commandStats: Map<String, CommandStatistics>
)

/**
 * Статистика по провайдеру
 */
data class ProviderStatistics(
    val requestCount: Int,
    val successRate: Double,
    val averageExecutionTime: Double,
    val totalTokensUsed: Long
)

/**
 * Статистика по командам
 */
data class CommandStatistics(
    val count: Int,
    val successRate: Double
)

/**
 * In-memory реализация для тестирования
 */
class InMemoryHistoryService : BaseHistoryService() {
    
    private val updateListeners = mutableListOf<(HistoryUpdate) -> Unit>()
    
    override fun getHistoryUpdates(): Flow<HistoryUpdate> {
        TODO("Implement with kotlinx.coroutines.flow")
    }
    
    override fun notifyHistoryUpdate(update: HistoryUpdate) {
        updateListeners.forEach { it(update) }
    }
    
    fun addUpdateListener(listener: (HistoryUpdate) -> Unit) {
        updateListeners.add(listener)
    }
    
    fun removeUpdateListener(listener: (HistoryUpdate) -> Unit) {
        updateListeners.remove(listener)
    }
}