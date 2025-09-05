package com.github.lonmstalker.aiintegration.jetbrains.toolwindow

import com.github.lonmstalker.aiintegration.jetbrains.AIBundle
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JLabel
import javax.swing.JPanel
import java.awt.BorderLayout

class AIToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout()).apply {
            add(JLabel(AIBundle.message("toolwindow.ai.title")), BorderLayout.NORTH)
        }
        val content = ContentFactory.getInstance().createContent(panel, AIBundle.message("toolwindow.ai.title"), false)
        toolWindow.contentManager.addContent(content)
    }
}

