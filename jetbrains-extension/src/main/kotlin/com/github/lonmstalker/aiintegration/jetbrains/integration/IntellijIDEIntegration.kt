package com.github.lonmstalker.aiintegration.jetbrains.integration

import com.github.lonmstalker.aiintegration.core.interfaces.*
import com.github.lonmstalker.aiintegration.core.models.ProjectContext
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import git4idea.repo.GitRepositoryManager
import java.awt.Toolkit
import javax.swing.JFileChooser

/**
 * IntelliJ-специфичная реализация IDE интеграции
 */
@Service(Service.Level.APP)
class IntellijIDEIntegration : IDEIntegration {
    
    companion object {
        fun getInstance(): IntellijIDEIntegration = com.intellij.openapi.components.service()
        
        private const val NOTIFICATION_GROUP_ID = "AI Integration"
    }
    
    private val logger = thisLogger()
    private val eventListeners = mutableListOf<IDEEventListener>()
    
    override fun getCurrentProjectContext(): ProjectContext? {
        val project = getCurrentProject() ?: return null
        val editor = getCurrentEditor() ?: return null
        
        return createProjectContext(project, editor)
    }
    
    override fun getSelectedText(): String? {
        return getCurrentEditor()?.selectionModel?.selectedText
    }
    
    override fun getCurrentFile(): IDEFile? {
        val project = getCurrentProject() ?: return null
        val editor = getCurrentEditor() ?: return null
        val document = editor.document
        val virtualFile = FileDocumentManager.getInstance().getFile(document) ?: return null
        
        return IntellijIDEFile(virtualFile, editor, project)
    }
    
    override fun getOpenFiles(): List<IDEFile> {
        val project = getCurrentProject() ?: return emptyList()
        val fileEditorManager = FileEditorManager.getInstance(project)
        
        return fileEditorManager.openFiles.mapNotNull { virtualFile ->
            val editor = EditorFactory.getInstance().editors(virtualFile).firstOrNull()
            editor?.let { IntellijIDEFile(virtualFile, it, project) }
        }
    }
    
