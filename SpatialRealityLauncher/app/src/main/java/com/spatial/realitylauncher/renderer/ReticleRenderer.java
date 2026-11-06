package com.spatial.realitylauncher.renderer;

import android.opengl.GLES30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Renders an Apple Vision Pro / Meta Quest style 3D reticle (circle + core dot)
 * that dynamically animates its scale and glow when a pinch gesture is detected.
 */
public class ReticleRenderer {

    private static final String VERTEX_SHADER =
            "#version 300 es\n" +
            "layout(location = 0) in vec3 a_Position;\n" +
            "uniform mat4 u_ModelViewProjection;\n" +
            "void main() {\n" +
            "    gl_Position = u_ModelViewProjection * vec4(a_Position, 1.0);\n" +
            "}\n";

    private static final String FRAGMENT_SHADER =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "uniform vec4 u_Color;\n" +
            "out vec4 o_FragColor;\n" +
            "void main() {\n" +
            "    o_FragColor = u_Color;\n" +
            "}\n";

    private static final int SEGMENTS = 36;
    private FloatBuffer ringBuffer;
    private FloatBuffer dotBuffer;

    private int program;
    private int mvpUniform;
    private int colorUniform;

    public void init() {
        // Build circle ring vertices
        float[] ringVertices = new float[(SEGMENTS + 1) * 3];
        for (int i = 0; i <= SEGMENTS; i++) {
            float theta = (float) (2.0 * Math.PI * i / SEGMENTS);
            ringVertices[i * 3] = (float) Math.cos(theta);
            ringVertices[i * 3 + 1] = (float) Math.sin(theta);
            ringVertices[i * 3 + 2] = 0.0f;
        }

        ByteBuffer bbRing = ByteBuffer.allocateDirect(ringVertices.length * 4);
        bbRing.order(ByteOrder.nativeOrder());
        ringBuffer = bbRing.asFloatBuffer();
        ringBuffer.put(ringVertices);
        ringBuffer.position(0);

        // Center dot quad
        float[] dotVertices = {
                -0.2f, -0.2f, 0.0f,
                 0.2f, -0.2f, 0.0f,
                 0.2f,  0.2f, 0.0f,
                -0.2f,  0.2f, 0.0f,
        };
        ByteBuffer bbDot = ByteBuffer.allocateDirect(dotVertices.length * 4);
        bbDot.order(ByteOrder.nativeOrder());
        dotBuffer = bbDot.asFloatBuffer();
        dotBuffer.put(dotVertices);
        dotBuffer.position(0);

        program = TextureHelper.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        mvpUniform = GLES30.glGetUniformLocation(program, "u_ModelViewProjection");
        colorUniform = GLES30.glGetUniformLocation(program, "u_Color");
    }

    /**
     * Draws the reticle with current MVP and pinch state.
     */
    public void draw(float[] mvpMatrix, boolean isPinching) {
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE); // Additive glowing blend

        GLES30.glUseProgram(program);
        GLES30.glUniformMatrix4fv(mvpUniform, 1, false, mvpMatrix, 0);

        // Ring color: Cyan on hover, Purple/Green on Pinch
        if (isPinching) {
            GLES30.glUniform4f(colorUniform, 0.0f, 1.0f, 0.5f, 0.95f); // Bright green-cyan
        } else {
            GLES30.glUniform4f(colorUniform, 1.0f, 1.0f, 1.0f, 0.85f); // Clean white ring
        }

        GLES30.glEnableVertexAttribArray(0);
        ringBuffer.position(0);
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, ringBuffer);
        GLES30.glLineWidth(4.0f);
        GLES30.glDrawArrays(GLES30.GL_LINE_STRIP, 0, SEGMENTS + 1);

        // Inner glowing dot
        dotBuffer.position(0);
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, dotBuffer);
        if (isPinching) {
            GLES30.glUniform4f(colorUniform, 0.0f, 0.9f, 1.0f, 1.0f);
        } else {
            GLES30.glUniform4f(colorUniform, 0.0f, 0.9f, 1.0f, 0.6f);
        }
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 4);

        GLES30.glDisableVertexAttribArray(0);
        GLES30.glDisable(GLES30.GL_BLEND);
    }
}
