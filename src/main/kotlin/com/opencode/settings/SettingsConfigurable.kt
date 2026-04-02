package com.opencode.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.io.HttpRequests
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.opencode.ui.ReviewTestDialog
import com.intellij.openapi.ui.ComboBox
import com.intellij.icons.AllIcons
import com.opencode.adapter.RestAdapter
import com.intellij.ui.TitledSeparator
import com.intellij.ui.JBColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.*

/**
 * Professional Swing implementation of the settings page with robust button state management.
 */
class AppSettingsConfigurable : Configurable {
    private val settings = AppSettingsState.instance
    
    // Connection UI
    private val restUrlField = JBTextField(settings.restBaseUrl)
    private val testButton = JButton("Test Connection", AllIcons.Actions.Checked)
    private val connectionStatusLabel = JBLabel("")
    
    // Model UI
    private val modelNameField = ComboBox<String>().apply { isEditable = true }
    private val refreshModelsButton = JButton("Refresh List", AllIcons.Actions.Refresh)
    
    // Persona UI
    private val systemPromptField = JBTextArea(settings.systemPrompt, 15, 60).apply {
        lineWrap = true
        wrapStyleWord = true
        font = Font("Monospaced", Font.PLAIN, 12)
    }
    private val showDebugCheckBox = JCheckBox("Display AI connection & debug information", settings.showDebugInfo)
    private val cliCommandPathField = JBTextField(settings.cliCommandPath)
    
    private val playgroundButton = JButton("Open AI Playground", AllIcons.Actions.Preview)
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun getDisplayName(): String = "OpenCode PR Review"

    override fun createComponent(): JComponent {
        setupListeners()
        modelNameField.addItem(settings.modelName)
        refreshAvailableModels()
        
        return FormBuilder.createFormBuilder()
            .addComponent(TitledSeparator("🌐 Server Connection"))
            .addLabeledComponent("Ollama Base URL:", createRow(restUrlField, testButton, connectionStatusLabel))
            
            .addVerticalGap(10)
            .addComponent(TitledSeparator("🧠 Model Configuration"))
            .addLabeledComponent("Active Model:", createRow(modelNameField, refreshModelsButton, null))
            
            .addVerticalGap(10)
            .addComponent(TitledSeparator("🎭 AI Reviewer Persona"))
            .addLabeledComponent(JBLabel("PR Review Rules & Standards:").apply { 
                verticalAlignment = SwingConstants.TOP 
                setToolTipText("Define how the AI should review your code, what rules to follow, and the fix style.")
            }, JBScrollPane(systemPromptField), 1, true)
            
            .addVerticalGap(10)
            .addComponent(TitledSeparator("🛠️ Advanced Settings"))
            .addComponent(showDebugCheckBox)
            .addLabeledComponent("CLI Executable:", cliCommandPathField)
            
            .addVerticalGap(20)
            .addComponent(JSeparator())
            .addComponent(JPanel(FlowLayout(FlowLayout.RIGHT)).apply { 
                add(JBLabel("Need to test your prompt? "))
                add(playgroundButton)
            })
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    private fun createRow(mainComp: JComponent, btn: JButton, status: JLabel?): JPanel {
        val p = JPanel(BorderLayout(5, 0))
        p.add(mainComp, BorderLayout.CENTER)
        val bp = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
        bp.add(btn)
        if (status != null) bp.add(status)
        p.add(bp, BorderLayout.EAST)
        return p
    }

    private fun setupListeners() {
        testButton.addActionListener { performGlobalNotificationTest(restUrlField.text) }
        refreshModelsButton.addActionListener { refreshAvailableModels() }
        playgroundButton.addActionListener { ReviewTestDialog().show() }
    }

    private fun refreshAvailableModels() {
        refreshModelsButton.isEnabled = false
        refreshModelsButton.text = "Fetching..."
        
        // Pass the CURRENT text field URL for immediate testing
        val adapter = RestAdapter(settings, restUrlField.text)
        
        scope.launch {
            try {
                val models = adapter.fetchModels()
                ApplicationManager.getApplication().invokeLater {
                    val current = modelNameField.item ?: settings.modelName
                    modelNameField.removeAllItems()
                    if (models.isNotEmpty()) {
                        models.forEach { modelNameField.addItem(it) }
                        if (models.contains(current)) modelNameField.item = current
                    } else {
                        modelNameField.addItem(current)
                    }
                }
            } catch (e: Exception) {
                System.err.println("OPENCODE ERR: Model fetch failed: ${e.message}")
            } finally {
                ApplicationManager.getApplication().invokeLater {
                    refreshModelsButton.isEnabled = true
                    refreshModelsButton.text = "Refresh List"
                }
            }
        }
    }

    private fun performGlobalNotificationTest(urlToTest: String) {
        testButton.isEnabled = false
        connectionStatusLabel.foreground = JBColor.BLUE
        connectionStatusLabel.text = "Testing..."
        
        // Pass the custom URL for testing before saving
        val adapter = RestAdapter(settings, urlToTest)
        
        scope.launch {
            try {
                val success = adapter.ping()
                ApplicationManager.getApplication().invokeLater {
                    if (success) {
                        connectionStatusLabel.foreground = JBColor.GREEN
                        connectionStatusLabel.text = "✅ Connection Successful"
                    } else {
                        connectionStatusLabel.foreground = JBColor.RED
                        connectionStatusLabel.text = "❌ Connection Failed"
                    }
                }
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    connectionStatusLabel.foreground = JBColor.RED
                    connectionStatusLabel.text = "❌ Error: ${e.message}"
                }
            } finally {
                ApplicationManager.getApplication().invokeLater {
                    testButton.isEnabled = true
                    testButton.text = "Test Connection"
                }
            }
        }
    }

    override fun isModified(): Boolean =
        restUrlField.text != settings.restBaseUrl ||
        modelNameField.item != settings.modelName ||
        cliCommandPathField.text != settings.cliCommandPath ||
        systemPromptField.text != settings.systemPrompt ||
        showDebugCheckBox.isSelected != settings.showDebugInfo

    override fun apply() {
        settings.restBaseUrl = restUrlField.text
        settings.modelName = modelNameField.item ?: settings.modelName
        settings.cliCommandPath = cliCommandPathField.text
        settings.systemPrompt = systemPromptField.text
        settings.showDebugInfo = showDebugCheckBox.isSelected
    }

    override fun reset() {
        restUrlField.text = settings.restBaseUrl
        modelNameField.item = settings.modelName
        cliCommandPathField.text = settings.cliCommandPath
        systemPromptField.text = settings.systemPrompt
        showDebugCheckBox.isSelected = settings.showDebugInfo
        connectionStatusLabel.text = ""
    }
}
