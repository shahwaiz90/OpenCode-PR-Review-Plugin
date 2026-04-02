package com.opencode.editor

import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.util.ui.EmptyIcon
import javax.swing.Icon
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction

/**
 * Custom GutterIconRenderer to show that a function has been reviewed.
 */
class OpenCodeGutterRenderer(private val reviewResult: String) : GutterIconRenderer() {
    
    override fun getIcon(): Icon = AllIcons.Actions.Checked

    override fun getTooltipText(): String = "OpenCode Review: ${reviewResult.take(100)}..."

    override fun getClickAction(): AnAction? = null

    override fun equals(other: Any?): Boolean = other is OpenCodeGutterRenderer && other.reviewResult == reviewResult

    override fun hashCode(): Int = reviewResult.hashCode()
}
