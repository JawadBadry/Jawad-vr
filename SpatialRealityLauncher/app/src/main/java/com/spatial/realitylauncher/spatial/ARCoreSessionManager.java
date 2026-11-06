package com.spatial.realitylauncher.spatial;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;

/**
 * Manages Google ARCore Session lifecycle, 6DoF camera tracking,
 * auto-focus configuration, and spatial anchor creation in physical space.
 */
public class ARCoreSessionManager {

    private final Activity activity;
    private Session session;
    private boolean installRequested = false;

    public ARCoreSessionManager(Activity activity) {
        this.activity = activity;
    }

    public Session getSession() {
        return session;
    }

    public boolean resume() {
        if (session == null) {
            try {
                switch (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return false;
                    case INSTALLED:
                        break;
                }

                session = new Session(activity);
                Config config = new Config(session);
                config.setFocusMode(Config.FocusMode.AUTO);
                config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
                config.setLightEstimationMode(Config.LightEstimationMode.DISABLED);
                session.configure(config);

            } catch (UnavailableArcoreNotInstalledException | UnavailableDeviceNotCompatibleException
                     | UnavailableApkTooOldException | UnavailableSdkTooOldException e) {
                Toast.makeText(activity, "ARCore error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return false;
            } catch (Exception e) {
                Toast.makeText(activity, "Failed to create AR session: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return false;
            }
        }

        try {
            session.resume();
            return true;
        } catch (CameraNotAvailableException e) {
            Toast.makeText(activity, "Camera not available: " + e.getMessage(), Toast.LENGTH_LONG).show();
            session = null;
            return false;
        }
    }

    public void pause() {
        if (session != null) {
            session.pause();
        }
    }

    public void destroy() {
        if (session != null) {
            session.close();
            session = null;
        }
    }

    /**
     * Creates an ARCore Spatial Anchor at a fixed distance in front of the user's camera.
     * @param distanceMeters Distance in front of camera (Z-axis)
     * @param yOffsetMeters Height offset relative to camera (Y-axis)
     * @return Anchor tracked in 6DoF space, or null if camera is not tracking.
     */
    public Anchor createAnchorInFrontOfCamera(float distanceMeters, float yOffsetMeters) {
        if (session == null) return null;
        try {
            Camera camera = session.update().getCamera();
            if (camera.getTrackingState() != TrackingState.TRACKING) {
                return null;
            }
            Pose cameraPose = camera.getPose();
            // Transform pose: local translation in camera coordinate frame
            Pose offsetPose = Pose.makeTranslation(0.0f, yOffsetMeters, -distanceMeters);
            Pose anchorPose = cameraPose.compose(offsetPose);

            return session.createAnchor(anchorPose);
        } catch (Exception e) {
            return null;
        }
    }
}
