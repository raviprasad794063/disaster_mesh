//package com.example.disastermesh
//
//import android.Manifest
//import android.content.pm.PackageManager
//import android.os.Build
//import android.os.Bundle
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.ContextCompat
//import com.example.disastermesh.databinding.ActivityMainBinding
//import com.example.disastermesh.model.DisasterAlert
//
//class MainActivity : AppCompatActivity() {
//    private lateinit var binding: ActivityMainBinding
//
//    private val requestPermissions = registerForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        ensurePermissions()
//
//        // If launched from notification with alert payload
//        val alert = intent.getParcelableExtra("alert", DisasterAlert::class.java)
//        if (alert != null) {
//            val frag = supportFragmentManager.findFragmentById(R.id.maps_fragment) as? MapsFragment
//            frag?.showAlert(alert)
//        }
//    }
//
//    private fun ensurePermissions() {
//        val needed = mutableListOf<String>()
//        if (Build.VERSION.SDK_INT >= 33) {
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//                needed += Manifest.permission.POST_NOTIFICATIONS
//            }
//        }
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            needed += Manifest.permission.ACCESS_FINE_LOCATION
//        }
//        if (Build.VERSION.SDK_INT >= 31) {
//            val btPerms = listOf(
//                Manifest.permission.BLUETOOTH_SCAN,
//                Manifest.permission.BLUETOOTH_CONNECT,
//                Manifest.permission.BLUETOOTH_ADVERTISE
//            )
//            btPerms.forEach { p ->
//                if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
//                    needed += p
//                }
//            }
//        }
//        if (Build.VERSION.SDK_INT >= 33) {
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
//                needed += Manifest.permission.NEARBY_WIFI_DEVICES
//            }
//        }
//        if (needed.isNotEmpty()) requestPermissions.launch(needed.toTypedArray())
//    }
//}
//
//
package com.example.disastermesh

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.disastermesh.databinding.ActivityMainBinding
import com.example.disastermesh.mesh.MeshService
import com.example.disastermesh.model.DisasterAlert
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var meshServiceStarted = false
    private val handler = Handler(Looper.getMainLooper())

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check if all critical permissions were granted
        val allGranted = permissions.entries.all { it.value }

        if (allGranted) {
            // Start mesh service after permissions are granted
            startMeshService()
            showMeshStatus("Mesh network starting...")
            hideLoadingOverlay()
            
            // Refresh location in the maps fragment now that we have permission
            val fragment = supportFragmentManager.findFragmentById(R.id.maps_fragment) as? MapsFragment
            fragment?.refreshCurrentLocation()
        } else {
            // Check which permissions were denied
            val deniedPermissions = permissions.entries
                .filter { !it.value }
                .map { it.key }

            // Identify critical permissions
            val criticalDenied = deniedPermissions.any { permission ->
                permission in listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.NEARBY_WIFI_DEVICES
                )
            }

            if (criticalDenied) {
                Snackbar.make(
                    binding.root,
                    "Mesh network requires location and Bluetooth permissions to function",
                    Snackbar.LENGTH_LONG
                ).setAction("Grant") {
                    ensurePermissions()
                }.show()
            } else {
                // Non-critical permissions denied (like notifications), start anyway
                startMeshService()
                showMeshStatus("Mesh network starting (limited features)...")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Show loading overlay initially
        showLoadingOverlay("Initializing DisasterMesh...")

        // Check and request permissions first
        ensurePermissions()

        // Handle notification launch with alert payload
        handleNotificationLaunch()

        // Hide loading after a short delay to show the app is ready
        handler.postDelayed({
            hideLoadingOverlay()
        }, 2000)
    }

    override fun onResume() {
        super.onResume()
        // Ensure mesh service is running when app comes to foreground
        if (!meshServiceStarted && hasRequiredPermissions()) {
            startMeshService()
        }
    }

    private fun handleNotificationLaunch() {
        // If launched from notification with alert payload
        val alert = intent.getParcelableExtra("alert", DisasterAlert::class.java)
        if (alert != null) {
            // Small delay to ensure fragment is ready
            handler.postDelayed({
                val frag = supportFragmentManager.findFragmentById(R.id.maps_fragment) as? MapsFragment
                frag?.showAlert(alert)
            }, 500)
        }
    }

    private fun startMeshService() {
        if (meshServiceStarted) return

        try {
            val intent = Intent(this, MeshService::class.java).apply {
                action = MeshService.ACTION_START_BROADCASTING
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            meshServiceStarted = true

            // Create initial presence broadcast after a short delay
            handler.postDelayed({
                broadcastPresence()
            }, 2000)

            showMeshStatus("Mesh network active - visible to nearby devices")

        } catch (e: Exception) {
            e.printStackTrace()
            showMeshStatus("Failed to start mesh network: ${e.message}")
        }
    }

    private fun broadcastPresence() { /* no-op: presence beacons removed */ }

    private fun setupMeshStatusIndicator() {
        // If you have a status view in your layout, update it here
        // For now, we'll use the existing UI elements

        // You can add a status indicator to your activity_main.xml layout
        // Example: A small TextView or ImageView showing mesh status

        // Update any existing status text if available
        updateMeshStatusUI(true)
    }

    private fun updateMeshStatusUI(isActive: Boolean) {
        // Update UI to show mesh network status
        // The fragment handles the actual UI updates, so we just pass the status
        val fragment = supportFragmentManager.findFragmentById(R.id.maps_fragment) as? MapsFragment
        if (isActive) {
            fragment?.updateMeshStatus("Active")
        } else {
            fragment?.updateMeshStatus("Inactive")
        }
    }

    private fun showMeshStatus(message: String) {
        // Update loading overlay if visible
        if (binding.loadingOverlay.visibility == View.VISIBLE) {
            binding.loadingText.text = message
        }
        
        // Update mesh status in fragment if available
        val fragment = supportFragmentManager.findFragmentById(R.id.maps_fragment) as? MapsFragment
        fragment?.updateMeshStatus(message)
        
        // Also log for debugging
        android.util.Log.d("DisasterMesh", message)
    }

    private fun hideLoadingOverlay() {
        binding.loadingOverlay.visibility = View.GONE
    }

    private fun showLoadingOverlay(message: String) {
        binding.loadingText.text = message
        binding.loadingOverlay.visibility = View.VISIBLE
    }

    private fun hasRequiredPermissions(): Boolean {
        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= 31) {
            requiredPermissions.addAll(listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ))
        }

        if (Build.VERSION.SDK_INT >= 33) {
            requiredPermissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun ensurePermissions() {
        val needed = mutableListOf<String>()

        // Notification permission (optional but recommended)
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                needed += Manifest.permission.POST_NOTIFICATIONS
            }
        }

        // Location permission (required for Nearby)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            needed += Manifest.permission.ACCESS_FINE_LOCATION
        }

        // Bluetooth permissions (required for Nearby on Android 12+)
        if (Build.VERSION.SDK_INT >= 31) {
            val btPerms = listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
            btPerms.forEach { p ->
                if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                    needed += p
                }
            }
        }

        // Nearby WiFi permission (required for Nearby on Android 13+)
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                needed += Manifest.permission.NEARBY_WIFI_DEVICES
            }
        }

        if (needed.isNotEmpty()) {
            requestPermissions.launch(needed.toTypedArray())
        } else {
            // All permissions already granted, start mesh service
            startMeshService()
            
            // Also refresh location since we have permission
            val fragment = supportFragmentManager.findFragmentById(R.id.maps_fragment) as? MapsFragment
            fragment?.refreshCurrentLocation()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Note: We don't stop the mesh service here as it should continue running
        // The service will handle its own lifecycle
    }
}