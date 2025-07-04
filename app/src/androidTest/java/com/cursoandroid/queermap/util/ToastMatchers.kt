package com.cursoandroid.queermap.util

import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry

fun isToastMessageDisplayed(expectedText: String): Boolean {
    val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
    val rootNode = uiAutomation.rootInActiveWindow

    fun findText(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.text?.toString() == expectedText) return true
        for (i in 0 until node.childCount) {
            if (findText(node.getChild(i))) return true
        }
        return false
    }

    return findText(rootNode)
}
