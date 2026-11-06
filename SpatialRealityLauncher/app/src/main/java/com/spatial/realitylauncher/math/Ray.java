package com.spatial.realitylauncher.math;

/**
 * 3D Ray (origin + normalized direction) used for spatial raycasting,
 * reticle pointing, and pinch-click collision detection in 6DoF space.
 */
public class Ray {
    public final float[] origin = new float[3];
    public final float[] direction = new float[3];

    public Ray(float ox, float oy, float oz, float dx, float dy, float dz) {
        origin[0] = ox;
        origin[1] = oy;
        origin[2] = oz;

        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0.00001f) {
            direction[0] = dx / len;
            direction[1] = dy / len;
            direction[2] = dz / len;
        } else {
            direction[0] = 0;
            direction[1] = 0;
            direction[2] = -1.0f;
        }
    }

    /**
     * Intersects ray with a 3D planar quad representing a spatial window.
     * @param center 3D center of quad
     * @param normal Normal vector of quad
     * @param halfWidth Half width of the window quad
     * @param halfHeight Half height of the window quad
     * @return Normalized UV coordinates [u, v] (0 to 1) if hit, or null if missed.
     */
    public float[] intersectQuad(float[] center, float[] normal, float halfWidth, float halfHeight) {
        float denom = direction[0] * normal[0] + direction[1] * normal[1] + direction[2] * normal[2];
        if (Math.abs(denom) < 1e-6) {
            return null; // Ray is parallel to plane
        }

        float p0_o_x = center[0] - origin[0];
        float p0_o_y = center[1] - origin[1];
        float p0_o_z = center[2] - origin[2];

        float t = (p0_o_x * normal[0] + p0_o_y * normal[1] + p0_o_z * normal[2]) / denom;
        if (t < 0.05f) {
            return null; // Hit is behind camera or too close
        }

        // Hit point
        float hx = origin[0] + t * direction[0];
        float hy = origin[1] + t * direction[1];

        // Offset from center (assuming axis-aligned or oriented quad in planar coordinates)
        float localX = hx - center[0];
        float localY = hy - center[1];

        if (Math.abs(localX) <= halfWidth && Math.abs(localY) <= halfHeight) {
            float u = (localX + halfWidth) / (2.0f * halfWidth);
            float v = (halfHeight - localY) / (2.0f * halfHeight);
            return new float[]{u, v, t};
        }
        return null;
    }
}
