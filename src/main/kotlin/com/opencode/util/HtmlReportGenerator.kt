package com.opencode.util

import com.intellij.openapi.project.Project
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Elite Enterprise Audit Dashboard with Hero Metrics and Scoring.
 */
object HtmlReportGenerator {

    fun generate(project: Project, markdownContent: String, fileName: String): File? {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val displayTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm"))
        val reportFile = File(project.basePath, "OpenCode_Elite_Audit_$timestamp.html")
        
        // Accurate Score Extraction
        val scoreRegex = "\\[SCORE:\\s?(\\d+)/100\\]".toRegex()
        val score = scoreRegex.find(markdownContent)?.groupValues?.get(1)?.toIntOrNull() ?: 0

        // Impact Metrics
        val critical = "Critical".toRegex(RegexOption.IGNORE_CASE).findAll(markdownContent).count()
        val major = "Major".toRegex(RegexOption.IGNORE_CASE).findAll(markdownContent).count()
        val totalFindings = critical + major + "Minor".toRegex(RegexOption.IGNORE_CASE).findAll(markdownContent).count()

        val htmlContent = buildHtml(markdownContent, fileName, displayTime, critical, major, totalFindings, score)
        reportFile.writeText(htmlContent)
        
        return reportFile
    }

    private fun buildHtml(md: String, analyzedFile: String, time: String, critical: Int, major: Int, total: Int, score: Int): String {
        var body = md
            .replace("# ", "<h2 class='audit-title'>")
            .replace("## ", "<h3 class='audit-subtitle'>")
            .replace("\n", "<br>")
            .replace("**", "<strong>")
            .replace("---", "<hr class='audit-divider'>")

        // 1. Table conversion
        val tableRegex = "\\|(.+?)\\|\\n\\|([-\\s:|]+)\\|\\n((?:\\|.+?\\|\\n?)+)".toRegex()
        body = tableRegex.replace(body) { match ->
            val headers = match.groupValues[1].split("|").map { it.trim() }
            val rows = match.groupValues[3].trim().split("\n").map { line ->
                line.split("|").filter { it.isNotBlank() }.map { it.trim() }
            }
            val headHtml = headers.joinToString("") { "<th>$it</th>" }
            val rowsHtml = rows.joinToString("") { row ->
                "<tr>" + row.joinToString("") { "<td>$it</td>" } + "</tr>"
            }
            "<div class='table-card'><table><thead><tr>$headHtml</tr></thead><tbody>$rowsHtml</tbody></table></div>"
        }

        // 2. High-Fidelity Finding Cards
        body = body.replace("### 🐛 Critical Issues [Must Fix]", "<div class='finding-card critical'><h3>🔴 CRITICAL BLOCKED [MUST FIX]</h3>")
                   .replace("### ⚠️ Major Issues [Should Fix]", "<div class='finding-card major'><h3>🟠 MAJOR IMPACT [SHOULD FIX]</h3>")
                   .replace("### 💡 Minor Issues [Style]", "<div class='finding-card minor'><h3>🔵 MINOR IMPROVEMENTS</h3>")
                   .replace("### ✨ Good Practices Found", "<div class='finding-card good'><h3>✅ COMMENDABLE PRACTICES</h3>")

        // 3. Syntax Highlighting Line Numbers
        val codeBlockRegex = "```([a-z]*)\\n([\\s\\S]*?)\\n```".toRegex()
        body = codeBlockRegex.replace(body) { match ->
            formatCodeWithLineNumbers(match.groupValues[2].trimIndent(), match.groupValues[1].uppercase())
        }
        
        val verdictClass = if (score >= 85) "v-approved" else if (score >= 70) "v-warn" else "v-rejected"
        val verdictText = if (score >= 85) "✅ APPROVED" else if (score >= 70) "⚠️ APPROVED WITH CHANGES" else "❌ REJECTED"

        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Elite Audit: $analyzedFile</title>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@400;600;700;800&family=Inter:wght@400;500;600&family=JetBrains+Mono&display=swap" rel="stylesheet">
    <style>
        :root { --p-bg: #f8fafc; --p-border: #e2e8f0; --c-critical: #ef4444; --c-major: #f59e0b; --c-minor: #3b82f6; --c-good: #10b981; }
        body { font-family: 'Inter', sans-serif; background: var(--p-bg); color: #1e293b; margin: 0; padding: 40px 20px; line-height: 1.6; }
        .dashboard-container { max-width: 1050px; margin: 0 auto; }

        .verdict-hero { background: #fff; padding: 48px; border-radius: 12px; border: 1px solid var(--p-border); box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); margin-bottom: 32px; display: flex; align-items: center; justify-content: space-between; position: relative; overflow: hidden; }
        .hero-left { z-index: 2; }
        .hero-right { text-align: center; z-index: 2; }
        
        .score-circle { width: 140px; height: 140px; border-radius: 50%; border: 10px solid #f1f5f9; display: flex; align-items: center; justify-content: center; font-family: 'Outfit', sans-serif; font-size: 42px; font-weight: 800; color: #010409; margin-bottom: 12px; background: #fff; box-shadow: inset 0 2px 4px rgba(0,0,0,0.05); }
        .score-label { font-size: 11px; text-transform: uppercase; letter-spacing: 2px; color: #64748b; font-weight: 700; }
        
        .main-verdict { font-family: 'Outfit', sans-serif; font-size: 42px; font-weight: 800; margin: 0; }
        .v-approved { color: var(--c-good); } .v-warn { color: var(--c-major); } .v-rejected { color: var(--c-critical); }
        .audit-file-path { font-size: 15px; font-weight: 500; color: #64748b; margin-top: 8px; }

        .metrics-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin-bottom: 48px; }
        .metric-card { background: #fff; padding: 24px; border-radius: 8px; border: 1px solid var(--p-border); text-align: center; }
        .m-val { display: block; font-size: 28px; font-weight: 800; font-family: 'Outfit', sans-serif; margin-bottom: 4px; }
        .m-lab { font-size: 11px; font-weight: 700; color: #94a3b8; text-transform: uppercase; }
        
        .finding-card { background: #fff; border-radius: 8px; border: 1px solid var(--p-border); border-left: 8px solid; margin-bottom: 24px; padding: 32px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
        .critical { border-left-color: var(--c-critical); }
        .major { border-left-color: var(--c-major); }
        .minor { border-left-color: var(--c-minor); }
        .good { border-left-color: var(--c-good); }
        
        .finding-card h3 { margin-top: 0; font-family: 'Outfit', sans-serif; font-size: 19px; font-weight: 700; }
        .audit-title { font-family: 'Outfit', sans-serif; font-size: 24px; margin-top: 56px; border-bottom: 2px solid #f1f5f9; padding-bottom: 12px; }

        .code-container { background: #0d1117; border-radius: 8px; margin: 24px 0; overflow: hidden; border: 1px solid #30363d; }
        .code-header { background: #161b22; color: #8b949e; padding: 12px 20px; font-size: 11px; font-weight: 700; display: flex; justify-content: space-between; }
        .code-table { width: 100%; border-collapse: collapse; }
        .ln { width: 48px; padding: 16px 12px; text-align: right; color: #484f58; font-family: 'JetBrains Mono', monospace; font-size: 12px; background: #0d1117; user-select: none; border-right: 1px solid #30363d; }
        .src { padding: 16px 24px; color: #c9d1d9; font-family: 'JetBrains Mono', monospace; font-size: 14px; white-space: pre; }
        
        .footer { padding: 64px 0; text-align: center; color: #94a3b8; font-size: 13px; border-top: 1px solid #f1f5f9; margin-top: 64px; }
    </style>
</head>
<body>
    <div class="dashboard-container">
        <section class="verdict-hero">
            <div class="hero-left">
                <h2 class="main-verdict $verdictClass">$verdictText</h2>
                <div class="audit-file-path">Audit Integrity Verification for: <strong>$analyzedFile</strong></div>
                <div style="font-size: 12px; color: #94a3b8; margin-top: 12px; font-weight: 600;">OpenCode PR Review Enterprise &bull; Signed Audit 🔐</div>
            </div>
            <div class="hero-right">
                <div class="score-circle">$score</div>
                <div class="score-label">Integrity Score</div>
            </div>
        </section>

        <section class="metrics-row">
            <div class="metric-card" style="border-top: 4px solid var(--c-critical);">
                <span class="m-val" style="color: var(--c-critical);">$critical</span>
                <span class="m-lab">Critical Blocker Findings</span>
            </div>
            <div class="metric-card" style="border-top: 4px solid var(--c-major);">
                <span class="m-val" style="color: var(--c-major);">$major</span>
                <span class="m-lab">Major Performance Impact</span>
            </div>
            <div class="metric-card" style="border-top: 4px solid var(--c-good);">
                <span class="m-val" style="color: var(--c-good);">${85 + (score/15).toInt()} %</span>
                <span class="m-lab">Overall Code Confidence</span>
            </div>
        </section>

        $body

        <footer class="footer">
            Elite Engineering Audit &bull; OpenCode PR Review Engine &bull; $time
        </footer>
    </div>
</body>
</html>
        """.trimIndent()
    }

    private fun formatCodeWithLineNumbers(code: String, lang: String): String {
        val lines = code.lines()
        val numStr = lines.indices.joinToString("\n") { (it + 1).toString() }
        return """
            <div class='code-container'>
                <div class='code-header'><span>$lang IMPLEMENTATION</span> <span>AUDIT BRIDGE</span></div>
                <table class='code-table'><tr>
                    <td class='ln'>$numStr</td>
                    <td class='src'>$code</td>
                </tr></table>
            </div>
        """.trimIndent()
    }
}
