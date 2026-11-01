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

    public static int bg() {
        return light() ? 0xFFF5F5F7 : 0xFF08080A;
    }

    public static int surface() {
        return light() ? 0xFFFFFFFF : 0xFF111114;
    }

    public static int surface2() {
        return light() ? 0xFFECECF0 : 0xFF19191E;
    }

    public static int surface3() {
        return light() ? 0xFFE2E2E8 : 0xFF232329;
    }

    public static int text() {
        return light() ? 0xFF101014 : 0xFFF5F5F7;
    }

    public static int dim() {
        return light() ? 0xFF6A6A73 : 0xFF9A9AA3;
    }

    public static int faint() {
        return light() ? 0xFF9C9CA5 : 0xFF6C6C75;
    }

    public static int border() {
        return light() ? 0xFFE3E3E9 : 0xFF25252B;
    }

    public static int danger() {
        return 0xFFFF6B6B;
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

    private static double luminance(int color) {
        double r = Color.red(color) / 255.0;
        double g = Color.green(color) / 255.0;
        double b = Color.blue(color) / 255.0;
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    public static void apply(Activity activity) {
        Window window = activity.getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(light() ? 0xFFFFFFFF : 0xFF08080A);
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
