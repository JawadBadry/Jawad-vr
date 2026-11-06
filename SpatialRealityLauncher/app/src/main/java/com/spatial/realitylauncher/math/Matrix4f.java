package com.spatial.realitylauncher.math;

import android.opengl.Matrix;

/**
 * High-performance 4x4 matrix helper for stereoscopic 3D calculations,
 * projection transforms, IPD offsets, and raycasting.
 */
public class Matrix4f {
    public final float[] data = new float[16];

    public Matrix4f() {
        setIdentity();
    }

    public Matrix4f(float[] initialData) {
        System.arraycopy(initialData, 0, data, 0, 16);
    }

    public void setIdentity() {
        Matrix.setIdentityM(data, 0);
    }

    public void translate(float x, float y, float z) {
        Matrix.translateM(data, 0, x, y, z);
    }

    public void rotate(float angleDeg, float x, float y, float z) {
        Matrix.rotateM(data, 0, angleDeg, x, y, z);
    }

    public void scale(float x, float y, float z) {
        Matrix.scaleM(data, 0, x, y, z);
    }

    public static Matrix4f multiply(Matrix4f lhs, Matrix4f rhs) {
        Matrix4f result = new Matrix4f();
        Matrix.multiplyMM(result.data, 0, lhs.data, 0, rhs.data, 0);
        return result;
    }

    public void setPerspective(float fovyDeg, float aspect, float near, float far) {
        Matrix.perspectiveM(data, 0, fovyDeg, aspect, near, far);
    }

    public void setLookAt(float eyeX, float eyeY, float eyeZ,
                          float centerX, float centerY, float centerZ,
                          float upX, float upY, float upZ) {
        Matrix.setLookAtM(data, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
    }

    public boolean invert(Matrix4f result) {
        return Matrix.invertM(result.data, 0, data, 0);
    }

    public float[] transformPoint(float[] point3D) {
        float[] in = new float[]{point3D[0], point3D[1], point3D[2], 1.0f};
        float[] out = new float[4];
        Matrix.multiplyMV(out, 0, data, 0, in, 0);
        return new float[]{out[0] / out[3], out[1] / out[3], out[2] / out[3]};
    }
}
