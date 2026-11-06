package com.spatial.realitylauncher.spatial;

import com.spatial.realitylauncher.math.Ray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages the collection of 3D spatial windows, performs spatial raycast selection,
 * and handles gesture click dispatching.
 */
public class SpatialWindowManager {

    private final List<SpatialWindow> windows = Collections.synchronizedList(new ArrayList<>());
    private SpatialWindow hoveredWindow = null;
    private float hoveredU = 0.5f;
    private float hoveredV = 0.5f;

    public void addWindow(SpatialWindow window) {
        windows.add(window);
    }

    public void removeWindow(SpatialWindow window) {
        windows.remove(window);
    }

    public void clearAll() {
        windows.clear();
        hoveredWindow = null;
    }

    public List<SpatialWindow> getWindows() {
        return windows;
    }

    public SpatialWindow getHoveredWindow() {
        return hoveredWindow;
    }

    /**
     * Raycasts against all spatial windows. Identifies the closest hovered window.
     */
    public boolean updateRaycast(Ray ray) {
        SpatialWindow closestWindow = null;
        float closestDist = Float.MAX_VALUE;
        float bestU = 0;
        float bestV = 0;

        synchronized (windows) {
            for (SpatialWindow window : windows) {
                if (!window.isVisible()) continue;

                float[] hit = window.raycast(ray);
                if (hit != null) {
                    float dist = hit[2];
                    if (dist < closestDist) {
                        closestDist = dist;
                        closestWindow = window;
                        bestU = hit[0];
                        bestV = hit[1];
                    }
                }
            }

            // Update hover flags
            for (SpatialWindow window : windows) {
                window.setHovered(window == closestWindow);
            }
        }

        hoveredWindow = closestWindow;
        hoveredU = bestU;
        hoveredV = bestV;
        return hoveredWindow != null;
    }

    /**
     * Triggers a pinch click on whichever window is currently hovered.
     */
    public boolean triggerPinchClick() {
        if (hoveredWindow != null) {
            hoveredWindow.performClick(hoveredU, hoveredV);
            return true;
        }
        return false;
    }
}
