//package com.example.disastermesh.mesh
//
//import android.app.Notification
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.Service
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import android.os.IBinder
//import androidx.core.app.NotificationCompat
//import com.example.disastermesh.R
//import com.example.disastermesh.data.ProcessedAlerts
//import com.example.disastermesh.logging.AlertLog
//import com.example.disastermesh.model.DisasterAlert
//import com.example.disastermesh.util.AlertCodec
//import com.google.android.gms.nearby.Nearby
//import com.google.android.gms.nearby.connection.AdvertisingOptions
//import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
//import com.google.android.gms.nearby.connection.ConnectionResolution
//import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
//import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
//import com.google.android.gms.nearby.connection.DiscoveryOptions
//import com.google.android.gms.nearby.connection.Payload
//import com.google.android.gms.nearby.connection.PayloadCallback
//import com.google.android.gms.nearby.connection.PayloadTransferUpdate
//import com.google.android.gms.nearby.connection.Strategy
//
//class MeshService : Service() {
//    companion object {
//        const val ACTION_RELAY_ALERT = "ACTION_RELAY_ALERT"
//        const val EXTRA_ALERT = "EXTRA_ALERT"
//        private const val SERVICE_ID = "com.example.disastermesh.mesh"
//        private const val CHANNEL_ID = "mesh"
//    }
//
//    private val connections by lazy { Nearby.getConnectionsClient(this) }
//    private var currentAlert: DisasterAlert? = null
//
//    override fun onBind(intent: Intent?): IBinder? = null
//
//    override fun onCreate() {
//        super.onCreate()
//        startForeground(1, buildNotification())
//        startAdvertisingAndDiscovery()
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        if (intent?.action == ACTION_RELAY_ALERT) {
//            val alert = intent.getParcelableExtra(EXTRA_ALERT, DisasterAlert::class.java)
//            if (alert != null) {
//                currentAlert = alert
//                relayAlert(alert)
//            }
//        }
//        return START_STICKY
//    }
//
//    private fun buildNotification(): Notification {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            nm.createNotificationChannel(
//                NotificationChannel(CHANNEL_ID, "Mesh Relay", NotificationManager.IMPORTANCE_LOW)
//            )
//        }
//        return NotificationCompat.Builder(this, CHANNEL_ID)
//            .setSmallIcon(android.R.drawable.stat_sys_upload)
//            .setContentTitle(getString(R.string.app_name))
//            .setContentText("Mesh relay active")
//            .build()
//    }
//
//    private fun startAdvertisingAndDiscovery() {
//        val strategy = Strategy.P2P_CLUSTER
//
//        connections.startAdvertising(
//            "DisasterMesh",
//            SERVICE_ID,
//            connectionLifecycle,
//            AdvertisingOptions.Builder().setStrategy(strategy).build()
//        )
//
//        connections.startDiscovery(
//            SERVICE_ID,
//            discoveryCallback,
//            DiscoveryOptions.Builder().setStrategy(strategy).build()
//        )
//    }
//
//    private val payloadCallback = object : PayloadCallback() {
//        override fun onPayloadReceived(endpointId: String, payload: Payload) {
//            val bytes = payload.asBytes() ?: return
//            val alert = AlertCodec.decode(bytes) ?: return
//            handleIncomingAlert(alert)
//        }
//
//        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
//    }
//
//    private val connectionLifecycle = object : ConnectionLifecycleCallback() {
//        override fun onConnectionInitiated(endpointId: String, connectionInfo: com.google.android.gms.nearby.connection.ConnectionInfo) {
//            connections.acceptConnection(endpointId, payloadCallback)
//        }
//
//        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
//            if (result.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
//                currentAlert?.let { sendAlert(endpointId, it) }
//            }
//        }
//
//        override fun onDisconnected(endpointId: String) {}
//    }
//
//    private val discoveryCallback = object : com.google.android.gms.nearby.connection.EndpointDiscoveryCallback() {
//        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
//            connections.requestConnection("DisasterMesh", endpointId, connectionLifecycle)
//        }
//
//        override fun onEndpointLost(endpointId: String) {}
//    }
//
//    private fun relayAlert(alert: DisasterAlert) {
//        currentAlert = alert
//        // endpoints found later will receive it in onConnectionResult
//    }
//
//    private fun sendAlert(endpointId: String, alert: DisasterAlert) {
//        val bytes = AlertCodec.encode(alert)
//        connections.sendPayload(endpointId, Payload.fromBytes(bytes))
//        AlertLog.d("Relayed alert ${alert.alertId} to $endpointId")
//    }
//
//    private fun handleIncomingAlert(alert: DisasterAlert) {
//        val processed = ProcessedAlerts(this)
//        if (processed.hasProcessed(alert.alertId)) return
//        processed.markProcessed(alert.alertId)
//
//        // Show notification
//        com.example.disastermesh.NotificationHelper(this)
//            .showAlertNotificationWithAlert(
//                title = getString(R.string.notification_title),
//                body = alert.message ?: "Risk ${alert.levelOfRisk}",
//                alert = alert
//            )
//
//        // Relay further
//        currentAlert = alert
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        connections.stopAdvertising()
//        connections.stopDiscovery()
//        connections.stopAllEndpoints()
//    }
//}
//
//
package com.example.disastermesh.mesh

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.disastermesh.R
import com.example.disastermesh.data.AuthorityManager
import com.example.disastermesh.data.ProcessedAlerts
import com.example.disastermesh.logging.AlertLog
import com.example.disastermesh.model.Authority
import com.example.disastermesh.model.DisasterAlert
import com.example.disastermesh.util.AlertCodec
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*

class MeshService : Service() {
    companion object {
        const val ACTION_RELAY_ALERT = "ACTION_RELAY_ALERT"
        const val ACTION_START_BROADCASTING = "ACTION_START_BROADCASTING"
        const val EXTRA_ALERT = "EXTRA_ALERT"
        private const val SERVICE_ID = "com.example.disastermesh.mesh"
        private const val CHANNEL_ID = "mesh"
    }

    private val connections by lazy { Nearby.getConnectionsClient(this) }
    private val connectedEndpoints = mutableSetOf<String>()
    private val discoveredEndpoints = mutableSetOf<String>()
    private var currentAlert: DisasterAlert? = null
    private var isAdvertising = false
    private var isDiscovering = false
    private lateinit var authorityManager: AuthorityManager

    // Unique device identifier for the mesh network
    private val deviceName = "DM_${Build.MODEL.replace(" ", "_")}_${Build.ID.takeLast(4)}"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        authorityManager = AuthorityManager(this)
        startForeground(1, buildNotification())

        // Auto-start mesh network on service creation
        startAdvertisingAndDiscovery()

        AlertLog.d("MeshService created - Broadcasting as: $deviceName")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RELAY_ALERT -> {
                val alert = intent.getParcelableExtra(EXTRA_ALERT, DisasterAlert::class.java)
                if (alert != null) {
                    currentAlert = alert
                    relayAlert(alert)
                }
            }
            ACTION_START_BROADCASTING -> {
                // Ensure mesh network is active
                if (!isAdvertising || !isDiscovering) {
                    startAdvertisingAndDiscovery()
                }
            }
            else -> {
                // Default action: ensure mesh is running
                if (!isAdvertising || !isDiscovering) {
                    startAdvertisingAndDiscovery()
                }
            }
        }
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Mesh Network",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows mesh network status"
                }
            )
        }

        val statusText = when {
            connectedEndpoints.isNotEmpty() ->
                "Connected: ${connectedEndpoints.size} device(s)"
            discoveredEndpoints.isNotEmpty() ->
                "Nearby: ${discoveredEndpoints.size} device(s)"
            isAdvertising ->
                "Broadcasting - Visible to nearby devices"
            else ->
                "Starting mesh network..."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(statusText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1, buildNotification())
    }

    private fun startAdvertisingAndDiscovery() {
        val strategy = Strategy.P2P_CLUSTER

        // Start advertising to make device visible
        if (!isAdvertising) {
            connections.startAdvertising(
                deviceName,
                SERVICE_ID,
                connectionLifecycle,
                AdvertisingOptions.Builder()
                    .setStrategy(strategy)
                    .build()
            ).addOnSuccessListener {
                isAdvertising = true
                AlertLog.d("Successfully started advertising as: $deviceName")
                AlertLog.append(this@MeshService, "Advertising as $deviceName")
                updateNotification()
            }.addOnFailureListener { e ->
                AlertLog.e("Failed to start advertising: ${e.message}")
                AlertLog.append(this@MeshService, "Failed to advertise: ${e.message}")
                // Retry after 3 seconds
                android.os.Handler(mainLooper).postDelayed({
                    startAdvertisingAndDiscovery()
                }, 3000)
            }
        }

        // Start discovery to find other devices
        if (!isDiscovering) {
            connections.startDiscovery(
                SERVICE_ID,
                discoveryCallback,
                DiscoveryOptions.Builder()
                    .setStrategy(strategy)
                    .build()
            ).addOnSuccessListener {
                isDiscovering = true
                AlertLog.d("Successfully started discovery")
                AlertLog.append(this@MeshService, "Discovery started")
            }.addOnFailureListener { e ->
                AlertLog.e("Failed to start discovery: ${e.message}")
                AlertLog.append(this@MeshService, "Failed discovery: ${e.message}")
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            val alert = AlertCodec.decode(bytes) ?: return
            handleIncomingAlert(alert)
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                AlertLog.d("Payload transfer successful to $endpointId")
            }
        }
    }

    private val connectionLifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            AlertLog.d("Connection initiated with: ${connectionInfo.endpointName}")
            // Auto-accept all connections for emergency mesh
            connections.acceptConnection(endpointId, payloadCallback)
                .addOnSuccessListener {
                    AlertLog.d("Accepted connection from ${connectionInfo.endpointName}")
                }
                .addOnFailureListener { e ->
                    AlertLog.e("Failed to accept connection: ${e.message}")
                }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                connectedEndpoints.add(endpointId)
                AlertLog.d("Connected to endpoint: $endpointId (Total: ${connectedEndpoints.size})")
                AlertLog.append(this@MeshService, "Connected: $endpointId")
                updateNotification()

                // Send current alert if available
                currentAlert?.let { sendAlert(endpointId, it) }
            } else {
                AlertLog.d("Connection failed: ${result.status.statusMessage}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpoints.remove(endpointId)
            AlertLog.d("Disconnected from: $endpointId (Remaining: ${connectedEndpoints.size})")
            AlertLog.append(this@MeshService, "Disconnected: $endpointId")
            updateNotification()
        }
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            AlertLog.d("Discovered device: ${info.endpointName}")
            discoveredEndpoints.add(endpointId)
            updateNotification()

            // Auto-connect to discovered endpoints
            if (!connectedEndpoints.contains(endpointId)) {
                connections.requestConnection(deviceName, endpointId, connectionLifecycle)
                    .addOnSuccessListener {
                        AlertLog.d("Requested connection to ${info.endpointName}")
                    }
                    .addOnFailureListener { e ->
                        AlertLog.e("Failed to request connection: ${e.message}")
                    }
            }
        }

        override fun onEndpointLost(endpointId: String) {
            discoveredEndpoints.remove(endpointId)
            AlertLog.d("Lost endpoint: $endpointId")
            updateNotification()
        }
    }

    private fun relayAlert(alert: DisasterAlert) {
        currentAlert = alert
        // Send to all connected endpoints
        connectedEndpoints.forEach { endpointId ->
            sendAlert(endpointId, alert)
        }
        AlertLog.d("Relaying alert ${alert.alertId} to ${connectedEndpoints.size} devices")
        AlertLog.append(this, "Relayed alert ${alert.alertId} to ${connectedEndpoints.size} devices")
    }

    private fun sendAlert(endpointId: String, alert: DisasterAlert) {
        val bytes = AlertCodec.encode(alert)
        connections.sendPayload(endpointId, Payload.fromBytes(bytes))
            .addOnSuccessListener {
                AlertLog.d("Sent alert ${alert.alertId} to $endpointId")
                AlertLog.append(this, "Sent alert ${alert.alertId} to $endpointId")
            }
            .addOnFailureListener { e ->
                AlertLog.e("Failed to send alert: ${e.message}")
                AlertLog.append(this, "Failed to send to $endpointId: ${e.message}")
            }
    }

    private fun handleIncomingAlert(alert: DisasterAlert) {
        val processed = ProcessedAlerts(this)
        if (processed.hasProcessed(alert.alertId)) {
            AlertLog.d("Alert ${alert.alertId} already processed, skipping")
            return
        }
        
        // Authority-based filtering
        if (!shouldProcessAlert(alert)) {
            AlertLog.d("Alert ${alert.alertId} filtered by authority rules")
            return
        }
        
        processed.markProcessed(alert.alertId)
        AlertLog.append(this, "Processed alert ${alert.alertId} (${alert.levelOfRisk}) from ${alert.senderAuthority}")

        // Show notification for alert
        com.example.disastermesh.NotificationHelper(this)
            .showAlertNotificationWithAlert(
                title = getString(R.string.notification_title),
                body = alert.message ?: "Risk Level: ${alert.levelOfRisk}",
                alert = alert
            )

        // Update current alert and relay further
        currentAlert = alert
        relayAlert(alert)
    }
    
    private fun shouldProcessAlert(alert: DisasterAlert): Boolean {
        val currentAuthority = authorityManager.getAuthority()
        
        return when (alert.targetAuthority) {
            Authority.ALL -> true // Everyone should see this
            Authority.ADMIN -> currentAuthority == Authority.ADMIN // Only admins
            Authority.USER -> currentAuthority == Authority.USER // Only users (for SOS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isAdvertising = false
        isDiscovering = false
        connections.stopAdvertising()
        connections.stopDiscovery()
        connections.stopAllEndpoints()
        AlertLog.d("MeshService destroyed")
    }
}