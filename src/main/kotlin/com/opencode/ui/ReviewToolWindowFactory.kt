package com.opencode.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Modern Tool Window Factory for the OpenCode PR Review plugin.
 * Initializes the Expert Audit Dashboard on the right-hand panel.
 */
class ReviewToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val reviewPanel = ReviewPanel(project)
        val content = ContentFactory.getInstance().createContent(reviewPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
