package com.opencode.util

import com.intellij.openapi.project.Project
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Enterprise Audit Dashboard Engine.
 * Optimized for Aligned Single-Row Tables, Point Masking, and Section-Aware Scoping.
 */
object HtmlReportGenerator {

    fun generate(project: Project, markdownContent: String, fileName: String): File? {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val displayTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm"))
        val reportFile = File(project.basePath, "OpenCode_Elite_Audit_$timestamp.html")
        
        val scoreRegex = "\\[SCORE:\\s?(\\d+)/100\\]".toRegex()
        val score = scoreRegex.find(markdownContent)?.groupValues?.get(1)?.toIntOrNull() ?: 0

        val criticalCount = "Critical|Must Fix|🔴".toRegex(RegexOption.IGNORE_CASE).findAll(markdownContent).count()
        val majorCount = "Major|Should Fix|🟠".toRegex(RegexOption.IGNORE_CASE).findAll(markdownContent).count()
        val totalFindings = criticalCount + majorCount + "Minor|Style|🔵".toRegex(RegexOption.IGNORE_CASE).findAll(markdownContent).count()

        val htmlContent = buildHtml(markdownContent, fileName, displayTime, criticalCount, majorCount, totalFindings, score)
        reportFile.writeText(htmlContent)
        
        return reportFile
    }

    private fun buildHtml(md: String, analyzedFile: String, time: String, critical: Int, major: Int, total: Int, score: Int): String {
        // 1. Structural Pre-processing
        val processedMd = reconstructBrokenTables(md)
        val lines = processedMd.lines()
        val bodyBuilder = StringBuilder()
        var currentCardClass: String? = null

        // 2. Section-Aware Architecture
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("##")) {
                if (currentCardClass != null) { bodyBuilder.append("</div>\n"); currentCardClass = null }
                val patternMap = mapOf(
                    "(Critical|Must Fix|🔴)" to "critical",
                    "(Major|Should Fix|🟠)" to "major",
                    "(Minor|Style|🔵)" to "minor",
                    "(Good|Commendable|✅)" to "good",
                    "(Growth|Mentorship|GUIDANCE|👨‍🏫)" to "mentor"
                )
                var matchedClass: String? = null
                for ((pattern, css) in patternMap) {
                    if (trimmed.contains(pattern.toRegex(RegexOption.IGNORE_CASE))) { matchedClass = css; break }
                }
                if (matchedClass != null) {
                    currentCardClass = matchedClass
                    bodyBuilder.append("<div class='finding-card $matchedClass'>\n")
                }
                val tag = if (trimmed.startsWith("###")) "h3" else "h2"
                bodyBuilder.append("<$tag class='audit-$tag'>${trimmed.replace("#", "").trim()}</$tag>\n")
            } else if (trimmed == "---") {
                if (currentCardClass != null) { bodyBuilder.append("</div>\n"); currentCardClass = null }
                bodyBuilder.append("<hr class='audit-hr'>\n")
            } else {
                bodyBuilder.append(line).append("\n")
            }
        }
        if (currentCardClass != null) { bodyBuilder.append("</div>\n") }

        var body = bodyBuilder.toString()
        body = body.replace("# ", "<h1 class='audit-h1'>").replace("\n", "<br>").replace("**", "<strong>", false)

        // 3. Surgical Single-Row Table Reconstruction
        val tableRegex = "(\\|[\\s\\S]+?\\|)".toRegex()
        body = tableRegex.replace(body) { match ->
            val content = match.groupValues[1].replace("<br>", "\n")
            val rows = content.lines().map { it.trim() }.filter { it.count { c -> c == '|' } >= 2 }
            if (rows.isEmpty()) return@replace match.groupValues[1]
            
            val headLine = rows[0]
            val dataRows = rows.drop(1).filter { !it.contains("---") && !it.contains(":---") }
            
            val headHtml = headLine.split("|").filter { it.isNotBlank() }.joinToString("") { "<th>${it.trim()}</th>" }
            val rowsHtml = dataRows.joinToString("") { row ->
                val cells = row.split("|").filter { it.isNotBlank() }.map { it.trim() }
                val stsClass = when {
                    cells.any { it.contains("✅") } -> "st-pos"
                    cells.any { it.contains("❌") } -> "st-neg"
                    else -> ""
                }
                "<tr>" + cells.joinToString("") { "<td><span class='$stsClass'>$it</span></td>" } + "</tr>"
            }
            "<div class='table-wrapper'><table><thead><tr>$headHtml</tr></thead><tbody>$rowsHtml</tbody></table></div>"
        }

        // 4. Code Highlighting
        val codeBlockRegex = "```([a-z]*)\\n([\\s\\S]*?)\\n```".toRegex()
        body = codeBlockRegex.replace(body) { match ->
            formatCodeWithLineNumbers(match.groupValues[2].trimIndent(), match.groupValues[1].uppercase())
        }

