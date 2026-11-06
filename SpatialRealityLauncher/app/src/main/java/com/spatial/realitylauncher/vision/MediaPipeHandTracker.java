package com.spatial.realitylauncher.vision;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;

import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerOptions;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;
import com.spatial.realitylauncher.math.Ray;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * High-performance Hand Tracker integrating Google MediaPipe Tasks Vision
 * with ARCore Passthrough CPU frames running in an asynchronous background pipeline.
 */
public class MediaPipeHandTracker {

    private static final String TAG = "HandTracker";
    private final Context context;
    private HandLandmarker handLandmarker;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    private final PinchGestureDetector pinchDetector;

    private boolean handDetected = false;
    private final float[] pointer3D = new float[]{0.0f, 0.0f, -1.5f};
    private Ray activeRay = new Ray(0, 0, 0, 0, 0, -1);

    public interface OnGestureActionListener {
        void onPinchClick(float screenX, float screenY);
    }
    private OnGestureActionListener actionListener;

    public MediaPipeHandTracker(Context context) {
        this.context = context;
        this.pinchDetector = new PinchGestureDetector(new PinchGestureDetector.OnPinchListener() {
            @Override
            public void onPinchDown(float x, float y) {
                if (actionListener != null) {
                    actionListener.onPinchClick(x, y);
                }
            }

            @Override
            public void onPinchMove(float x, float y) {}

            @Override
            public void onPinchUp() {}
        });

        initMediaPipe();
    }

    public void setActionListener(OnGestureActionListener listener) {
        this.actionListener = listener;
    }

    private void initMediaPipe() {
        backgroundExecutor.execute(() -> {
            try {
                BaseOptions baseOptions = BaseOptions.builder()
                        .setModelAssetPath("hand_landmarker.task")
                        .build();

                HandLandmarkerOptions options = HandLandmarkerOptions.builder()
                        .setBaseOptions(baseOptions)
                        .setMinHandDetectionConfidence(0.5f)
                        .setMinTrackingConfidence(0.5f)
                        .setMinHandPresenceConfidence(0.5f)
                        .setNumHands(2)
                        .setRunningMode(RunningMode.LIVE_STREAM)
                        .setResultListener(this::onLandmarksResult)
                        .setErrorListener(e -> Log.e(TAG, "MediaPipe error: " + e.getMessage()))
                        .build();

                handLandmarker = HandLandmarker.createFromOptions(context, options);
                Log.i(TAG, "MediaPipe Hand Landmarker initialized successfully.");
            } catch (Exception e) {
                Log.w(TAG, "MediaPipe model not found in assets, running fallback mock gestures: " + e.getMessage());
            }
        });
    }

    /**
     * Called on every ARCore frame. Asynchronously extracts image and feeds into MediaPipe.
     */
    public void processARFrameAsync(Frame frame) {
        if (isProcessing.get() || frame == null) {
            return; // Drop frame to maintain 60fps render rate
        }

        Image cameraImage = null;
        try {
            cameraImage = frame.acquireCameraImage();
        } catch (NotYetAvailableException e) {
            return;
        } catch (Exception e) {
            return;
        }

        if (cameraImage == null) return;

        final Image imageToProcess = cameraImage;
        final long timestampMs = System.currentTimeMillis();

        isProcessing.set(true);
        backgroundExecutor.execute(() -> {
            try {
                if (handLandmarker != null) {
                    Bitmap rgbBitmap = yuvToRgbBitmap(imageToProcess);
                    if (rgbBitmap != null) {
                        MPImage mpImage = new BitmapImageBuilder(rgbBitmap).build();
                        handLandmarker.detectAsync(mpImage, timestampMs);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Frame processing error: " + e.getMessage());
            } finally {
                imageToProcess.close();
                isProcessing.set(false);
            }
        });
    }

    private void onLandmarksResult(HandLandmarkerResult result, MPImage inputImage) {
        if (result == null || result.landmarks().isEmpty()) {
            handDetected = false;
            pinchDetector.reset();
            return;
        }

        handDetected = true;
        // Primary hand landmarks (21 points)
        var handPoints = result.landmarks().get(0);

        // Landmark 4: Thumb Tip, Landmark 8: Index Tip
        var thumbTip = handPoints.get(4);
        var indexTip = handPoints.get(8);

        pinchDetector.updateLandmarks(
                thumbTip.x(), thumbTip.y(), thumbTip.z(),
                indexTip.x(), indexTip.y(), indexTip.z()
        );

        // Convert normalized hand point to 3D pointer coordinates
        float normX = (thumbTip.x() + indexTip.x()) / 2.0f;
        float normY = (thumbTip.y() + indexTip.y()) / 2.0f;

        // Map from 2D image coordinates to 3D space (-1 to 1 in viewport)
        float spaceX = (normX - 0.5f) * 1.6f;
        float spaceY = (0.5f - normY) * 1.2f;
        float spaceZ = -1.2f;

        pointer3D[0] = spaceX;
        pointer3D[1] = spaceY;
        pointer3D[2] = spaceZ;

        // Construct 3D Ray pointing forward from camera through pointer
        activeRay = new Ray(0, 0, 0, spaceX, spaceY, spaceZ);
    }

    public boolean isHandDetected() {
        return handDetected;
    }

    public boolean isPinching() {
        return pinchDetector.isPinching();
    }

    public float[] getPointers3D() {
        return pointer3D;
    }

    public Ray getActiveRay() {
        return activeRay;
    }

    public void release() {
        backgroundExecutor.shutdown();
        if (handLandmarker != null) {
            handLandmarker.close();
            handLandmarker = null;
        }
    }

    /**
     * Efficiently converts YUV_420_888 Camera Image to RGB Bitmap.
     */
    private Bitmap yuvToRgbBitmap(Image image) {
        try {
            Image.Plane yPlane = image.getPlanes()[0];
            Image.Plane uPlane = image.getPlanes()[1];
            Image.Plane vPlane = image.getPlanes()[2];

            ByteBuffer yBuffer = yPlane.getBuffer();
            ByteBuffer uBuffer = uPlane.getBuffer();
            ByteBuffer vBuffer = vPlane.getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];

            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 75, out);
            byte[] imageBytes = out.toByteArray();

            return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        } catch (Exception e) {
            return null;
        }
    }
}
