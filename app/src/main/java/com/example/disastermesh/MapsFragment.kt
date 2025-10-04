package com.example.disastermesh

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.disastermesh.databinding.FragmentMapsBinding
import com.example.disastermesh.databinding.DialogBroadcastAlertBinding
import com.example.disastermesh.data.AuthorityManager
import com.example.disastermesh.model.Authority
import com.example.disastermesh.model.DisasterAlert
import com.example.disastermesh.model.RiskLevel
import com.example.disastermesh.util.RiskColor
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polygon

class MapsFragment : Fragment() {
    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!
    private lateinit var authorityManager: AuthorityManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentAlert: DisasterAlert? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        android.util.Log.d("DisasterMesh", "MapsFragment onViewCreated")
        
        // Hide empty state initially since we have a map
        binding.emptyState.visibility = View.GONE
        
        // Initialize location services first
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        setupMap()
        setupUI()
        updateRoleDisplay()
        updateMeshStatus("Starting...")
    }

    private fun setupMap() {
        android.util.Log.d("DisasterMesh", "Setting up map")
        
        try {
            val ctx = requireContext().applicationContext
            Configuration.getInstance().userAgentValue = ctx.packageName
            binding.map.setMultiTouchControls(true)
            
            // Set initial zoom level
            binding.map.controller.setZoom(15.0)
            
            // Set a default center first (this should always work)
            val defaultCenter = GeoPoint(37.7749, -122.4194) // San Francisco
            binding.map.controller.setCenter(defaultCenter)
            
            android.util.Log.d("DisasterMesh", "Map initialized with default center")
            
            // Wait for map to be ready, then try to get current location
            binding.map.post {
                android.util.Log.d("DisasterMesh", "Map is ready, attempting to get current location")
                getCurrentLocationForMap()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("DisasterMesh", "Error setting up map: ${e.message}")
            // Still try to get location even if map setup partially failed
            getCurrentLocationForMap()
        }
    }

    private fun getCurrentLocationForMap() {
        android.util.Log.d("DisasterMesh", "=== getCurrentLocationForMap called ===")
        
        // Check if we have location permission
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            requireContext(), 
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        android.util.Log.d("DisasterMesh", "Location permission granted: $hasLocationPermission")
        
        if (!hasLocationPermission) {
            android.util.Log.d("DisasterMesh", "No location permission, using default location")
            setDefaultLocation()
            return
        }
        
        // Show user that we're trying to get location
        Snackbar.make(binding.root, "Getting your location...", Snackbar.LENGTH_SHORT).show()
        
        // Try to get last known location first (fastest)
        try {
            android.util.Log.d("DisasterMesh", "Requesting last known location from FusedLocationClient")
            
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    android.util.Log.d("DisasterMesh", "Last location callback - location: $location")
                    
                    if (location != null) {
                        android.util.Log.d("DisasterMesh", "Location details: lat=${location.latitude}, lng=${location.longitude}, accuracy=${location.accuracy}, time=${location.time}")
                        
                        if (isLocationValid(location)) {
                            android.util.Log.d("DisasterMesh", "Location is valid, centering map")
                            centerMapOnLocation(location)
                        } else {
                            android.util.Log.d("DisasterMesh", "Location is too old, requesting fresh location")
                            requestFreshLocation()
                        }
                    } else {
                        android.util.Log.d("DisasterMesh", "Last location is null, requesting fresh location")
                        requestFreshLocation()
                    }
                }
                .addOnFailureListener { exception ->
                    android.util.Log.w("DisasterMesh", "Last location failed: ${exception.message}")
                    android.util.Log.w("DisasterMesh", "Exception type: ${exception.javaClass.simpleName}")
                    requestFreshLocation()
                }
        } catch (e: SecurityException) {
            android.util.Log.e("DisasterMesh", "Security exception: ${e.message}")
            setDefaultLocation()
        } catch (e: Exception) {
            android.util.Log.e("DisasterMesh", "Exception getting location: ${e.message}")
            setDefaultLocation()
        }
    }

    private fun isLocationValid(location: Location): Boolean {
        // Check if location is not too old (older than 5 minutes)
        val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)
        return location.time > fiveMinutesAgo
    }

    private fun centerMapOnLocation(location: Location) {
        android.util.Log.d("DisasterMesh", "=== Centering map on location ===")
        android.util.Log.d("DisasterMesh", "Location: lat=${location.latitude}, lng=${location.longitude}")
        
        try {
            val geoPoint = GeoPoint(location.latitude, location.longitude)
            
            // Clear any existing overlays first
            binding.map.overlays.clear()
            
            // Center the map
            binding.map.controller.setCenter(geoPoint)
            android.util.Log.d("DisasterMesh", "Map center set to: ${geoPoint.latitude}, ${geoPoint.longitude}")
            
            // Add marker
            addCurrentLocationMarker(geoPoint)
            
            // Force map refresh
            binding.map.invalidate()
            
            // Show success message
            Snackbar.make(binding.root, getString(R.string.location_centered), Snackbar.LENGTH_LONG).show()
            
            android.util.Log.d("DisasterMesh", "Map successfully centered on current location")
            
        } catch (e: Exception) {
            android.util.Log.e("DisasterMesh", "Error centering map on location: ${e.message}")
            Snackbar.make(binding.root, "Error centering map: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun requestFreshLocation() {
        android.util.Log.d("DisasterMesh", "=== Requesting fresh location ===")
        
        // Simple timeout flag to prevent multiple calls to setDefaultLocation
        var locationReceived = false
        
        try {
            android.util.Log.d("DisasterMesh", "Calling getCurrentLocation with PRIORITY_BALANCED_POWER_ACCURACY")
            
            fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                null // No cancellation token for simplicity
            )
            .addOnSuccessListener { location: Location? ->
                if (locationReceived) {
                    android.util.Log.d("DisasterMesh", "Fresh location success callback called but already processed")
                    return@addOnSuccessListener
                }
                locationReceived = true
                
                android.util.Log.d("DisasterMesh", "Fresh location success callback - location: $location")
                
                if (location != null) {
                    android.util.Log.d("DisasterMesh", "Fresh location details: lat=${location.latitude}, lng=${location.longitude}, accuracy=${location.accuracy}")
                    centerMapOnLocation(location)
                } else {
                    android.util.Log.d("DisasterMesh", "Fresh location is null, using default")
                    setDefaultLocation()
                }
            }
            .addOnFailureListener { exception ->
                if (locationReceived) {
                    android.util.Log.d("DisasterMesh", "Fresh location failure callback called but already processed")
                    return@addOnFailureListener
                }
                locationReceived = true
                
                android.util.Log.w("DisasterMesh", "Fresh location failed: ${exception.message}")
                android.util.Log.w("DisasterMesh", "Exception type: ${exception.javaClass.simpleName}")
                setDefaultLocation()
            }
            
            // Fallback timeout after 8 seconds
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (!locationReceived) {
                    locationReceived = true
                    android.util.Log.d("DisasterMesh", "Fresh location request timed out after 8 seconds, using default")
                    setDefaultLocation()
                }
            }, 8000)
            
        } catch (e: SecurityException) {
            android.util.Log.e("DisasterMesh", "Security exception in fresh location: ${e.message}")
            setDefaultLocation()
        } catch (e: Exception) {
            android.util.Log.e("DisasterMesh", "Exception in fresh location: ${e.message}")
            setDefaultLocation()
        }
    }

    private fun setDefaultLocation() {
        android.util.Log.d("DisasterMesh", "Setting default location")
        
        // Fallback to a default location (San Francisco)
        val defaultLocation = GeoPoint(37.7749, -122.4194)
        binding.map.controller.setCenter(defaultLocation)
        
        android.util.Log.d("DisasterMesh", "Map centered on default location: San Francisco (${defaultLocation.latitude}, ${defaultLocation.longitude})")
        
        // Show a message to the user
        Snackbar.make(binding.root, getString(R.string.location_default_message), Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.location_enable)) {
                // Retry getting current location
                getCurrentLocationForMap()
            }
            .show()
    }

    private fun addCurrentLocationMarker(location: GeoPoint) {
        android.util.Log.d("DisasterMesh", "Adding current location marker at: ${location.latitude}, ${location.longitude}")
        
        try {
            // Remove any existing location markers
            binding.map.overlays.removeAll { overlay ->
                overlay is org.osmdroid.views.overlay.Marker && overlay.title == "Current Location"
            }
            
            // Add a marker for current location
            val marker = org.osmdroid.views.overlay.Marker(binding.map)
            marker.position = location
            marker.title = "Current Location"
            marker.snippet = "You are here"
            
            // Set a custom icon for current location
            try {
                val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_location_24)
                drawable?.setTint(ContextCompat.getColor(requireContext(), R.color.md_theme_light_primary))
                marker.icon = drawable
                android.util.Log.d("DisasterMesh", "Custom marker icon set successfully")
            } catch (e: Exception) {
                android.util.Log.w("DisasterMesh", "Failed to set custom marker icon: ${e.message}")
                // Use default marker if custom icon fails
            }
            
            binding.map.overlays.add(marker)
            binding.map.invalidate()
            
            android.util.Log.d("DisasterMesh", "Location marker added successfully")
            
        } catch (e: Exception) {
            android.util.Log.e("DisasterMesh", "Error adding location marker: ${e.message}")
        }
    }

    private fun setupUI() {
        authorityManager = AuthorityManager(requireContext())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Setup click listeners
        binding.mainActionFab.setOnClickListener { showBroadcastDialog() }
        binding.viewLogsFab.setOnClickListener { showLogsDialog() }
        binding.locationFab.setOnClickListener { 
            android.util.Log.d("DisasterMesh", "Location FAB clicked - getting current location")
            
            // Just try to get current location when FAB is clicked
            refreshCurrentLocation()
        }
        
        // Long press for debug info and test
        binding.locationFab.setOnLongClickListener {
            android.util.Log.d("DisasterMesh", "Location FAB long pressed - showing debug and test")
            debugLocationServices()
            
            // Also run the test centering to verify map functionality
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                testMapCentering()
            }, 2000)
            true
        }
        binding.switchRoleBtn.setOnClickListener { switchRole() }
        binding.dismissAlertBtn.setOnClickListener { dismissAlert() }
        
        // Setup mesh status chip click
        binding.meshStatusChip.setOnClickListener { 
            showMeshStatusDialog()
        }
    }

    fun showAlert(alert: DisasterAlert) {
        currentAlert = alert
        val center = GeoPoint(alert.latitude, alert.longitude)
        
        // Clear existing overlays and add new alert circle
        binding.map.overlays.clear()
        val circle = createCircle(center, alert.radiusMeters)
        circle.outlinePaint.color = RiskColor.strokeColor(alert.levelOfRisk)
        circle.outlinePaint.strokeWidth = 4f
        circle.fillPaint.color = RiskColor.fillColor(alert.levelOfRisk)
        binding.map.overlays.add(circle)

        // Update map view
        binding.map.controller.setZoom(14.0)
        binding.map.controller.setCenter(center)
        binding.map.invalidate()

        // Show alert card with details
        showAlertCard(alert)
        
        // Hide empty state
        binding.emptyState.visibility = View.GONE
    }

    private fun showAlertCard(alert: DisasterAlert) {
        binding.alertCard.visibility = View.VISIBLE
        
        // Update risk level chip
        binding.riskLevelChip.text = when (alert.levelOfRisk) {
            RiskLevel.LOW -> getString(R.string.risk_low)
            RiskLevel.MEDIUM -> getString(R.string.risk_medium)
            RiskLevel.HIGH -> getString(R.string.risk_high)
            RiskLevel.CRITICAL -> getString(R.string.risk_critical)
        }
        
        binding.riskLevelChip.setChipBackgroundColorResource(
            when (alert.levelOfRisk) {
                RiskLevel.LOW -> R.color.risk_low
                RiskLevel.MEDIUM -> R.color.risk_medium
                RiskLevel.HIGH -> R.color.risk_high
                RiskLevel.CRITICAL -> R.color.risk_critical
            }
        )

        // Format alert details
        val time = android.text.format.DateFormat.format("MMM dd, HH:mm", alert.receivedAtMillis)
        val location = "%.4f, %.4f".format(alert.latitude, alert.longitude)
        val radius = "%.0f meters".format(alert.radiusMeters)
        
        val details = buildString {
            append("📍 $location\n")
            append("📏 Radius: $radius\n")
            append("🕒 $time\n")
            if (!alert.message.isNullOrBlank()) {
                append("\n${alert.message}")
            }
        }
        
        binding.alertDetails.text = details
    }

    private fun dismissAlert() {
        binding.alertCard.visibility = View.GONE
        binding.map.overlays.clear()
        binding.map.invalidate()
        currentAlert = null
        
        // Show empty state if no alerts
        binding.emptyState.visibility = View.VISIBLE
    }

    private fun showBroadcastDialog() {
        val dialogBinding = DialogBroadcastAlertBinding.inflate(layoutInflater)
        
        // Set dialog title based on authority
        val currentAuthority = authorityManager.getAuthority()
        dialogBinding.dialogTitle.text = if (currentAuthority == Authority.ADMIN) {
            getString(R.string.broadcast_alert_title)
        } else {
            getString(R.string.sos_alert_title)
        }
        
        // Set default values for SOS
        if (currentAuthority == Authority.USER) {
            dialogBinding.radiusInput.setText("500") // Default 500m radius for SOS
            dialogBinding.chipCritical.isChecked = true // Default to critical for SOS
        } else {
            dialogBinding.chipLow.isChecked = true // Default to low for admin broadcasts
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        // Setup button listeners
        dialogBinding.cancelBtn.setOnClickListener { dialog.dismiss() }
        
        dialogBinding.useLocationBtn.setOnClickListener {
            getCurrentLocation(dialogBinding.latitudeInput, dialogBinding.longitudeInput)
        }
        
        dialogBinding.sendBtn.setOnClickListener {
            if (validateAndSendAlert(dialogBinding)) {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun validateAndSendAlert(dialogBinding: DialogBroadcastAlertBinding): Boolean {
        try {
            val lat = dialogBinding.latitudeInput.text.toString().toDoubleOrNull()
            val lon = dialogBinding.longitudeInput.text.toString().toDoubleOrNull()
            val radius = dialogBinding.radiusInput.text.toString().toDoubleOrNull()
            
            if (lat == null || lon == null || radius == null) {
                showError("Please fill in all required fields")
                return false
            }
            
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                showError("Invalid coordinates")
                return false
            }
            
            if (radius <= 0 || radius > 50000) {
                showError("Radius must be between 1 and 50,000 meters")
                return false
            }

            // Get selected risk level
            val risk = when (dialogBinding.riskLevelChipGroup.checkedChipId) {
                R.id.chipLow -> RiskLevel.LOW
                R.id.chipMedium -> RiskLevel.MEDIUM
                R.id.chipHigh -> RiskLevel.HIGH
                R.id.chipCritical -> RiskLevel.CRITICAL
                else -> RiskLevel.LOW
            }

            val currentAuthority = authorityManager.getAuthority()
            val alert = DisasterAlert(
                alertId = "${currentAuthority.name.lowercase()}-${System.currentTimeMillis()}",
                latitude = lat,
                longitude = lon,
                radiusMeters = radius,
                levelOfRisk = risk,
                message = dialogBinding.messageInput.text.toString().ifBlank { null },
                receivedAtMillis = System.currentTimeMillis(),
                senderAuthority = currentAuthority,
                targetAuthority = if (currentAuthority == Authority.ADMIN) Authority.ALL else Authority.ADMIN
            )

            // Show alert on map
            showAlert(alert)
            
            // Send to mesh network
            sendAlertToMesh(alert)
            
            // Show success message
            val message = if (currentAuthority == Authority.ADMIN) {
                "Alert broadcasted to mesh network"
            } else {
                "SOS signal sent to emergency responders"
            }
            
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                .setAction("View") { 
                    // Alert is already shown, just scroll to it
                }
                .show()
            
            return true
            
        } catch (e: Exception) {
            showError("Error creating alert: ${e.message}")
            return false
        }
    }

    private fun sendAlertToMesh(alert: DisasterAlert) {
        val intent = android.content.Intent(requireContext(), com.example.disastermesh.mesh.MeshService::class.java).apply {
            action = com.example.disastermesh.mesh.MeshService.ACTION_RELAY_ALERT
            putExtra(com.example.disastermesh.mesh.MeshService.EXTRA_ALERT, alert)
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
    
    private fun getCurrentLocation(latInput: com.google.android.material.textfield.TextInputEditText, lonInput: com.google.android.material.textfield.TextInputEditText) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(binding.root, getString(R.string.location_permission_required), Snackbar.LENGTH_LONG).show()
            return
        }
        
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                latInput.setText(location.latitude.toString())
                lonInput.setText(location.longitude.toString())
                Snackbar.make(binding.root, getString(R.string.location_updated), Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(binding.root, getString(R.string.location_unavailable), Snackbar.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Snackbar.make(binding.root, "Failed to get location", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showLogsDialog() {
        val lines = com.example.disastermesh.logging.AlertLog.readAll(requireContext(), 300)
        val message = if (lines.isEmpty()) {
            getString(R.string.no_logs_message)
        } else {
            lines.joinToString("\n")
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.logs_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }

    private fun showMeshStatusDialog() {
        val statusMessage = buildString {
            append("Mesh Network Status\n\n")
            append("• Broadcasting: Active\n")
            append("• Discovery: Active\n")
            append("• Connected Devices: 0\n")
            append("• Last Activity: Just now\n\n")
            append("The mesh network allows emergency alerts to be shared between nearby devices even without internet connectivity.")
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Network Status")
            .setMessage(statusMessage)
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }
    
    private fun updateRoleDisplay() {
        val currentRole = authorityManager.getAuthority()
        
        // Update role chip
        binding.roleChip.text = when (currentRole) {
            Authority.ADMIN -> getString(R.string.role_admin)
            Authority.USER -> getString(R.string.role_user)
            else -> getString(R.string.role_user)
        }
        
        binding.roleChip.setChipBackgroundColorResource(
            if (currentRole == Authority.ADMIN) R.color.authority_admin else R.color.authority_user
        )
        
        // Update switch role button
        binding.switchRoleBtn.text = if (currentRole == Authority.ADMIN) {
            getString(R.string.switch_to_user)
        } else {
            getString(R.string.switch_to_admin)
        }
        
        // Update main action button
        updateMainActionButton()
    }
    
    private fun updateMainActionButton() {
        val currentRole = authorityManager.getAuthority()
        if (currentRole == Authority.ADMIN) {
            binding.mainActionFab.text = getString(R.string.broadcast_alert)
            binding.mainActionFab.setIconResource(R.drawable.ic_emergency_24)
            binding.mainActionFab.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.authority_admin)
        } else {
            binding.mainActionFab.text = getString(R.string.send_sos)
            binding.mainActionFab.setIconResource(R.drawable.ic_emergency_24)
            binding.mainActionFab.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.md_theme_light_error)
        }
    }

    fun updateMeshStatus(status: String, connectedDevices: Int = 0) {
        val statusText = when {
            connectedDevices > 0 -> getString(R.string.mesh_status_connected, connectedDevices)
            status.contains("discovering", ignoreCase = true) -> getString(R.string.mesh_status_discovering)
            status.contains("broadcasting", ignoreCase = true) -> getString(R.string.mesh_status_broadcasting)
            status.contains("offline", ignoreCase = true) -> getString(R.string.mesh_status_offline)
            else -> getString(R.string.mesh_status_starting)
        }
        
        binding.meshStatusChip.text = statusText
        
        // Update chip color based on status
        val colorRes = when {
            connectedDevices > 0 -> R.color.mesh_connected
            status.contains("offline", ignoreCase = true) -> R.color.mesh_disconnected
            status.contains("error", ignoreCase = true) -> R.color.mesh_error
            else -> R.color.mesh_connecting
        }
        
        binding.meshStatusChip.setChipBackgroundColorResource(colorRes)
    }

    // Public method to refresh location when permissions are granted
    fun refreshCurrentLocation() {
        android.util.Log.d("DisasterMesh", "refreshCurrentLocation called")
        getCurrentLocationForMap()
    }

    // Secure admin password verification
    private fun verifyAdminPassword(enteredPassword: String): Boolean {
        // TODO: Replace with secure authentication method
        // For now, using a more complex password - CHANGE THIS IN PRODUCTION!
        val adminPassword = "DisasterMesh2024!Emergency"
        
        // In production, use:
        // 1. Hashed passwords with bcrypt
        // 2. Android Keystore for secure storage
        // 3. Time-based codes for emergency access
        // 4. Certificate-based authentication
        
        return enteredPassword == adminPassword
    }

    // Debug method to check location services status
    private fun debugLocationServices() {
        android.util.Log.d("DisasterMesh", "=== LOCATION DEBUG INFO ===")
        
        // Check permissions
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            requireContext(), 
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            requireContext(), 
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        android.util.Log.d("DisasterMesh", "Fine location permission: $fineLocationPermission")
        android.util.Log.d("DisasterMesh", "Coarse location permission: $coarseLocationPermission")
        
        // Check if location services are enabled
        val locationManager = requireContext().getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
        val networkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        
        android.util.Log.d("DisasterMesh", "GPS enabled: $gpsEnabled")
        android.util.Log.d("DisasterMesh", "Network location enabled: $networkEnabled")
        
        // Check map state
        val mapCenter = binding.map.mapCenter
        android.util.Log.d("DisasterMesh", "Current map center: ${mapCenter.latitude}, ${mapCenter.longitude}")
        android.util.Log.d("DisasterMesh", "Current map zoom: ${binding.map.zoomLevelDouble}")
        
        android.util.Log.d("DisasterMesh", "=== END DEBUG INFO ===")
        
        // Show debug info to user
        val debugInfo = buildString {
            append("Location Debug:\n")
            append("Fine permission: $fineLocationPermission\n")
            append("GPS enabled: $gpsEnabled\n")
            append("Network enabled: $networkEnabled\n")
            append("Map center: ${mapCenter.latitude}, ${mapCenter.longitude}")
        }
        
        Snackbar.make(binding.root, debugInfo, Snackbar.LENGTH_LONG).show()
    }

    // Test method to verify map centering works (only for debugging)
    private fun testMapCentering() {
        android.util.Log.d("DisasterMesh", "Testing map centering with known location")
        val testLocation = GeoPoint(40.7128, -74.0060) // New York City
        
        try {
            // Clear existing overlays first
            binding.map.overlays.clear()
            
            binding.map.controller.setCenter(testLocation)
            android.util.Log.d("DisasterMesh", "Map center set to: ${testLocation.latitude}, ${testLocation.longitude}")
            
            // Add test marker
            val marker = org.osmdroid.views.overlay.Marker(binding.map)
            marker.position = testLocation
            marker.title = "Test Location"
            marker.snippet = "NYC Test"
            binding.map.overlays.add(marker)
            binding.map.invalidate()
            
            Snackbar.make(binding.root, "Test: Map centered on NYC", Snackbar.LENGTH_SHORT).show()
            android.util.Log.d("DisasterMesh", "Test centering completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("DisasterMesh", "Error in test centering: ${e.message}")
            Snackbar.make(binding.root, "Test failed: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }
    
    private fun switchRole() {
        val currentRole = authorityManager.getAuthority()
        
        if (currentRole == Authority.ADMIN) {
            // Switch from ADMIN to USER (no password needed)
            authorityManager.setAuthority(Authority.USER)
            updateRoleDisplay()
            Snackbar.make(binding.root, getString(R.string.role_switched_user), Snackbar.LENGTH_SHORT).show()
        } else {
            // Switch from USER to ADMIN (password required)
            showAdminPasswordDialog()
        }
    }
    
    private fun showAdminPasswordDialog() {
        val passwordInput = com.google.android.material.textfield.TextInputEditText(requireContext()).apply {
            hint = getString(R.string.admin_password_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        
        val inputLayout = com.google.android.material.textfield.TextInputLayout(requireContext()).apply {
            addView(passwordInput)
            boxStrokeColor = ContextCompat.getColor(context, R.color.md_theme_light_primary)
            hintTextColor = ContextCompat.getColorStateList(context, R.color.md_theme_light_primary)
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Admin Access")
            .setMessage(getString(R.string.admin_password_prompt))
            .setView(inputLayout)
            .setPositiveButton("Login") { _, _ ->
                val enteredPassword = passwordInput.text.toString()
                if (verifyAdminPassword(enteredPassword)) {
                    authorityManager.setAuthority(Authority.ADMIN)
                    updateRoleDisplay()
                    Snackbar.make(binding.root, getString(R.string.role_switched_admin), Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(binding.root, getString(R.string.invalid_password), Snackbar.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun createCircle(center: GeoPoint, radiusMeters: Double): Polygon {
        val circle = Polygon()
        val points = ArrayList<GeoPoint>(72)
        val earthRadius = 6378137.0
        val lat = Math.toRadians(center.latitude)
        val lon = Math.toRadians(center.longitude)
        val d = radiusMeters / earthRadius
        for (i in 0..71) {
            val brng = Math.toRadians(i * 5.0)
            val lat2 = Math.asin(
                Math.sin(lat) * Math.cos(d) + Math.cos(lat) * Math.sin(d) * Math.sin(brng)
            )
            val lon2 = lon + Math.atan2(
                Math.cos(lat) * Math.sin(d) * Math.cos(brng),
                Math.cos(d) - Math.sin(lat) * Math.sin(lat2)
            )
            points.add(GeoPoint(Math.toDegrees(lat2), Math.toDegrees(lon2)))
        }
        circle.points = points
        return circle
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


