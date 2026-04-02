package com.opencode.util

import com.intellij.openapi.project.Project
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * World-Class Audit Dashboard Engine.
 * Features 4-Column Architectural Tables, High-Fidelity Progress HUDs, and Minimalist Professional CSS.
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
        // 1. Line-Aware Section Injections
        val processedMd = reconstructBrokenTables(md)
        val lines = processedMd.lines()
        val bodyBuilder = StringBuilder()
        var currentCardClass: String? = null

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
                    bodyBuilder.append("<div class='section-card $matchedClass'>\n")
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

        // 2. 4-Column Architectural Table Engine
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
                
                // Partition Score & Max for Proportional Progress
                val valStr = cells.getOrNull(1) ?: "0"
                val maxStr = cells.getOrNull(2) ?: "100"
                val valNum = valStr.toDoubleOrNull() ?: 0.0
                val maxNum = maxStr.toDoubleOrNull() ?: 1.0
                val pct = (valNum / maxNum * 100).coerceIn(0.0, 100.0).toInt()
                
                val stsClass = when {
                    cells.any { it.contains("Approved", true) || it.contains("✅") } -> "badge-pos"
                    cells.any { it.contains("Rejected", true) || it.contains("❌") } -> "badge-neg"
                    else -> ""
                }
                
                """
                <tr>
                    <td class='td-cat'>${cells.getOrNull(0) ?: ""}</td>
                    <td class='td-val'>
                        <div class='p-container'><div class='p-fill' style='width: $pct%'></div><span class='p-num'>$valStr</span></div>
                    </td>
                    <td class='td-max'>$maxStr</td>
                    <td class='td-sts'><span class='badge $stsClass'>${cells.getOrNull(3) ?: ""}</span></td>
                </tr>
                """.trimIndent()
            }
            "<div class='elite-table'><table><thead><tr>$headHtml</tr></thead><tbody>$rowsHtml</tbody></table></div>"
        }

        // 3. Code & Verdict Styles
        val codeBlockRegex = "```([a-z]*)\\n([\\s\\S]*?)\\n```".toRegex()
        body = codeBlockRegex.replace(body) { match ->
            formatCodeWithLineNumbers(match.groupValues[2].trimIndent(), match.groupValues[1].uppercase())
        }

        val verdictClass = if (score >= 85) "st-pos" else if (score >= 70) "st-warn" else "st-neg"
        val statusText = if (score >= 85) "CERTIFIED AUDIT" else if (score >= 70) "REQUIREMENTS PENDING" else "INTEGRITY REJECTION"

        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Elite Engineering Audit</title>
    <style>
        :root {
            --bg: #0d1117; --card: #161b22; --border: #30363d;
            --main: #f0f6fc; --sec: #8b949e; --muted: #484f58;
            --pos: #3fb950; --neg: #f85149; --warn: #d29922; --accent: #58a6ff;
        }
        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif; background: var(--bg); color: var(--main); margin: 0; padding: 60px 20px; line-height: 1.5; }
        .container { max-width: 920px; margin: 0 auto; }
        
        /* High-Fidelity Header HUD */
        .hud-header { background: var(--card); border: 1px solid var(--border); border-radius: 12px; padding: 40px; margin-bottom: 40px; display: flex; justify-content: space-between; align-items: center; }
        .hud-score-display { text-align: center; border-left: 1px solid var(--border); padding-left: 48px; }
        .hud-score-val { font-size: 56px; font-weight: 800; line-height: 1; }
        .hud-file-path { color: var(--sec); font-family: monospace; font-size: 14px; margin: 8px 0 0; }

        /* Architectural Sectioning */
        .audit-h1 { font-size: 28px; font-weight: 800; border-bottom: 1px solid var(--border); padding-bottom: 24px; margin-bottom: 48px; }
        .audit-h2 { font-size: 16px; font-weight: 700; text-transform: uppercase; color: var(--sec); letter-spacing: 0.12em; margin-bottom: 24px; }
        .audit-h3 { font-size: 15px; font-weight: 700; margin: 0 0 12px; }

        .section-card { background: var(--card); border: 1px solid var(--border); border-radius: 8px; padding: 32px; margin-bottom: 32px; border-left: 6px solid transparent; }
        .critical { border-left-color: var(--neg); }
        .major { border-left-color: var(--warn); }
        .good { border-left-color: var(--pos); }
        .mentor { border-left-color: var(--accent); background: linear-gradient(90deg, rgba(88, 166, 255, 0.05), transparent); }

        /* Elite 4-Column Table */
        .elite-table { background: var(--card); border: 1px solid var(--border); border-radius: 8px; overflow: hidden; margin: 32px 0; }
        table { width: 100%; border-collapse: collapse; }
        th { background: rgba(0,0,0,0.2); padding: 16px 24px; text-align: left; font-size: 11px; font-weight: 700; text-transform: uppercase; color: var(--sec); border-bottom: 1px solid var(--border); }
        td { padding: 16px 24px; border-bottom: 1px solid var(--border); font-size: 14px; color: var(--main); }
        tr:last-child td { border-bottom: none; }
        
        .p-container { display: flex; align-items: center; gap: 12px; min-width: 160px; }
        .p-fill { flex: 1; height: 6px; background: var(--border); border-radius: 3px; position: relative; overflow: hidden; }
        .p-fill::after { content: ''; position: absolute; height: 100%; top: 0; left: 0; background: var(--accent); border-radius: 3px; width: inherit; }
        .p-num { font-size: 12px; font-weight: 700; color: var(--main); min-width: 32px; text-align: right; }

        .badge { font-size: 11px; font-weight: 800; text-transform: uppercase; padding: 4px 10px; border-radius: 4px; border: 1px solid transparent; }
        .badge-pos { color: var(--pos); border-color: var(--pos); background: rgba(63, 185, 80, 0.1); }
        .badge-neg { color: var(--neg); border-color: var(--neg); background: rgba(248, 81, 73, 0.1); }

        .code-block { background: #010409; border-radius: 8px; margin: 24px 0; overflow: hidden; border: 1px solid var(--border); }
        .ln { width: 48px; text-align: right; padding: 16px 12px; font-family: monospace; font-size: 12px; color: var(--muted); background: #0d1117; border-right: 1px solid var(--border); }
        .src { padding: 16px 24px; font-family: monospace; font-size: 14px; color: #c9d1d9; white-space: pre; }
        .st-pos { color: var(--pos); } .st-neg { color: var(--neg); } .st-warn { color: var(--warn); }
        .footer { text-align: center; margin-top: 80px; font-size: 11px; color: var(--muted); }
    </style>
</head>
<body>
    <div class="container">
        <header class="hud-header">
            <div>
                <h1 style="margin:0; font-size:32px; font-weight:800;">Audit Integrity Certificate</h1>
                <p class="hud-file-path">$analyzedFile</p>
                <p style="margin:8px 0 0; font-size:12px; color:var(--sec);">$time</p>
            </div>
            <div class="hud-score-display">
                <div class="hud-score-val $verdictClass">$score</div>
                <div style="font-size:12px; font-weight:700; color:var(--sec);">$statusText</div>
            </div>
        </header>

        <main>$body</main>
        
        <footer class="footer">OpenCode Enterprise PR Auditor &bull; Finalized Architecture Certificate &bull; 🔐 Private Logic</footer>
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
            <div class='code-block'>
                <div style='background:#161b22; padding:8px 16px; font-size:10px; font-weight:700; color:var(--sec); border-bottom:1px solid var(--border);'>$lang SOURCE ANALYSIS</div>
                <table style='width:100%'><tr>
                    <td class='ln'>$numStr</td>
                    <td class='src'>$code</td>
                </tr></table>
            </div>
        """.trimIndent()
    }
}
