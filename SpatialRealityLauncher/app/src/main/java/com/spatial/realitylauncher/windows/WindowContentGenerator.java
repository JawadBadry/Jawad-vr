package com.spatial.realitylauncher.windows;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * Generates beautiful, high-contrast, VisionOS glassmorphic UI textures
 * for in-app spatial floating windows (Web Browser, Notes, Media Cinema).
 */
public class WindowContentGenerator {

    /**
     * Creates a 3D Web Browser spatial window texture.
     */
    public static Bitmap createWebBrowserBitmap(String url) {
        int width = 1024;
        int height = 640;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Glass background
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setShader(new LinearGradient(0, 0, 0, height, 0xEE1A1E29, 0xFF10121A, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(new RectF(8, 8, width - 8, height - 8), 32, 32, bgPaint);

        // Window Title Bar
        Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(0x30FFFFFF);
        canvas.drawRoundRect(new RectF(8, 8, width - 8, 70), 32, 32, barPaint);

        // Window Controls (Red, Yellow, Green Mac/Vision style dots)
        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(0xFFFF5F56);
        canvas.drawCircle(36, 40, 10, dotPaint);
        dotPaint.setColor(0xFFFFBD2E);
        canvas.drawCircle(66, 40, 10, dotPaint);
        dotPaint.setColor(0xFF27C93F);
        canvas.drawCircle(96, 40, 10, dotPaint);

        // URL Pill Bar
        Paint urlBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        urlBarPaint.setColor(0x40000000);
        canvas.drawRoundRect(new RectF(140, 18, width - 140, 62), 22, 22, urlBarPaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF00E5FF);
        textPaint.setTextSize(20);
        canvas.drawText("🔒 " + url, 160, 47, textPaint);

        // Web Page Content Placeholder
        Paint contentHeader = new Paint(Paint.ANTI_ALIAS_FLAG);
        contentHeader.setColor(Color.WHITE);
        contentHeader.setTextSize(36);
        contentHeader.setFakeBoldText(true);
        canvas.drawText("Spatial Web 3.0 Experience", 60, 150, contentHeader);

        Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(0xCCFFFFFF);
        bodyPaint.setTextSize(22);
        canvas.drawText("• Fully spatialized 6DoF viewport navigation", 60, 210, bodyPaint);
        canvas.drawText("• Interpupillary Distance (IPD) calibrated stereo passthrough", 60, 255, bodyPaint);
        canvas.drawText("• Micro-gesture pinch detection with sub-millimeter precision", 60, 300, bodyPaint);
        canvas.drawText("• Real-time relocalization using persistent spatial coordinate anchors", 60, 345, bodyPaint);

        // Interactive Button Simulation
        Paint btnPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        btnPaint.setColor(0xFF7C4DFF);
        canvas.drawRoundRect(new RectF(60, 420, 280, 490), 20, 20, btnPaint);
        Paint btnText = new Paint(Paint.ANTI_ALIAS_FLAG);
        btnText.setColor(Color.WHITE);
        btnText.setTextSize(22);
        btnText.setFakeBoldText(true);
        btnText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Explore Metaverse", 170, 464, btnText);

        return bitmap;
    }

    /**
     * Creates a 3D Notes / Todo spatial window texture.
     */
    public static Bitmap createNotesBitmap() {
        int width = 800;
        int height = 600;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setShader(new LinearGradient(0, 0, 0, height, 0xEE2A1E29, 0xFF1D1420, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(new RectF(8, 8, width - 8, height - 8), 32, 32, bgPaint);

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(0xFFFF80AB);
        titlePaint.setTextSize(32);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("📝 Spatial Spatial Notes", 50, 70, titlePaint);

        Paint noteText = new Paint(Paint.ANTI_ALIAS_FLAG);
        noteText.setColor(Color.WHITE);
        noteText.setTextSize(22);
        canvas.drawText("☑ Calibrate VR Box lenses to 50mm IPD", 60, 150, noteText);
        canvas.drawText("☑ Room relocalization anchor locked (X=0.0, Y=0.0, Z=-1.2)", 60, 210, noteText);
        canvas.drawText("☑ Google MediaPipe Hand Landmarker active at 60 FPS", 60, 270, noteText);
        canvas.drawText("☑ App Drawer indexed 100% of installed system apps", 60, 330, noteText);

        return bitmap;
    }

    /**
     * Creates a 3D Cinema / Media Player spatial window texture.
     */
    public static Bitmap createMediaCinemaBitmap() {
        int width = 1024;
        int height = 576; // 16:9 aspect
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(0xFF000000); // OLED Black Screen
        canvas.drawRoundRect(new RectF(8, 8, width - 8, height - 8), 28, 28, bgPaint);

        // Glowing Neon Ambient Edge
        Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setStrokeWidth(4.0f);
        edgePaint.setColor(0xFF00E676);
        canvas.drawRoundRect(new RectF(8, 8, width - 8, height - 8), 28, 28, edgePaint);

        // Center Cinema Play Icon
        Paint playPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        playPaint.setColor(0x8000E676);
        canvas.drawCircle(width / 2.0f, height / 2.0f, 60, playPaint);

        Paint playText = new Paint(Paint.ANTI_ALIAS_FLAG);
        playText.setColor(Color.WHITE);
        playText.setTextSize(44);
        playText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("▶", width / 2.0f + 4, height / 2.0f + 16, playText);

        Paint descPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        descPaint.setColor(Color.WHITE);
        descPaint.setTextSize(24);
        descPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Spatial Cinema 4K • Immersive Theater Mode", width / 2.0f, height - 60, descPaint);

        return bitmap;
    }
}
