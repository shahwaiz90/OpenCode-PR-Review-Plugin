package com.opencode.adapter

import com.opencode.settings.AppSettingsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Duration

/**
 * REST adapter that uses Anthropic-style 'messages' API for code reviews.
 * Supports streaming by reading 'content_block_delta' lines from the response body.
 */
class RestAdapter(private val settings: AppSettingsState, private val customBaseUrl: String? = null) : OpenCodeAdapter {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
    
    private val baseUrl: String
        get() = (customBaseUrl ?: settings.restBaseUrl).removeSuffix("/")

    override fun review(content: String, context: String): Flow<String> = flow {
        val modelName = settings.modelName
        val endpoint = "$baseUrl/api/chat"
        val escapedPrompt = escape(settings.systemPrompt)
        val escapedContent = escape(content)
        
        val payload = """
            {
                "model": "$modelName",
                "messages": [
                    { "role": "system", "content": "$escapedPrompt" },
                    { "role": "user", "content": "Review this Kotlin code:\n\n```kotlin\n$escapedContent\n```" }
                ],
                "stream": true
            }
        """.trimIndent()

        val request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build()

        val response = client.send(request, BodyHandlers.ofInputStream())
        if (response.statusCode() != 200) {
            emit("❌ Server Error: HTTP ${response.statusCode()}")
            return@flow
        }

        val reader = BufferedReader(InputStreamReader(response.body()))
        reader.use {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmedLine = line?.trim() ?: ""
                if (trimmedLine.startsWith("data:")) {
                    // Skip 'data: ' prefix
                }
                
                if (trimmedLine.isNotEmpty()) {
                    try {
                        // Ollama chat API response line format:
                        // {"model":"...","message":{"role":"assistant","content":"..."},"done":false}
                        
                        // Extract content value using simple regex to avoid full JSON parser overhead
                        val contentRegex = "\"content\":\"(.*?)\"".toRegex()
                        val match = contentRegex.find(trimmedLine)
                        if (match != null) {
                            val chunk = unescapeUnicode(match.groupValues[1])
                                .replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\")
                                .replace("\\t", "\t")
                            emit(chunk)
                        }
                    } catch (e: Exception) {
                        emit("\n[Parser Error: ${e.message} on line: $trimmedLine]\n")
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
            val payload = """
                {
                    "model": "${settings.modelName}",
                    "messages": [
                        { "role": "user", "content": "ping" }
                    ],
                    "stream": false
                }
            """.trimIndent()
            
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/api/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build()
            client.send(request, BodyHandlers.discarding()).statusCode() == 200
        } catch (e: Exception) {
            System.err.println("OPENCODE DEBUG: Ping failed: ${e.message}")
            false
        }
    }

    suspend fun fetchModels(): List<String> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "$baseUrl/api/tags"
            System.err.println("OPENCODE DEBUG: Fetching models from $endpoint")
            
            val request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build()
            
            val response = client.send(request, BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val body = response.body()
                
                // Matches "name":"modelname"
                val foundModels = "\"name\":\"(.*?)\"".toRegex().findAll(body)
                    .map { it.groupValues[1] }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .toList()
                
                System.err.println("OPENCODE DEBUG: Successfully found ${foundModels.size} models")
                foundModels
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            System.err.println("OPENCODE DEBUG: Fetch models Exception: ${e.message}")
            emptyList()
        }
    }

    private fun escape(raw: String): String = raw
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
}
