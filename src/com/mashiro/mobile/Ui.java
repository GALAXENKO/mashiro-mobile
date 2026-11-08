package com.mashiro.mobile;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/** Small view toolkit: the project deliberately has no AndroidX dependency. */
public final class Ui {
    private Ui() {}

    public static int dp(Context c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    public static int screenWidth(Context c) {
        return c.getResources().getDisplayMetrics().widthPixels;
    }

    public static int statusBar(Context c) {
        return dim(c, "status_bar_height");
    }

    public static int navBar(Context c) {
        int id = c.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return id > 0 ? c.getResources().getDimensionPixelSize(id) : dp(c, 48);
    }

    private static int dim(Context c, String name) {
        int id = c.getResources().getIdentifier(name, "dimen", "android");
        return id > 0 ? c.getResources().getDimensionPixelSize(id) : dp(c, 24);
    }

    public static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static TextView text(Context c, CharSequence text, float size, int color, boolean medium) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif" + (medium ? "-medium" : ""), medium ? Typeface.BOLD : Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        t.setLineSpacing(0, 1.14f);
        return t;
    }

    public static TextView title(Context c, String text, float size) {
        return text(c, text, size, Theme.text(), true);
    }

    /** Wide-tracked uppercase caption used above sections and headers. */
    public static TextView overline(Context c, String text) {
        TextView t = text(c, text.toUpperCase(Locale.getDefault()), 10.5f, Theme.faint(), true);
        t.setLetterSpacing(0.16f);
        return t;
    }

    public static TextView label(Context c, String text) {
        TextView t = text(c, text, 12, Theme.dim(), false);
        t.setLetterSpacing(0.04f);
        return t;
    }

    public static TextView section(Context c, String text) {
        TextView t = overline(c, text);
        t.setPadding(0, dp(c, 24), 0, dp(c, 10));
        return t;
    }

    /** Rounded surface container used for grouped content. */
    public static LinearLayout card(Context c, float radiusDp) {
        LinearLayout box = column(c);
        box.setBackground(strokeBg(c, Theme.surface(), radiusDp, Theme.border(), 1));
        return box;
    }

