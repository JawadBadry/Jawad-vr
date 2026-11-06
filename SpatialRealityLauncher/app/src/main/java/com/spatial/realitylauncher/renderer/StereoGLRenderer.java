package com.spatial.realitylauncher.renderer;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.spatial.realitylauncher.math.Matrix4f;
import com.spatial.realitylauncher.spatial.SpatialWindow;
import com.spatial.realitylauncher.spatial.SpatialWindowManager;
import com.spatial.realitylauncher.vision.MediaPipeHandTracker;

import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Master OpenGL ES 3.0 Stereoscopic Renderer.
 * Supports Fullscreen Mono mode and Stereo VR (SBS) mode with real-time
 * IPD (Interpupillary Distance) calibration and 6DoF ARCore Spatial Anchoring.
 */
public class StereoGLRenderer implements GLSurfaceView.Renderer {

    private final Context context;
    private Session arSession;
    private SpatialWindowManager windowManager;
    private MediaPipeHandTracker handTracker;

    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final SpatialPanelRenderer panelRenderer = new SpatialPanelRenderer();
    private final ReticleRenderer reticleRenderer = new ReticleRenderer();

    // Viewport dimensions
    private int surfaceWidth = 1920;
    private int surfaceHeight = 1080;

    // View mode: true = Stereo SBS for VR Cardboard/Box, false = Fullscreen Mono
    private boolean isStereoMode = true;

    // IPD Slider value (0 to 100, default 50 = standard ~64mm human IPD)
    private int ipdSliderValue = 50;

    // Projection & View Matrices
    private final float[] baseProjectionMatrix = new float[16];
    private final float[] baseViewMatrix = new float[16];
    private final float[] eyeViewMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // Reticle 3D coordinates
    private float[] reticlePos = new float[]{0.0f, 0.0f, -1.5f};
    private boolean isPinching = false;

    public interface OnFrameUpdateListener {
        void onFrame(Frame frame);
    }
    private OnFrameUpdateListener frameListener;

    public StereoGLRenderer(Context context) {
        this.context = context;
    }

    public void setArSession(Session session) {
        this.arSession = session;
    }

    public void setWindowManager(SpatialWindowManager windowManager) {
        this.windowManager = windowManager;
    }

    public void setHandTracker(MediaPipeHandTracker handTracker) {
        this.handTracker = handTracker;
    }

    public void setFrameListener(OnFrameUpdateListener listener) {
        this.frameListener = listener;
    }

    public void setStereoMode(boolean stereo) {
        this.isStereoMode = stereo;
    }

    public void setIpdSliderValue(int value) {
        this.ipdSliderValue = Math.max(0, Math.min(100, value));
    }

    public int getCameraTextureId() {
        return backgroundRenderer.getCameraTextureId();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);

        backgroundRenderer.init();
        panelRenderer.init();
        reticleRenderer.init();

