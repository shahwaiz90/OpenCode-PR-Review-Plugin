package com.opencode.ui

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.ui.dsl.builder.panel
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import com.opencode.settings.AppSettingsState
import com.opencode.util.HtmlReportGenerator
import com.intellij.ide.BrowserUtil
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.border.MatteBorder

/**
 * Premium Review Panel UI with Export HTML capability.
 */
class ReviewPanel(private val project: Project) : SimpleToolWindowPanel(true, true) {
    private val editor: EditorEx = createEditor()
    private val loadingIcon = AsyncProcessIcon("OpenCodeLoading")
    private var lastTriggeredEditor: Editor? = null
    private var currentFileName: String = "Untitled"
    
    private var reviewStartOffset: Int = -1
    private var reviewEndOffset: Int = -1
    
    init {
        val toolbarPanel = JPanel(BorderLayout())
        val innerToolbar = JPanel(FlowLayout(FlowLayout.LEFT, 10, 8))
        innerToolbar.background = UIUtil.getPanelBackground()
        
        val applyBtn = JButton("Apply Fix").apply { 
            addActionListener { applySuggestedFix() }
            foreground = JBColor.BLUE
        }
        val kdocBtn = JButton("Just KDocs").apply { 
            addActionListener { applyJustKDoc() }
        }
        val exportBtn = JButton("Export HTML", AllIcons.ToolbarDecorator.Export).apply {
            addActionListener { exportToHtml() }
            foreground = JBColor.decode("#10b981")
        }
        val copyBtn = JButton("Copy").apply { 
            addActionListener { copyToClipboard() }
        }
        val clearBtn = JButton("Clear").apply { 
            addActionListener { clear() }
        }
        
        innerToolbar.add(applyBtn)
        innerToolbar.add(kdocBtn)
        innerToolbar.add(exportBtn)
        innerToolbar.add(copyBtn)
        innerToolbar.add(clearBtn)
        innerToolbar.add(loadingIcon)
        
        toolbarPanel.add(innerToolbar, BorderLayout.WEST)
        toolbarPanel.border = MatteBorder(0, 0, 1, 0, JBColor.border())
        
        setToolbar(toolbarPanel)
        
        val contentWrapper = JPanel(BorderLayout())
        contentWrapper.border = JBUI.Borders.empty(10)
        contentWrapper.add(editor.component, BorderLayout.CENTER)
        
        setContent(contentWrapper)
        loadingIcon.suspend()
    }

    private fun createEditor(): EditorEx {
        val document = EditorFactory.getInstance().createDocument("")
        val fileType = FileTypeManager.getInstance().getFileTypeByExtension("md")
        val editor = EditorFactory.getInstance().createEditor(document, project, fileType!!, true) as EditorEx
        
        editor.settings.apply {
            isLineNumbersShown = false
            setGutterIconsShown(true)
            isRightMarginShown = false
            additionalLinesCount = 5
            isUseSoftWraps = true
            isVirtualSpace = false
        }
        
        editor.colorsScheme.apply {
            editorFontSize = 14
            lineSpacing = 1.3f
        }
        
        return editor
    }

    fun setTarget(editor: Editor, start: Int, end: Int) {
        this.lastTriggeredEditor = editor
        this.reviewStartOffset = start
        this.reviewEndOffset = end
    }

    fun appendText(text: String, isDebug: Boolean = false) {
        val settings = AppSettingsState.instance
        if (isDebug && !settings.showDebugInfo) return
        
        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(project) {
                editor.document.insertString(editor.document.textLength, text)
            }
        }
    }

    fun showHeader(fileName: String) {
        clear()
        this.currentFileName = fileName
        val header = """
            # 🛡️ OpenCode PR Review
            **Analyzing Source:** `${fileName}`
            
            ---
            
        """.trimIndent()
        appendText(header)
    }

    fun clear() {
        WriteCommandAction.runWriteCommandAction(project) {
            editor.document.setText("")
        }
    }

    fun setLoading(loading: Boolean) {
        loadingIcon.isVisible = loading
        if (loading) loadingIcon.resume() else loadingIcon.suspend()
    }

    private fun exportToHtml() {
        val content = editor.document.text
        if (content.isBlank()) return
        
        val file = HtmlReportGenerator.generate(project, content, currentFileName)
        if (file != null) {
            BrowserUtil.browse(file)
        }
    }

    private fun applySuggestedFix() {
        val fullText = editor.document.text
        val codeBlockRegex = "```([a-z]*)\\n([\\s\\S]*?)\\n```".toRegex()
        val allMatches = codeBlockRegex.findAll(fullText).toList()
        if (allMatches.isEmpty()) return

        var fixMatch = allMatches.find { match ->
            val lang = match.groupValues[1].lowercase()
            lang == "kotlin" || lang == "kt" || lang == "java"
        }

        if (fixMatch == null) {
            fixMatch = allMatches.find { match ->
                val content = match.groupValues[2]
                !content.contains("curl ") && !content.contains("http://") && !content.contains("apt-get")
            }
        }
        
        if (fixMatch == null) {
            fixMatch = allMatches.maxByOrNull { it.groupValues[2].length }
        }

        val fix = fixMatch?.groupValues?.get(2) ?: return
        val targetEditor = lastTriggeredEditor ?: FileEditorManager.getInstance(project).selectedTextEditor ?: return

        WriteCommandAction.runWriteCommandAction(project) {
            val selectionModel = targetEditor.selectionModel
            when {
                selectionModel.hasSelection() -> {
                    targetEditor.document.replaceString(selectionModel.selectionStart, selectionModel.selectionEnd, fix)
                }
                reviewStartOffset >= 0 && reviewEndOffset > reviewStartOffset -> {
                    val safeEnd = minOf(reviewEndOffset, targetEditor.document.textLength)
                    targetEditor.document.replaceString(reviewStartOffset, safeEnd, fix)
                }
                else -> {
                    targetEditor.document.insertString(targetEditor.caretModel.offset, fix)
                }
            }
        }
    }

    private fun applyJustKDoc() {
        val fullText = editor.document.text
        val kdocRegex = "/\\*\\*([\\s\\S]*?)\\*/".toRegex()
        val match = kdocRegex.find(fullText)
        val kdoc = match?.value ?: return
        
        val targetEditor = lastTriggeredEditor ?: FileEditorManager.getInstance(project).selectedTextEditor ?: return

        WriteCommandAction.runWriteCommandAction(project) {
            val insertOffset = when {
                targetEditor.selectionModel.hasSelection() -> targetEditor.selectionModel.selectionStart
                reviewStartOffset >= 0 -> reviewStartOffset
                else -> targetEditor.caretModel.offset
            }
            val safeOffset = minOf(insertOffset, targetEditor.document.textLength)
            targetEditor.document.insertString(safeOffset, "$kdoc\n")
        }
    }

    private fun copyToClipboard() {
        val selection = editor.selectionModel.selectedText ?: editor.document.text
        if (selection.isNotEmpty()) {
            java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(
                java.awt.datatransfer.StringSelection(selection), null
            )
        }
    }
}

class ReviewToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = ReviewPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
