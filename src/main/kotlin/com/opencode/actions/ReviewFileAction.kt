package com.opencode.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager
import com.opencode.adapter.CliAdapter
import com.opencode.adapter.RestAdapter
import com.opencode.settings.AppSettingsState
import com.opencode.ui.ReviewPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * Reviews the full content of the current file.
 */
class ReviewFileAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        
        val content = psiFile.text ?: ""
        if (content.isBlank()) return

        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("OpenCode Review") ?: return
        toolWindow.show()
        val reviewPanel = toolWindow.contentManager.getContent(0)?.component as? ReviewPanel ?: return
        
        reviewPanel.clear()
        val settings = AppSettingsState.instance
        val adapter = if (settings.mode == "REST") RestAdapter(settings) else CliAdapter(settings)

        CoroutineScope(Dispatchers.Main).launch {
            adapter.review(content, psiFile.name)
                .onStart { reviewPanel.setLoading(true) }
                .onCompletion { reviewPanel.setLoading(false) }
                .collect { chunk ->
                    reviewPanel.appendText(chunk)
                }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null && e.getData(CommonDataKeys.PSI_FILE) != null
    }
}
