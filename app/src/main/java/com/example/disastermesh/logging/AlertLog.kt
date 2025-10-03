package com.example.disastermesh.logging

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AlertLog {
    private const val TAG = "DisasterMesh"
    private const val LOG_FILE_NAME = "alert_log.txt"
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }

    fun append(context: Context, line: String) {
        try {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(dir, LOG_FILE_NAME)
            val ts = timeFormat.format(Date())
            file.appendText("[$ts] $line\n")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to write log: $line", t)
        }
    }

    fun readAll(context: Context, maxLines: Int = 500): List<String> {
        return try {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(dir, LOG_FILE_NAME)
            if (!file.exists()) emptyList()
            else file.readLines().takeLast(maxLines)
        } catch (t: Throwable) {
            emptyList()
        }
    }
}


