package com.opencode.editor

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.application.ApplicationManager
import com.opencode.adapter.RestAdapter
import com.opencode.settings.AppSettingsState
import com.opencode.ui.ReviewPanel
import com.opencode.ui.ReviewToolWindowFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.psi.KtNamedFunction

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtClass

class ReviewLineMarkerProvider : LineMarkerProvider {
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // Target Kotlin or Java functions OR Classes
        val result = when (element) {
            is KtClass -> element.nameIdentifier to true
            is PsiClass -> element.nameIdentifier to true
            is KtNamedFunction -> element.nameIdentifier to false
            is PsiMethod -> element.nameIdentifier to false
            else -> null
        } ?: return null

        val identifier = result.first ?: return null
        val isFileReview = result.second

        val icon = if (isFileReview) AllIcons.Actions.PreviewDetails else AllIcons.Actions.Search
        val tooltip = if (isFileReview) "OpenCode: Review Entire File" else "OpenCode: Review Function"

        return LineMarkerInfo(
            identifier,
            identifier.textRange,
            icon,
            { tooltip },
            { _, _ -> if (isFileReview) startFullFileReview(element.containingFile) else startQuickReview(element) },
            GutterIconRenderer.Alignment.LEFT,
            { tooltip }
        )
    }

    private fun startFullFileReview(file: PsiFile) {
        val project = file.project
        val settings = AppSettingsState.instance
        val adapter = RestAdapter(settings)
        
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("OpenCode Review") ?: return
        
        toolWindow.show { 
            val content = toolWindow.contentManager.getContent(0)
            val panel = content?.component as? ReviewPanel ?: return@show
            
            panel.clear()
            panel.appendText("📄 [Full File Review] Analyzing file: '${file.name}'...\n\n")
            panel.setLoading(true)

            scope.launch {
                try {
                    adapter.review(file.text, file.name).collect { chunk ->
                        panel.appendText(chunk)
                    }
                } catch (e: Exception) {
                    panel.appendText("\n❌ Review Failed: ${e.message}")
                } finally {
                    panel.setLoading(false)
                }
            }
        }
    }

    private fun startQuickReview(function: PsiElement) {
        val project = function.project
        val settings = AppSettingsState.instance
        val adapter = RestAdapter(settings)
        
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("OpenCode PR Review") ?: return
        
        toolWindow.show { 
            val content = toolWindow.contentManager.getContent(0)
            val panel = content?.component as? ReviewPanel ?: return@show
            
            val editor = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).selectedTextEditor
            if (editor != null) {
                panel.setTarget(editor, function.textRange.startOffset, function.textRange.endOffset)
            }

            val fileName = function.containingFile.name
            panel.showHeader(fileName)
            
            val settings = AppSettingsState.instance
            val endpoint = if (settings.restBaseUrl.endsWith("/")) "${settings.restBaseUrl}api/tags" else "${settings.restBaseUrl}/api/tags"
            panel.appendText("🔍 [DEBUG] Model Discovery: $endpoint\n", isDebug = true)
            
            panel.setLoading(true)

            scope.launch {
                try {
                    adapter.review(function.text, "function_review.kt").collect { chunk: String ->
                        panel.appendText(chunk)
                    }
                } catch (e: Exception) {
                    panel.appendText("\n❌ Review Failed: ${e.message}")
                } finally {
                    panel.setLoading(false)
                }
            }
        }
    }
}
