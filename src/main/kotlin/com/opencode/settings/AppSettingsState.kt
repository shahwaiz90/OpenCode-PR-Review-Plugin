package com.opencode.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.openapi.components.Service

/**
 * Persists user configuration for the OpenCode PR Review plugin.
 */
@State(
    name = "com.opencode.settings.AppSettingsState",
    storages = [Storage("OpenCodePlugin.xml")]
)
@Service
class AppSettingsState : PersistentStateComponent<AppSettingsState> {
    var mode: String = "REST"
    var restBaseUrl: String = "http://localhost:11434"
    var modelName: String = "qwen2.5-coder:latest"
    var cliCommandPath: String = "opencode"
    var showDebugInfo: Boolean = false
    
    var weightCodeQuality: Int = 25
    var weightBestPractices: Int = 25
    var weightPerformance: Int = 20
    var weightReadability: Int = 15
    var weightSecurity: Int = 15
    var passingThreshold: Int = 70
    
    var systemPrompt: String = DEFAULT_SYSTEM_PROMPT

    companion object {
        val instance: AppSettingsState
            get() = ApplicationManager.getApplication().getService(AppSettingsState::class.java)

        val DEFAULT_SYSTEM_PROMPT: String = """
            You are an expert Software Engineering Auditor and Technical Lead Mentor. 
            
            [SCORE_SYSTEM]
            You MUST start your response with: [SCORE: X/100]
            - If MAJOR PERFORMANCE or CRITICAL BLOCKERS exist: Status is ❌ REJECTED and Score capped at 50/100.
            
            [STRICT_TABLE_FORMAT]
            - CATEGORY SCORES BREAKDOWN MUST be a clean 4-column table.
            - Columns: | Category | Score | Max | Status |
            - Example: | Code Quality | 25 | 25 | Approved |
            
            [FORMATTING_PROTOCOL]
            1. ONLY bold the issue title or question. Example: **Performance Leak:** explanation...
            2. Detailed explanations and Growth Guidance MUST be in normal text.
            3. Use triple backticks for code blocks.
            
            [REPORT_STRUCTURE]
            # 📋 PR AUDIT REPORT [SCORE: X/100]
            **Developer Level:** [Junior / Mid-Level / Senior]
            ---
            ## 🏆 DEVELOPER SCORE
            ---
            ## 📊 EXECUTIVE SUMMARY
            ---
            ## 🔍 DETAILED AUDIT FINDINGS
            ### 🐛 Critical Issues [Must Fix]
            ### ⚠️ Major Issues [Should Fix]
            ### 💡 Minor Issues [Style]
            ---
            ## 📈 CATEGORY SCORES BREAKDOWN
            | Category | Score | Max | Status |
            |:---|:---|:---|:---|
            | Code Quality | 25 | 25 | Approved |
            | Best Practices | 25 | 25 | Approved |
            | Performance | 10 | 20 | Approved |
            | Readability | 15 | 15 | Approved |
            | Security | 15 | 15 | Approved |
            ---
            ## 👨‍🏫 GROWTH GUIDANCE (Professional Mentorship)
            ## 💬 FINAL VERDICT
        """.trimIndent()
    }

    override fun getState(): AppSettingsState = this
    override fun loadState(state: AppSettingsState) { XmlSerializerUtil.copyBean(state, this) }
}
