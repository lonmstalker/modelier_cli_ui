package com.github.lonmstalker.aiintegration.core.interfaces

import com.github.lonmstalker.aiintegration.core.history.HistoryEntry
import com.github.lonmstalker.aiintegration.core.history.HistoryFilter
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс для хранения истории AI взаимодействий
 * Реализации должны обеспечивать персистентность записей истории
 */
interface HistoryStorage {
    
    /**
     * Сохраняет запись в историю
     */
    suspend fun saveEntry(entry: HistoryEntry): Boolean
    
    /**
     * Получает записи истории с пагинацией
     */
    suspend fun getEntries(
        limit: Int = 100,
        offset: Int = 0,
        filter: HistoryFilter? = null
    ): List<HistoryEntry>
    
    /**
     * Получает количество записей в истории
     */
    suspend fun getEntriesCount(filter: HistoryFilter? = null): Long
    
    /**
     * Получает запись по ID
     */
    suspend fun getEntry(id: String): HistoryEntry?
    
    /**
     * Обновляет запись в истории
     */
    suspend fun updateEntry(entry: HistoryEntry): Boolean
    
    /**
     * Удаляет запись из истории
     */
    suspend fun deleteEntry(id: String): Boolean
    
    /**
     * Очищает всю историю
     */
    suspend fun clearHistory(): Boolean
    
    /**
     * Ищет записи по текстовому запросу
     */
    suspend fun searchEntries(
        query: String,
        limit: Int = 50
    ): List<HistoryEntry>
    
    /**
     * Получает записи за определенный период
     */
    suspend fun getEntriesByDateRange(
        startTimestamp: Long,
        endTimestamp: Long,
        limit: Int = 100
    ): List<HistoryEntry>
    
    /**
     * Получает записи по провайдеру
     */
    suspend fun getEntriesByProvider(
        provider: String,
        limit: Int = 100
    ): List<HistoryEntry>
    
    /**
     * Проверяет достижение лимита размера истории
     */
    suspend fun checkSizeLimit(maxSize: Int): Boolean
    
    /**
     * Удаляет старые записи при превышении лимита
     */
    suspend fun cleanupOldEntries(maxSize: Int): Int
    
    /**
     * Экспортирует историю в строку
     */
    suspend fun exportHistory(filter: HistoryFilter? = null): String
    
    /**
     * Импортирует историю из строки
     */
    suspend fun importHistory(data: String): ImportHistoryResult
    
    /**
     * Получает метрики использования хранилища
     */
    suspend fun getStorageMetrics(): HistoryStorageMetrics
}

/**
 * Интерфейс для уведомлений об изменениях в истории
 */
interface HistoryChangeNotifier {
    
    /**
     * Поток уведомлений об изменениях в истории
     */
    fun getChangeNotifications(): Flow<HistoryChangeEvent>
    
    /**
     * Уведомляет о добавлении записи
     */
    suspend fun notifyEntryAdded(entry: HistoryEntry)
    
    /**
     * Уведомляет об обновлении записи
     */
    suspend fun notifyEntryUpdated(entry: HistoryEntry)
    
    /**
     * Уведомляет об удалении записи
     */
    suspend fun notifyEntryDeleted(entryId: String)
    
    /**
     * Уведомляет об очистке истории
     */
    suspend fun notifyHistoryCleared(count: Int)
}

/**
 * События изменения истории
 */
sealed class HistoryChangeEvent {
    data class EntryAdded(val entry: HistoryEntry) : HistoryChangeEvent()
    data class EntryUpdated(val entry: HistoryEntry) : HistoryChangeEvent()
    data class EntryDeleted(val entryId: String) : HistoryChangeEvent()
    data class HistoryCleared(val count: Int) : HistoryChangeEvent()
}

/**
 * Результат импорта истории
 */
data class ImportHistoryResult(
    val successCount: Int,
    val failedCount: Int,
    val duplicateCount: Int,
    val errors: List<String> = emptyList()
)

/**
 * Метрики хранилища истории
 */
data class HistoryStorageMetrics(
    val totalEntries: Long,
    val storageSize: Long, // bytes
    val oldestEntryTimestamp: Long,
    val newestEntryTimestamp: Long,
    val averageEntrySize: Double,
    val providerDistribution: Map<String, Long>,
    val successRate: Double
)

/**
 * Абстрактная реализация HistoryStorage с базовой логикой
 */
abstract class AbstractHistoryStorage : HistoryStorage {
    
    /**
     * Валидирует запись перед сохранением
     */
    protected fun validateEntry(entry: HistoryEntry): List<String> {
        val errors = mutableListOf<String>()
        
        if (entry.id.isBlank()) {
            errors.add("Entry ID cannot be blank")
        }
        
        if (entry.provider.isBlank()) {
            errors.add("Provider name cannot be blank")
        }
        
        if (entry.request.requestId.isBlank()) {
            errors.add("Request ID cannot be blank")
        }
        
        return errors
    }
    
    /**
     * Применяет фильтр к списку записей
     */
    protected fun applyFilter(entries: List<HistoryEntry>, filter: HistoryFilter?): List<HistoryEntry> {
        if (filter == null) return entries
        
        return entries.filter { entry ->
            var matches = true
            
            filter.provider?.let { provider ->
                matches = matches && entry.provider.equals(provider, ignoreCase = true)
            }
            
            filter.success?.let { success ->
                matches = matches && entry.success == success
            }
            
            filter.dateFrom?.let { dateFrom ->
                matches = matches && entry.timestamp >= dateFrom
            }
            
            filter.dateTo?.let { dateTo ->
                matches = matches && entry.timestamp <= dateTo
            }
            
            filter.commandType?.let { commandType ->
                matches = matches && entry.request.command.toString().contains(commandType, ignoreCase = true)
            }
            
            if (filter.tags.isNotEmpty()) {
                matches = matches && filter.tags.any { tag -> entry.tags.contains(tag) }
            }
            
            matches
        }
    }
    
