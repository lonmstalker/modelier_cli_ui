package com.github.lonmstalker.aiintegration.core.interfaces

/**
 * Интерфейс для векторной базы данных
 */
interface VectorDatabase {
    /**
     * Добавляет вектор в базу данных
     */
    suspend fun add(id: String, vector: FloatArray, metadata: Map<String, Any>)
    
    /**
     * Добавляет множество векторов
     */
    suspend fun addBatch(items: List<VectorItem>)
    
    /**
     * Поиск похожих векторов
     */
    suspend fun search(
        queryVector: FloatArray, 
        limit: Int = 10, 
        threshold: Double = 0.0,
        filter: Map<String, Any> = emptyMap()
    ): List<SearchResult>
    
    /**
     * Обновляет существующий вектор
     */
    suspend fun update(id: String, vector: FloatArray, metadata: Map<String, Any>)
    
    /**
     * Удаляет вектор по ID
     */
    suspend fun delete(id: String)
    
    /**
     * Удаляет множество векторов
     */
    suspend fun deleteBatch(ids: List<String>)
    
    /**
     * Очищает всю базу данных
     */
    suspend fun clear()
    
    /**
     * Получает размер базы данных
     */
    suspend fun size(): Long
    
    /**
     * Проверяет существование вектора
     */
    suspend fun exists(id: String): Boolean
    
    /**
     * Получает метаданные по ID
     */
    suspend fun getMetadata(id: String): Map<String, Any>?
    
    /**
     * Создает индекс для ускорения поиска
     */
    suspend fun buildIndex()
    
    /**
     * Закрывает соединение с базой данных
     */
    suspend fun close()
}

/**
 * Элемент векторной базы данных
 */
data class VectorItem(
    val id: String,
    val vector: FloatArray,
    val metadata: Map<String, Any> = emptyMap()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VectorItem

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

/**
 * Результат поиска в векторной БД
 */
data class SearchResult(
    val id: String,
    val similarity: Double,
    val metadata: Map<String, Any>,
    val vector: FloatArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SearchResult

        if (id != other.id) return false
        if (similarity != other.similarity) return false
        if (metadata != other.metadata) return false
        if (vector != null) {
            if (other.vector == null) return false
            if (!vector.contentEquals(other.vector)) return false
        } else if (other.vector != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + similarity.hashCode()
        result = 31 * result + metadata.hashCode()
        result = 31 * result + (vector?.contentHashCode() ?: 0)
        return result
    }
}