package com.example.disastermesh.util

import com.example.disastermesh.model.DisasterAlert
import com.example.disastermesh.model.RiskLevel
import org.json.JSONObject

object AlertCodec {
    fun encode(alert: DisasterAlert): ByteArray {
        val json = JSONObject()
            .put("alert_id", alert.alertId)
            .put("latitude", alert.latitude)
            .put("longitude", alert.longitude)
            .put("radius", alert.radiusMeters)
            .put("level_of_risk", alert.levelOfRisk.name.lowercase())
            .put("message", alert.message)
            .put("received_at", alert.receivedAtMillis)
            .put("sender_authority", alert.senderAuthority.name)
            .put("target_authority", alert.targetAuthority.name)
        return json.toString().toByteArray(Charsets.UTF_8)
    }

    fun decode(bytes: ByteArray): DisasterAlert? {
        return try {
            val json = JSONObject(String(bytes, Charsets.UTF_8))
            val level = when (json.optString("level_of_risk").lowercase()) {
                "low" -> RiskLevel.LOW
                "medium" -> RiskLevel.MEDIUM
                "high" -> RiskLevel.HIGH
                "critical" -> RiskLevel.CRITICAL
                else -> RiskLevel.LOW
            }
            val senderAuth = try { 
                com.example.disastermesh.model.Authority.valueOf(json.optString("sender_authority", "USER"))
            } catch (e: Exception) { com.example.disastermesh.model.Authority.USER }
            val targetAuth = try { 
                com.example.disastermesh.model.Authority.valueOf(json.optString("target_authority", "ALL"))
            } catch (e: Exception) { com.example.disastermesh.model.Authority.ALL }
            DisasterAlert(
                alertId = json.getString("alert_id"),
                latitude = json.getDouble("latitude"),
                longitude = json.getDouble("longitude"),
                radiusMeters = json.getDouble("radius"),
                levelOfRisk = level,
                message = json.optString("message").takeIf { it.isNotEmpty() },
                receivedAtMillis = json.optLong("received_at", System.currentTimeMillis()),
                senderAuthority = senderAuth,
                targetAuthority = targetAuth
            )
        } catch (e: Exception) {
            null
        }
    }
}


