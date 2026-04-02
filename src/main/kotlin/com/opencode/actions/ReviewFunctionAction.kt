package com.opencode.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilBase
import com.opencode.adapter.CliAdapter
import com.opencode.adapter.RestAdapter
import com.opencode.settings.AppSettingsState
import com.opencode.ui.ReviewPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.util.parentOfType

/**
 * Action triggered to review a selection or the current function.
 * Uses Coroutines to handle asynchronous response streaming to the tool window.
 */
class ReviewFunctionAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        
        val selectionModel = editor.selectionModel
        
        // Define the review region
        val (selectedText, regionStart, regionEnd) = when {
            selectionModel.hasSelection() -> {
                Triple(selectionModel.selectedText ?: "", selectionModel.selectionStart, selectionModel.selectionEnd)
            }
            else -> {
                // Try to find current function at caret
                val element = psiFile.findElementAt(editor.caretModel.offset)
                val parent = element?.parentOfType<PsiElement>(true)
                if (parent != null) {
                    Triple(parent.text ?: "", parent.textRange.startOffset, parent.textRange.endOffset)
                } else {
                    // Default: Entire file
                    Triple(psiFile.text ?: "", 0, psiFile.textLength)
                }
            }
        }

        if (selectedText.isBlank()) return

        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("OpenCode PR Review") ?: return
        toolWindow.show()
        val reviewPanel = toolWindow.contentManager.getContent(0)?.component as? ReviewPanel ?: return
        
        // Setup the panel to target the exact code region
        reviewPanel.setTarget(editor, regionStart, regionEnd)
        reviewPanel.showHeader(psiFile.name)
        
        val settings = AppSettingsState.instance
        val adapter = if (settings.mode == "REST") RestAdapter(settings) else CliAdapter(settings)

        // Debug info (only shows if enabled in settings)
        val endpoint = if (settings.restBaseUrl.endsWith("/")) "${settings.restBaseUrl}api/chat" else "${settings.restBaseUrl}/api/chat"
        val curlCmd = "curl $endpoint -H \"Content-Type: application/json\" -d '{\"model\": \"${settings.modelName}\", \"stream\": true}'"
        reviewPanel.appendText("🔍 [DEBUG] Equivalent Curl Command:\n```bash\n$curlCmd\n```\n\n", isDebug = true)

        // Launch logic in Coroutine scope
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            try {
                val scopeType = when {
                    selectionModel.hasSelection() -> "SELECTION REVIEW"
                    regionStart == 0 && regionEnd == psiFile.textLength -> "FILE REVIEW"
                    else -> "FUNCTION REVIEW"
                }
                
                val contextualPrompt = "$scopeType\n\n$selectedText"
                
                adapter.review(contextualPrompt, psiFile.name)
                    .onStart { reviewPanel.setLoading(true) }
                    .onCompletion { reviewPanel.setLoading(false) }
                    .collect { chunk: String ->
                        reviewPanel.appendText(chunk)
                    }
            } catch (e: Exception) {
                reviewPanel.appendText("\n❌ Review Trigger Failed: ${e.message}")
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = project != null && editor != null
    }
}
