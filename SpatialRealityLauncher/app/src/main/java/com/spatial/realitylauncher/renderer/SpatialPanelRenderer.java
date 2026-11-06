package com.spatial.realitylauncher.renderer;

import android.opengl.GLES30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Renders floating 3D planar windows (Apple Vision Pro / Meta Quest style)
 * with rounded glass borders, drop shadow simulation, and dynamic textures.
 */
public class SpatialPanelRenderer {

    private static final String VERTEX_SHADER =
            "#version 300 es\n" +
            "layout(location = 0) in vec3 a_Position;\n" +
            "layout(location = 1) in vec2 a_TexCoord;\n" +
            "uniform mat4 u_ModelViewProjection;\n" +
            "out vec2 v_TexCoord;\n" +
            "void main() {\n" +
            "    v_TexCoord = a_TexCoord;\n" +
            "    gl_Position = u_ModelViewProjection * vec4(a_Position, 1.0);\n" +
            "}\n";

    private static final String FRAGMENT_SHADER =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "uniform sampler2D u_Texture;\n" +
            "uniform float u_Alpha;\n" +
            "uniform int u_IsHovered;\n" +
            "in vec2 v_TexCoord;\n" +
            "out vec4 o_FragColor;\n" +
            "void main() {\n" +
            "    vec4 texColor = texture(u_Texture, v_TexCoord);\n" +
            "    // Subtle VisionOS edge highlight\n" +
            "    float edgeDist = min(min(v_TexCoord.x, 1.0 - v_TexCoord.x), min(v_TexCoord.y, 1.0 - v_TexCoord.y));\n" +
            "    vec4 borderColor = vec4(0.0, 0.9, 1.0, 1.0); // Cyan glow\n" +
            "    if (u_IsHovered == 1 && edgeDist < 0.03) {\n" +
            "        texColor = mix(texColor, borderColor, 0.7);\n" +
            "    }\n" +
            "    o_FragColor = vec4(texColor.rgb, texColor.a * u_Alpha);\n" +
            "}\n";

    // Quad geometry centered at (0, 0, 0) in local space
    private static final float[] VERTICES = {
            -0.5f, -0.5f, 0.0f,
             0.5f, -0.5f, 0.0f,
             0.5f,  0.5f, 0.0f,
            -0.5f,  0.5f, 0.0f,
    };

    private static final float[] TEX_COORDS = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f,
    };

    private static final short[] INDICES = {0, 1, 2, 0, 2, 3};

    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;
    private ShortBuffer indexBuffer;

    private int program;
    private int mvpUniform;
    private int textureUniform;
    private int alphaUniform;
    private int hoveredUniform;

    public void init() {
        ByteBuffer bbPos = ByteBuffer.allocateDirect(VERTICES.length * 4);
        bbPos.order(ByteOrder.nativeOrder());
        vertexBuffer = bbPos.asFloatBuffer();
        vertexBuffer.put(VERTICES);
        vertexBuffer.position(0);

        ByteBuffer bbTex = ByteBuffer.allocateDirect(TEX_COORDS.length * 4);
        bbTex.order(ByteOrder.nativeOrder());
        texCoordBuffer = bbTex.asFloatBuffer();
        texCoordBuffer.put(TEX_COORDS);
        texCoordBuffer.position(0);

        ByteBuffer bbIdx = ByteBuffer.allocateDirect(INDICES.length * 2);
        bbIdx.order(ByteOrder.nativeOrder());
        indexBuffer = bbIdx.asShortBuffer();
        indexBuffer.put(INDICES);
        indexBuffer.position(0);

        program = TextureHelper.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        mvpUniform = GLES30.glGetUniformLocation(program, "u_ModelViewProjection");
        textureUniform = GLES30.glGetUniformLocation(program, "u_Texture");
        alphaUniform = GLES30.glGetUniformLocation(program, "u_Alpha");
        hoveredUniform = GLES30.glGetUniformLocation(program, "u_IsHovered");
    }

    /**
     * Draws a spatial 3D window panel.
     */
    public void draw(float[] mvpMatrix, int textureId, float alpha, boolean isHovered) {
        if (textureId <= 0) return;

        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        GLES30.glUseProgram(program);

        GLES30.glUniformMatrix4fv(mvpUniform, 1, false, mvpMatrix, 0);
        GLES30.glUniform1f(alphaUniform, alpha);
        GLES30.glUniform1i(hoveredUniform, isHovered ? 1 : 0);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(textureUniform, 0);

        GLES30.glEnableVertexAttribArray(0);
        vertexBuffer.position(0);
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        GLES30.glEnableVertexAttribArray(1);
        texCoordBuffer.position(0);
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 0, texCoordBuffer);

        indexBuffer.position(0);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, INDICES.length, GLES30.GL_UNSIGNED_SHORT, indexBuffer);

        GLES30.glDisableVertexAttribArray(0);
        GLES30.glDisableVertexAttribArray(1);

        GLES30.glDisable(GLES30.GL_BLEND);
    }
}
