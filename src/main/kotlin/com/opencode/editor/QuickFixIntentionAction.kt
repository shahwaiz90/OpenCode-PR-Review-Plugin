package com.opencode.editor

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

/**
 * Offering AI-suggested fixes as native "Intention Actions" (the lightbulb).
 * Triggers when OpenCode suggests a specific file change.
 */
class OpenCodeQuickFixAction(private val fix: String) : IntentionAction {

    override fun getText(): String = "Apply OpenCode AI Fix"

    override fun getFamilyName(): String = "OpenCode Fixes"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        // Implementation logic for applying string-diffs or full replacement
        if (editor == null) return
        val selectionModel = editor.selectionModel
        if (selectionModel.hasSelection()) {
           // Apply fix to the selection
           editor.document.replaceString(selectionModel.selectionStart, selectionModel.selectionEnd, fix)
        }
    }

    override fun startInWriteAction(): Boolean = true
}
