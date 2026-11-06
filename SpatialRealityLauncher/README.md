# Spatial Reality Launcher (Meta Quest 3 & Apple Vision Pro Experience for Android)

A complete, production-ready Spatial Computing Launcher written in **Java** for **Android Studio**, utilizing **Google ARCore 6DoF SLAM**, **Google MediaPipe Tasks Vision**, and a custom **OpenGL ES 3.0 Stereoscopic Pipeline**.

---

## 🌟 Key Architecture & Capabilities

1. **Stereo VR Passthrough & IPD Calibration:**
   - Real-time Passthrough camera background using ARCore's hardware OES texture.
   - Dual-viewport Side-by-Side (SBS) stereoscopic projection designed for Google Cardboard / VR Box headsets.
   - **Interactive IPD Calibration Slider (0 to 100)**: Shifts left and right eye projection viewports with sub-millimeter precision to eliminate eye strain and match your headset's interpupillary distance.
   - Instant toggle between **Fullscreen Mono** and **Stereo VR SBS** modes via the floating Control Center sidebar.

2. **Advanced Hand Tracking & Micro-Gesture Control:**
   - Powered by **Google MediaPipe Tasks Vision (Hand Landmarker)** tracking all 21 3D joint landmarks.
   - Non-blocking asynchronous frame pipeline: Camera images are extracted via `Frame.acquireCameraImage()` and processed on a background worker thread, maintaining a locked 60 FPS rendering rate.
   - **Pinch Gesture Detector**: Calculates 3D Euclidean distances between the thumb tip (Landmark 4) and index finger tip (Landmark 8) with exponential moving average smoothing and hysteresis to eliminate jitter.
   - **Dynamic 3D Reticle / Cursor**: Floating glowing cursor in 3D space with VisionOS-style contraction and color shifts upon pinching.

3. **6DoF Spatial Anchoring & Relocalization:**
   - Native integration with **ARCore Spatial Anchors**.
   - Floating panels are locked to physical coordinates $(X, Y, Z)$ in your room.
   - Full 6DoF mobility: You can walk through your room, inspect windows from any angle or distance, walk away and return — windows remain anchored exactly where you left them.

4. **App Drawer & Dynamic In-App Spatial Windows:**
   - **App Drawer Library**: Queries `PackageManager` to retrieve all installed apps and generates a high-contrast glassmorphic 3D grid. Pinching any app launches it via its Android `Intent`.
   - **In-App Spatial Windows**: Spawn multiple persistent floating windows:
     - 🌐 **3D Web Browser Window**
     - 📝 **3D Spatial Notes Window**
     - 🎬 **3D Cinema / Media Screen**

---

## 🚀 How to Open and Run in Android Studio

1. **Open the Project:**
   - Launch **Android Studio (Giraffe, Hedgehog, Iguana, or newer)**.
   - Select **File -> Open...** and browse to:
     `C:\Users\jawad\.gemini\antigravity\scratch\SpatialRealityLauncher`
   - Wait for Gradle to sync dependencies.

2. **Download MediaPipe Hand Model:**
   - Download the official `hand_landmarker.task` file from Google MediaPipe:
     `https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task`
   - Place the file inside:
     `app/src/main/assets/hand_landmarker.task`

3. **Deploy to Device:**
   - Connect an ARCore-compatible Android phone via USB debugging.
   - Ensure **Google Play Services for AR** (ARCore) is installed on your phone.
   - Click **Run (Shift + F10)**.
   - Slide your phone into your VR headset holder (VR Box / Cardboard) and calibrate the IPD slider!
