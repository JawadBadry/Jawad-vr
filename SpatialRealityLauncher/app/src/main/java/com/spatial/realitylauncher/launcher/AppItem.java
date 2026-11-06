package com.spatial.realitylauncher.launcher;

import android.content.Intent;
import android.graphics.Bitmap;

/**
 * Model representing an installed Android application.
 */
public class AppItem {
    private final String label;
    private final String packageName;
    private final Bitmap icon;
    private final Intent launchIntent;

    public AppItem(String label, String packageName, Bitmap icon, Intent launchIntent) {
        this.label = label;
        this.packageName = packageName;
        this.icon = icon;
        this.launchIntent = launchIntent;
    }

    public String getLabel() { return label; }
    public String getPackageName() { return packageName; }
    public Bitmap getIcon() { return icon; }
    public Intent getLaunchIntent() { return launchIntent; }
}
