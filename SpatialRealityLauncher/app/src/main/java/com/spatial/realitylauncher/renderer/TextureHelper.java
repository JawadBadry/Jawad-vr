package com.spatial.realitylauncher.renderer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.GLUtils;

public class TextureHelper {

    /**
     * Creates an OpenGL texture suitable for ARCore camera passthrough (GL_TEXTURE_EXTERNAL_OES).
     */
    public static int createCameraOESTexture() {
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        int textureId = textures[0];

        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);

        return textureId;
    }

    /**
     * Loads a standard 2D Bitmap into an OpenGL 2D texture.
     */
    public static int loadTextureFromBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return 0;
        }

        int[] textureHandles = new int[1];
        GLES30.glGenTextures(1, textureHandles, 0);

        if (textureHandles[0] != 0) {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureHandles[0]);

            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
            GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        }

        return textureHandles[0];
    }

    /**
     * Updates an existing 2D texture with a new Bitmap.
     */
    public static void updateTexture(int textureId, Bitmap bitmap) {
        if (textureId <= 0 || bitmap == null || bitmap.isRecycled()) return;
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLUtils.texSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0, bitmap);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
    }

    /**
     * Compiles an OpenGL shader and checks for compile errors.
     */
    public static int compileShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);

        int[] compileStatus = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            String error = GLES30.glGetShaderInfoLog(shader);
            GLES30.glDeleteShader(shader);
            throw new RuntimeException("Shader compilation failed (" + type + "): " + error);
        }
        return shader;
    }

    /**
     * Links vertex and fragment shaders into an OpenGL Program.
     */
    public static int createProgram(String vertexCode, String fragmentCode) {
        int vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexCode);
        int fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentCode);

        int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);
        GLES30.glLinkProgram(program);

        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            String error = GLES30.glGetProgramInfoLog(program);
            GLES30.glDeleteProgram(program);
            throw new RuntimeException("Program linking failed: " + error);
        }
        return program;
    }
}