    public static GradientDrawable gradient(Context c, int top, int bottom, float radiusDp) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{top, bottom});
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static View insetDivider(Context c, float leftDp) {
        LinearLayout holder = new LinearLayout(c);
        holder.setOrientation(LinearLayout.HORIZONTAL);
        holder.setPadding(dp(c, leftDp), 0, 0, 0);
        View v = new View(c);
        v.setBackgroundColor(Theme.hairline());
        holder.addView(v, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(c, 0.6f))));
        return holder;
    }

    public static View space(Context c, float height) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, height)));
        return v;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(Theme.border());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(c, 0.6f)));
        v.setLayoutParams(lp);
        return v;
    }

    public static GradientDrawable bg(Context c, int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static GradientDrawable strokeBg(Context c, int fill, float radiusDp, int stroke, float widthDp) {
        GradientDrawable d = bg(c, fill, radiusDp);
        d.setStroke(Math.max(1, dp(c, widthDp)), stroke);
        return d;
    }

    public static void ripple(View v, int color) {
        if (v.getBackground() == null) return;
        RippleDrawable ripple = new RippleDrawable(ColorStateList.valueOf(color), v.getBackground(), null);
        v.setBackground(ripple);
    }

    public static void padding(View v, float l, float t, float r, float b) {
        Context c = v.getContext();
        v.setPadding(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
    }

    public static void margin(View v, float l, float t, float r, float b) {
        Context c = v.getContext();
        ViewGroup.LayoutParams raw = v.getLayoutParams();
        if (!(raw instanceof ViewGroup.MarginLayoutParams)) return;
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) raw;
        lp.setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
        v.setLayoutParams(lp);
    }

    public static LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    public static LinearLayout.LayoutParams lp(Context c, float widthDp, float heightDp) {
        return new LinearLayout.LayoutParams(dp(c, widthDp), dp(c, heightDp));
    }

    public static LinearLayout.LayoutParams weight(Context c, float heightDp, float weight) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(c, heightDp), weight);
        return lp;
    }

    public static ImageView icon(Context c, int res, float sizeDp, int tint) {
        ImageView iv = new ImageView(c);
        iv.setImageResource(res);
        iv.setColorFilter(tint);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setLayoutParams(new FrameLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return iv;
    }

    public static View iconButton(Context c, int res, float boxDp, float iconDp, int tint, View.OnClickListener click) {
        FrameLayout box = new FrameLayout(c);
        box.setLayoutParams(new LinearLayout.LayoutParams(dp(c, boxDp), dp(c, boxDp)));
        box.setBackground(bg(c, Color.TRANSPARENT, boxDp / 2));
        ImageView iv = icon(c, res, iconDp, tint);
        FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(dp(c, iconDp), dp(c, iconDp));
        ilp.gravity = Gravity.CENTER;
        box.addView(iv, ilp);
        box.setOnClickListener(click);
        ripple(box, Theme.light() ? 0x14101014 : 0x1AFFFFFF);
        return box;
    }

    public static TextView primaryButton(Context c, String text, View.OnClickListener click) {
        TextView b = text(c, text, 15, Theme.accentText(), true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(c, 20), dp(c, 15), dp(c, 20), dp(c, 15));
        b.setBackground(bg(c, Theme.accent(), 16));
        b.setOnClickListener(click);
        ripple(b, Theme.accentText() == 0xFF08080A ? 0x22000000 : 0x22FFFFFF);
        return b;
    }

    public static TextView secondaryButton(Context c, String text, View.OnClickListener click) {
        TextView b = text(c, text, 14, Theme.text(), true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(c, 18), dp(c, 13), dp(c, 18), dp(c, 13));
        b.setBackground(strokeBg(c, Theme.surface2(), 14, Theme.border(), 1));
        b.setOnClickListener(click);
        ripple(b, Theme.light() ? 0x14101014 : 0x1AFFFFFF);
        return b;
    }

    public static TextView chip(Context c, String text, boolean on, View.OnClickListener click) {
        TextView b = Ui.text(c, text, 12.5f, on ? Theme.accentText() : Theme.dim(), on);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(c, 15), dp(c, 9), dp(c, 15), dp(c, 9));
        b.setBackground(on ? bg(c, Theme.accent(), 18)
                : strokeBg(c, Theme.surface(), 18, Theme.border(), 1));
        if (click != null) {
            b.setOnClickListener(click);
            ripple(b, on ? 0x22000000 : (Theme.light() ? 0x12101014 : 0x18FFFFFF));
        }
        return b;
    }

    public static TextView pill(Context c, String text, int color) {
        TextView t = text(c, text, 10.5f, color, true);
        t.setLetterSpacing(0.02f);
        t.setPadding(dp(c, 8), dp(c, 4), dp(c, 8), dp(c, 4));
        t.setBackground(bg(c, withAlpha(color, 28), 8));
        return t;
    }

    public static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    public static ProgressBar progress(Context c, boolean small) {
        ProgressBar p = new ProgressBar(c);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                dp(c, small ? 18 : 26), dp(c, small ? 18 : 26));
        p.setLayoutParams(lp);
        p.setIndeterminateTintList(ColorStateList.valueOf(Theme.accent()));
        return p;
    }

    public static void toast(Context c, String text) {
        Toast.makeText(c, text, Toast.LENGTH_SHORT).show();
    }

    public static void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static String plural(int count, String one, String few, String many) {
        int n = Math.abs(count) % 100;
        int n1 = n % 10;
        if (n > 10 && n < 20) return many;
        if (n1 > 1 && n1 < 5) return few;
        if (n1 == 1) return one;
        return many;
    }

    public static String duration(long ms) {
        if (ms <= 0) return "0:00";
        long total = ms / 1000;
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        if (h > 0) return String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s);
        return String.format(Locale.ROOT, "%d:%02d", m, s);
    }

    public static String count(long value) {
        if (value >= 1_000_000) return String.format(Locale.ROOT, "%.1f млн", value / 1_000_000.0);
        if (value >= 1_000) return String.format(Locale.ROOT, "%.1f тыс.", value / 1_000.0);
        return String.valueOf(value);
    }

    public static void marquee(TextView view) {
        view.setSingleLine(true);
        view.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        view.setMarqueeRepeatLimit(-1);
        view.setHorizontallyScrolling(true);
        view.setSelected(true);
    }

    public static void applyTopInset(View view) {
        Context c = view.getContext();
        view.setPadding(view.getPaddingLeft(), view.getPaddingTop() + statusBar(c),
                view.getPaddingRight(), view.getPaddingBottom());
    }

    public static void applyBottomInset(View view) {
        Context c = view.getContext();
        view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                view.getPaddingRight(), view.getPaddingBottom() + navBar(c));
    }

    // --------------------------------------------------------------- dialogs

    public static Dialog sheet(Context c, View content) {
        Dialog d = new Dialog(c);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout root = new FrameLayout(c);
        root.setBackgroundColor(0x88000000);
        root.setOnClickListener(v -> d.dismiss());
        LinearLayout holder = column(c);
        holder.setBackground(bg(c, Theme.surface(), 26));
        holder.setPadding(0, dp(c, 12), 0, dp(c, 10));
        holder.setOnClickListener(v -> {});
        FrameLayout.LayoutParams hl = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hl.gravity = Gravity.BOTTOM;
        root.addView(holder, hl);
        // grabber
        View grab = new View(c);
        grab.setBackground(bg(c, Theme.surface3(), 3));
        FrameLayout.LayoutParams gl = new FrameLayout.LayoutParams(dp(c, 42), dp(c, 4));
        gl.gravity = Gravity.CENTER_HORIZONTAL;
        gl.bottomMargin = dp(c, 10);
        holder.addView(grab, gl);
        holder.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.setPadding(0, 0, 0, navBar(c));
        d.setContentView(root);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            w.setGravity(Gravity.BOTTOM);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attrs = w.getAttributes();
            attrs.dimAmount = 0.55f;
            w.setAttributes(attrs);
        }
        return d;
    }

    public interface InputCallback {
        void onInput(String value);
    }

    public static Dialog input(Context c, String title, String initial, String hint, String action,
                               boolean multiline, InputCallback callback) {
        Dialog d = new Dialog(c);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = column(c);
        root.setBackground(bg(c, Theme.surface(), 22));
        root.setPadding(dp(c, 20), dp(c, 20), dp(c, 20), dp(c, 16));
        TextView t = title(c, title, 18);
        root.addView(t);
        root.addView(space(c, 14));
        EditText input = new EditText(c);
        input.setText(initial == null ? "" : initial);
        input.setHint(hint == null ? "" : hint);
        input.setTextColor(Theme.text());
        input.setHintTextColor(Theme.faint());
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        input.setBackground(strokeBg(c, Theme.surface2(), 12, Theme.border(), 1));
        input.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        input.setSingleLine(!multiline);
        if (multiline) input.setMinLines(4);
        root.addView(input);
        root.addView(space(c, 14));
        LinearLayout actions = row(c);
        actions.setGravity(Gravity.END);
        TextView cancel = text(c, "Отмена", 14, Theme.dim(), true);
        cancel.setPadding(dp(c, 14), dp(c, 10), dp(c, 14), dp(c, 10));
        cancel.setOnClickListener(v -> d.dismiss());
        TextView ok = text(c, action, 14, Theme.accent(), true);
        ok.setPadding(dp(c, 14), dp(c, 10), dp(c, 14), dp(c, 10));
        ok.setOnClickListener(v -> {
            String value = input.getText().toString().trim();
            d.dismiss();
            callback.onInput(value);
        });
        actions.addView(cancel);
        actions.addView(ok);
        root.addView(actions);
        d.setContentView(root);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = Math.min(screenWidth(c) - dp(c, 44), dp(c, 480));
            w.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.CENTER);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attrs = w.getAttributes();
            attrs.dimAmount = 0.6f;
            w.setAttributes(attrs);
        }
        d.setOnShowListener(x -> {
            input.requestFocus();
            input.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }, 120);
        });
        return d;
    }

    public interface ConfirmCallback {
        void onConfirm();
    }

    public static Dialog confirm(Context c, String title, String message, String action, ConfirmCallback callback) {
        Dialog d = new Dialog(c);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = column(c);
        root.setBackground(bg(c, Theme.surface(), 22));
        root.setPadding(dp(c, 20), dp(c, 20), dp(c, 20), dp(c, 14));
        root.addView(title(c, title, 18));
        if (message != null && !message.isEmpty()) {
            root.addView(space(c, 8));
            TextView body = text(c, message, 14, Theme.dim(), false);
            root.addView(body);
        }
        root.addView(space(c, 16));
        LinearLayout actions = row(c);
        actions.setGravity(Gravity.END);
        TextView cancel = text(c, "Отмена", 14, Theme.dim(), true);
        cancel.setPadding(dp(c, 14), dp(c, 10), dp(c, 14), dp(c, 10));
        cancel.setOnClickListener(v -> d.dismiss());
        TextView ok = text(c, action, 14, Theme.danger(), true);
        ok.setPadding(dp(c, 14), dp(c, 10), dp(c, 14), dp(c, 10));
        ok.setOnClickListener(v -> {
            d.dismiss();
            callback.onConfirm();
        });
        actions.addView(cancel);
        actions.addView(ok);
        root.addView(actions);
        d.setContentView(root);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = Math.min(screenWidth(c) - dp(c, 44), dp(c, 480));
            w.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.CENTER);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attrs = w.getAttributes();
            attrs.dimAmount = 0.6f;
            w.setAttributes(attrs);
        }
        return d;
    }

    public static void setMargins(View v, int l, int t, int r, int b) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) v.getLayoutParams()).setMargins(l, t, r, b);
            v.requestLayout();
        }
    }

    public static void setWidth(View v, int widthPx) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp != null) {
            lp.width = widthPx;
            v.setLayoutParams(lp);
        }
    }

    public static void setHeight(View v, int heightPx) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp != null) {
            lp.height = heightPx;
            v.setLayoutParams(lp);
        }
    }
}