        val verdictClass = if (score >= 85) "st-approved" else if (score >= 70) "st-warning" else "st-rejected"
        val statusText = if (score >= 85) "CERTIFIED" else if (score >= 70) "ACTION REQUIRED" else "BLOCKING ISSUES"

        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Elite Audit Report</title>
    <style>
        :root {
            --bg: #f9fafb; --white: #ffffff;
            --text-main: #111827; --text-sec: #374151; --text-muted: #9ca3af;
            --border: #e5e7eb; --clr-red: #ef4444; --clr-orange: #f59e0b; --clr-green: #10b981; --clr-blue: #3b82f6;
        }
        body { font-family: -apple-system, system-ui, sans-serif; background: var(--bg); color: var(--text-main); margin: 0; padding: 60px 20px; line-height: 1.6; }
        .container { max-width: 900px; margin: 0 auto; }
        
        .report-header { background: var(--white); border: 1px solid var(--border); border-radius: 12px; padding: 40px; margin-bottom: 40px; display: flex; justify-content: space-between; align-items: center; }
        .score-num { font-size: 48px; font-weight: 800; line-height: 1; }
        .score-status { font-size: 12px; font-weight: 700; border-radius: 4px; padding: 4px 12px; margin-top: 8px; display: inline-block; }
        .st-approved { color: var(--clr-green); background: #ecfdf5; }
        .st-warning { color: var(--clr-orange); background: #fffbeb; }
        .st-rejected { color: var(--clr-red); background: #fef2f2; }

        .audit-h1 { font-size: 26px; font-weight: 800; border-bottom: 1px solid var(--border); padding-bottom: 16px; margin-bottom: 32px; }
        .audit-h2 { font-size: 18px; font-weight: 700; margin-bottom: 16px; }
        .audit-h3 { font-size: 15px; font-weight: 700; margin-bottom: 12px; }

        .finding-card { background: var(--white); border: 1px solid var(--border); border-left: 5px solid; border-radius: 8px; padding: 32px; margin-bottom: 32px; }
        .critical { border-left-color: var(--clr-red); }
        .major { border-left-color: var(--clr-orange); }
        .minor { border-left-color: var(--clr-blue); }
        .mentor { border-left-color: #6366f1; background: #f5f3ff; }

        .table-wrapper { background: var(--white); border: 1px solid var(--border); border-radius: 8px; overflow: hidden; margin: 24px 0; }
        table { width: 100%; border-collapse: collapse; }
        th { background: #fcfcfd; border-bottom: 1px solid var(--border); padding: 12px 16px; text-align: left; font-size: 11px; font-weight: 700; color: var(--text-sec); text-transform: uppercase; }
        td { padding: 14px 16px; border-bottom: 1px solid #f9fafb; font-size: 14px; color: var(--text-sec); }
        .st-pos { color: var(--clr-green); font-weight: 600; }
        .st-neg { color: var(--clr-red); font-weight: 600; }

        .code-wrap { background: #0f172a; border-radius: 8px; margin: 24px 0; overflow: hidden; border: 1px solid #1e293b; color: #cbd5e1; font-family: monospace; font-size: 13px; }
        .ln { width: 40px; padding: 16px 0; text-align: center; color: #475569; background: #020617; border-right: 1px solid #1e293b; }
        .src { padding: 16px 20px; white-space: pre; }
        
        .footer { text-align: center; margin-top: 80px; font-size: 12px; color: var(--text-muted); }
    </style>
</head>
<body>
    <div class="container">
        <header class="report-header">
            <div>
                <h4 style="margin:0; font-size:11px; color:var(--text-muted); text-transform:uppercase;">Analyzed Resource</h4>
                <p style="margin:4px 0 0; font-weight:600; font-size:16px;">$analyzedFile</p>
                <p style="margin:8px 0 0; font-size:12px; color:var(--text-muted);">$time</p>
            </div>
            <div style="text-align:right;">
                <div class="score-num">$score</div>
                <div class="score-status $verdictClass">$statusText</div>
            </div>
        </header>
        <main>$body</main>
        <footer class="footer">OpenCode PR Review &bull; Master Auditor &bull; 🔐 Private Logic</footer>
    </div>
</body>
</html>
        """.trimIndent()
    }

    private fun reconstructBrokenTables(md: String): String {
        val lines = md.lines()
        val builder = StringBuilder()
        var inTable = false
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                if (!inTable) inTable = true
                builder.append(trimmed).append("\n")
            } else {
                if (inTable && trimmed.isBlank()) {
                } else {
                    if (inTable) { builder.append("\n"); inTable = false }
                    builder.append(line).append("\n")
                }
            }
        }
        return builder.toString()
    }

    private fun formatCodeWithLineNumbers(code: String, lang: String): String {
        val lines = code.lines()
        val numStr = lines.indices.joinToString("\n") { (it + 1).toString() }
        return """
            <div class='code-wrap'>
                <div style='background:#1e293b; padding:6px 16px; font-size:10px; font-weight:700; color:#94a3b8;'>$lang SOURCE ANALYSIS</div>
                <table style='width:100%'><tr>
                    <td class='ln'>$numStr</td>
                    <td class='src'>$code</td>
                </tr></table>
            </div>
        """.trimIndent()
    }
}
