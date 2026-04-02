package com.opencode.adapter

import com.opencode.settings.AppSettingsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import java.io.InputStreamReader
import java.io.BufferedReader

/**
 * CLI adapter that spawns an opencode child process to perform reviews.
 * Reads stdout from the process and streams it line-by-line.
 */
class CliAdapter(private val settings: AppSettingsState) : OpenCodeAdapter {

    override fun review(content: String, context: String): Flow<String> = flow {
       val command = settings.cliCommandPath.split(" ").toMutableList().apply {
           add("review")
           add("--file")
           add(context)
       }

       val processBuilder = ProcessBuilder(command)
       processBuilder.redirectErrorStream(true)
       
       val process = processBuilder.start()
       // Send the content to Reviewer's stdin
       process.outputStream.write(content.toByteArray())
       process.outputStream.flush()
       process.outputStream.close()

       BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
           var line: String?
           while (reader.readLine().also { line = it } != null) {
               emit(line!!)
           }
       }
       process.waitFor()
    }.flowOn(Dispatchers.IO) // IO dispatch for blocking CLI process.

    override suspend fun ping(): Boolean {
       return try {
           val status = ProcessBuilder(settings.cliCommandPath.split(" ") + "--version").start().waitFor()
           status == 0
       } catch (e: Exception) {
           false
       }
    }
}
