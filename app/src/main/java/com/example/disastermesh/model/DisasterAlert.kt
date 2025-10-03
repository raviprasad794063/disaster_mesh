package com.example.disastermesh.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DisasterAlert(
    val alertId: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val levelOfRisk: RiskLevel,
    val message: String?,
    val receivedAtMillis: Long,
    val senderAuthority: Authority = Authority.USER,
    val targetAuthority: Authority = Authority.ALL
) : Parcelable

enum class RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }

enum class Authority { USER, ADMIN, ALL }


