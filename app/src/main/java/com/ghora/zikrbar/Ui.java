package com.ghora.zikrbar;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.view.WindowManager;

import java.util.HashMap;
import java.util.Map;

/** Shared fonts, number formatting and helpers that keep the app working on Android 6 and 7. */
final class Ui {
    private static final Map<String, Typeface> fonts = new HashMap<>();

    private Ui() {}

    /** Loads a font from assets/fonts (works on every Android version). */
    static Typeface font(Context c, String name) {
        Typeface t = fonts.get(name);
        if (t == null) {
            try {
                t = Typeface.createFromAsset(c.getApplicationContext().getAssets(), "fonts/" + name + ".ttf");
            } catch (RuntimeException e) {
                t = Typeface.DEFAULT;
            }
            fonts.put(name, t);
        }
        return t;
    }

    static Typeface amiri(Context c) {
        return font(c, "amiri");
    }

    static Typeface amiriBold(Context c) {
        return font(c, "amiri_bold");
    }

    /** Starts the dhikr service: foreground-start on Android 8+, normal start on Android 6/7. */
    static void startZikrService(Context c, Intent i) {
        if (Build.VERSION.SDK_INT >= 26) {
            c.startForegroundService(i);
        } else {
            c.startService(i);
        }
    }

    /** Notification builder that uses channels only where Android supports them (8+). */
    @SuppressWarnings("deprecation")
    static Notification.Builder notif(Context c, String channel, boolean quiet) {
        if (Build.VERSION.SDK_INT >= 26) return new Notification.Builder(c, channel);
        Notification.Builder b = new Notification.Builder(c);
        b.setPriority(quiet ? Notification.PRIORITY_LOW : Notification.PRIORITY_DEFAULT);
        return b;
    }

    /** Window type for views drawn over other apps. */
    @SuppressWarnings("deprecation")
    static int overlayType() {
        return Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    /** Writes a number with the digits of the chosen language. */
    static String fa(int n) {
        return L.num(n);
    }
}
