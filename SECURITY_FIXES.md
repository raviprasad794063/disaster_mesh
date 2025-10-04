# 🔒 DisasterMesh Security Fixes

## 🚨 CRITICAL: Admin Password Security

### Current Vulnerability
```kotlin
// INSECURE - Hardcoded password
if (enteredPassword == "admin123") {
    authorityManager.setAuthority(Authority.ADMIN)
}
```

### Secure Solution 1: Hashed Password
```kotlin
// Add to build.gradle dependencies
implementation 'org.mindrot:jbcrypt:0.4'

// In MapsFragment.kt
private fun verifyAdminPassword(enteredPassword: String): Boolean {
    // Store hashed password (generate this once)
    val hashedPassword = "$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqfuVDdXd/OZg7XrRnNy5F2" // bcrypt hash
    return BCrypt.checkpw(enteredPassword, hashedPassword)
}

// Usage
if (verifyAdminPassword(enteredPassword)) {
    authorityManager.setAuthority(Authority.ADMIN)
}
```

### Secure Solution 2: Android Keystore
```kotlin
// Store admin credentials securely
class SecureAdminAuth(private val context: Context) {
    private val keyAlias = "admin_auth_key"
    
    fun verifyAdmin(password: String): Boolean {
        val encryptedPassword = getEncryptedPassword()
        return decryptAndVerify(password, encryptedPassword)
    }
    
    private fun getEncryptedPassword(): String {
        // Retrieve from Android Keystore
        val sharedPrefs = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
        return sharedPrefs.getString("admin_hash", "") ?: ""
    }
}
```

### Secure Solution 3: Time-Based Codes
```kotlin
// Generate time-based admin codes for emergency use
class EmergencyAdminAuth {
    fun generateDailyCode(): String {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)
        
        // Generate secure daily code
        val seed = "DisasterMesh_Emergency_$year$dayOfYear"
        return MessageDigest.getInstance("SHA-256")
            .digest(seed.toByteArray())
            .take(6)
            .joinToString("") { "%02x".format(it) }
    }
}
```

## 🔐 Message Authentication

### Current Issue
- No verification of admin messages
- Potential for spoofed emergency alerts

### Solution: Digital Signatures
```kotlin
// Add message signing for admin broadcasts
class MessageSigner {
    fun signMessage(message: DisasterAlert, privateKey: PrivateKey): String {
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(message.toByteArray())
        return Base64.encodeToString(signature.sign(), Base64.DEFAULT)
    }
    
    fun verifyMessage(message: DisasterAlert, signature: String, publicKey: PublicKey): Boolean {
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initVerify(publicKey)
        sig.update(message.toByteArray())
        return sig.verify(Base64.decode(signature, Base64.DEFAULT))
    }
}
```

## 🛡️ Location Privacy Protection

### Current Issue
- Location data transmitted without user consent
- No encryption of location data

### Solution: Privacy Controls
```kotlin
class LocationPrivacyManager {
    fun requestLocationSharing(context: Context, callback: (Boolean) -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Share Location for Emergency?")
            .setMessage("Your location will be shared with nearby emergency responders. This helps coordinate rescue efforts.")
            .setPositiveButton("Share Location") { _, _ -> callback(true) }
            .setNegativeButton("Keep Private") { _, _ -> callback(false) }
            .show()
    }
    
    fun obfuscateLocation(location: GeoPoint, radiusMeters: Double): GeoPoint {
        // Add random offset for privacy while maintaining emergency utility
        val offsetLat = (Math.random() - 0.5) * 0.001 // ~100m offset
        val offsetLng = (Math.random() - 0.5) * 0.001
        return GeoPoint(location.latitude + offsetLat, location.longitude + offsetLng)
    }
}
```

## 🔒 Input Validation & Sanitization

### Current Issue
- Insufficient validation of alert data
- Potential for malformed data injection

