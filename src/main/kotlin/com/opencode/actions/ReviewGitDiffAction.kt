package com.opencode.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager
import com.opencode.adapter.CliAdapter
import com.opencode.adapter.RestAdapter
import com.opencode.settings.AppSettingsState
import com.opencode.ui.ReviewPanel
import com.opencode.util.GitDiffUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * Reviews all current Git changes in the workspace.
 * Uses GitDiffUtil to gather affected content before calling OpenCode.
 */
class ReviewGitDiffAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        val diffContent = GitDiffUtil.getUnstagedDiff(project)
        if (diffContent.isBlank()) return

        // Updated ToolWindow ID to match current branding
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("OpenCode PR Review") ?: return
        toolWindow.show()
        val reviewPanel = toolWindow.contentManager.getContent(0)?.component as? ReviewPanel ?: return
        
        reviewPanel.showHeader("Git Diff")
        val settings = AppSettingsState.instance
        val adapter = if (settings.mode == "REST") RestAdapter(settings) else CliAdapter(settings)

        val contextualPrompt = "GIT DIFF REVIEW\n\n$diffContent"

        CoroutineScope(Dispatchers.Main).launch {
            adapter.review(contextualPrompt, "Current Changes")
                .onStart { reviewPanel.setLoading(true) }
                .onCompletion { reviewPanel.setLoading(false) }
                .collect { chunk ->
                    reviewPanel.appendText(chunk)
                }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
