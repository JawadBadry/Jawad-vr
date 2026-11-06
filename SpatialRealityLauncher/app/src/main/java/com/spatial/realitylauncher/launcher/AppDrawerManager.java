package com.spatial.realitylauncher.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Queries PackageManager to load all user-facing installed applications
 * and dynamically renders them into a high-contrast VisionOS 3D spatial grid texture.
 */
public class AppDrawerManager {

    private final Context context;
    private final List<AppItem> installedApps = new ArrayList<>();

    private static final int COLS = 4;
    private static final int ROWS = 3;
    private static final int APPS_PER_PAGE = COLS * ROWS;
    private int currentPage = 0;

    public AppDrawerManager(Context context) {
        this.context = context;
        loadInstalledApps();
    }

    public void loadInstalledApps() {
        installedApps.clear();
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo appInfo : packages) {
            Intent launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName);
            if (launchIntent != null && !appInfo.packageName.equals(context.getPackageName())) {
                String label = pm.getApplicationLabel(appInfo).toString();
                Drawable iconDrawable = pm.getApplicationIcon(appInfo);
                Bitmap iconBitmap = drawableToBitmap(iconDrawable);

                installedApps.add(new AppItem(label, appInfo.packageName, iconBitmap, launchIntent));
            }
        }

        // Sort alphabetically
        Collections.sort(installedApps, (a, b) -> a.getLabel().compareToIgnoreCase(b.getLabel()));
    }

    public List<AppItem> getInstalledApps() {
        return installedApps;
    }

    /**
     * Renders a 1024x768 Apple Vision Pro style App Grid Canvas to a Bitmap.
     */
    public Bitmap renderAppGridBitmap() {
        int width = 1024;
        int height = 768;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // 1. Frosted Glass Background
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setShader(new LinearGradient(0, 0, 0, height,
                0xDD12151D, 0xEE1E222D, Shader.TileMode.CLAMP));
        RectF cardRect = new RectF(16, 16, width - 16, height - 16);
        canvas.drawRoundRect(cardRect, 40, 40, bgPaint);

        // Subtle glowing border
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3.0f);
        borderPaint.setColor(0x4000E5FF); // Cyan glass rim
        canvas.drawRoundRect(cardRect, 40, 40, borderPaint);

        // Title Header
        Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(Color.WHITE);
        headerPaint.setTextSize(34);
        headerPaint.setFakeBoldText(true);
        headerPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Spatial App Library", 60, 80, headerPaint);

        // Subtitle / Page info
        Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subPaint.setColor(0xAAFFFFFF);
        subPaint.setTextSize(20);
        int totalPages = Math.max(1, (int) Math.ceil((double) installedApps.size() / APPS_PER_PAGE));
        canvas.drawText("Pinch any app icon to launch • Page " + (currentPage + 1) + " of " + totalPages, 60, 115, subPaint);

        // Grid parameters
        float startX = 60;
        float startY = 160;
        float cellWidth = (width - 120) / (float) COLS;
        float cellHeight = (height - 220) / (float) ROWS;

        int startIndex = currentPage * APPS_PER_PAGE;
        int endIndex = Math.min(startIndex + APPS_PER_PAGE, installedApps.size());

        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextSize(18);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        Paint iconBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconBgPaint.setColor(0x30FFFFFF);

        for (int i = startIndex; i < endIndex; i++) {
            int localIndex = i - startIndex;
            int col = localIndex % COLS;
            int row = localIndex / COLS;

            float cx = startX + col * cellWidth + cellWidth / 2.0f;
            float cy = startY + row * cellHeight + 60;

            AppItem app = installedApps.get(i);

            // Icon background circle
            canvas.drawCircle(cx, cy, 48, iconBgPaint);

            // Draw app icon
            if (app.getIcon() != null) {
                Rect dst = new Rect((int) (cx - 40), (int) (cy - 40), (int) (cx + 40), (int) (cy + 40));
                canvas.drawBitmap(app.getIcon(), null, dst, null);
            }

            // Truncate label if too long
            String title = app.getLabel();
            if (title.length() > 14) {
                title = title.substring(0, 12) + "..";
            }
            canvas.drawText(title, cx, cy + 68, labelPaint);
        }

        return bitmap;
    }

    /**
     * Resolves click at normalized (u, v) and launches the corresponding app.
     */
    public boolean handleGridClick(float u, float v) {
        // Normalize u, v against content grid area
        float gridTop = 160f / 768f;
        float gridBottom = 700f / 768f;
        float gridLeft = 60f / 1024f;
        float gridRight = (1024f - 60f) / 1024f;

        if (u < gridLeft || u > gridRight || v < gridTop || v > gridBottom) {
            return false;
        }

        float colFraction = (u - gridLeft) / (gridRight - gridLeft);
        float rowFraction = (v - gridTop) / (gridBottom - gridTop);

        int col = Math.min(COLS - 1, Math.max(0, (int) (colFraction * COLS)));
        int row = Math.min(ROWS - 1, Math.max(0, (int) (rowFraction * ROWS)));

        int clickedIndex = currentPage * APPS_PER_PAGE + (row * COLS + col);
        if (clickedIndex < installedApps.size()) {
            AppItem app = installedApps.get(clickedIndex);
            launchApp(app);
            return true;
        }
        return false;
    }

    private void launchApp(AppItem app) {
        try {
            Intent intent = app.getLaunchIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Toast.makeText(context, "Opening " + app.getLabel(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "Cannot launch: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        int w = Math.max(1, drawable.getIntrinsicWidth());
        int h = Math.max(1, drawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
