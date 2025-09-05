package com.github.lonmstalker.aiintegration.jetbrains.startup

import com.github.lonmstalker.aiintegration.jetbrains.AIBundle
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class AIPluginStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("AI CLI Integration")
            .createNotification(AIBundle.message("startup.init"), NotificationType.INFORMATION)
            .notify(project)
    }
}

