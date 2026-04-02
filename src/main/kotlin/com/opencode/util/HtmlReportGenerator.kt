package com.opencode.util

import com.intellij.openapi.project.Project
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Enterprise-Grade Audit Dashboard Engine.
 * Implements a SaaS-inspired Design System with high-fidelity typography and surgical alignment.
 */
object HtmlReportGenerator {

    fun generate(project: Project, markdownContent: String, fileName: String): File? {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val displayTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm"))
        val reportFile = File(project.basePath, "OpenCode_Elite_Audit_$timestamp.html")
        
        val scoreRegex = "\\[SCORE:\\s?(\\d+)/100\\]".toRegex()
        val score = scoreRegex.find(markdownContent)?.groupValues?.get(1)?.toIntOrNull() ?: 0

        val critical = "Critical".toRegex(RegexOption.IGNORE_CASE).findAll(markdownContent).count()
        val major = "Major".toRegex(RegexOption.IGNORE_CASE).findAll(markdownContent).count()
        val totalFindings = critical + major + "Minor".toRegex(RegexOption.IGNORE_CASE).findAll(markdownContent).count()

        val htmlContent = buildHtml(markdownContent, fileName, displayTime, critical, major, totalFindings, score)
        reportFile.writeText(htmlContent)
        
        return reportFile
    }

    private fun buildHtml(md: String, analyzedFile: String, time: String, critical: Int, major: Int, total: Int, score: Int): String {
        // Clinical Markdown Pre-processing
        val processedMd = reconstructBrokenTables(md)
        
        var body = processedMd
            .replace("# ", "<h1 class='audit-h1'>")
            .replace("## ", "<h2 class='audit-h2'>")
            .replace("### ", "<h3 class='audit-h3'>")
            .replace("\n", "<br>")
            .replace("**", "<strong>", false)
            .replace("---", "<hr class='audit-hr'>")

        // 1. Surgical Table Reconstruction (Enterprise Style)
        val finalTableRegex = "(\\|[\\s\\S]+?\\|)".toRegex()
        body = finalTableRegex.replace(body) { match ->
            val rows = match.groupValues[1].split("<br>").map { it.trim() }.filter { it.count { c -> c == '|' } >= 2 }
            if (rows.isEmpty()) return@replace match.groupValues[1]
            
            val headLine = rows[0]
            val rest = rows.drop(1).filter { !it.contains("---") && !it.contains(":---") }
            
            val headCells = headLine.split("|").filter { it.isNotBlank() }.map { it.trim() }
            val bodyRows = rest.map { it.split("|").filter { it.isNotBlank() }.map { it.trim() } }
            
            val headHtml = headCells.joinToString("") { "<th>$it</th>" }
            val rowsHtml = bodyRows.joinToString("") { row ->
                "<tr>" + row.joinToString("") { "<td>$it</td>" } + "</tr>"
            }
            "<div class='table-wrapper'><table><thead><tr>$headHtml</tr></thead><tbody>$rowsHtml</tbody></table></div>"
        }

        // 2. Finding Card Injections
        body = body.replace("<h3 class='audit-h3'>🐛 Critical Issues [Must Fix]</h3>", "<div class='finding-card critical'><h3 class='card-title'>Critical System Blockers</h3>")
                   .replace("<h3 class='audit-h3'>⚠️ Major Issues [Should Fix]</h3>", "<div class='finding-card major'><h3 class='card-title'>Architectural Degradations</h3>")
                   .replace("<h3 class='audit-h3'>💡 Minor Issues [Style]</h3>", "<div class='finding-card minor'><h3 class='card-title'>Design & Style Optimizations</h3>")
                   .replace("<h3 class='audit-h3'>✨ Good Practices</h3>", "<div class='finding-card good'><h3 class='card-title'>Commendable Patterns</h3>")
                   .replace("<h3 class='audit-h3'>GROWTH GUIDANCE</h3>", "<div class='finding-card mentor'><h3 class='card-title'>Technical Mentorship & Growth</h3>")

        // Close cards (simplified logic for markdown flow)
        body = body.replace("<hr class='audit-hr'>", "</div><hr class='audit-hr'>")

        // 3. Code Block Highlighting
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
    <title>OpenCode Elite Audit Dashboard</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg: #f9fafb; --white: #ffffff;
            --text-main: #111827; --text-sec: #4b5563; --text-muted: #9ca3af;
            --border: #e5e7eb; --accent: #3b82f6;
            --clr-red: #ef4444; --clr-orange: #f59e0b; --clr-green: #10b981; --clr-blue: #3b82f6;
        }
        * { box-sizing: border-box; }
        body { font-family: 'Inter', system-ui, -apple-system, sans-serif; background: var(--bg); color: var(--text-main); margin: 0; padding: 60px 20px; line-height: 1.5; -webkit-font-smoothing: antialiased; }
        
        .container { max-width: 900px; margin: 0 auto; }
        