    override fun openFile(path: String): Boolean {
        return try {
            val project = getCurrentProject() ?: return false
            val fileEditorManager = FileEditorManager.getInstance(project)
            val virtualFile = com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(path)
            
            if (virtualFile != null) {
                ApplicationManager.getApplication().invokeLater {
                    fileEditorManager.openFile(virtualFile, true)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            logger.error("Failed to open file: $path", e)
            false
        }
    }
    
    override fun insertTextAtCursor(text: String): Boolean {
        return try {
            val editor = getCurrentEditor() ?: return false
            
            ApplicationManager.getApplication().runWriteAction {
                val document = editor.document
                val offset = editor.caretModel.offset
                document.insertString(offset, text)
                editor.caretModel.moveToOffset(offset + text.length)
            }
            
            true
        } catch (e: Exception) {
            logger.error("Failed to insert text at cursor", e)
            false
        }
    }
    
    override fun replaceSelectedText(text: String): Boolean {
        return try {
            val editor = getCurrentEditor() ?: return false
            val selectionModel = editor.selectionModel
            
            if (!selectionModel.hasSelection()) {
                return insertTextAtCursor(text)
            }
            
            ApplicationManager.getApplication().runWriteAction {
                val document = editor.document
                val startOffset = selectionModel.selectionStart
                val endOffset = selectionModel.selectionEnd
                
                document.replaceString(startOffset, endOffset, text)
                editor.caretModel.moveToOffset(startOffset + text.length)
                selectionModel.removeSelection()
            }
            
            true
        } catch (e: Exception) {
            logger.error("Failed to replace selected text", e)
            false
        }
    }
    
    override fun showNotification(title: String, message: String, type: NotificationType) {
        try {
            val notificationType = when (type) {
                com.github.lonmstalker.aiintegration.core.interfaces.NotificationType.INFO -> 
                    com.intellij.notification.NotificationType.INFORMATION
                com.github.lonmstalker.aiintegration.core.interfaces.NotificationType.WARNING -> 
                    com.intellij.notification.NotificationType.WARNING
                com.github.lonmstalker.aiintegration.core.interfaces.NotificationType.ERROR -> 
                    com.intellij.notification.NotificationType.ERROR
                com.github.lonmstalker.aiintegration.core.interfaces.NotificationType.SUCCESS -> 
                    com.intellij.notification.NotificationType.INFORMATION
            }
            
            val notificationGroup = NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
            
            val notification = notificationGroup.createNotification(title, message, notificationType)
            notification.notify(getCurrentProject())
            
        } catch (e: Exception) {
            logger.error("Failed to show notification", e)
        }
    }
    
    override fun showDialog(title: String, message: String, type: DialogType): DialogResult {
        return try {
            val project = getCurrentProject()
            
            val result = when (type) {
                DialogType.INFO -> {
                    Messages.showInfoMessage(project, message, title)
                    DialogResult.OK
                }
                DialogType.WARNING -> {
                    Messages.showWarningDialog(project, message, title)
                    DialogResult.OK
                }
                DialogType.ERROR -> {
                    Messages.showErrorDialog(project, message, title)
                    DialogResult.OK
                }
                DialogType.QUESTION -> {
                    val answer = Messages.showYesNoDialog(project, message, title, Messages.getQuestionIcon())
                    if (answer == Messages.YES) DialogResult.YES else DialogResult.NO
                }
                DialogType.CONFIRMATION -> {
                    val answer = Messages.showOkCancelDialog(project, message, title, Messages.getQuestionIcon())
                    if (answer == Messages.OK) DialogResult.OK else DialogResult.CANCEL
                }
            }
            
            result
        } catch (e: Exception) {
            logger.error("Failed to show dialog", e)
            DialogResult.CANCEL
        }
    }
    
    override fun showFileChooser(title: String, initialDirectory: String?): String? {
        return try {
            val fileChooser = JFileChooser()
            fileChooser.dialogTitle = title
            
            initialDirectory?.let { dir ->
                fileChooser.currentDirectory = java.io.File(dir)
            }
            
            val result = fileChooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                fileChooser.selectedFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error("Failed to show file chooser", e)
            null
        }
    }
    
    override fun getProjectRoot(): String? {
        return getCurrentProject()?.basePath
    }
    
    override fun getIDESettings(): IDESettings {
        val editorSettings = com.intellij.openapi.editor.EditorFactory.getInstance().globalScheme
        
        return IDESettings(
            theme = if (com.intellij.ide.ui.LafManager.getInstance().currentUIThemeLookAndFeel?.isDark == true) "dark" else "light",
            fontSize = editorSettings.editorFontSize,
            fontFamily = editorSettings.editorFontName,
            tabSize = editorSettings.getTabSize(null),
            useTabs = !editorSettings.isUseTabCharacter(null),
            lineNumbers = editorSettings.isLineNumbersShown,
            wordWrap = editorSettings.isUseSoftWraps
        )
    }
    
    override fun getFileLanguage(file: IDEFile): String? {
        if (file is IntellijIDEFile) {
            return file.language
        }
        return null
    }
    
    override fun runInUIThread(action: () -> Unit) {
        ApplicationManager.getApplication().invokeLater(action)
    }
    
    override fun runInBackground(action: () -> Unit) {
        ApplicationManager.getApplication().executeOnPooledThread(action)
    }
    
    override fun getVCSInfo(): VCSInfo? {
        val project = getCurrentProject() ?: return null
        
        return try {
            val gitRepositoryManager = GitRepositoryManager.getInstance(project)
            val repository = gitRepositoryManager.repositories.firstOrNull() ?: return null
            
            VCSInfo(
                type = VCSType.GIT,
                rootPath = repository.root.path,
                currentBranch = repository.currentBranch?.name,
                hasUncommittedChanges = repository.isDirty,
                modifiedFiles = repository.untrackedFilesHolder.untrackedFiles.map { it.path },
                stagedFiles = emptyList() // TODO: Implement staged files detection
            )
        } catch (e: Exception) {
            logger.error("Failed to get VCS info", e)
            null
        }
    }
    
    override fun registerEventListener(listener: IDEEventListener) {
        eventListeners.add(listener)
    }
    
    override fun unregisterEventListener(listener: IDEEventListener) {
        eventListeners.remove(listener)
    }
    
    private fun getCurrentProject(): Project? {
        return ProjectManager.getInstance().openProjects.firstOrNull()
    }
    
    private fun getCurrentEditor(): Editor? {
        val project = getCurrentProject() ?: return null
        val fileEditorManager = FileEditorManager.getInstance(project)
        return fileEditorManager.selectedTextEditor
    }
    
    private fun createProjectContext(project: Project, editor: Editor): ProjectContext {
        val document = editor.document
        val virtualFile = FileDocumentManager.getInstance().getFile(document)
        val selectedText = editor.selectionModel.selectedText
        
        return ProjectContext(
            workingDirectory = project.basePath ?: "",
            selectedFiles = listOfNotNull(virtualFile),
            currentFile = virtualFile,
            selectedText = selectedText,
            projectName = project.name
        )
    }
}

/**
 * IntelliJ-специфичная реализация IDEFile
 */
class IntellijIDEFile(
    private val virtualFile: VirtualFile,
    private val editor: Editor,
    private val project: Project
) : IDEFile {
    
    override val path: String get() = virtualFile.path
    override val name: String get() = virtualFile.name
    override val extension: String get() = virtualFile.extension ?: ""
    override val content: String get() = editor.document.text
    override val isModified: Boolean get() = FileDocumentManager.getInstance().isDocumentUnsaved(editor.document)
    override val language: String? get() = virtualFile.fileType.name
    
    override fun save(): Boolean {
        return try {
            ApplicationManager.getApplication().runWriteAction {
                FileDocumentManager.getInstance().saveDocument(editor.document)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun getSelectedText(): String? {
        return editor.selectionModel.selectedText
    }
    
    override fun getCursorPosition(): CursorPosition {
        val logicalPosition = editor.caretModel.logicalPosition
        return CursorPosition(
            line = logicalPosition.line,
            column = logicalPosition.column,
            offset = editor.caretModel.offset
        )
    }
    
    override fun setCursorPosition(position: CursorPosition) {
        ApplicationManager.getApplication().runWriteAction {
            editor.caretModel.moveToOffset(position.offset)
        }
    }
}

/**
 * IntelliJ-специфичная реализация FileSystemProvider
 */
@Service(Service.Level.APP) 
class IntellijFileSystemProvider : FileSystemProvider {
    
    companion object {
        fun getInstance(): IntellijFileSystemProvider = com.intellij.openapi.components.service()
    }
    
    private val defaultProvider = DefaultFileSystemProvider()
    
    override fun exists(path: String): Boolean = defaultProvider.exists(path)
    override fun readFile(path: String): String? = defaultProvider.readFile(path)
    override fun writeFile(path: String, content: String): Boolean = defaultProvider.writeFile(path, content)
    override fun createDirectory(path: String): Boolean = defaultProvider.createDirectory(path)
    override fun delete(path: String): Boolean = defaultProvider.delete(path)
    override fun listFiles(path: String, recursive: Boolean): List<String> = defaultProvider.listFiles(path, recursive)
    override fun getFileInfo(path: String): FileInfo? = defaultProvider.getFileInfo(path)
    override fun copyFile(sourcePath: String, targetPath: String): Boolean = defaultProvider.copyFile(sourcePath, targetPath)
    override fun moveFile(sourcePath: String, targetPath: String): Boolean = defaultProvider.moveFile(sourcePath, targetPath)
    override fun getFileSize(path: String): Long = defaultProvider.getFileSize(path)
    override fun getLastModified(path: String): Long = defaultProvider.getLastModified(path)
    override fun createTempFile(prefix: String, suffix: String): String? = defaultProvider.createTempFile(prefix, suffix)
    override fun getCurrentDirectory(): String = defaultProvider.getCurrentDirectory()
    override fun normalizePath(path: String): String = defaultProvider.normalizePath(path)
    override fun getRelativePath(basePath: String, targetPath: String): String = defaultProvider.getRelativePath(basePath, targetPath)
}