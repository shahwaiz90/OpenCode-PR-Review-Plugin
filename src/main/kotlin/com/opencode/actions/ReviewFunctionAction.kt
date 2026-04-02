package com.opencode.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.opencode.adapter.RestAdapter
import com.opencode.adapter.CliAdapter
import com.opencode.settings.AppSettingsState
import com.opencode.ui.ReviewPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Expert Auditor Action that determines the review scope (Selection/Function/File).
 * Injects contextual metrics into the AI stream.
 */
class ReviewFunctionAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        
        val selectionModel = editor.selectionModel
        val (selectedText, regionStart, regionEnd) = if (selectionModel.hasSelection()) {
            Triple(selectionModel.selectedText, selectionModel.selectionStart, selectionModel.selectionEnd)
        } else {
            val element = psiFile.findElementAt(editor.caretModel.offset)
            val method = PsiTreeUtil.getParentOfType(element, com.intellij.psi.PsiMethod::class.java, org.jetbrains.kotlin.psi.KtNamedFunction::class.java)
            if (method != null) {
                Triple(method.text, method.textRange.startOffset, method.textRange.endOffset)
            } else {
                Triple(psiFile.text, 0, psiFile.textLength)
            }
        }

        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow("OpenCode PR Review") ?: return
        toolWindow.show()
        val reviewPanel = toolWindow.contentManager.getContent(0)?.component as? ReviewPanel ?: return
        
        reviewPanel.setTarget(editor, regionStart, regionEnd)
        reviewPanel.showHeader(psiFile.name)
        reviewPanel.clear()
        
        val settings = AppSettingsState.instance
        val adapter = if (settings.mode == "REST") RestAdapter(settings) else CliAdapter(settings)

        // Debug info (only shows if enabled)
        val endpoint = settings.restBaseUrl.trimEnd('/') + "/api/chat"
        val debugCmd = "curl $endpoint -H \"Content-Type: application/json\" -d '{\"model\": \"${settings.modelName}\", \"stream\": true}'"
        reviewPanel.appendText("🔍 [DEBUG] Equivalent Curl Command:\n```bash\n$debugCmd\n```\n\n", isDebug = true)

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
                    .collect { chunk ->
                        reviewPanel.appendText(chunk)
                    }
            } catch (e: Exception) {
                reviewPanel.appendText("\n❌ ERROR: ${e.message}\n")
            } finally {
                reviewPanel.setLoading(false)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = project != null && editor != null
    }
}
