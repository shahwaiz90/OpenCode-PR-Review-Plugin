package com.opencode.util

import com.intellij.openapi.project.Project
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Generates a premium, dashboard-style HTML report for AI Reviews.
 */
object HtmlReportGenerator {

    fun generate(project: Project, markdownContent: String, fileName: String): File? {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val displayTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm"))
        
        val reportFile = File(project.basePath, "OpenCode_PR_Report_$timestamp.html")
        val htmlContent = buildHtml(markdownContent, fileName, displayTime)
        reportFile.writeText(htmlContent)
        
        return reportFile
    }

    private fun buildHtml(md: String, analyzedFile: String, time: String): String {
        // Convert Markdown to structured HTML with pro-level styling
        var body = md
            .replace("# ", "<h1>")
            .replace("\n", "<br>")
            .replace("**", "<strong>")
            .replace("`", "<code>")
            .replace("---", "<hr>")
            
        // Code Block Rendering with Pro Theme
        body = body.replace("```kotlin", "<div class='code-card'><div class='code-header'>Kotlin Fix</div><pre><code>")
                   .replace("```bash", "<div class='code-card'><div class='code-header'>Debug CLI</div><pre><code>")
                   .replace("```", "</code></pre></div>")

        // Status/Severity Badges
        body = body.replace("🔴 CRITICAL", "<span class='badge critical'>🔴 CRITICAL</span>")
                   .replace("🟠 HIGH", "<span class='badge high'>🟠 HIGH</span>")
                   .replace("🟡 MEDIUM", "<span class='badge medium'>🟡 MEDIUM</span>")
                   .replace("🔵 LOW", "<span class='badge low'>🔵 LOW</span>")

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>OpenCode PR Report - $analyzedFile</title>
                <link href="https://fonts.googleapis.com/css2?family=Inter:ital,wght@0,300;0,400;0,600;0,700;1,400&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
                <style>
                    :root {
                        --bg: #0b0f19;
                        --card-bg: rgba(30, 41, 59, 0.7);
                        --text: #e2e8f0;
                        --muted: #94a3b8;
                        --accent: #60a5fa;
                        --border: rgba(51, 65, 85, 0.5);
                        --critical: #ef4444; --high: #f97316; --medium: #eab308; --low: #3b82f6;
                        --card-shadow: 0 10px 30px -10px rgba(0,0,0,0.5);
                    }
                    body {
                        font-family: 'Inter', sans-serif;
                        background: radial-gradient(circle at top right, #1e1b4b, var(--bg));
                        color: var(--text);
                        line-height: 1.7;
                        margin: 0;
                        padding: 60px 20px;
                        -webkit-font-smoothing: antialiased;
                    }
                    .container { max-width: 1000px; margin: 0 auto; }
                    
                    .header {
                        margin-bottom: 50px;
                        text-align: center;
                    }
                    .header h1 {
                        font-size: 3rem;
                        font-weight: 700;
                        background: linear-gradient(to right, #60a5fa, #a855f7);
                        -webkit-background-clip: text;
                        -webkit-text-fill-color: transparent;
                        margin-bottom: 10px;
                        letter-spacing: -0.02em;
                    }
                    .header .badge-main {
                        background: rgba(96, 165, 250, 0.1);
                        color: #60a5fa;
                        padding: 6px 16px;
                        border-radius: 20px;
                        font-size: 0.8rem;
                        font-weight: 600;
                        border: 1px solid rgba(96, 165, 250, 0.3);
                    }
                    
                    .report-card {
                        background: var(--card-bg);
                        backdrop-filter: blur(12px);
                        border: 1px solid var(--border);
                        border-radius: 24px;
                        padding: 40px;
                        box-shadow: var(--card-shadow);
                    }

                    .meta-row {
                        display: flex;
                        justify-content: space-between;
                        padding-bottom: 25px;
                        border-bottom: 1px solid var(--border);
                        margin-bottom: 30px;
                    }
                    .meta-item strong { color: var(--muted); font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.05em; display: block; }
                    .meta-item span { font-weight: 600; font-size: 1.1rem; color: #fff; }

                    .badge {
                        padding: 4px 10px;
                        border-radius: 8px;
                        font-weight: 600;
                        font-size: 0.75rem;
                        display: inline-block;
                        margin: 4px 0;
                        text-shadow: 0 0 10px rgba(0,0,0,0.5);
                    }
                    .critical { background: rgba(239, 68, 68, 0.2); color: #fca5a5; border: 1px solid var(--critical); }
                    .high { background: rgba(249, 115, 22, 0.2); color: #ffedd5; border: 1px solid var(--high); }
                    .medium { background: rgba(234, 179, 8, 0.2); color: #fef9c3; border: 1px solid var(--medium); }
                    .low { background: rgba(59, 130, 246, 0.2); color: #dbeafe; border: 1px solid var(--low); }

                    .code-card {
                        background: #020617;
                        border-radius: 16px;
                        margin: 25px 0;
                        border: 1px solid var(--border);
                        overflow: hidden;
                        box-shadow: 0 4px 20px rgba(0,0,0,0.3);
                    }
                    .code-header {
                        background: #1e293b;
                        padding: 8px 16px;
                        color: var(--muted);
                        font-size: 0.75rem;
                        font-weight: 600;
                        text-transform: uppercase;
                        letter-spacing: 0.1em;
                        border-bottom: 1px solid var(--border);
                    }
                    pre {
                        margin: 0;
                        padding: 20px;
                        font-family: 'JetBrains Mono', monospace;
                        font-size: 0.95rem;
                        overflow-x: auto;
                        color: #94a3b8;
                    }
                    code { color: #f1f5f9; }

                    table {
                        width: 100%;
                        border-collapse: separate;
                        border-spacing: 0;
                        margin: 30px 0;
                        border: 1px solid var(--border);
                        border-radius: 12px;
                        overflow: hidden;
                        background: rgba(30, 41, 59, 0.3);
                    }
                    th, td { padding: 16px; text-align: left; border-bottom: 1px solid var(--border); }
                    th { background: rgba(94, 163, 250, 0.05); color: var(--muted); font-size: 0.75rem; text-transform: uppercase; font-weight: 600; }
                    tr:last-child td { border-bottom: none; }
                    tr:hover td { background: rgba(255,255,255,0.02); }

                    hr { border: 0; border-top: 1px solid var(--border); margin: 40px 0; }
                    h1, h2, h3 { color: #fff; margin-top: 30px; letter-spacing: -0.01em; }
                    strong { color: #fff; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <span class="badge-main">🛡️ ELITE PR REVIEW</span>
                        <h1>OpenCode Hub</h1>
                    </div>
                    
                    <div class="report-card">
                        <div class="meta-row">
                            <div class="meta-item">
                                <strong>File Analysed</strong>
                                <span>$analyzedFile</span>
                            </div>
                            <div class="meta-item" style="text-align: right;">
                                <strong>Generated At</strong>
                                <span>$time</span>
                            </div>
                        </div>
                        
                        <div class="content">
                            $body
                        </div>
                    </div>

                    <div style="text-align: center; margin-top: 50px; color: var(--muted); font-size: 0.8rem;">
                        Generated securely by OpenCode PR Review Plugin for Android Studio
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
