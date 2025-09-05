package com.github.lonmstalker.aiintegration.jetbrains.actions

import com.github.lonmstalker.aiintegration.jetbrains.AIBundle
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

class GenerateCodeAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val msg = AIBundle.message("notification.action.invoked", templatePresentation.text)
        NotificationGroupManager.getInstance()
            .getNotificationGroup("AI CLI Integration")
            .createNotification(msg, NotificationType.INFORMATION)
            .notify(project)
        // TODO: Call AIService with GenerateCode command
    }
}

