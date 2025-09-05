package com.github.lonmstalker.aiintegration.jetbrains

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

object AIBundle : DynamicBundle("messages.AIBundle") {
    private const val BUNDLE = "messages.AIBundle"

    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String = getMessage(key, *params)
}

