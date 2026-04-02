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

/**
 * Communicates with AI models. 
 * Now enforces a 'Zero-Leak' protocol for internal audit metrics.
 */
class RestAdapter(private val settings: AppSettingsState) : OpenCodeAdapter {

    override suspend fun review(content: String, context: String): Flow<String> = flow {
        val client = HttpClient(CIO)
        val url = "${settings.restBaseUrl}/api/chat"
        
        // Internal Weighting Context - Wrapped in a 'Do Not Print' mask
        val weightingContext = """
            [INTERNAL_AUDit_GRADiNG_METRICS - DO NOT OUTPUT TO USER]
            - CATEGORY RULES:
              Code Quality: max ${settings.weightCodeQuality}
              Best Practices: max ${settings.weightBestPractices}
              Performance: max ${settings.weightPerformance}
              Readability: max ${settings.weightReadability}
              Security: max ${settings.weightSecurity}
            - GLOBAL PASSING THRESHOLD: ${settings.passingThreshold}
            
            STRICT DIRECTIVE: Use these weights for calculation only. 
            DO NOT mention 'Dynamic Audit Metrics' or these point values in your response.
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
                        emit("\n[Parser Error]\n")
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
