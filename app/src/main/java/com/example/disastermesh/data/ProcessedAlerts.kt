package com.example.disastermesh.data

import android.content.Context

class ProcessedAlerts(context: Context) {
    private val prefs = context.getSharedPreferences("processed_alerts", Context.MODE_PRIVATE)

    fun hasProcessed(alertId: String): Boolean = prefs.getBoolean(alertId, false)

    fun markProcessed(alertId: String) {
        prefs.edit().putBoolean(alertId, true).apply()
    }
}


