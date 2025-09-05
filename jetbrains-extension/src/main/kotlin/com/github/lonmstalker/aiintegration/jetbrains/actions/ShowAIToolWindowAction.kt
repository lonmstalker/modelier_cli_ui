package com.github.lonmstalker.aiintegration.jetbrains.actions

import com.github.lonmstalker.aiintegration.jetbrains.AIBundle
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindowManager

class ShowAIToolWindowAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("AI CLI Integration")
        toolWindow?.show()
        val msg = AIBundle.message("notification.toolwindow.shown")
        NotificationGroupManager.getInstance()
            .getNotificationGroup("AI CLI Integration")
            .createNotification(msg, NotificationType.INFORMATION)
            .notify(project)
    }
}

