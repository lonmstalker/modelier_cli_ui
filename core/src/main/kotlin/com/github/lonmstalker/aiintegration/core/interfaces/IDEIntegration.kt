package com.github.lonmstalker.aiintegration.core.interfaces

import com.github.lonmstalker.aiintegration.core.models.ProjectContext
import java.io.File
import java.nio.file.Path

/**
 * Интерфейс для интеграции с IDE
 * Абстрагирует специфичные для IDE операции
 */
interface IDEIntegration {
    
    /**
     * Получает контекст текущего проекта
     */
    fun getCurrentProjectContext(): ProjectContext?
    
    /**
     * Получает выделенный текст в активном редакторе
     */
    fun getSelectedText(): String?
    
    /**
     * Получает текущий файл в редакторе
     */
    fun getCurrentFile(): IDEFile?
    
    /**
     * Получает все открытые файлы
     */
    fun getOpenFiles(): List<IDEFile>
    
    /**
     * Открывает файл в редакторе
     */
    fun openFile(path: String): Boolean
    
    /**
     * Вставляет текст в текущую позицию курсора
     */
    fun insertTextAtCursor(text: String): Boolean
    
    /**
     * Заменяет выделенный текст
     */
    fun replaceSelectedText(text: String): Boolean
    
    /**
     * Показывает уведомление пользователю
     */
    fun showNotification(title: String, message: String, type: NotificationType = NotificationType.INFO)
    
    /**
     * Показывает диалог с сообщением
     */
    fun showDialog(title: String, message: String, type: DialogType = DialogType.INFO): DialogResult
    
    /**
     * Показывает диалог выбора файла
     */
    fun showFileChooser(title: String, initialDirectory: String? = null): String?
    
    /**
     * Получает корневой путь проекта
     */
    fun getProjectRoot(): String?
    
    /**
     * Получает настройки IDE
     */
    fun getIDESettings(): IDESettings
    
    /**
     * Получает информацию о языке файла
     */
    fun getFileLanguage(file: IDEFile): String?
    
    /**
     * Выполняет действие в UI потоке IDE
     */
    fun runInUIThread(action: () -> Unit)
    
    /**
     * Выполняет действие в фоновом потоке
     */
    fun runInBackground(action: () -> Unit)
    
    /**
     * Получает VCS информацию
     */
    fun getVCSInfo(): VCSInfo?
    
    /**
     * Регистрирует слушателя событий IDE
     */
    fun registerEventListener(listener: IDEEventListener)
    
    /**
     * Отменяет регистрацию слушателя
     */
    fun unregisterEventListener(listener: IDEEventListener)
}

/**
 * Представление файла в IDE
 */
interface IDEFile {
    val path: String
    val name: String
    val extension: String
    val content: String
    val isModified: Boolean
    val language: String?
    
    /**
     * Сохраняет файл
     */
    fun save(): Boolean
    
    /**
     * Получает выделенный текст в этом файле
     */
    fun getSelectedText(): String?
    
    /**
     * Получает позицию курсора
     */
    fun getCursorPosition(): CursorPosition
    
    /**
     * Устанавливает позицию курсора
     */
    fun setCursorPosition(position: CursorPosition)
}

/**
 * Позиция курсора в файле
 */
data class CursorPosition(
    val line: Int,
    val column: Int,
    val offset: Int
)

/**
 * Настройки IDE
 */
data class IDESettings(
    val theme: String,
    val fontSize: Int,
    val fontFamily: String,
    val tabSize: Int,
    val useTabs: Boolean,
    val lineNumbers: Boolean,
    val wordWrap: Boolean
)

/**
 * Информация о системе контроля версий
 */
data class VCSInfo(
    val type: VCSType,
    val rootPath: String,
    val currentBranch: String?,
    val hasUncommittedChanges: Boolean,
    val modifiedFiles: List<String> = emptyList(),
    val stagedFiles: List<String> = emptyList()
)

/**
 * Тип системы контроля версий
 */
enum class VCSType {
    GIT, SVN, MERCURIAL, UNKNOWN
}

/**
 * Тип уведомления
 */
enum class NotificationType {
    INFO, WARNING, ERROR, SUCCESS
}

/**
 * Тип диалога
 */
enum class DialogType {
    INFO, WARNING, ERROR, QUESTION, CONFIRMATION
}

/**
 * Результат диалога
 */
enum class DialogResult {
    OK, CANCEL, YES, NO, RETRY
}

/**
 * Слушатель событий IDE
 */
interface IDEEventListener {
    
    /**
     * Вызывается при открытии файла
     */
    fun onFileOpened(file: IDEFile) {}
    
    /**
     * Вызывается при закрытии файла
     */
    fun onFileClosed(file: IDEFile) {}
    
    /**
     * Вызывается при изменении файла
     */
    fun onFileChanged(file: IDEFile) {}
    
