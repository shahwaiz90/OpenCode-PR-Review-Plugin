package com.opencode.adapter

import kotlinx.coroutines.flow.Flow

/**
 * Common interface for OpenCode communication.
 * Returns a Flow of strings for streaming responses.
 */
interface OpenCodeAdapter {
    /**
     * Sends the code context to OpenCode and returns the review stream.
     * @param content The code to review.
     * @param context Additional metadata about the code (e.g. filename, git context).
     * @return A Flow that emits response chunks line by line.
     */
    suspend fun review(content: String, context: String): Flow<String>
    
    /**
     * Checks if the adapter can connect to the OpenCode engine.
     */
    suspend fun ping(): Boolean
}
