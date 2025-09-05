package com.github.lonmstalker.aiintegration.jetbrains.toolwindow

import com.github.lonmstalker.aiintegration.jetbrains.AIBundle
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class AIToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val chatPanel = AIChatPanel(project)
        val content = ContentFactory.getInstance().createContent(chatPanel, "AI Assistant", false)
        toolWindow.contentManager.addContent(content)
    }
}

