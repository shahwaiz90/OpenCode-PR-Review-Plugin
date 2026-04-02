package com.opencode.ui

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import com.intellij.ide.BrowserUtil
import com.opencode.util.HtmlReportGenerator
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.*
import java.awt.Color

/**
 * Enhanced Review Panel with professional Grade HUD and Real-time Scoring.
 */
class ReviewPanel(private val project: Project) : SimpleToolWindowPanel(true, true) {

    private val editor: Editor
    private val loadingIcon = AsyncProcessIcon("Loading")
    private val headerLabel = JBLabel("Select code to review...").apply {
        font = font.deriveFont(Font.BOLD, 14f)
    }
    
    // The Score HUD
    private val scoreBadge = JBLabel("--").apply {
        font = font.deriveFont(Font.BOLD, 18f)
        foreground = Color(0x64748b) // Slate gray
        border = JBUI.Borders.empty(4, 12)
    }
    private val scorePoints = JBLabel("Await Scan...").apply {
        font = font.deriveFont(11f)
        foreground = Color(0x94a3b8)
    }

    private var currentFileName: String = "unknown.kt"
    private var targetEditor: Editor? = null
    private var targetStart: Int = -1
    private var targetEnd: Int = -1

    init {
        val editorFactory = EditorFactory.getInstance()
        val document = editorFactory.createDocument("")
        editor = editorFactory.createEditor(document, project, com.intellij.openapi.fileTypes.PlainTextFileType.INSTANCE, false)
        
        setupUI()
    }

    private fun setupUI() {
        val topPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(12)
            background = Color(0xf8fafc)
        }
        
        val leftPanel = JPanel(VerticalFlowLayout(0, 0)).apply { isOpaque = false }
        leftPanel.add(headerLabel)
        leftPanel.add(scorePoints)
        
        val rightPanel = JPanel(BorderLayout()).apply { isOpaque = false }
        rightPanel.add(scoreBadge, BorderLayout.CENTER)
        rightPanel.add(loadingIcon, BorderLayout.EAST)
        
        topPanel.add(leftPanel, BorderLayout.WEST)
        topPanel.add(rightPanel, BorderLayout.EAST)
        
        val buttonBar = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
            border = JBUI.Borders.empty(4)
        }
        val exportBtn = JButton("📊 Export Dashboard").apply { addActionListener { exportToHtml() } }
        val applyBtn = JButton("🚀 Apply Strict Fix").apply { addActionListener { applySuggestedFix() } }
        val clearBtn = JButton("🧹 Clear").apply { addActionListener { clear() } }
        
        buttonBar.add(clearBtn)
        buttonBar.add(exportBtn)
        buttonBar.add(applyBtn)

        val bottomPanel = JPanel(BorderLayout())
        bottomPanel.add(buttonBar, BorderLayout.CENTER)

        setToolbar(topPanel)
        setContent(editor.component)
        loadingIcon.isVisible = false
    }

    fun setTarget(editor: Editor, start: Int, end: Int) {
        this.targetEditor = editor
        this.targetStart = start
        this.targetEnd = end
    }

    fun showHeader(fileName: String) {
        this.currentFileName = fileName
        headerLabel.text = "Reviewing: $fileName"
        scoreBadge.text = "--"
        scoreBadge.foreground = Color(0x64748b)
        scorePoints.text = "Initializing AI Auditor..."
    }

    fun appendText(chunk: String) {
        // Real-time Score Extraction Logic
        if (chunk.contains("[SCORE:")) {
            val scoreMatch = "\\[SCORE:\\s?(\\d+)/100\\]".toRegex().find(chunk)
            val scoreVal = scoreMatch?.groupValues?.get(1)?.toIntOrNull()
            if (scoreVal != null) {
                updateScoreUI(scoreVal)
            }
        }

        WriteCommandAction.runWriteCommandAction(project) {
            val document = editor.document
            document.insertString(document.textLength, chunk)
            
            // Auto-scroll to the end of the streaming review
            val scrollModel = editor.scrollingModel
            scrollModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.MAKE_VISIBLE)
        }
    }

    private fun updateScoreUI(score: Int) {
        val grade = when {
            score >= 90 -> "A+" to Color(0x10b981) // Green
            score >= 80 -> "B" to Color(0x3b82f6)  // Blue
            score >= 70 -> "C" to Color(0xf59e0b)  // Orange
            else -> "F" to Color(0xef4444)         // Red
        }
        
        SwingUtilities.invokeLater {
            scoreBadge.text = grade.first
            scoreBadge.foreground = grade.second
            scorePoints.text = "Audit Integrity Score: $score / 100"
        }
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
        if (file != null) BrowserUtil.browse(file)
    }

    private fun applySuggestedFix() {
        val fullText = editor.document.text
        val codeBlockRegex = "```([a-z]*)\\n([\\s\\S]*?)\\n```".toRegex()
        val allMatches = codeBlockRegex.findAll(fullText).toList()
        if (allMatches.isEmpty()) return

        val fixMatch = allMatches.find { it.groupValues[1].lowercase() in listOf("kotlin", "kt", "java") }
            ?: allMatches.find { m -> !m.groupValues[2].contains("curl ") }
        
        if (fixMatch != null && targetEditor != null) {
            val replacement = fixMatch.groupValues[2].trimIndent()
            WriteCommandAction.runWriteCommandAction(project) {
                targetEditor?.document?.replaceString(targetStart, targetEnd, replacement)
            }
        }
    }
}

/** Utility layout for vertical stacking in header */
class VerticalFlowLayout(val vgap: Int = 5, val hgap: Int = 0) : java.awt.LayoutManager2 {
    private val components = mutableListOf<java.awt.Component>()
    override fun addLayoutComponent(c: java.awt.Component, constraints: Any?) { components.add(c) }
    override fun removeLayoutComponent(c: java.awt.Component) { components.remove(c) }
    override fun addLayoutComponent(name: String?, c: java.awt.Component) { components.add(c) }
    override fun preferredLayoutSize(parent: java.awt.Container): java.awt.Dimension {
        var h = vgap
        var w = 0
        components.forEach { h += it.preferredSize.height + vgap; w = maxOf(w, it.preferredSize.width) }
        return java.awt.Dimension(w + hgap * 2, h)
    }
    override fun minimumLayoutSize(parent: java.awt.Container) = preferredLayoutSize(parent)
    override fun layoutContainer(parent: java.awt.Container) {
        var y = vgap
        components.forEach {
            it.setBounds(hgap, y, it.preferredSize.width, it.preferredSize.height)
            y += it.preferredSize.height + vgap
        }
    }
    override fun getLayoutAlignmentX(target: java.awt.Container) = 0.5f
    override fun getLayoutAlignmentY(target: java.awt.Container) = 0.5f
    override fun invalidateLayout(target: java.awt.Container) {}
    override fun maximumLayoutSize(target: java.awt.Container) = preferredLayoutSize(target)
}
