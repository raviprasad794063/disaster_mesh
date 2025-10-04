# 📱 DisasterMesh APK Build Guide

## 🎉 **APK Files Created Successfully!**

Your DisasterMesh app has been built into APK files:

### 📍 **APK Locations:**

#### **Debug APK (Ready to Install)**
- **File**: `app/build/outputs/apk/debug/app-debug.apk`
- **Size**: ~15-20 MB (estimated)
- **Purpose**: Testing and development
- **Installation**: Can be installed directly on any Android device
- **Signing**: Automatically signed with debug certificate

#### **Release APK (Unsigned)**
- **File**: `app/build/outputs/apk/release/app-release-unsigned.apk`
- **Size**: ~10-15 MB (estimated, optimized)
- **Purpose**: Production distribution (needs signing)
- **Installation**: Cannot be installed until signed
- **Optimization**: Code is minified and optimized

## 🚀 **Quick Installation Guide**

### **For Testing (Debug APK)**

1. **Copy the debug APK** to your Android device
2. **Enable "Install from Unknown Sources"** in device settings
3. **Tap the APK file** to install
4. **Grant permissions** when prompted (Location, Bluetooth, etc.)
5. **Launch DisasterMesh** from app drawer

### **Installation Steps by Android Version:**

#### **Android 8.0+ (API 26+)**
1. Settings → Apps & notifications → Special app access → Install unknown apps
2. Select your file manager → Allow from this source
3. Install the APK

#### **Android 7.1 and below**
1. Settings → Security → Unknown sources → Enable
2. Install the APK

## 📦 **Build Commands Reference**

### **Debug Build (For Testing)**
```bash
# Build debug APK
./gradlew assembleDebug

# Clean and build
./gradlew clean assembleDebug

# Build and install on connected device
./gradlew installDebug
```

### **Release Build (For Distribution)**
```bash
# Build unsigned release APK
./gradlew assembleRelease

# Build signed release APK (requires signing config)
./gradlew assembleRelease --signing-config

# Build App Bundle for Google Play
./gradlew bundleRelease
```

## 🔐 **Signing Release APK (Optional)**

To create a signed release APK for distribution:

### **Step 1: Create Keystore**
```bash
keytool -genkey -v -keystore disastermesh.keystore -alias disastermesh -keyalg RSA -keysize 2048 -validity 10000
```

### **Step 2: Configure Signing**
Create `signing.properties` file:
```properties
STORE_FILE=disastermesh.keystore
STORE_PASSWORD=your_keystore_password
KEY_ALIAS=disastermesh
KEY_PASSWORD=your_key_password
```

### **Step 3: Update build.gradle**
Uncomment the signing configuration in `app/build.gradle`

### **Step 4: Build Signed APK**
```bash
./gradlew assembleRelease
```

## 📊 **APK Information**

### **Debug APK Features:**
- ✅ **Installable immediately**
- ✅ **Full debugging enabled**
- ✅ **All features functional**
- ❌ **Larger file size**
- ❌ **Not optimized**
- ❌ **Debug certificate (not for distribution)**

### **Release APK Features:**
- ✅ **Optimized and minified**
- ✅ **Smaller file size**
- ✅ **Production-ready**
- ❌ **Needs signing for installation**
- ❌ **No debugging info**

## 🎯 **Recommended APK for Different Uses**

### **For Personal Testing:**
- Use **debug APK** (`app-debug.apk`)
- Easy to install and test
- Full functionality available

### **For Sharing with Friends:**
- Use **debug APK** for now
- Quick to share and install
- No signing complexity

### **For Public Distribution:**
- Create **signed release APK**
- Upload to GitHub releases
- Submit to Google Play Store

### **For Emergency Deployment:**
- Keep both **debug and release** APKs ready
- Debug APK for immediate deployment
- Release APK for official distribution

## 📱 **Testing Your APK**

### **Essential Tests:**
- [ ] **App launches** without crashes
- [ ] **Location detection** works
- [ ] **Map displays** correctly
- [ ] **Permissions** are requested properly
- [ ] **Mesh networking** initializes
- [ ] **Emergency alerts** can be created
- [ ] **Role switching** functions
- [ ] **UI themes** work (light/dark)

### **Device Compatibility:**
- [ ] **Android 7.0+** (minimum supported)
- [ ] **Different screen sizes**
- [ ] **Various manufacturers** (Samsung, Google, etc.)
- [ ] **Different Android versions**

## 🌐 **Sharing Your APK**

### **GitHub Release (Recommended)**
1. Go to your GitHub repository
2. Click "Releases" → "Create a new release"
3. Tag version: `v1.0.0`
4. Upload your APK file
5. Write release notes
6. Publish release

### **Direct Sharing**
- **Google Drive** - Upload and share link
- **Dropbox** - Upload and share link
- **Email** - Attach APK file (if under 25MB)
- **USB Transfer** - Copy directly to device

### **QR Code Sharing**
Generate QR code for download link:
- Use online QR generators
- Point to GitHub release download
- Easy scanning for mobile users

## ⚠️ **Important Notes**

### **Security Considerations:**
- **Debug APKs** are signed with a debug certificate
- **Don't distribute debug APKs** publicly for security reasons
- **Always use signed release APKs** for public distribution
- **Verify APK integrity** before installation

### **Installation Requirements:**
- **Android 7.0+** (API level 24)
- **~50MB free space** for installation
- **Location services** enabled for full functionality
- **Bluetooth** enabled for mesh networking
- **Google Play Services** installed

### **Permissions Needed:**
- 📍 **Location** (Fine and Coarse)
- 📶 **Bluetooth** (Scan, Advertise, Connect)
- 📱 **Nearby WiFi Devices** (Android 13+)
- 🔔 **Notifications** (Android 13+)

## 🆘 **Emergency Distribution**

For disaster response scenarios:
- **Keep APKs offline** on multiple devices
- **Test installation** on various devices beforehand
- **Document installation steps** for non-technical users
- **Prepare multiple distribution methods**
- **Have backup copies** in different locations

---

**Your DisasterMesh APK is ready for testing and distribution!** 🚨📱

**Debug APK Location**: `app/build/outputs/apk/debug/app-debug.apk`
**File Size**: Check the actual file for exact size
**Ready to Install**: Yes, immediately on any Android 7.0+ device