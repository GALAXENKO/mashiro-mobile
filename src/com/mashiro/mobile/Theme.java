package com.mashiro.mobile;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;

/** Colors and platform window styling. */
public final class Theme {
    private Theme() {}

    public static boolean light() {
        return Store.isLight();
    }

    /** Main canvas. */
    public static int bg() {
        return light() ? 0xFFF3F3F6 : 0xFF07070A;
    }

    /** Cards, sheets and bars that sit on the canvas. */
    public static int surface() {
        return light() ? 0xFFFFFFFF : 0xFF121216;
    }

    /** Secondary fills: covers, icon circles, inputs. */
    public static int surface2() {
        return light() ? 0xFFECECF1 : 0xFF1C1C22;
    }

    /** Tertiary fills: tracks, pressed states, sliders. */
    public static int surface3() {
        return light() ? 0xFFDFDFE7 : 0xFF2A2A33;
    }

    public static int text() {
        return light() ? 0xFF0E0E12 : 0xFFF6F6F8;
    }

    public static int dim() {
        return light() ? 0xFF63636D : 0xFF9E9EA9;
    }

    public static int faint() {
        return light() ? 0xFF94949E : 0xFF6B6B77;
    }

    public static int border() {
        return light() ? 0xFFE1E1E9 : 0xFF232329;
    }

    /** Softer hairline for dividers inside cards. */
    public static int hairline() {
        return light() ? 0x1A0E0E12 : 0x1AFFFFFF;
    }

    public static int danger() {
        return 0xFFFF6B6B;
    }

    public static int success() {
        return 0xFF4CD787;
    }

    public static int accent() {
        String raw = Store.accent();
        int color;
        try {
            color = Color.parseColor(raw);
        } catch (Exception e) {
            color = 0xFFFFFFFF;
        }
        if (light() && luminance(color) > 0.85) return 0xFF111114;
        return color;
    }

    public static int accentText() {
        return luminance(accent()) > 0.55 ? 0xFF08080A : 0xFFFFFFFF;
    }

    public static int accentSoft(int alpha) {
        int a = accent();
        return Color.argb(alpha, Color.red(a), Color.green(a), Color.blue(a));
    }

    public static double luminance(int color) {
        double r = Color.red(color) / 255.0;
        double g = Color.green(color) / 255.0;
        double b = Color.blue(color) / 255.0;
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    public static void apply(Activity activity) {
        Window window = activity.getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(bg());
        if (Build.VERSION.SDK_INT >= 29) {
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            window.setNavigationBarDividerColor(Color.TRANSPARENT);
            window.setNavigationBarContrastEnforced(false);
        }
        View decor = window.getDecorView();
        int flags = decor.getSystemUiVisibility();
        if (Build.VERSION.SDK_INT >= 23) {
            if (light()) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            else flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (Build.VERSION.SDK_INT >= 26) {
            if (light()) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            else flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        decor.setSystemUiVisibility(flags);
    }
}
