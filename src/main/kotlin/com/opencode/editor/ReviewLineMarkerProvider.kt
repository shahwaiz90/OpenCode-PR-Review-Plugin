package com.opencode.editor

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.opencode.ui.ReviewPanel
import com.opencode.ui.ReviewToolWindowFactory
import com.opencode.adapter.RestAdapter
import com.opencode.adapter.CliAdapter
import com.opencode.settings.AppSettingsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * Provides a "Review with OpenCode" gutter icon for every Kotlin/Java function.
 * Instantly triggers an expert audit of the specific function.
 */
class ReviewLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is PsiMethod && element !is org.jetbrains.kotlin.psi.KtNamedFunction) return null
        
        val identifier = when (element) {
            is PsiMethod -> element.nameIdentifier ?: return null
            is org.jetbrains.kotlin.psi.KtNamedFunction -> element.nameIdentifier ?: return null
            else -> return null
        }

        return LineMarkerInfo(
            identifier,
            identifier.textRange,
            AllIcons.Actions.Preview,
            { "Review function with OpenCode Auditor" },
            { _, _ -> performReview(element) },
            GutterIconRenderer.Alignment.LEFT,
            { "Expert Audit" }
        )
    }

    private fun performReview(element: PsiElement) {
        val project = element.project
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow("OpenCode PR Review") ?: return
        
        toolWindow.show()
        val reviewPanel = toolWindow.contentManager.getContent(0)?.component as? ReviewPanel ?: return
        
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val range = element.textRange
        
        reviewPanel.setTarget(editor, range.startOffset, range.endOffset)
        reviewPanel.showHeader(element.containingFile.name)
        reviewPanel.clear()

        val settings = AppSettingsState.instance
        val adapter = if (settings.mode == "REST") RestAdapter(settings) else CliAdapter(settings)

        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            try {
                val code = element.text
                val contextualPrompt = "FUNCTION REVIEW\n\n$code"
                
                adapter.review(contextualPrompt, element.containingFile.name)
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
}
