package com.github.lonmstalker.aiintegration.core.vectordb

import com.github.lonmstalker.aiintegration.core.interfaces.SearchResult
import com.github.lonmstalker.aiintegration.core.interfaces.VectorDatabase
import com.github.lonmstalker.aiintegration.core.interfaces.VectorItem
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

/**
 * Реализация векторной базы данных в памяти
 * Подходит для небольших объемов данных и тестирования
 */
class InMemoryVectorDatabase : VectorDatabase {
    
    private val vectors = ConcurrentHashMap<String, VectorEntry>()
    
    data class VectorEntry(
        val id: String,
        val vector: FloatArray,
        val metadata: Map<String, Any>,
        val norm: Double = calculateNorm(vector)
    ) {
        companion object {
            private fun calculateNorm(vector: FloatArray): Double {
                return sqrt(vector.map { it.toDouble().pow(2) }.sum())
            }
        }
        
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as VectorEntry
            
            if (id != other.id) return false
            if (!vector.contentEquals(other.vector)) return false
            if (metadata != other.metadata) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + vector.contentHashCode()
            result = 31 * result + metadata.hashCode()
            return result
        }
    }
    
    override suspend fun add(id: String, vector: FloatArray, metadata: Map<String, Any>) {
        vectors[id] = VectorEntry(id, vector, metadata)
    }
    
    override suspend fun addBatch(items: List<VectorItem>) {
        items.forEach { item ->
            vectors[item.id] = VectorEntry(item.id, item.vector, item.metadata)
        }
    }
    
    override suspend fun search(
        queryVector: FloatArray,
        limit: Int,
        threshold: Double,
        filter: Map<String, Any>
    ): List<SearchResult> {
        val queryNorm = sqrt(queryVector.map { it.toDouble().pow(2) }.sum())
        
        return vectors.values
            .filter { entry ->
                // Применяем фильтр метаданных
                if (filter.isEmpty()) return@filter true
                
                filter.all { (key, value) ->
                    entry.metadata[key] == value
                }
            }
            .map { entry ->
                val similarity = calculateCosineSimilarity(queryVector, entry.vector, queryNorm, entry.norm)
                SearchResult(
                    id = entry.id,
                    similarity = similarity,
                    metadata = entry.metadata,
                    vector = entry.vector
                )
            }
            .filter { it.similarity >= threshold }
            .sortedByDescending { it.similarity }
            .take(limit)
    }
    
    override suspend fun update(id: String, vector: FloatArray, metadata: Map<String, Any>) {
        if (vectors.containsKey(id)) {
            vectors[id] = VectorEntry(id, vector, metadata)
        }
    }
    
    override suspend fun delete(id: String) {
        vectors.remove(id)
    }
    
    override suspend fun deleteBatch(ids: List<String>) {
        ids.forEach { vectors.remove(it) }
    }
    
    override suspend fun clear() {
        vectors.clear()
    }
    
    override suspend fun size(): Long {
        return vectors.size.toLong()
    }
    
    override suspend fun exists(id: String): Boolean {
        return vectors.containsKey(id)
    }
    
    override suspend fun getMetadata(id: String): Map<String, Any>? {
        return vectors[id]?.metadata
    }
    
    override suspend fun buildIndex() {
        // В in-memory реализации индекс не нужен
    }
    
    override suspend fun close() {
        vectors.clear()
    }
    
    private fun calculateCosineSimilarity(
        vector1: FloatArray,
        vector2: FloatArray,
        norm1: Double,
        norm2: Double
    ): Double {
        if (vector1.size != vector2.size) return 0.0
        if (norm1 == 0.0 || norm2 == 0.0) return 0.0
        
        val dotProduct = vector1.zip(vector2) { a, b -> a * b }.sum().toDouble()
        return dotProduct / (norm1 * norm2)
    }
}

/**
 * Файловая реализация векторной базы данных
 * Использует простое персистентное хранение
 */
class FileVectorDatabase(private val filePath: String) : VectorDatabase {
    
    private val inMemoryDB = InMemoryVectorDatabase()
    
    init {
        loadFromFile()
    }
    
    override suspend fun add(id: String, vector: FloatArray, metadata: Map<String, Any>) {
        inMemoryDB.add(id, vector, metadata)
        saveToFile()
    }
    
    override suspend fun addBatch(items: List<VectorItem>) {
        inMemoryDB.addBatch(items)
        saveToFile()
    }
    
    override suspend fun search(
        queryVector: FloatArray,
        limit: Int,
        threshold: Double,
        filter: Map<String, Any>
    ): List<SearchResult> {
        return inMemoryDB.search(queryVector, limit, threshold, filter)
    }
    
    override suspend fun update(id: String, vector: FloatArray, metadata: Map<String, Any>) {
        inMemoryDB.update(id, vector, metadata)
        saveToFile()
    }
    