    /**
     * Вызывается при изменении выделения текста
     */
    fun onSelectionChanged(file: IDEFile, selectedText: String?) {}
    
    /**
     * Вызывается при изменении позиции курсора
     */
    fun onCursorPositionChanged(file: IDEFile, position: CursorPosition) {}
    
    /**
     * Вызывается при открытии/закрытии проекта
     */
    fun onProjectChanged(projectPath: String?) {}
}

/**
 * Интерфейс для работы с файловой системой
 */
interface FileSystemProvider {
    
    /**
     * Проверяет существование файла или директории
     */
    fun exists(path: String): Boolean
    
    /**
     * Читает содержимое файла
     */
    fun readFile(path: String): String?
    
    /**
     * Записывает содержимое в файл
     */
    fun writeFile(path: String, content: String): Boolean
    
    /**
     * Создает директорию
     */
    fun createDirectory(path: String): Boolean
    
    /**
     * Удаляет файл или директорию
     */
    fun delete(path: String): Boolean
    
    /**
     * Получает список файлов в директории
     */
    fun listFiles(path: String, recursive: Boolean = false): List<String>
    
    /**
     * Получает информацию о файле
     */
    fun getFileInfo(path: String): FileInfo?
    
    /**
     * Копирует файл
     */
    fun copyFile(sourcePath: String, targetPath: String): Boolean
    
    /**
     * Перемещает файл
     */
    fun moveFile(sourcePath: String, targetPath: String): Boolean
    
    /**
     * Получает размер файла
     */
    fun getFileSize(path: String): Long
    
    /**
     * Получает время последнего изменения
     */
    fun getLastModified(path: String): Long
    
    /**
     * Создает временный файл
     */
    fun createTempFile(prefix: String, suffix: String): String?
    
    /**
     * Получает рабочую директорию
     */
    fun getCurrentDirectory(): String
    
    /**
     * Нормализует путь
     */
    fun normalizePath(path: String): String
    
    /**
     * Получает относительный путь
     */
    fun getRelativePath(basePath: String, targetPath: String): String
}

/**
 * Информация о файле
 */
data class FileInfo(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val isReadable: Boolean,
    val isWritable: Boolean,
    val extension: String
)

/**
 * Базовая реализация FileSystemProvider
 */
class DefaultFileSystemProvider : FileSystemProvider {
    
    override fun exists(path: String): Boolean = File(path).exists()
    
    override fun readFile(path: String): String? {
        return try {
            File(path).readText()
        } catch (e: Exception) {
            null
        }
    }
    
    override fun writeFile(path: String, content: String): Boolean {
        return try {
            File(path).writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun createDirectory(path: String): Boolean {
        return try {
            File(path).mkdirs()
        } catch (e: Exception) {
            false
        }
    }
    
    override fun delete(path: String): Boolean {
        return try {
            File(path).deleteRecursively()
        } catch (e: Exception) {
            false
        }
    }
    
    override fun listFiles(path: String, recursive: Boolean): List<String> {
        return try {
            val dir = File(path)
            if (!dir.exists() || !dir.isDirectory) return emptyList()
            
            if (recursive) {
                dir.walkTopDown().map { it.absolutePath }.toList()
            } else {
                dir.listFiles()?.map { it.absolutePath } ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override fun getFileInfo(path: String): FileInfo? {
        return try {
            val file = File(path)
            if (!file.exists()) return null
            
            FileInfo(
                path = file.absolutePath,
                name = file.name,
                size = file.length(),
                lastModified = file.lastModified(),
                isDirectory = file.isDirectory,
                isReadable = file.canRead(),
                isWritable = file.canWrite(),
                extension = file.extension
            )
        } catch (e: Exception) {
            null
        }
    }
    
    override fun copyFile(sourcePath: String, targetPath: String): Boolean {
        return try {
            File(sourcePath).copyTo(File(targetPath), overwrite = true)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun moveFile(sourcePath: String, targetPath: String): Boolean {
        return try {
            File(sourcePath).renameTo(File(targetPath))
        } catch (e: Exception) {
            false
        }
    }
    
    override fun getFileSize(path: String): Long {
        return try {
            File(path).length()
        } catch (e: Exception) {
            0L
        }
    }
    
    override fun getLastModified(path: String): Long {
        return try {
            File(path).lastModified()
        } catch (e: Exception) {
            0L
        }
    }
    
    override fun createTempFile(prefix: String, suffix: String): String? {
        return try {
            kotlin.io.createTempFile(prefix, suffix).absolutePath
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getCurrentDirectory(): String {
        return System.getProperty("user.dir")
    }
    
    override fun normalizePath(path: String): String {
        return try {
            Path.of(path).normalize().toString()
        } catch (e: Exception) {
            path
        }
    }
    
    override fun getRelativePath(basePath: String, targetPath: String): String {
        return try {
            Path.of(basePath).relativize(Path.of(targetPath)).toString()
        } catch (e: Exception) {
            targetPath
        }
    }
}