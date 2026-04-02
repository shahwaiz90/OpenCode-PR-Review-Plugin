# 🛡️ OpenCode PR Review - Enterprise AI Auditor

**OpenCode PR Review** is a high-performance Android Studio plugin that transforms your IDE into a **Rigorous Technical Lead**. It uses local AI models to perform deep architectural, performance, and security audits—complete with **Dynamic Weighting**, **Mentorship Guidance**, and **Professional PDF-Ready Reports**.

---

## 🚀 Key Features
- **🕵️ Expert Software Engineering Auditor**: A specialized AI persona that hunts for Main-thread blocking, string-concatenation leaks, and architectural violations.
- **⚖️ Dynamic Audit Dashboard**: Manually adjust weights for **Code Quality**, **Performance**, **Security**, and **Best Practices** in your settings.
- **🏁 Custom Passing Thresholds**: Set your own quality bar (e.g., Reject any PR with a score < 80).
- **👨‍🏫 Growth Mentorship**: Every finding includes "Growth Guidance" with links to official **Kotlin** and **Android** documentation.
- **📊 Enterprise Audit Dashboard**: Export beautiful, print-ready HTML reports with executive summaries and professional line-numbered code cards.
- **🧪 AI Playground**: Test your prompt scripts and weighting logic in a real-time sandbox before deploying them to your team.

---

## 🏗️ Getting Started

### 1. Setup Local AI (Ollama)
OpenCode PR Review works best with a local, private LLM for absolute code security.
- **Download Ollama**: [ollama.com](https://ollama.com/)
- **Pull a Coding Model**: Open your terminal and run:
  ```bash
  ollama pull qwen2.5-coder:latest
  ```
- **Verify**: Ensure the server is running at `http://localhost:11434`.

### 2. Configure the Plugin
1. Open **Android Studio** → **Settings / Preferences**.
2. Navigate to **Tools** → **OpenCode PR Review**.
3. Set your **Ollama Server URL**: `http://localhost:11434`.
4. Choose your **Active AI Model**: `qwen2.5-coder:latest`.
5. Dial in your **Audit Weighting & Thresholds** to match your team’s standards.
6. Click **Apply**.

---

## 🤝 Contribution Guide (What Files Do What)
OpenCode is built for open-source growth! Here is where the magic happens:

- **`src/main/kotlin/com/opencode/settings/AppSettingsState.kt`**: The "Source of Truth." This file contains the **Expert Auditor System Prompt** and the persistence logic for your weights and thresholds.
- **`src/main/kotlin/com/opencode/adapter/RestAdapter.kt`**: The communication engine. This file bridges the plugin to Ollama, handles streaming, and injects your dynamic weighting context into every request.
- **`src/main/kotlin/com/opencode/util/HtmlReportGenerator.kt`**: The report architect. This transforms AI Markdown into the **Professional Audit Dashboard** (handling tables, badges, and line numbers).
- **`src/main/kotlin/com/opencode/ui/ReviewPanel.kt`**: The visual heart. It handles real-time streaming, the "Apply Fix" logic, and the "Expert Mentor" UI sections.
- **`src/main/kotlin/com/opencode/actions/`**: Action triggers for right-click context menus, gutter icons, and tool-window actions.

---

## 🛠️ Build & Run
To compile the plugin from source:
```bash
./gradlew buildPlugin
```
To launch a sandbox IDE with the latest changes:
```bash
./gradlew runIde
```

---

## 🎯 Our Mission
Our goal is to help developers grow while ensuring enterprise-level code quality. We believe in strict enforcement paired with compassionate mentorship.

### You feel me? 🛡️✨🏢
---
*OpenCode PR Review is an open-source project by Ahmad Shahwaiz and the community.*
