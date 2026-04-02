# 🚀 OpenCode PR Review Plugin for Android Studio

Welcome to **OpenCode PR Review**! This is a professional-grade, AI-powered code review tool designed to sit directly inside your Android Studio or IntelliJ IDEA. It catch bugs, identifies performance bottlenecks, and enforces clean architecture standards before you ever submit a Pull Request.

---

## ✨ Key Features

- **🛡️ Elite AI PR Reviewer**: Uses an expert Senior Android Developer persona to scan for memory leaks, logic bugs, and clean architecture violations.
- **⚡ Surgical Fixes**: Automatically suggests and applies full code replacements based on the exact function or selection you're reviewing.
- **📑 HTML Dashboard**: Generate a premium, high-fidelity HTML report of your overall review with one click.
- **🎯 Contextual Scopes**: Intelligent enough to know if you're reviewing a single Function, a Selection, the Whole File, or a Git Diff.
- **📝 Automatic KDocs**: Enforces and generates high-quality KDoc documentation (`/** ... */`) for every fix.
- **🎨 Premium UI**: A modern, high-readability tool window with soft wraps and syntax highlighting.

---

## 🛠 Prerequisites

Ensure you have:
*   **Android Studio** or **IntelliJ IDEA** (2023.2+) installed.
*   **Ollama** running locally (default: `http://localhost:11434`).

---

## 🚀 Getting Started

### 1. Launching the Plugin
To run the plugin in a sandbox IDE for testing:
```bash
./gradlew runIde
```

### 2. Configuration
Go to **Settings → OpenCode PR Review** to:
- Set your **Ollama Base URL** and **Active Model**.
- Test your connection with one click.
- Customize the **Elite PR Review Rules** to match your team's standards.
- Use the **AI Playground** to test your prompts against real code snippets.

---

## 📖 How to Use

### 1. Reviewing Code
-   **Gutter Icon**: Click the Magnifying Glass 🔍 next to any function to start a surgical review.
-   **Context Menu**: Right-click any selection or file -> **OpenCode PR Review**.
-   **Git Diff**: Right-click -> **Review Git Diff** to analyze your unstaged changes.

### 2. Applying Fixes
-   **Apply Fix**: Replaces the original code with the AI's suggested professional version.
-   **Just KDocs**: Only inserts the documentation above your function without changing logic.
-   **Export HTML**: Generates a beautiful HTML report and opens it in your browser.

---

## 🏗 Project Architecture

- **`com.opencode.adapter`**: Logic for streaming responses from local LLMs.
- **`com.opencode.ui`**: Premium UI components and the HTML Dashboard generator.
- **`com.opencode.actions`**: Context-aware review actions for IDE integration.
- **`com.opencode.settings`**: Persistent state management for your custom rules.

---

## 🛫 Publishing
To build the distribution ZIP:
```bash
./gradlew buildPlugin
```

Happy code reviewing! 🛡️🚀
