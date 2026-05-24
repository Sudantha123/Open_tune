/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

data class LogEntry(val time: Long, val level: Int, val tag: String?, val message: String)

object GlobalLog {
    // Reduced from 500 → 100: each entry ~200 bytes, 500 entries = ~100 KB held live in RAM.
    // 100 entries is enough for the in-app debug panel while saving ~80 KB.
    private const val MAX_ENTRIES = 100
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs = _logs.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun append(level: Int, tag: String?, message: String) {
        val entry = LogEntry(System.currentTimeMillis(), level, tag, message)
        // Only update the StateFlow (causing a recomposition) if buffer is at capacity or on DEBUG/WARN/ERROR.
        // This avoids constant recomposition for verbose/info logs.
        val current = _logs.value
        val new = if (current.size >= MAX_ENTRIES) current.drop(1) + entry else current + entry
        _logs.value = new
    }

    fun clear() {
        _logs.value = emptyList()
    }

    fun format(entry: LogEntry): String {
        val ts = timeFormat.format(Date(entry.time))
        val lvl = when (entry.level) {
            android.util.Log.VERBOSE -> "V"
            android.util.Log.DEBUG -> "D"
            android.util.Log.INFO -> "I"
            android.util.Log.WARN -> "W"
            android.util.Log.ERROR -> "E"
            else -> "?"
        }
        val tag = entry.tag ?: ""
        return "[$ts] $lvl/$tag: ${entry.message}"
    }
}

/** Timber Tree that forwards logs to GlobalLog */
class GlobalLogTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        try {
            val final = if (t != null) "$message\n$t" else message
            GlobalLog.append(priority, tag, final)
        } catch (_: Exception) {
            // swallow
        }
    }
}
