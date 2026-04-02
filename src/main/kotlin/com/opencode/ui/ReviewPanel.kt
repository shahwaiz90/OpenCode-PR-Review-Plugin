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
import com.opencode.settings.AppSettingsState
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.*
import java.awt.Color

/**
 * Expert Auditor Review Panel with Dashboard Actions and Grade HUD.
 */
class ReviewPanel(private val project: Project) : SimpleToolWindowPanel(true, true) {

    private val editor: Editor
    private val loadingIcon = AsyncProcessIcon("Loading")
    private val headerLabel = JBLabel("Select code to review...").apply {
        font = font.deriveFont(Font.BOLD, 14f)
    }
    
    private val scoreBadge = JBLabel("--").apply {
        font = font.deriveFont(Font.BOLD, 18f)
        foreground = Color(0x64748b)
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
        // TOP HUD (Scoring & Header)
        val topPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(12)
            background = Color(0xf8fafc)
        }
        val leftPanel = JPanel(VerticalFlowLayout(0, 0)).apply { isOpaque = false }
        leftPanel.add(headerLabel); leftPanel.add(scorePoints)
        val rightPanel = JPanel(BorderLayout()).apply { isOpaque = false }
        rightPanel.add(scoreBadge, BorderLayout.CENTER); rightPanel.add(loadingIcon, BorderLayout.EAST)
        topPanel.add(leftPanel, BorderLayout.WEST); topPanel.add(rightPanel, BorderLayout.EAST)
        
        // BOTTOM ACTION BAR
        val buttonBar = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
            border = JBUI.Borders.customLine(Color(0xe2e8f0), 1, 0, 0, 0)
            background = Color.WHITE
        }
        val exportBtn = JButton("📊 Export Dashboard").apply { addActionListener { exportToHtml() } }
        val applyBtn = JButton("🚀 Apply Strict Fix").apply { addActionListener { applySuggestedFix() } }
        val clearBtn = JButton("🧹 Clear").apply { addActionListener { clear() } }
        buttonBar.add(clearBtn); buttonBar.add(exportBtn); buttonBar.add(applyBtn)

        // MAIN CONTENT (Editor + Bottom Bar)
        val mainContent = JPanel(BorderLayout())
        mainContent.add(editor.component, BorderLayout.CENTER)
        mainContent.add(buttonBar, BorderLayout.SOUTH)

        setToolbar(topPanel)
        setContent(mainContent)
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

    fun appendText(chunk: String, isDebug: Boolean = false) {
        val settings = AppSettingsState.instance
        if (isDebug && !settings.showDebugInfo) return
        if (chunk.contains("[SCORE:")) {
            val sm = "\\[SCORE:\\s?(\\d+)/100\\]".toRegex().find(chunk)
            sm?.groupValues?.get(1)?.toIntOrNull()?.let { updateScoreUI(it) }
        }
        WriteCommandAction.runWriteCommandAction(project) {
            val d = editor.document
            d.insertString(d.textLength, chunk)
            editor.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.MAKE_VISIBLE)
        }
    }

    private fun updateScoreUI(score: Int) {
        val grade = when {
            score >= 90 -> "A+" to Color(0x10b981); score >= 80 -> "B" to Color(0x3b82f6)
            score >= 70 -> "C" to Color(0xf59e0b); else -> "F" to Color(0xef4444)
        }
        SwingUtilities.invokeLater {
            scoreBadge.text = grade.first; scoreBadge.foreground = grade.second
            scorePoints.text = "Audit Integrity Score: $score / 100"
        }
    }

    fun clear() { WriteCommandAction.runWriteCommandAction(project) { editor.document.setText("") } }

    fun setLoading(loading: Boolean) {
        loadingIcon.isVisible = loading
        if (loading) loadingIcon.resume() else loadingIcon.suspend()
    }

    private fun exportToHtml() {
        val content = editor.document.text
        if (content.isBlank()) return
        HtmlReportGenerator.generate(project, content, currentFileName)?.let { BrowserUtil.browse(it) }
    }

    private fun applySuggestedFix() {
        val ft = editor.document.text
        val cbr = "```([a-z]*)\\n([\\s\\S]*?)\\n```".toRegex()
        val am = cbr.findAll(ft).toList()
        if (am.isEmpty()) return
        val fm = am.find { it.groupValues[1].lowercase() in listOf("kotlin", "kt", "java") } ?: am[0]
        if (targetEditor != null) {
            val rep = fm.groupValues[2].trimIndent()
            WriteCommandAction.runWriteCommandAction(project) {
                targetEditor?.document?.replaceString(targetStart, targetEnd, rep)
            }
        }
    }
}

class VerticalFlowLayout(val vgap: Int = 5, val hgap: Int = 0) : java.awt.LayoutManager2 {
    private val components = mutableListOf<java.awt.Component>()
    override fun addLayoutComponent(c: java.awt.Component, cs: Any?) { components.add(c) }
    override fun removeLayoutComponent(c: java.awt.Component) { components.remove(c) }
    override fun addLayoutComponent(n: String?, c: java.awt.Component) { components.add(c) }
    override fun preferredLayoutSize(p: java.awt.Container): java.awt.Dimension {
        var h = vgap; var w = 0
        components.forEach { h += it.preferredSize.height + vgap; w = maxOf(w, it.preferredSize.width) }
        return java.awt.Dimension(w + hgap * 2, h)
    }
    override fun minimumLayoutSize(p: java.awt.Container) = preferredLayoutSize(p)
    override fun layoutContainer(p: java.awt.Container) {
        var y = vgap
        components.forEach { it.setBounds(hgap, y, it.preferredSize.width, it.preferredSize.height); y += it.preferredSize.height + vgap }
    }
    override fun getLayoutAlignmentX(t: java.awt.Container) = 0.5f; override fun getLayoutAlignmentY(t: java.awt.Container) = 0.5f
    override fun invalidateLayout(t: java.awt.Container) {}; override fun maximumLayoutSize(t: java.awt.Container) = preferredLayoutSize(t)
}