### Solution: Comprehensive Validation
```kotlin
class AlertValidator {
    fun validateAlert(alert: DisasterAlert): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate coordinates
        if (alert.latitude < -90 || alert.latitude > 90) {
            errors.add("Invalid latitude")
        }
        if (alert.longitude < -180 || alert.longitude > 180) {
            errors.add("Invalid longitude")
        }
        
        // Validate radius
        if (alert.radiusMeters <= 0 || alert.radiusMeters > 50000) {
            errors.add("Invalid radius (must be 1-50000 meters)")
        }
        
        // Validate message content
        alert.message?.let { message ->
            if (message.length > 500) {
                errors.add("Message too long (max 500 characters)")
            }
            if (containsMaliciousContent(message)) {
                errors.add("Message contains prohibited content")
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    private fun containsMaliciousContent(message: String): Boolean {
        val prohibitedPatterns = listOf(
            "<script", "javascript:", "data:", "vbscript:",
            "onload=", "onerror=", "onclick="
        )
        return prohibitedPatterns.any { message.lowercase().contains(it) }
    }
}
```

## 🚫 Rate Limiting & Abuse Prevention

### Solution: Network Abuse Protection
```kotlin
class NetworkAbuseProtection {
    private val alertCounts = mutableMapOf<String, MutableList<Long>>()
    private val maxAlertsPerHour = 10
    private val maxAlertsPerMinute = 2
    
    fun canSendAlert(deviceId: String): Boolean {
        val now = System.currentTimeMillis()
        val deviceAlerts = alertCounts.getOrPut(deviceId) { mutableListOf() }
        
        // Remove old alerts (older than 1 hour)
        deviceAlerts.removeAll { now - it > 3600000 }
        
        // Check hourly limit
        if (deviceAlerts.size >= maxAlertsPerHour) {
            return false
        }
        
        // Check per-minute limit
        val recentAlerts = deviceAlerts.count { now - it < 60000 }
        if (recentAlerts >= maxAlertsPerMinute) {
            return false
        }
        
        // Record this alert
        deviceAlerts.add(now)
        return true
    }
}
```

## 🔐 Secure APK Distribution

### Current Issue
- Debug APK not suitable for production
- No integrity verification

### Solution: Signed Release Process
```bash
# 1. Generate release keystore
keytool -genkey -v -keystore disastermesh-release.keystore \
    -alias disastermesh -keyalg RSA -keysize 2048 -validity 10000

# 2. Configure signing in build.gradle
android {
    signingConfigs {
        release {
            storeFile file('disastermesh-release.keystore')
            storePassword System.getenv("KEYSTORE_PASSWORD")
            keyAlias "disastermesh"
            keyPassword System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}

# 3. Build signed APK
./gradlew assembleRelease

# 4. Verify APK signature
jarsigner -verify -verbose -certs app-release.apk
```

## 📱 Emergency Security Considerations

### Secure Emergency Deployment
```kotlin
class EmergencySecurityManager {
    // Generate emergency access codes for disaster scenarios
    fun generateEmergencyCode(location: GeoPoint, timestamp: Long): String {
        val locationHash = "${location.latitude.toInt()}_${location.longitude.toInt()}"
        val dayCode = (timestamp / 86400000).toString() // Day-based code
        return MessageDigest.getInstance("SHA-256")
            .digest("EMERGENCY_${locationHash}_$dayCode".toByteArray())
            .take(8)
            .joinToString("") { "%02x".format(it) }
    }
    
    // Verify emergency responder credentials
    fun verifyEmergencyResponder(credentials: String): Boolean {
        // Implement emergency responder verification
        // Could use pre-shared keys or certificate-based auth
        return verifyResponderCertificate(credentials)
    }
}
```

## 🔒 Implementation Priority

### Phase 1 (Immediate - Before Public Release)
1. ✅ Replace hardcoded admin password
2. ✅ Add input validation
3. ✅ Create signed release APK
4. ✅ Remove debug logging from release

### Phase 2 (Short Term - Within 1 Month)
1. ✅ Implement message signing
2. ✅ Add location privacy controls
3. ✅ Implement rate limiting
4. ✅ Add certificate pinning

### Phase 3 (Long Term - Future Versions)
1. ✅ End-to-end encryption
2. ✅ Advanced authentication methods
3. ✅ Audit logging
4. ✅ Security monitoring

---

**CRITICAL**: Do not distribute the current APK publicly until the hardcoded password is fixed!