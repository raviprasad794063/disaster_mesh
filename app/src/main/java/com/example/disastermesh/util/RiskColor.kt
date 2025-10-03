package com.example.disastermesh.util

import android.graphics.Color
import com.example.disastermesh.model.RiskLevel

object RiskColor {
    fun fillColor(level: RiskLevel): Int = when (level) {
        RiskLevel.LOW -> Color.argb(80, 76, 175, 80)        // Green
        RiskLevel.MEDIUM -> Color.argb(90, 255, 193, 7)     // Amber
        RiskLevel.HIGH -> Color.argb(100, 244, 67, 54)      // Red
        RiskLevel.CRITICAL -> Color.argb(110, 156, 39, 176) // Purple
    }

    fun strokeColor(level: RiskLevel): Int = when (level) {
        RiskLevel.LOW -> Color.rgb(56, 142, 60)
        RiskLevel.MEDIUM -> Color.rgb(255, 160, 0)
        RiskLevel.HIGH -> Color.rgb(211, 47, 47)
        RiskLevel.CRITICAL -> Color.rgb(123, 31, 162)
    }
}


