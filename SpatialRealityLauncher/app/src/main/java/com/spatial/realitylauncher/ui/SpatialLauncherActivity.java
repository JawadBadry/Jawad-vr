package com.spatial.realitylauncher.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.ar.core.Anchor;
import com.spatial.realitylauncher.R;
import com.spatial.realitylauncher.launcher.AppDrawerManager;
import com.spatial.realitylauncher.renderer.StereoGLRenderer;
import com.spatial.realitylauncher.spatial.ARCoreSessionManager;
import com.spatial.realitylauncher.spatial.SpatialWindow;
import com.spatial.realitylauncher.spatial.SpatialWindowManager;
import com.spatial.realitylauncher.vision.MediaPipeHandTracker;
import com.spatial.realitylauncher.windows.WindowContentGenerator;

/**
 * Main Activity for the Spatial Reality Launcher.
 * Integrates 6DoF ARCore Passthrough, Side-by-Side Stereoscopic rendering with IPD adjustment,
 * MediaPipe hand & pinch gesture tracking, App Drawer launcher, and In-App floating 3D windows.
 */
public class SpatialLauncherActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 1001;

    private GLSurfaceView glSurfaceView;
    private View stereoCenterDivider;
    private MaterialButton btnToggleSidebar;
    private CardView sidebarPanel;
    private TextView tvTrackingHud;
    private RadioGroup rgViewMode;
    private RadioButton rbFullscreen;
    private RadioButton rbStereoVR;
    private SeekBar sbIpd;
    private TextView tvIpdLabel;

    private MaterialButton btnSpawnBrowser;
    private MaterialButton btnSpawnNotes;
    private MaterialButton btnSpawnMedia;
    private MaterialButton btnResetAnchors;

    // Subsystems
    private ARCoreSessionManager arSessionManager;
    private StereoGLRenderer glRenderer;
    private SpatialWindowManager windowManager;
    private MediaPipeHandTracker handTracker;
    private AppDrawerManager appDrawerManager;

    // Primary Spatial Windows
    private SpatialWindow appDrawerWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on & immersive VR fullscreen mode
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUI();

        setContentView(R.layout.activity_spatial_launcher);

        initViews();
        initSubsystems();
        setupSidebarControls();

        checkCameraPermission();
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private void initViews() {
        glSurfaceView = findViewById(R.id.glSurfaceView);
        stereoCenterDivider = findViewById(R.id.stereoCenterDivider);
        btnToggleSidebar = findViewById(R.id.btnToggleSidebar);
        sidebarPanel = findViewById(R.id.sidebarPanel);
        tvTrackingHud = findViewById(R.id.tvTrackingHud);
        rgViewMode = findViewById(R.id.rgViewMode);
        rbFullscreen = findViewById(R.id.rbFullscreen);
        rbStereoVR = findViewById(R.id.rbStereoVR);
        sbIpd = findViewById(R.id.sbIpd);
        tvIpdLabel = findViewById(R.id.tvIpdLabel);

        btnSpawnBrowser = findViewById(R.id.btnSpawnBrowser);
        btnSpawnNotes = findViewById(R.id.btnSpawnNotes);
        btnSpawnMedia = findViewById(R.id.btnSpawnMedia);
        btnResetAnchors = findViewById(R.id.btnResetAnchors);

        // Sidebar Collapse / Expand Toggle
        btnToggleSidebar.setOnClickListener(v -> {
            boolean isVisible = sidebarPanel.getVisibility() == View.VISIBLE;
            sidebarPanel.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            btnToggleSidebar.setText(isVisible ? "⚙ Open Controls" : "⚙ Close Controls");
        });
    }

    private void initSubsystems() {
        arSessionManager = new ARCoreSessionManager(this);
        windowManager = new SpatialWindowManager();
        handTracker = new MediaPipeHandTracker(this);
        appDrawerManager = new AppDrawerManager(this);

        glRenderer = new StereoGLRenderer(this);
        glRenderer.setWindowManager(windowManager);
        glRenderer.setHandTracker(handTracker);

        // Connect GL frame listener to Computer Vision Pipeline
        glRenderer.setFrameListener(frame -> {
            // Asynchronously process image for hand gestures
            handTracker.processARFrameAsync(frame);

            // Update spatial raycast selection
            windowManager.updateRaycast(handTracker.getActiveRay());

            // Update UI HUD status on main thread
            runOnUiThread(this::updateHudStatus);
        });

        // Set up pinch gesture click action
        handTracker.setActionListener((screenX, screenY) -> {
            runOnUiThread(() -> {
                boolean hit = windowManager.triggerPinchClick();
                if (hit) {
                    Toast.makeText(this, "Pinch Selected!", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // OpenGL ES 3.0 Surface View Configuration
        glSurfaceView.setEGLContextClientVersion(3);
        glSurfaceView.setRenderer(glRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    private void setupSidebarControls() {
        // 1. View Mode Switch: Fullscreen Mono vs Stereo VR (SBS)
        rgViewMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbFullscreen) {
                glRenderer.setStereoMode(false);
                stereoCenterDivider.setVisibility(View.GONE);
                Toast.makeText(this, "Mode: Fullscreen Single View", Toast.LENGTH_SHORT).show();
            } else if (checkedId == R.id.rbStereoVR) {
                glRenderer.setStereoMode(true);
                stereoCenterDivider.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Mode: Stereo VR Side-by-Side (Cardboard/Box)", Toast.LENGTH_SHORT).show();
            }
        });

        // 2. IPD Slider (0 to 100)
        sbIpd.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                glRenderer.setIpdSliderValue(progress);
                tvIpdLabel.setText(String.format("IPD Eye Separation: %d / 100", progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 3. Spawning In-App Floating Windows
        btnSpawnBrowser.setOnClickListener(v -> spawnBrowserWindow());
        btnSpawnNotes.setOnClickListener(v -> spawnNotesWindow());
        btnSpawnMedia.setOnClickListener(v -> spawnMediaWindow());

        // 4. Reset & Recenter Spatial Anchors
        btnResetAnchors.setOnClickListener(v -> recenterSpatialEnvironment());
    }

    private void recenterSpatialEnvironment() {
        windowManager.clearAll();

        // 1. Create Main App Drawer Window in front of user
        appDrawerWindow = new SpatialWindow("app_drawer", "App Library", 1.2f, 0.85f);
        Bitmap appGridBitmap = appDrawerManager.renderAppGridBitmap();
        appDrawerWindow.setContentBitmap(appGridBitmap);

        appDrawerWindow.setClickListener((window, u, v) -> {
            appDrawerManager.handleGridClick(u, v);
        });

        Anchor anchor = arSessionManager.createAnchorInFrontOfCamera(1.4f, 0.0f);
        if (anchor != null) {
            appDrawerWindow.setAnchor(anchor);
        } else {
            // Fallback pose if ARCore is warming up
            appDrawerWindow.setPose(com.google.ar.core.Pose.makeTranslation(0.0f, 0.0f, -1.4f));
        }

        windowManager.addWindow(appDrawerWindow);
        Toast.makeText(this, "Space Recentered & Anchored 6DoF", Toast.LENGTH_SHORT).show();
    }

    private void spawnBrowserWindow() {
        SpatialWindow browserWindow = new SpatialWindow("web_browser", "Web 3D", 1.1f, 0.7f);
        Bitmap browserBmp = WindowContentGenerator.createWebBrowserBitmap("https://metaverse.spatial.org");
        browserWindow.setContentBitmap(browserBmp);

        Anchor anchor = arSessionManager.createAnchorInFrontOfCamera(1.6f, 0.1f);
        if (anchor != null) {
            browserWindow.setAnchor(anchor);
        } else {
            browserWindow.setPose(com.google.ar.core.Pose.makeTranslation(-0.8f, 0.1f, -1.5f));
        }

        windowManager.addWindow(browserWindow);
        Toast.makeText(this, "3D Browser Window Anchored", Toast.LENGTH_SHORT).show();
    }

    private void spawnNotesWindow() {
        SpatialWindow notesWindow = new SpatialWindow("spatial_notes", "Notes", 0.85f, 0.65f);
        Bitmap notesBmp = WindowContentGenerator.createNotesBitmap();
        notesWindow.setContentBitmap(notesBmp);

        Anchor anchor = arSessionManager.createAnchorInFrontOfCamera(1.5f, -0.2f);
        if (anchor != null) {
            notesWindow.setAnchor(anchor);
        } else {
            notesWindow.setPose(com.google.ar.core.Pose.makeTranslation(0.8f, -0.1f, -1.5f));
        }

        windowManager.addWindow(notesWindow);
        Toast.makeText(this, "Spatial Notes Window Anchored", Toast.LENGTH_SHORT).show();
    }

    private void spawnMediaWindow() {
        SpatialWindow mediaWindow = new SpatialWindow("media_cinema", "Cinema 4K", 1.4f, 0.8f);
        Bitmap mediaBmp = WindowContentGenerator.createMediaCinemaBitmap();
        mediaWindow.setContentBitmap(mediaBmp);

        Anchor anchor = arSessionManager.createAnchorInFrontOfCamera(2.0f, 0.4f);
        if (anchor != null) {
            mediaWindow.setAnchor(anchor);
        } else {
            mediaWindow.setPose(com.google.ar.core.Pose.makeTranslation(0.0f, 0.5f, -2.0f));
        }

        windowManager.addWindow(mediaWindow);
        Toast.makeText(this, "Cinema Screen Anchored in Room", Toast.LENGTH_SHORT).show();
    }

    private void updateHudStatus() {
        boolean detected = handTracker.isHandDetected();
        boolean pinching = handTracker.isPinching();
        String status = String.format("6DoF Spatial: Anchored | Hand: %s | Pinch: %s",
                detected ? "Detected" : "Searching...",
                pinching ? "ACTIVE (CLICK)" : "Idle");
        tvTrackingHud.setText(status);
        tvTrackingHud.setTextColor(pinching ? 0xFF00E676 : (detected ? 0xFF00E5FF : 0xFFFFFFFF));
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                resumeAR();
            } else {
                Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            resumeAR();
        }
    }

    private void resumeAR() {
        if (arSessionManager.resume()) {
            glRenderer.setArSession(arSessionManager.getSession());
            glSurfaceView.onResume();

            // Spawn initial App Drawer after AR session is initialized
            glSurfaceView.postDelayed(this::recenterSpatialEnvironment, 1200);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
        arSessionManager.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handTracker.release();
        arSessionManager.destroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }
}