    override suspend fun delete(id: String) {
        inMemoryDB.delete(id)
        saveToFile()
    }
    
    override suspend fun deleteBatch(ids: List<String>) {
        inMemoryDB.deleteBatch(ids)
        saveToFile()
    }
    
    override suspend fun clear() {
        inMemoryDB.clear()
        saveToFile()
    }
    
    override suspend fun size(): Long = inMemoryDB.size()
    
    override suspend fun exists(id: String): Boolean = inMemoryDB.exists(id)
    
    override suspend fun getMetadata(id: String): Map<String, Any>? = inMemoryDB.getMetadata(id)
    
    override suspend fun buildIndex() {
        // В данной реализации индекс не строится
    }
    
    override suspend fun close() {
        saveToFile()
        inMemoryDB.close()
    }
    
    private fun loadFromFile() {
        try {
            val file = java.io.File(filePath)
            if (!file.exists()) return
            
            // Простая десериализация - в реальности лучше использовать protobuf или другой формат
            val lines = file.readLines()
            lines.forEach { line ->
                try {
                    val parts = line.split("|", limit = 4)
                    if (parts.size >= 3) {
                        val id = parts[0]
                        val vector = parts[1].split(",").map { it.toFloat() }.toFloatArray()
                        val metadata = parseMetadata(parts.getOrNull(2) ?: "")
                        
                        inMemoryDB.add(id, vector, metadata)
                    }
                } catch (e: Exception) {
                    // Игнорируем поврежденные строки
                }
            }
        } catch (e: Exception) {
            // Если файл не может быть загружен, продолжаем с пустой БД
        }
    }
    
    private fun saveToFile() {
        try {
            val file = java.io.File(filePath)
            file.parentFile?.mkdirs()
            
            file.writeText("")
            
            // Простая сериализация
            // В реальной реализации лучше использовать более эффективный формат
            // TODO: Implement proper serialization
        } catch (e: Exception) {
            // Логирование ошибки сохранения
        }
    }
    
    private fun parseMetadata(metadataStr: String): Map<String, Any> {
        if (metadataStr.isEmpty()) return emptyMap()
        
        return try {
            // Простой парсинг key:value,key:value
            metadataStr.split(",")
                .associate { pair ->
                    val (key, value) = pair.split(":", limit = 2)
                    key.trim() to value.trim()
                }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

/**
 * Утилиты для работы с векторами
 */
object VectorUtils {
    
    /**
     * Нормализует вектор
     */
    fun normalizeVector(vector: FloatArray): FloatArray {
        val norm = sqrt(vector.map { it.toDouble().pow(2) }.sum()).toFloat()
        return if (norm > 0) {
            vector.map { it / norm }.toFloatArray()
        } else {
            vector
        }
    }
    
    /**
     * Вычисляет расстояние между векторами
     */
    fun calculateDistance(vector1: FloatArray, vector2: FloatArray): Double {
        if (vector1.size != vector2.size) return Double.MAX_VALUE
        
        return sqrt(
            vector1.zip(vector2) { a, b ->
                (a - b).toDouble().pow(2)
            }.sum()
        )
    }
    
    /**
     * Создает случайный вектор заданной размерности
     */
    fun createRandomVector(dimension: Int): FloatArray {
        return FloatArray(dimension) { (Math.random() * 2 - 1).toFloat() }
    }
    
    /**
     * Создает нулевой вектор
     */
    fun createZeroVector(dimension: Int): FloatArray {
        return FloatArray(dimension) { 0f }
    }
    
    /**
     * Проверяет валидность вектора
     */
    fun isValidVector(vector: FloatArray): Boolean {
        return vector.isNotEmpty() && vector.all { !it.isNaN() && it.isFinite() }
    }
}

/**
 * Фабрика для создания векторных баз данных
 */
object VectorDatabaseFactory {
    
    /**
     * Создает in-memory векторную БД
     */
    fun createInMemory(): VectorDatabase = InMemoryVectorDatabase()
    
    /**
     * Создает файловую векторную БД
     */
    fun createFile(filePath: String): VectorDatabase = FileVectorDatabase(filePath)
    
    /**
     * Создает векторную БД на основе конфигурации
     */
    fun create(config: VectorDatabaseConfig): VectorDatabase {
        return when (config.type) {
            VectorDatabaseType.IN_MEMORY -> InMemoryVectorDatabase()
            VectorDatabaseType.FILE -> FileVectorDatabase(config.filePath ?: "vectors.db")
        }
    }
}

/**
 * Конфигурация векторной БД
 */
data class VectorDatabaseConfig(
    val type: VectorDatabaseType,
    val filePath: String? = null,
    val maxSize: Long = Long.MAX_VALUE,
    val dimension: Int = 768 // Размерность векторов по умолчанию
)

/**
 * Тип векторной БД
 */
enum class VectorDatabaseType {
    IN_MEMORY,
    FILE
}