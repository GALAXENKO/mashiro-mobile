package com.mashiro.mobile.screens;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mashiro.mobile.MainActivity;
import com.mashiro.mobile.R;
import com.mashiro.mobile.Theme;
import com.mashiro.mobile.Ui;

/**
 * Opens the Yandex OAuth page in an embedded browser and picks the access token
 * straight out of the redirect, so the user never has to copy anything.
 */
public final class OAuthLogin {
    public interface Callback {
        void onToken(String token);
    }

    private static final String AUTH_URL =
            "https://oauth.yandex.ru/authorize?response_type=token&client_id="
                    + com.mashiro.mobile.api.YandexApi.OAUTH_CLIENT_ID;

    private final MainActivity a;
    private final Callback callback;
    private Dialog dialog;
    private WebView web;
    private boolean captured;

    public OAuthLogin(MainActivity a, Callback callback) {
        this.a = a;
        this.callback = callback;
    }

    public void show() {
        dialog = new Dialog(a);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = Ui.column(a);
        root.setBackgroundColor(Theme.bg());

        LinearLayout header = Ui.row(a);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(Ui.dp(a, 6), Ui.dp(a, 8), Ui.dp(a, 10), Ui.dp(a, 8));
        header.setBackgroundColor(Theme.bg());

        header.addView(Ui.iconButton(a, R.drawable.ic_close, 40, 20, Theme.text(),
                v -> dialog.dismiss()));

        LinearLayout copy = Ui.column(a);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = Ui.dp(a, 10);
        copy.setLayoutParams(clp);
        copy.addView(Ui.text(a, "Вход через Яндекс", 15, Theme.text(), true));
        TextView state = Ui.text(a, "Токен подставится автоматически", 11, Theme.faint(), false);
        copy.addView(state);
        header.addView(copy);
        root.addView(header);

        View divider = new View(a);
        divider.setBackgroundColor(Theme.border());
        root.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1));

        web = new WebView(a);
        web.setBackgroundColor(Theme.bg());
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setUserAgentString(settings.getUserAgentString() + " Mashiro/1.2");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handle(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handle(request.getUrl() == null ? "" : request.getUrl().toString());
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (handle(url)) view.stopLoading();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (captured) return;
                if (handle(url)) {
                    view.stopLoading();
                    return;
                }
                // The fragment is not always reported to the client, so ask the page.
                view.evaluateJavascript("(function(){try{return location.href}catch(e){return ''}})()",
                        value -> {
                            String href = unquote(value);
                            if (!href.isEmpty()) handle(href);
                        });
            }
        });

        root.addView(web, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        dialog.setContentView(root);
        dialog.setOnDismissListener(d -> {
            if (web != null) {
                web.stopLoading();
                web.destroy();
                web = null;
            }
        });
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            w.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.show();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        web.loadUrl(AUTH_URL);
    }

    /** Returns true when the URL was consumed (token found or external scheme). */
    private boolean handle(String url) {
        if (captured || url == null || url.isEmpty()) return captured;
        String token = extractToken(url);
        if (token != null) {
            captured = true;
            if (dialog != null) dialog.dismiss();
            callback.onToken(token);
            return true;
        }
        String error = extractParam(url, "error");
        if (error != null) {
            captured = true;
            if (dialog != null) dialog.dismiss();
            Ui.toast(a, "Вход отменён или отклонён Яндексом");
            return true;
        }
        // Non-web schemes (intent://, market://, yandexmusic://) would crash the WebView.
        if (!url.startsWith("http://") && !url.startsWith("https://")
                && !url.startsWith("about:") && !url.startsWith("data:")) {
            try {
                a.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,
                        Uri.parse(url)));
            } catch (Exception ignored) {
                // no app can handle it — just stay in the WebView
            }
            return true;
        }
        return false;
    }

    static String extractToken(String url) {
        if (url == null) return null;
        String token = extractParam(url, "access_token");
        if (token == null || token.length() < 10) return null;
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '.' || c == '_' || c == '-';
            if (!ok) return null;
        }
        return token;
    }

    private static String extractParam(String url, String name) {
        int at = url.indexOf(name + "=");
        if (at < 0) return null;
        int start = at + name.length() + 1;
        int end = start;
        while (end < url.length()) {
            char c = url.charAt(end);
            if (c == '&' || c == '#' || c == '?' || c == '/' || c == '"') break;
            end++;
        }
        if (end <= start) return null;
        return Uri.decode(url.substring(start, end));
    }

    private static String unquote(String value) {
        if (value == null) return "";
        String v = value.trim();
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
            v = v.substring(1, v.length() - 1);
        }
        return v.replace("\\/", "/").replace("\\u0026", "&").replace("\\\"", "\"");
    }
}