    /**
     * Выполняет текстовый поиск в записи
     */
    protected fun matchesSearchQuery(entry: HistoryEntry, query: String): Boolean {
        val lowerQuery = query.lowercase()
        
        return entry.request.command.toString().lowercase().contains(lowerQuery) ||
                entry.response.content.lowercase().contains(lowerQuery) ||
                entry.provider.lowercase().contains(lowerQuery) ||
                entry.note?.lowercase()?.contains(lowerQuery) == true ||
                entry.tags.any { it.lowercase().contains(lowerQuery) }
    }
}

/**
 * In-memory реализация для тестирования
 */
class InMemoryHistoryStorage : AbstractHistoryStorage() {
    
    private val entries = mutableMapOf<String, HistoryEntry>()
    
    override suspend fun saveEntry(entry: HistoryEntry): Boolean {
        val errors = validateEntry(entry)
        if (errors.isNotEmpty()) {
            throw IllegalArgumentException("Entry validation failed: ${errors.joinToString(", ")}")
        }
        
        entries[entry.id] = entry
        return true
    }
    
    override suspend fun getEntries(
        limit: Int,
        offset: Int,
        filter: HistoryFilter?
    ): List<HistoryEntry> {
        val filtered = applyFilter(entries.values.toList(), filter)
        val sorted = filtered.sortedByDescending { it.timestamp }
        return sorted.drop(offset).take(limit)
    }
    
    override suspend fun getEntriesCount(filter: HistoryFilter?): Long {
        return applyFilter(entries.values.toList(), filter).size.toLong()
    }
    
    override suspend fun getEntry(id: String): HistoryEntry? {
        return entries[id]
    }
    
    override suspend fun updateEntry(entry: HistoryEntry): Boolean {
        if (!entries.containsKey(entry.id)) return false
        entries[entry.id] = entry
        return true
    }
    
    override suspend fun deleteEntry(id: String): Boolean {
        return entries.remove(id) != null
    }
    
    override suspend fun clearHistory(): Boolean {
        val count = entries.size
        entries.clear()
        return count > 0
    }
    
    override suspend fun searchEntries(query: String, limit: Int): List<HistoryEntry> {
        return entries.values
            .filter { matchesSearchQuery(it, query) }
            .sortedByDescending { it.timestamp }
            .take(limit)
    }
    
    override suspend fun getEntriesByDateRange(
        startTimestamp: Long,
        endTimestamp: Long,
        limit: Int
    ): List<HistoryEntry> {
        return entries.values
            .filter { it.timestamp.toEpochMilli() in startTimestamp..endTimestamp }
            .sortedByDescending { it.timestamp }
            .take(limit)
    }
    
    override suspend fun getEntriesByProvider(provider: String, limit: Int): List<HistoryEntry> {
        return entries.values
            .filter { it.provider.equals(provider, ignoreCase = true) }
            .sortedByDescending { it.timestamp }
            .take(limit)
    }
    
    override suspend fun checkSizeLimit(maxSize: Int): Boolean {
        return entries.size >= maxSize
    }
    
    override suspend fun cleanupOldEntries(maxSize: Int): Int {
        if (entries.size <= maxSize) return 0
        
        val sorted = entries.values.sortedBy { it.timestamp }
        val toRemove = sorted.take(entries.size - maxSize)
        
        toRemove.forEach { entry ->
            entries.remove(entry.id)
        }
        
        return toRemove.size
    }
    
    override suspend fun exportHistory(filter: HistoryFilter?): String {
        val entriesToExport = applyFilter(entries.values.toList(), filter)
        // Простая JSON-like реализация
        return entriesToExport.joinToString("\n") { entry ->
            "${entry.id}|${entry.timestamp}|${entry.provider}|${entry.success}|${entry.response.content.take(100)}"
        }
    }
    
    override suspend fun importHistory(data: String): ImportHistoryResult {
        // Простая реализация импорта
        val lines = data.split("\n").filter { it.isNotBlank() }
        var successCount = 0
        val errors = mutableListOf<String>()
        
        lines.forEach { line ->
            try {
                // Простой парсинг - в реальности нужен более сложный
                successCount++
            } catch (e: Exception) {
                errors.add("Failed to parse line: $line - ${e.message}")
            }
        }
        
        return ImportHistoryResult(successCount, lines.size - successCount, 0, errors)
    }
    
    override suspend fun getStorageMetrics(): HistoryStorageMetrics {
        val allEntries = entries.values
        
        return HistoryStorageMetrics(
            totalEntries = allEntries.size.toLong(),
            storageSize = allEntries.sumOf { it.response.content.length }.toLong(),
            oldestEntryTimestamp = allEntries.minOfOrNull { it.timestamp.toEpochMilli() } ?: 0L,
            newestEntryTimestamp = allEntries.maxOfOrNull { it.timestamp.toEpochMilli() } ?: 0L,
            averageEntrySize = if (allEntries.isNotEmpty()) {
                allEntries.map { it.response.content.length }.average()
            } else 0.0,
            providerDistribution = allEntries.groupingBy { it.provider }.eachCount().mapValues { it.value.toLong() },
            successRate = if (allEntries.isNotEmpty()) {
                allEntries.count { it.success }.toDouble() / allEntries.size
            } else 0.0
        )
    }
}