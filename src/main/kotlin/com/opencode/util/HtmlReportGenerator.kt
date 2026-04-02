package com.opencode.util

import com.intellij.openapi.project.Project
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Robust World-Class Audit Dashboard Engine.
 * Optimized for Aligned Tables, Point Masking, and Zero-Bold Leakage.
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
        // High-Fidelity Table Reconstruction Engine
        val processedMd = reconstructBrokenTables(md)
        
        var body = processedMd
            .replace("# ", "<h2 class='audit-title'>")
            .replace("## ", "<h3 class='audit-subtitle'>")
            .replace("\n", "<br>")
            .replace("**", "<strong>", false)
            .replace("---", "<hr class='audit-divider'>")

        // 1. Surgical Table Refinement (Ensures correct semantic rendering)
        val finalTableRegex = "(\\|[\\s\\S]+?\\|)".toRegex()
        body = finalTableRegex.replace(body) { match ->
            val rows = match.groupValues[1].split("<br>").map { it.trim() }.filter { it.count { c -> c == '|' } >= 2 }
            if (rows.isEmpty()) return@replace match.groupValues[1]
            
            val headLine = rows[0]
            val rest = rows.drop(1).filter { !it.contains("---") && !it.contains(":---") } // Strip alignment artifacts
            
            val headCells = headLine.split("|").filter { it.isNotBlank() }.map { it.trim() }
            val bodyRows = rest.map { it.split("|").filter { it.isNotBlank() }.map { it.trim() } }
            
            val headHtml = headCells.joinToString("") { "<th>$it</th>" }
            val rowsHtml = bodyRows.joinToString("") { row ->
                "<tr>" + row.joinToString("") { "<td>$it</td>" } + "</tr>"
            }
            "<div class='table-card'><table><thead><tr>$headHtml</tr></thead><tbody>$rowsHtml</tbody></table></div>"
        }

        // 2. High-Contrast Severity Mapping & Point Extraction
        body = body.replace("### 🐛 Critical Issues [Must Fix]", "<div class='finding-card critical'><h3>🔴 CRITICAL BLOCKED [MUST FIX]</h3>")
                   .replace("### ⚠️ Major Issues [Should Fix]", "<div class='finding-card major'><h3>🟠 MAJOR IMPACT [SHOULD FIX]</h3>")
                   .replace("### 💡 Minor Issues [Style]", "<div class='finding-card minor'><h3>🔵 MINOR IMPROVEMENTS</h3>")
                   .replace("### ✨ Good Practices", "<div class='finding-card good'><h3>✅ COMMENDABLE PRACTICES</h3>")

        // 3. Precision Code Highlighting
        val codeBlockRegex = "```([a-z]*)\\n([\\s\\S]*?)\\n```".toRegex()
        body = codeBlockRegex.replace(body) { match ->
            formatCodeWithLineNumbers(match.groupValues[2].trimIndent(), match.groupValues[1].uppercase())
        }

        val verdictClass = if (score >= 85) "v-approved" else if (score >= 75) "v-warn" else "v-rejected"
        val verdictText = if (score >= 85) "✅ APPROVED" else if (score >= 75) "⚠️ PENDING CHANGES" else "❌ REJECTED"

        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Elite Audit Report</title>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@400;600;700;800&family=Inter:wght@400;500;600&family=JetBrains+Mono&display=swap" rel="stylesheet">
    <style>
        :root { --p-bg: #f8fafc; --p-border: #e2e8f0; --c-critical: #ef4444; --c-major: #f59e0b; --c-minor: #3b82f6; --c-good: #10b981; }
        body { font-family: 'Inter', sans-serif; background: var(--p-bg); color: #1e293b; margin: 0; padding: 40px 20px; line-height: 1.6; }
        .dashboard-container { max-width: 1050px; margin: 0 auto; }

        .verdict-hero { background: #fff; padding: 48px; border-radius: 12px; border: 1px solid var(--p-border); box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); margin-bottom: 32px; display: flex; align-items: center; justify-content: space-between; position: relative; overflow: hidden; }
        .main-verdict { font-family: 'Outfit', sans-serif; font-size: 42px; font-weight: 800; margin: 0; }
        .v-approved { color: var(--c-good); } .v-warn { color: var(--c-major); } .v-rejected { color: var(--c-critical); }
        .score-circle { width: 140px; height: 140px; border-radius: 50%; border: 10px solid #f1f5f9; display: flex; align-items: center; justify-content: center; font-family: 'Outfit', sans-serif; font-size: 42px; font-weight: 800; color: #010409; margin-bottom: 12px; background: #fff; }
        .score-label { font-size: 11px; text-transform: uppercase; letter-spacing: 2px; color: #64748b; font-weight: 700; }
        
        .metric-card { background: #fff; padding: 24px; border-radius: 8px; border: 1px solid var(--p-border); text-align: center; }
        .metrics-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin-bottom: 48px; }
        
        .finding-card { background: #fff; border-radius: 8px; border-left: 8px solid; margin-bottom: 24px; padding: 32px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
        .critical { border-left-color: var(--c-critical); } .major { border-left-color: var(--c-major); } .minor { border-left-color: var(--c-minor); }

        .table-card { background: #fff; border-radius: 8px; border: 1px solid var(--p-border); overflow: hidden; margin: 24px 0; }
        table { width: 100%; border-collapse: collapse; }
        th, td { padding: 16px 24px; text-align: left; border-bottom: 1px solid #f1f5f9; font-size: 14px; }
        th { background: #f8fafc; font-size: 11px; font-weight: 800; color: #64748b; text-transform: uppercase; letter-spacing: 1px; }

        .code-container { background: #0d1117; border-radius: 8px; margin: 24px 0; overflow: hidden; }
        .ln { width: 48px; padding: 16px 12px; text-align: right; color: #484f58; font-family: 'JetBrains Mono', monospace; font-size: 12px; background: #0d1117; border-right: 1px solid #30363d; }
        .src { padding: 16px 24px; color: #c9d1d9; font-family: 'JetBrains Mono', monospace; font-size: 14px; white-space: pre; }
        
        /* Specialized Title Bolding only */
        strong:first-child { color: #010409; font-weight: 800; }
        strong { font-weight: 500; color: #475569; } /* Reduce weight for following bolds if they leak */
    </style>
</head>
<body>
    <div class="dashboard-container">
        <section class="verdict-hero">
            <div class="hero-left">
                <h2 class="main-verdict $verdictClass">$verdictText</h2>
                <div class="audit-file-path">Integrity Audit for: <strong>$analyzedFile</strong></div>
            </div>
            <div class="hero-right">
                <div class="score-circle">$score</div>
                <div class="score-label">Integrity Score</div>
            </div>
        </section>

        <section class="metrics-row">
            <div class="metric-card" style="border-top: 4px solid var(--c-critical);"><span style="font-size: 28px; font-weight: 800; color: var(--c-critical);">$critical</span><br><small>BLOCKERS</small></div>
            <div class="metric-card" style="border-top: 4px solid var(--c-major);"><span style="font-size: 28px; font-weight: 800; color: var(--c-major);">$major</span><br><small>MAJOR ISSUES</small></div>
            <div class="metric-card" style="border-top: 4px solid var(--c-good);"><span style="font-size: 28px; font-weight: 800; color: var(--c-good);">${(score * 0.9).toInt()}%</span><br><small>CONFIDENCE</small></div>
        </section>

        $body

        <footer style="text-align: center; margin-top: 64px; color: #94a3b8; font-size: 11px;">
            Elite Engineering Audit &bull; OpenCode PR Review Engine &bull; $time
        </footer>
    </div>
</body>
</html>
        """.trimIndent()
    }

    /**
     * Finds multi-line pipe streams and collapses them into single blocks for Markdown processing.
     */
    private fun reconstructBrokenTables(md: String): String {
        val lines = md.lines()
        val builder = StringBuilder()
        var inTable = false
        
        lines.forEach { line ->
            if (line.contains("|")) {
                if (!inTable) inTable = true
                builder.append(line.trim()).append("\n")
            } else if (line.isBlank() && inTable) {
                // Keep the stream alive if it's just a blank line
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
            <div class='code-container'>
                <table class='code-table'><tr>
                    <td class='ln'>$numStr</td>
                    <td class='src'>$code</td>
                </tr></table>
            </div>
        """.trimIndent()
    }
}
