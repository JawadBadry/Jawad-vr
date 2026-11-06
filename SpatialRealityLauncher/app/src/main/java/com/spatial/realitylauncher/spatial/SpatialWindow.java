package com.spatial.realitylauncher.spatial;

import android.graphics.Bitmap;
import android.opengl.Matrix;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.spatial.realitylauncher.math.Ray;
import com.spatial.realitylauncher.renderer.TextureHelper;

/**
 * Represents an individual 3D floating window panel anchored in the room (6DoF).
 */
public class SpatialWindow {

    private final String id;
    private final String title;
    private Anchor anchor;
    private Pose localPose;

    private float widthMeters = 0.8f;
    private float heightMeters = 0.5f;

    private int textureId = -1;
    private Bitmap currentBitmap;
    private boolean isVisible = true;
    private boolean isHovered = false;

    private final float[] modelMatrix = new float[16];

    public interface OnWindowClickListener {
        void onClick(SpatialWindow window, float normalizedU, float normalizedV);
    }
    private OnWindowClickListener clickListener;

    public SpatialWindow(String id, String title, float widthMeters, float heightMeters) {
        this.id = id;
        this.title = title;
        this.widthMeters = widthMeters;
        this.heightMeters = heightMeters;
        Matrix.setIdentityM(modelMatrix, 0);
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public boolean isVisible() { return isVisible; }
    public void setVisible(boolean visible) { this.isVisible = visible; }
    public boolean isHovered() { return isHovered; }
    public void setHovered(boolean hovered) { this.isHovered = hovered; }
    public int getTextureId() { return textureId; }
    public float getWidthMeters() { return widthMeters; }
    public float getHeightMeters() { return heightMeters; }

    public void setClickListener(OnWindowClickListener listener) {
        this.clickListener = listener;
    }

    public void setAnchor(Anchor anchor) {
        this.anchor = anchor;
    }

    public void setPose(Pose pose) {
        this.localPose = pose;
    }

    /**
     * Updates and returns the 4x4 Model Matrix using the ARCore Anchor or Pose.
     */
    public float[] getModelMatrix() {
        if (anchor != null && anchor.getTrackingState() == com.google.ar.core.TrackingState.TRACKING) {
            anchor.getPose().toMatrix(modelMatrix, 0);
        } else if (localPose != null) {
            localPose.toMatrix(modelMatrix, 0);
        }
        // Scale by window physical dimensions
        Matrix.scaleM(modelMatrix, 0, widthMeters, heightMeters, 1.0f);
        return modelMatrix;
    }

    public void setContentBitmap(Bitmap bitmap) {
        this.currentBitmap = bitmap;
        if (textureId > 0) {
            TextureHelper.updateTexture(textureId, bitmap);
        } else {
            textureId = TextureHelper.loadTextureFromBitmap(bitmap);
        }
    }

    /**
     * Intersects a 3D ray with this window's planar surface.
     * @return [u, v, distance] or null if not intersecting.
     */
    public float[] raycast(Ray ray) {
        float[] pos = new float[3];
        if (anchor != null && anchor.getTrackingState() == com.google.ar.core.TrackingState.TRACKING) {
            pos[0] = anchor.getPose().tx();
            pos[1] = anchor.getPose().ty();
            pos[2] = anchor.getPose().tz();
        } else if (localPose != null) {
            pos[0] = localPose.tx();
            pos[1] = localPose.ty();
            pos[2] = localPose.tz();
        } else {
            return null;
        }

        // Window forward normal (usually facing toward camera)
        float[] normal = new float[]{0.0f, 0.0f, 1.0f};
        return ray.intersectQuad(pos, normal, widthMeters / 2.0f, heightMeters / 2.0f);
    }

    public void performClick(float u, float v) {
        if (clickListener != null) {
            clickListener.onClick(this, u, v);
        }
    }
}
