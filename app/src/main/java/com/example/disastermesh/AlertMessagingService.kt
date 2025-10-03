package com.example.disastermesh

import android.content.Intent
import com.example.disastermesh.mesh.MeshService
import com.example.disastermesh.data.ProcessedAlerts
import com.example.disastermesh.logging.AlertLog
import com.example.disastermesh.model.DisasterAlert
import com.example.disastermesh.model.RiskLevel
import com.example.disastermesh.util.AlertCodec
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AlertMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data.isEmpty()) return

        val alert = tryDecode(data) ?: return

        val processed = ProcessedAlerts(this)
        if (processed.hasProcessed(alert.alertId)) {
            AlertLog.d("Duplicate alert ${alert.alertId} ignored")
            return
        }
        processed.markProcessed(alert.alertId)

        NotificationHelper(this).showAlertNotificationWithAlert(
            title = getString(R.string.notification_title),
            body = alert.message ?: "Risk ${alert.levelOfRisk}",
            alert = alert
        )

        // Start mesh relay
        val relay = Intent(this, MeshService::class.java).apply {
            action = MeshService.ACTION_RELAY_ALERT
            putExtra(MeshService.EXTRA_ALERT, alert)
        }
        startForegroundService(relay)
    }

    private fun tryDecode(data: Map<String, String>): DisasterAlert? {
        return try {
            val alertId = data["alert_id"] ?: return null
            val lat = data["coordinate_lat"]?.toDouble()
                ?: data["latitude"]?.toDouble() ?: return null
            val lon = data["coordinate_lng"]?.toDouble()
                ?: data["longitude"]?.toDouble() ?: return null
            val radius = data["radius"]?.toDouble() ?: return null
            val level = when ((data["level_of_risk"] ?: "low").lowercase()) {
                "low" -> RiskLevel.LOW
                "medium" -> RiskLevel.MEDIUM
                "high" -> RiskLevel.HIGH
                "critical" -> RiskLevel.CRITICAL
                else -> RiskLevel.LOW
            }
            val msg = data["message"]
            DisasterAlert(
                alertId = alertId,
                latitude = lat,
                longitude = lon,
                radiusMeters = radius,
                levelOfRisk = level,
                message = msg,
                receivedAtMillis = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            AlertLog.e("Failed to decode alert", e)
            null
        }
    }
}