        if (arSession != null) {
            arSession.setCameraTextureName(backgroundRenderer.getCameraTextureId());
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;
        GLES30.glViewport(0, 0, width, height);

        if (arSession != null) {
            arSession.setDisplayGeometry(0, width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        if (arSession == null) return;

        Frame frame = null;
        try {
            frame = arSession.update();
        } catch (Exception e) {
            return;
        }

        if (frame == null) return;

        // Notify frame listener (for MediaPipe processing)
        if (frameListener != null) {
            frameListener.onFrame(frame);
        }

        Camera camera = frame.getCamera();
        if (camera.getTrackingState() != TrackingState.TRACKING) {
            return;
        }

        // Update passthrough background UVs
        backgroundRenderer.updateTexCoords(frame);

        // Fetch standard ARCore projection & view
        camera.getProjectionMatrix(baseProjectionMatrix, 0, 0.1f, 100.0f);
        camera.getViewMatrix(baseViewMatrix, 0);

        // Update hand tracking cursor if active
        updateHandCursor(frame);

        if (isStereoMode) {
            renderStereo(frame);
        } else {
            renderMono(frame);
        }
    }

    private void updateHandCursor(Frame frame) {
        if (handTracker != null && handTracker.isHandDetected()) {
            isPinching = handTracker.isPinching();
            float[] pointer = handTracker.getPointers3D();
            if (pointer != null) {
                reticlePos[0] = pointer[0];
                reticlePos[1] = pointer[1];
                reticlePos[2] = pointer[2];
            }
        } else {
            isPinching = false;
            // Default center reticle (1.5 meters ahead of camera)
            reticlePos[0] = 0.0f;
            reticlePos[1] = 0.0f;
            reticlePos[2] = -1.5f;
        }
    }

    /**
     * Renders standard single Fullscreen viewport.
     */
    private void renderMono(Frame frame) {
        GLES30.glViewport(0, 0, surfaceWidth, surfaceHeight);
        backgroundRenderer.draw();
        renderSpatialScene(baseViewMatrix, baseProjectionMatrix);
    }

    /**
     * Renders Side-by-Side (SBS) dual viewports with IPD separation.
     */
    private void renderStereo(Frame frame) {
        int halfWidth = surfaceWidth / 2;

        // Calculate IPD eye offset in meters:
        // Slider 50 = standard 64mm (0.064m / 2 = 0.032m each eye).
        // Range 0..100 maps to 40mm..80mm separation.
        float totalIpdMeters = 0.040f + (ipdSliderValue / 100.0f) * 0.040f;
        float halfIpd = totalIpdMeters / 2.0f;

        // LEFT EYE VIEWPORT
        GLES30.glViewport(0, 0, halfWidth, surfaceHeight);
        backgroundRenderer.draw();

        System.arraycopy(baseViewMatrix, 0, eyeViewMatrix, 0, 16);
        Matrix.translateM(eyeViewMatrix, 0, -halfIpd, 0.0f, 0.0f); // Shift camera left
        renderSpatialScene(eyeViewMatrix, baseProjectionMatrix);

        // RIGHT EYE VIEWPORT
        GLES30.glViewport(halfWidth, 0, halfWidth, surfaceHeight);
        backgroundRenderer.draw();

        System.arraycopy(baseViewMatrix, 0, eyeViewMatrix, 0, 16);
        Matrix.translateM(eyeViewMatrix, 0, +halfIpd, 0.0f, 0.0f); // Shift camera right
        renderSpatialScene(eyeViewMatrix, baseProjectionMatrix);
    }

    /**
     * Renders all anchored spatial windows and the 3D reticle.
     */
    private void renderSpatialScene(float[] viewMatrix, float[] projMatrix) {
        float[] viewProjMatrix = new float[16];
        Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0);

        // 1. Draw floating spatial windows
        if (windowManager != null) {
            List<SpatialWindow> windows = windowManager.getWindows();
            for (SpatialWindow window : windows) {
                if (!window.isVisible()) continue;

                float[] modelMatrix = window.getModelMatrix();
                Matrix.multiplyMM(mvpMatrix, 0, viewProjMatrix, 0, modelMatrix, 0);

                panelRenderer.draw(mvpMatrix, window.getTextureId(), 1.0f, window.isHovered());
            }
        }

        // 2. Draw 3D Reticle
        float[] reticleModel = new float[16];
        Matrix.setIdentityM(reticleModel, 0);
        Matrix.translateM(reticleModel, 0, reticlePos[0], reticlePos[1], reticlePos[2]);

        // Reticle scales down when pinching
        float scale = isPinching ? 0.035f : 0.055f;
        Matrix.scaleM(reticleModel, 0, scale, scale, scale);

        Matrix.multiplyMM(mvpMatrix, 0, viewProjMatrix, 0, reticleModel, 0);
        reticleRenderer.draw(mvpMatrix, isPinching);
    }
}
