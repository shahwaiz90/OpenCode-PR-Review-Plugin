package com.opencode.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.FormBuilder
import com.opencode.settings.AppSettingsState
import com.opencode.adapter.RestAdapter
import kotlinx.coroutines.*
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.icons.AllIcons

/**
 * AI Playground Dialog for testing prompts and models.
 * Now includes 'Load current selection' from the editor.
 */
class ReviewTestDialog : DialogWrapper(true) {
    private val settings = AppSettingsState.instance
    private val inputArea = JBTextArea("fun example() {\n    println(\"Test code here\")\n}", 10, 50).apply {
        lineWrap = true
        wrapStyleWord = true
    }
    private val outputArea = JBTextArea("", 10, 50).apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }
    
    // Status label showing the active model name correctly
    private val infoLabel = JBLabel("Running with Active Model: ${settings.modelName}").apply {
        foreground = com.intellij.ui.JBColor.GRAY
    }
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    init {
        title = "OpenCode PR Review - Playground (${settings.modelName})"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val testButton = JButton("Run Review", AllIcons.Actions.Execute)
        val selectionButton = JButton("Load from Selection", AllIcons.General.CopyHovered)
        val kdocButton = JButton("Extract KDocs", AllIcons.Nodes.Annotationtype)
        
        testButton.addActionListener { runTest() }
        selectionButton.addActionListener { loadFromEditor() }
        kdocButton.addActionListener { applyKDocOnly() }

        val buttons = JPanel()
        buttons.add(selectionButton)
        buttons.add(testButton)
        buttons.add(kdocButton)

        return FormBuilder.createFormBuilder()
            .addComponent(infoLabel)
            .addVerticalGap(10)
            .addLabeledComponent("Test Code:", JBScrollPane(inputArea), 1, true)
            .addComponent(buttons)
            .addVerticalGap(10)
            .addLabeledComponent("AI Response:", JBScrollPane(outputArea), 1, true)
            .panel
    }

    private fun loadFromEditor() {
        val projects = ProjectManager.getInstance().openProjects
        if (projects.isEmpty()) return
        val project = projects[0]
        
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor != null) {
            val selection = editor.selectionModel.selectedText ?: editor.document.text
            if (selection.isNotBlank()) {
                inputArea.text = selection
            }
        }
    }

    private fun runTest() {
        outputArea.text = "Loading..."
        val adapter = RestAdapter(settings)
        val code = inputArea.text
        
        coroutineScope.launch {
            try {
                var first = true
                adapter.review(code, "playground.kt").collect { chunk ->
                    if (first) {
                        outputArea.text = ""
                        first = false
                    }
                    outputArea.append(chunk)
                }
            } catch (e: Exception) {
                outputArea.text = "Error: ${e.message}"
            }
        }
    }

    private fun applyKDocOnly() {
        val fullText = outputArea.text
        val kdocRegex = "/\\*\\*([\\s\\S]*?)\\*/".toRegex()
        val match = kdocRegex.find(fullText)
        val kdoc = match?.value ?: return
        
        val projects = ProjectManager.getInstance().openProjects
        if (projects.isEmpty()) return
        val project = projects[0]
        
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor != null) {
            WriteCommandAction.runWriteCommandAction(project) {
                editor.document.insertString(editor.caretModel.offset, "$kdoc\n")
            }
        }
    }

    override fun dispose() {
        coroutineScope.cancel()
        super.dispose()
    }
}
