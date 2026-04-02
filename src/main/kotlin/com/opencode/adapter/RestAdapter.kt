package com.opencode.adapter

import com.opencode.settings.AppSettingsState
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.nio.charset.StandardCharsets

/**
 * Communicates with local AI models (via Ollama or similar REST APIs).
 * Now supports dynamic weighting and audit thresholds.
 */
class RestAdapter(private val settings: AppSettingsState) : OpenCodeAdapter {

    override suspend fun review(content: String, context: String): Flow<String> = flow {
        val client = HttpClient(CIO)
        val url = "${settings.restBaseUrl}/api/chat"
        
        // Dynamic Weighted Context
        val weightingContext = """
            [DYNAMIC AUDIT METRICS]
            - Code Quality: max ${settings.weightCodeQuality} points
            - Best Practices: max ${settings.weightBestPractices} points
            - Performance: max ${settings.weightPerformance} points
            - Readability: max ${settings.weightReadability} points
            - Security: max ${settings.weightSecurity} points
            - PASSING THRESHOLD: ${settings.passingThreshold}
            
            (Reject if total score < ${settings.passingThreshold})
        """.trimIndent()

        val fullSystemMessage = "${settings.systemPrompt}\n\n$weightingContext"

        val payload = """
        {
            "model": "${settings.modelName}",
            "messages": [
                { "role": "system", "content": ${escapeJson(fullSystemMessage)} },
                { "role": "user", "content": ${escapeJson("SOURCE: $context\n\nCODE:\n$content")} }
            ],
            "stream": true
        }
        """.trimIndent()

        client.preparePost(url) {
            setBody(payload)
            header("Content-Type", "application/json")
        }.execute { response ->
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (line.startsWith("{")) {
                    try {
                        val contentRegex = "\"content\":\"(.*?)\"".toRegex()
                        val match = contentRegex.find(line)
                        if (match != null) {
                            val chunk = unescapeUnicode(match.groupValues[1])
                                .replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\")
                                .replace("\\t", "\t")
                            emit(chunk)
                        }
                    } catch (e: Exception) {
                        emit("\n[Parser Error: ${e.message}]\n")
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun unescapeUnicode(input: String): String {
        val regex = "\\\\u([0-9a-fA-F]{4})".toRegex()
        return regex.replace(input) { match ->
            match.groupValues[1].toInt(16).toChar().toString()
        }
    }

    override suspend fun ping(): Boolean {
        return try {
            val client = HttpClient(CIO)
            val response = client.get("${settings.restBaseUrl}/api/tags")
            response.status.value == 200
        } catch (e: Exception) {
            false
        }
    }

    private fun escapeJson(input: String): String {
        val escaped = input.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }
}
