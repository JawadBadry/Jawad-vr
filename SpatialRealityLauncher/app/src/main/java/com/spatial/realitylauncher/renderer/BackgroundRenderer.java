package com.spatial.realitylauncher.renderer;

import android.opengl.GLES11Ext;
import android.opengl.GLES30;

import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Renders the real-time ARCore Passthrough camera feed as the stereo background
 * using GL_TEXTURE_EXTERNAL_OES.
 */
public class BackgroundRenderer {

    private static final String VERTEX_SHADER =
            "#version 300 es\n" +
            "layout(location = 0) in vec4 a_Position;\n" +
            "layout(location = 1) in vec2 a_TexCoord;\n" +
            "out vec2 v_TexCoord;\n" +
            "void main() {\n" +
            "    gl_Position = a_Position;\n" +
            "    v_TexCoord = a_TexCoord;\n" +
            "}\n";

    private static final String FRAGMENT_SHADER =
            "#version 300 es\n" +
            "#extension GL_OES_EGL_image_external_essl3 : require\n" +
            "precision mediump float;\n" +
            "uniform samplerExternalOES u_Texture;\n" +
            "in vec2 v_TexCoord;\n" +
            "out vec4 o_FragColor;\n" +
            "void main() {\n" +
            "    o_FragColor = texture(u_Texture, v_TexCoord);\n" +
            "}\n";

    // Full screen quad in NDC (-1 to 1)
    private static final float[] QUAD_COORDS = {
            -1.0f, -1.0f, 0.0f,
            +1.0f, -1.0f, 0.0f,
            -1.0f, +1.0f, 0.0f,
            +1.0f, +1.0f, 0.0f,
    };

    // Normalized screen view coordinates for ARCore UV mapping
    private static final float[] QUAD_VIEW_COORDS = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
    };

    private FloatBuffer quadPositionBuffer;
    private FloatBuffer quadTexCoordBuffer;
    private FloatBuffer quadViewCoordBuffer;

    private int program;
    private int positionAttrib;
    private int texCoordAttrib;
    private int textureUniform;

    private int cameraTextureId = -1;

    public void init() {
        ByteBuffer bbPos = ByteBuffer.allocateDirect(QUAD_COORDS.length * 4);
        bbPos.order(ByteOrder.nativeOrder());
        quadPositionBuffer = bbPos.asFloatBuffer();
        quadPositionBuffer.put(QUAD_COORDS);
        quadPositionBuffer.position(0);

        ByteBuffer bbTex = ByteBuffer.allocateDirect(QUAD_VIEW_COORDS.length * 4);
        bbTex.order(ByteOrder.nativeOrder());
        quadTexCoordBuffer = bbTex.asFloatBuffer();

        ByteBuffer bbView = ByteBuffer.allocateDirect(QUAD_VIEW_COORDS.length * 4);
        bbView.order(ByteOrder.nativeOrder());
        quadViewCoordBuffer = bbView.asFloatBuffer();
        quadViewCoordBuffer.put(QUAD_VIEW_COORDS);
        quadViewCoordBuffer.position(0);

        cameraTextureId = TextureHelper.createCameraOESTexture();
        program = TextureHelper.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);

        positionAttrib = GLES30.glGetAttribLocation(program, "a_Position");
        texCoordAttrib = GLES30.glGetAttribLocation(program, "a_TexCoord");
        textureUniform = GLES30.glGetUniformLocation(program, "u_Texture");
    }

    public int getCameraTextureId() {
        return cameraTextureId;
    }

    /**
     * Updates texture coordinates based on display rotation and ARCore frame aspect.
     */
    public void updateTexCoords(Frame frame) {
        if (frame == null) return;
        quadViewCoordBuffer.position(0);
        quadTexCoordBuffer.position(0);
        frame.transformCoordinates2d(
                Coordinates2d.VIEW_NORMALIZED,
                quadViewCoordBuffer,
                Coordinates2d.TEXTURE_NORMALIZED,
                quadTexCoordBuffer
        );
        quadTexCoordBuffer.position(0);
    }

    /**
     * Draws the passthrough background onto the current active viewport.
     */
    public void draw() {
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glDepthMask(false);

        GLES30.glUseProgram(program);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId);
        GLES30.glUniform1i(textureUniform, 0);

        GLES30.glEnableVertexAttribArray(positionAttrib);
        quadPositionBuffer.position(0);
        GLES30.glVertexAttribPointer(positionAttrib, 3, GLES30.GL_FLOAT, false, 0, quadPositionBuffer);

        GLES30.glEnableVertexAttribArray(texCoordAttrib);
        quadTexCoordBuffer.position(0);
        GLES30.glVertexAttribPointer(texCoordAttrib, 2, GLES30.GL_FLOAT, false, 0, quadTexCoordBuffer);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(positionAttrib);
        GLES30.glDisableVertexAttribArray(texCoordAttrib);

        GLES30.glDepthMask(true);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }
}
