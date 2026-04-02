package com.opencode.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcsUtil.VcsUtil

/**
 * Utility to extract Git diffs from the workspace.
 * Interacts with IntelliJ ProjectLevelVcsManager to find changes.
 */
object GitDiffUtil {
    fun getUnstagedDiff(project: Project): String {
        val changeListManager = ChangeListManager.getInstance(project)
        val changes = changeListManager.allChanges
        
        val diffBuilder = StringBuilder()
        for (change in changes) {
            val before = change.beforeRevision?.content ?: ""
            val after = change.afterRevision?.content ?: ""
            // Simple visual diff building for the reviewer
            diffBuilder.append("File: ${change.afterRevision?.file?.name ?: "unknown"}\n")
            diffBuilder.append(after) // In a real app we'd use a diff-format tool
            diffBuilder.append("\n\n")
        }
        return diffBuilder.toString()
    }
}
