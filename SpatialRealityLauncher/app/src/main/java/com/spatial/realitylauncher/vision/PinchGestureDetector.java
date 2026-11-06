package com.spatial.realitylauncher.vision;

/**
 * Robust pinch gesture detector with exponential moving average smoothing
 * and hysteresis thresholding to prevent click jitter.
 */
public class PinchGestureDetector {

    public interface OnPinchListener {
        void onPinchDown(float x, float y);
        void onPinchMove(float x, float y);
        void onPinchUp();
    }

    private final OnPinchListener listener;

    // Hysteresis thresholds in normalized coordinate space
    private static final float PINCH_DOWN_THRESHOLD = 0.055f;
    private static final float PINCH_UP_THRESHOLD = 0.080f;
    private static final float SMOOTHING_FACTOR = 0.65f;

    private boolean isPinching = false;
    private float smoothedX = 0.5f;
    private float smoothedY = 0.5f;

    public PinchGestureDetector(OnPinchListener listener) {
        this.listener = listener;
    }

    public boolean isPinching() {
        return isPinching;
    }

    /**
     * Updates detector with current positions of Thumb Tip (Landmark 4) and Index Tip (Landmark 8).
     */
    public void updateLandmarks(float thumbX, float thumbY, float thumbZ,
                                float indexX, float indexY, float indexZ) {

        float dx = thumbX - indexX;
        float dy = thumbY - indexY;
        float dz = thumbZ - indexZ;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Center point between fingertips
        float rawCenterX = (thumbX + indexX) / 2.0f;
        float rawCenterY = (thumbY + indexY) / 2.0f;

        // Apply low-pass smoothing
        smoothedX = smoothedX * (1.0f - SMOOTHING_FACTOR) + rawCenterX * SMOOTHING_FACTOR;
        smoothedY = smoothedY * (1.0f - SMOOTHING_FACTOR) + rawCenterY * SMOOTHING_FACTOR;

        if (!isPinching && distance < PINCH_DOWN_THRESHOLD) {
            isPinching = true;
            if (listener != null) {
                listener.onPinchDown(smoothedX, smoothedY);
            }
        } else if (isPinching && distance > PINCH_UP_THRESHOLD) {
            isPinching = false;
            if (listener != null) {
                listener.onPinchUp();
            }
        } else if (isPinching) {
            if (listener != null) {
                listener.onPinchMove(smoothedX, smoothedY);
            }
        }
    }

    public void reset() {
        if (isPinching && listener != null) {
            listener.onPinchUp();
        }
        isPinching = false;
    }
}
