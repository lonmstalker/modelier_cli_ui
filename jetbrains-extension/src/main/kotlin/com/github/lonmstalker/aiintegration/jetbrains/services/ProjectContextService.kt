package com.github.lonmstalker.aiintegration.jetbrains.services

import com.github.lonmstalker.aiintegration.core.interfaces.*
import com.github.lonmstalker.aiintegration.core.models.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Project-level service that adapts IDE context to Core ProjectContextProvider API.
 * Provides minimal, safe stubs that can be extended incrementally.
 */
@Service(Service.Level.PROJECT)
class ProjectContextService(private val project: Project) : ProjectContextProvider {

    private val updates = MutableSharedFlow<ContextUpdateEvent>(replay = 0)

    override suspend fun extractContext(query: ContextQuery): ProjectContext {
        // Minimal context for first iteration; enrich later with Git, structure, etc.
        val currentFile = query.currentFile?.let { path ->
            FileInfo(
                path = path,
                relativePath = path.substringAfterLast(project.basePath ?: ""),
                name = path.substringAfterLast('/') ,
                extension = path.substringAfterLast('.', ""),
                size = 0,
                lastModified = System.currentTimeMillis(),
                content = null,
                language = null
            )
        }

        return ProjectContext(
            projectPath = query.projectPath,
            projectName = project.name,
            selectedFiles = emptyList(),
            currentFile = currentFile,
            selectedText = query.selectedText,
            cursorPosition = query.cursorPosition,
            gitStatus = null,
            projectStructure = null,
            relevantDependencies = emptyList(),
            semanticContext = emptyList(),
            language = null,
            framework = null
        )
    }

    override suspend fun buildSmartContext(query: String, projectPath: String, maxTokens: Int): SmartContext {
        // Stub: returns empty smart context; replace with semantic search later.
        return SmartContext(
            primaryElements = emptyList(),
            relatedElements = emptyList(),
            dependencies = emptyList(),
            examples = emptyList(),
            documentation = emptyList(),
            relevanceScore = 0.0,
            tokenCount = 0,
            buildStrategy = ContextBuildStrategy.HYBRID
        )
    }

    override suspend fun updateContext(projectPath: String, changes: List<FileChange>) {
        // Emit change events so ToolWindow/UI can react if needed.
        for (change in changes) {
            updates.emit(ContextUpdateEvent.FileChanged(change))
        }
        updates.emit(ContextUpdateEvent.IndexUpdated(changes.map { it.filePath }))
    }

    override suspend fun getRelevantFiles(query: String, projectPath: String, limit: Int): List<FileInfo> {
        // Minimal implementation: returns up to [limit] files from the project root without content.
        val basePath = project.basePath ?: return emptyList()
        val vfs = com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(basePath) ?: return emptyList()
        val result = mutableListOf<FileInfo>()
        collectFiles(vfs, result, limit)
        return result
    }

    override fun watchContextChanges(projectPath: String): Flow<ContextUpdateEvent> = updates

    // Convenience helper for Actions: build context from IDE state
    suspend fun buildContextFromIde(editor: Editor?): ProjectContext {
        val projectPath = project.basePath ?: ""
        val currentFilePath = editor?.document?.let { doc ->
            com.intellij.psi.PsiDocumentManager.getInstance(project).getPsiFile(doc)?.virtualFile?.path
        }
        val selectedText = editor?.selectionModel?.selectedText
        val caret = editor?.caretModel?.currentCaret
        val cursor = if (caret != null) CursorPosition(caret.logicalPosition.line, caret.logicalPosition.column, caret.offset) else null
        return extractContext(
            ContextQuery(
                query = selectedText ?: "",
                projectPath = projectPath,
                currentFile = currentFilePath,
                selectedText = selectedText,
                cursorPosition = cursor,
                includeGitInfo = false,
                includeDependencies = false,
                maxTokens = 50_000
            )
        )
    }

    private fun collectFiles(dir: VirtualFile, acc: MutableList<FileInfo>, limit: Int) {
        if (acc.size >= limit) return
        if (dir.isDirectory) {
            dir.children.forEach { child ->
                if (acc.size < limit) collectFiles(child, acc, limit)
            }
        } else {
            acc += FileInfo(
                path = dir.path,
                relativePath = project.basePath?.let { base -> dir.path.removePrefix(base).trimStart('/') } ?: dir.name,
                name = dir.name,
                extension = dir.extension ?: "",
                size = dir.length,
                lastModified = dir.timeStamp,
                content = null,
                language = null
            )
        }
    }
}

