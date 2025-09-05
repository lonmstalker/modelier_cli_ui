package com.github.lonmstalker.aiintegration.jetbrains.settings

import com.github.lonmstalker.aiintegration.jetbrains.AIBundle
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import java.awt.BorderLayout

class AIPluginConfigurable : Configurable {
    private var panel: JPanel? = null

    override fun getDisplayName(): String = AIBundle.message("settings.displayName")

    override fun createComponent(): JComponent {
        if (panel == null) {
            panel = JPanel(BorderLayout()).apply {
                add(JLabel(AIBundle.message("settings.displayName")), BorderLayout.NORTH)
            }
        }
        return panel as JPanel
    }

    override fun isModified(): Boolean = false

    override fun apply() {}
}

