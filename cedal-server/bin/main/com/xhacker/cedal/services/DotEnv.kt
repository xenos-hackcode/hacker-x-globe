package com.xhacker.cedal.services

import java.io.File

// Real OS environment variables (System.getenv) always win, but this repo's
// keys actually live in a root-level .env (c:\Users\WINDOWS11\Documents\
// projects\.env) that nothing here was reading — cedal-server has no dotenv
// loader of its own and no env vars were ever exported into the shell that
// launches it. Searches upward from the working directory (cedal-server/
// when run via `gradlew run`) so it finds that file without needing an
// absolute path baked in.
object DotEnv {
    private val values: Map<String, String> by lazy { load() }

    private fun load(): Map<String, String> {
        val candidates = listOf(File(".env"), File("../.env"), File("../../.env"))
        val file = candidates.firstOrNull { it.isFile } ?: return emptyMap()
        return file.readLines().mapNotNull { parseLine(it) }.toMap()
    }

    private fun parseLine(rawLine: String): Pair<String, String>? {
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#")) return null
        val separatorIndex = line.indexOf('=')
        if (separatorIndex <= 0) return null
        val key = line.substring(0, separatorIndex).trim()
        var value = line.substring(separatorIndex + 1).trim()
        if (value.length >= 2 && (value.first() == '"' && value.last() == '"' || value.first() == '\'' && value.last() == '\'')) {
            value = value.substring(1, value.length - 1)
        }
        return key to value
    }

    fun get(key: String): String? = System.getenv(key) ?: values[key]
}
