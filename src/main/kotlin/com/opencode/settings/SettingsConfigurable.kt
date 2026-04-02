package com.opencode.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.opencode.adapter.RestAdapter
import com.opencode.ui.ReviewTestDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.JBIntSpinner
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Font

/**
 * Advanced Settings UI for OpenCode PR Review with Audit Weighting.
 */
class AppSettingsConfigurable : Configurable {
    private val baseUrlField = JBTextField()
    private val modelNameField = JBTextField()
    private val systemPromptField = JBTextArea(10, 60).apply {
        lineWrap = true
        wrapStyleWord = true
        font = Font("Monospaced", Font.PLAIN, 12)
    }

    // Weighting Spinners
    private val codeQualityWeight = JBIntSpinner(25, 0, 100)
    private val bestPracticesWeight = JBIntSpinner(25, 0, 100)
    private val performanceWeight = JBIntSpinner(20, 0, 100)
    private val readabilityWeight = JBIntSpinner(15, 0, 100)
    private val securityWeight = JBIntSpinner(15, 0, 100)
    private val passingThreshold = JBIntSpinner(70, 0, 100)

    private val testStatusLabel = JBLabel("")
    private val testButton = JButton("Check Connection", AllIcons.Actions.Refresh)
    private val playgroundButton = JButton("Open AI Playground", AllIcons.Actions.Preview)
    private val restoreDefaultButton = JButton("Restore to Factory Defaults", AllIcons.Actions.Rollback)
    
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun getDisplayName(): String = "OpenCode PR Review"

    override fun createComponent(): JComponent {
        setupListeners()

        val weightPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Code Quality (Max Points):", codeQualityWeight)
            .addLabeledComponent("Best Practices (Max Points):", bestPracticesWeight)
            .addLabeledComponent("Performance & Optimization (Max Points):", performanceWeight)
            .addLabeledComponent("Readability & Maintainability (Max Points):", readabilityWeight)
            .addLabeledComponent("Security & Null Safety (Max Points):", securityWeight)
            .addSeparator()
            .addLabeledComponent("Minimum Passing Threshold (Verdict ❌ vs ⚠️):", passingThreshold)
            .panel

        val promptPanel = JPanel(BorderLayout())
        promptPanel.add(JBScrollPane(systemPromptField), BorderLayout.CENTER)
        val buttonBar = JPanel(FlowLayout(FlowLayout.RIGHT))
        buttonBar.add(restoreDefaultButton)
        promptPanel.add(buttonBar, BorderLayout.SOUTH)

        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Ollama Server URL:", baseUrlField)
            .addLabeledComponent("Active AI Model:", modelNameField)
            .addSeparator()
            .addComponent(JBLabel("Audit Scoring Algorithm & Thresholds:").apply { font = font.deriveFont(Font.BOLD) })
            .addComponent(weightPanel)
            .addSeparator()
            .addComponent(JBLabel("Review Script (System Prompt):").apply { font = font.deriveFont(Font.BOLD) })
            .addComponent(promptPanel)
            .addSeparator()
            .addLabeledComponent("Connectivity Test:", testButton)
            .addComponent(testStatusLabel)
            .addSeparator()
            .addLabeledComponent("Experimentation Tool:", playgroundButton)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    private fun setupListeners() {
        testButton.addActionListener { performConnectionTest() }
        playgroundButton.addActionListener { ReviewTestDialog().show() }
        restoreDefaultButton.addActionListener {
            systemPromptField.text = AppSettingsState.DEFAULT_SYSTEM_PROMPT
        }
    }

    private fun performConnectionTest() {
        testStatusLabel.text = "Checking connectivity to ${baseUrlField.text}..."
        val settings = AppSettingsState.instance
        val tempAdapter = RestAdapter(settings)
        scope.launch {
            val success = tempAdapter.ping()
            testStatusLabel.text = if (success) "✅ Connection Successful" else "❌ Connection Failed"
        }
    }

    override fun isModified(): Boolean {
        val s = AppSettingsState.instance
        return baseUrlField.text != s.restBaseUrl ||
               modelNameField.text != s.modelName ||
               systemPromptField.text != s.systemPrompt ||
               codeQualityWeight.number != s.weightCodeQuality ||
               bestPracticesWeight.number != s.weightBestPractices ||
               performanceWeight.number != s.weightPerformance ||
               readabilityWeight.number != s.weightReadability ||
               securityWeight.number != s.weightSecurity ||
               passingThreshold.number != s.passingThreshold
    }

    override fun apply() {
        val s = AppSettingsState.instance
        s.restBaseUrl = baseUrlField.text
        s.modelName = modelNameField.text
        s.systemPrompt = systemPromptField.text
        s.weightCodeQuality = codeQualityWeight.number
        s.weightBestPractices = bestPracticesWeight.number
        s.weightPerformance = performanceWeight.number
        s.weightReadability = readabilityWeight.number
        s.weightSecurity = securityWeight.number
        s.passingThreshold = passingThreshold.number
    }

    override fun reset() {
        val s = AppSettingsState.instance
        baseUrlField.text = s.restBaseUrl
        modelNameField.text = s.modelName
        systemPromptField.text = s.systemPrompt
        codeQualityWeight.number = s.weightCodeQuality
        bestPracticesWeight.number = s.weightBestPractices
        performanceWeight.number = s.weightPerformance
        readabilityWeight.number = s.weightReadability
        securityWeight.number = s.weightSecurity
        passingThreshold.number = s.passingThreshold
    }
}