        /* Header Certificate */
        .report-header { background: var(--white); border: 1px solid var(--border); border-radius: 12px; padding: 40px; margin-bottom: 40px; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
        .meta-group h4 { margin: 0 0 4px 0; font-size: 11px; text-transform: uppercase; color: var(--text-muted); letter-spacing: 0.05em; }
        .meta-group p { margin: 0; font-weight: 600; font-size: 15px; color: var(--text-sec); }
        
        .score-display { text-align: right; }
        .score-num { font-size: 48px; font-weight: 800; line-height: 1; margin-bottom: 2px; }
        .score-status { font-size: 12px; font-weight: 700; border-radius: 4px; padding: 2px 8px; display: inline-block; }
        .st-approved { color: var(--clr-green); background: #ecfdf5; }
        .st-warning { color: var(--clr-orange); background: #fffbeb; }
        .st-rejected { color: var(--clr-red); background: #fef2f2; }

        /* Typography */
        .audit-h1 { font-size: 28px; font-weight: 800; margin: 0 0 24px 0; border-bottom: 1px solid var(--border); padding-bottom: 16px; }
        .audit-h2 { font-size: 20px; font-weight: 700; margin: 40px 0 16px 0; display: flex; align-items: center; }
        .audit-hr { border: 0; height: 1px; background: var(--border); margin: 48px 0; }
        
        /* Metrics Grid */
        .metrics-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin-bottom: 40px; }
        .metric-unit { background: var(--white); border: 1px solid var(--border); border-radius: 8px; padding: 20px; }
        .metric-unit .val { display: block; font-size: 24px; font-weight: 700; margin-bottom: 4px; }
        .metric-unit .lab { font-size: 12px; color: var(--text-sec); font-weight: 500; }

        /* Finding Cards */
        .finding-card { background: var(--white); border: 1px solid var(--border); border-left: 5px solid; border-radius: 8px; padding: 24px; margin-bottom: 20px; }
        .card-title { margin: 0 0 16px 0; font-size: 14px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.02em; }
        .critical { border-left-color: var(--clr-red); }
        .major { border-left-color: var(--clr-orange); }
        .minor { border-left-color: var(--clr-blue); }
        .good { border-left-color: var(--clr-green); }
        .mentor { border-left-color: #6366f1; background: #f5f3ff; }

        /* Tables */
        .table-wrapper { background: var(--white); border: 1px solid var(--border); border-radius: 8px; overflow: hidden; margin: 24px 0; }
        table { width: 100%; border-collapse: collapse; }
        th { background: #fcfcfd; border-bottom: 1px solid var(--border); padding: 12px 16px; text-align: left; font-size: 11px; font-weight: 700; color: var(--text-sec); text-transform: uppercase; }
        td { padding: 14px 16px; border-bottom: 1px solid #f9fafb; font-size: 14px; }
        tr:last-child td { border-bottom: 0; }

        /* Code Block */
        .code-wrap { background: #111827; border-radius: 8px; margin: 20px 0; overflow: hidden; color: #e5e7eb; font-family: 'JetBrains Mono', monospace; font-size: 13px; border: 1px solid #374151; }
        .code-title { background: #1f2937; padding: 8px 16px; font-size: 10px; font-weight: 700; color: var(--text-muted); border-bottom: 1px solid #374151; }
        .ln { width: 40px; padding: 12px 0; text-align: center; color: #4b5563; background: #0b0f1a; border-right: 1px solid #374151; user-select: none; }
        .src { padding: 12px 20px; white-space: pre; }

        strong { color: var(--text-main); font-weight: 600; }
        .footer { text-align: center; margin-top: 80px; font-size: 12px; color: var(--text-muted); font-weight: 500; }
    </style>
</head>
<body>
    <div class="container">
        <header class="report-header">
            <div class="header-info">
                <div class="meta-group">
                    <h4>Analyzed Resource</h4>
                    <p>$analyzedFile</p>
                </div>
                <div class="meta-group" style="margin-top: 20px;">
                    <h4>Audit Timestamp</h4>
                    <p>$time</p>
                </div>
            </div>
            <div class="score-display">
                <div class="score-num">$score</div>
                <div class="score-status $verdictClass">$statusText</div>
            </div>
        </header>

        <section class="metrics-grid">
            <div class="metric-unit">
                <span class="val" style="color: var(--clr-red);">$critical</span>
                <span class="lab">System Blockers</span>
            </div>
            <div class="metric-unit">
                <span class="val" style="color: var(--clr-orange);">$major</span>
                <span class="lab">Major Findings</span>
            </div>
            <div class="metric-unit">
                <span class="val" style="color: var(--clr-green);">${(score * 0.9 + 5).toInt()}%</span>
                <span class="lab">Audit Integrity</span>
            </div>
        </section>

        <main class="audit-body">
            $body
        </main>

        <footer class="footer">
            OpenCode PR Review &bull; Enterprise Auditor v2.4 &bull; Local Private Logic 🔐
        </footer>
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
            if (line.trim().startsWith("|")) {
                if (!inTable) inTable = true
                builder.append(line.trim()).append("\n")
            } else if (line.isBlank() && inTable) {
                // Keep streaming table rows
            } else {
                if (inTable) { builder.append("\n"); inTable = false }
                builder.append(line).append("\n")
            }
        }
        return builder.toString()
    }

    private fun formatCodeWithLineNumbers(code: String, lang: String): String {
        val lines = code.lines()
        val numStr = lines.indices.joinToString("\n") { (it + 1).toString() }
        return """
            <div class='code-wrap'>
                <div class='code-title'>$lang SOURCE ANALYSIS</div>
                <table style='width:100%'><tr>
                    <td class='ln'>$numStr</td>
                    <td class='src'>$code</td>
                </tr></table>
            </div>
        """.trimIndent()
    }
}
