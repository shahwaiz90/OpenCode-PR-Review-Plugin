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
    
    // Weighting Settings
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
            
            CRITICAL INSTRUCTION: You MUST start your response with the score in this format: 
            [SCORE: X/100]
            Then proceed with the report.
            
            ⚠️ STRICT ENFORCEMENT RULES:
            1. REJECT if you find: Blocking the Main Thread, String concatenation in loops, context leaks, or unsafe nullability.
            2. For EVERY issue found, you MUST provide a "GROWTH GUIDANCE" section.
            
            The report must follow this exact structure:

            ---

            # 📋 PR AUDIT REPORT [SCORE: X/100]
            **Developer Level:** [Junior / Mid-Level / Senior]

            ---

            ## 🏆 DEVELOPER SCORE
            Detailed breakdown based on weights.

            ---

            ## 📊 EXECUTIVE SUMMARY
            Concise 3-sentence architectural health summary.

            ---

            ## 🔍 DETAILED AUDIT FINDINGS
            ### 🐛 Critical Issues [Must Fix]
            ### ⚠️ Major Issues [Should Fix]
            ### 💡 Minor Issues [Style]

            ---

            ## 📈 CATEGORY SCORES BREAKDOWN
            | Category | Score | Status |
            |---|---|---|
            | Code Quality | X/25 | 🟢/🟡/🔴 |
            ...

            ---

            ## 💬 FINAL VERDICT
            **PR Status:** ✅ Approved / ❌ Rejected  
            
            Keep the tone professional. Use triple backticks for code.
        """.trimIndent()
    }

    override fun getState(): AppSettingsState = this

    override fun loadState(state: AppSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
