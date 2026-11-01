package com.mashiro.mobile.screens;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mashiro.mobile.App;
import com.mashiro.mobile.MainActivity;
import com.mashiro.mobile.R;
import com.mashiro.mobile.Screen;
import com.mashiro.mobile.Store;
import com.mashiro.mobile.Theme;
import com.mashiro.mobile.Ui;
import com.mashiro.mobile.Widgets;
import com.mashiro.mobile.api.Models;

import org.json.JSONObject;

public class LoginScreen extends Screen {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean polling = false;

    public LoginScreen(MainActivity a) {
        super(a);
    }

    @Override
    protected View build() {
        ScrollView scroll = new ScrollView(a);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Theme.bg());
        LinearLayout body = Ui.column(a);
        body.setPadding(Ui.dp(a, 26), Ui.dp(a, 24), Ui.dp(a, 26), Ui.dp(a, 30));
        scroll.addView(body, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // soft accent glow at the top
        View glow = new View(a) {
            @Override
            protected void onDraw(android.graphics.Canvas canvas) {
                android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                paint.setShader(new android.graphics.RadialGradient(getWidth() / 2f, 0,
                        Math.max(getWidth(), Ui.dp(getContext(), 260)),
                        new int[]{Theme.accentSoft(38), Theme.accentSoft(0)},
                        new float[]{0f, 1f}, android.graphics.Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            }
        };
        body.addView(glow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 130)));

        LinearLayout mark = Ui.row(a);
        mark.setGravity(Gravity.CENTER);
        LinearLayout logo = Ui.row(a);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(Ui.bg(a, Theme.accentSoft(24), 26));
        android.widget.ImageView icon = Ui.icon(a, R.drawable.ic_wave, 40, Theme.accent());
        logo.addView(icon);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(Ui.dp(a, 84), Ui.dp(a, 84));
        mark.addView(logo, llp);
        body.addView(mark);

        TextView title = Ui.title(a, "Mashiro", 34);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, Ui.dp(a, 20), 0, 0);
        body.addView(title);

        TextView sub = Ui.text(a, "неофициальный клиент Яндекс Музыки", 13, Theme.dim(), false);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, Ui.dp(a, 6), 0, 0);
        body.addView(sub);

        body.addView(Ui.space(a, 34));

        LinearLayout card = Ui.column(a);
        card.setBackground(Ui.strokeBg(a, Theme.surface(), 18, Theme.border(), 1));
        card.setPadding(Ui.dp(a, 18), Ui.dp(a, 18), Ui.dp(a, 18), Ui.dp(a, 18));
        card.addView(feature(R.drawable.ic_library, "Вся ваша медиатека",
                "Лайки, плейлисты и альбомы — в одном месте"));
        card.addView(feature(R.drawable.ic_wave, "Моя волна",
                "Персональная станция с лайками и дизлайками"));
        card.addView(feature(R.drawable.ic_headphones, "Фоновое воспроизведение",
                "Музыка продолжает играть на экране блокировки"));
        body.addView(card);

        body.addView(Ui.space(a, 22));

        TextView login = Ui.primaryButton(a, "Войти через Яндекс", v -> startDeviceFlow());
        login.setPadding(0, Ui.dp(a, 15), 0, Ui.dp(a, 15));
        body.addView(login);

        body.addView(Ui.space(a, 10));

        TextView token = Ui.secondaryButton(a, "Вставить OAuth-токен", v -> pasteToken());
        token.setPadding(0, Ui.dp(a, 14), 0, Ui.dp(a, 14));
        body.addView(token);

        TextView how = Ui.text(a, "Как получить токен?", 12, Theme.accent(), true);
        how.setGravity(Gravity.CENTER);
        how.setPadding(0, Ui.dp(a, 18), 0, 0);
        how.setOnClickListener(v -> showTokenHelp());
        body.addView(how);

        TextView note = Ui.text(a,
                "Токен хранится только на этом устройстве. Для полного воспроизведения нужна активная подписка Яндекс Плюс. Используется неофициальное API.",
                11, Theme.faint(), false);
        note.setGravity(Gravity.CENTER);
        note.setLineSpacing(0, 1.25f);
        note.setPadding(Ui.dp(a, 10), Ui.dp(a, 26), Ui.dp(a, 10), 0);
        body.addView(note);

        TextView version = Ui.text(a, "Mashiro Mobile 1.0.0", 10, Theme.faint(), false);
        version.setGravity(Gravity.CENTER);
        version.setPadding(0, Ui.dp(a, 20), 0, 0);
        body.addView(version);
        return scroll;
    }

    private View feature(int icon, String title, String subtitle) {
        LinearLayout row = Ui.row(a);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(a, 14);
        row.setLayoutParams(lp);
        row.addView(Widgets.circleIcon(a, icon, 36, Theme.surface2(), Theme.text()));
        LinearLayout copy = Ui.column(a);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = Ui.dp(a, 12);
        copy.setLayoutParams(clp);
        copy.addView(Ui.text(a, title, 13.5f, Theme.text(), true));
        copy.addView(Ui.text(a, subtitle, 11.5f, Theme.dim(), false));
        row.addView(copy);
        return row;
    }

    private void startDeviceFlow() {
        Ui.toast(a, "Получаем код…");
        a.bg(() -> App.api.requestDeviceCode(), result -> {
            String code = result.optString("user_code", "");
            String deviceCode = result.optString("device_code", "");
            String url = result.optString("verification_url", "https://ya.ru/device");
            int interval = Math.max(2, result.optInt("interval", 5));
            if (code.isEmpty() || deviceCode.isEmpty()) {
                Ui.toast(a, "Не удалось получить код");
                return;
            }
            showCodeDialog(code, url);
            polling = true;
            poll(deviceCode, interval);
        });
    }

    private void poll(String deviceCode, int seconds) {
        if (!polling) return;
        handler.postDelayed(() -> {
            if (!polling) return;
            a.bg(() -> App.api.pollDeviceToken(deviceCode), token -> {
                if (!polling) return;
                if (token == null || token.isEmpty()) {
                    poll(deviceCode, seconds);
                } else {
                    polling = false;
                    finishLogin(token);
                }
            }, true);
        }, seconds * 1000L);
    }

    private void showCodeDialog(String code, String url) {
        Dialog dialog = new Dialog(a);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        LinearLayout root = Ui.column(a);
        root.setBackground(Ui.bg(a, Theme.surface(), 22));
        root.setPadding(Ui.dp(a, 22), Ui.dp(a, 22), Ui.dp(a, 22), Ui.dp(a, 16));
        root.addView(Ui.title(a, "Код для входа", 20));
        TextView hint = Ui.text(a, "Откройте ya.ru/device и введите этот код", 12.5f, Theme.dim(), false);
        hint.setPadding(0, Ui.dp(a, 6), 0, Ui.dp(a, 16));
        root.addView(hint);
        TextView codeView = Ui.text(a, code, 30, Theme.accent(), true);
        codeView.setGravity(Gravity.CENTER);
        codeView.setLetterSpacing(0.14f);
        codeView.setBackground(Ui.bg(a, Theme.surface2(), 14));
        codeView.setPadding(Ui.dp(a, 14), Ui.dp(a, 18), Ui.dp(a, 14), Ui.dp(a, 18));
        root.addView(codeView);
        root.addView(Ui.space(a, 16));
        LinearLayout actions = Ui.row(a);
        TextView open = Ui.primaryButton(a, "Открыть сайт", v -> a.openUrl(url));
        open.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView cancel = Ui.secondaryButton(a, "Отмена", v -> {
            polling = false;
            dialog.dismiss();
        });
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = Ui.dp(a, 8);
        cancel.setLayoutParams(clp);
        actions.addView(open);
        actions.addView(cancel);
        root.addView(actions);
        TextView wait = Ui.text(a, "Ожидаем подтверждения…", 11, Theme.faint(), false);
        wait.setGravity(Gravity.CENTER);
        wait.setPadding(0, Ui.dp(a, 12), 0, 0);
        root.addView(wait);
        dialog.setContentView(root);
        dialog.setOnDismissListener(d -> polling = false);
        android.view.Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            int width = Math.min(Ui.screenWidth(a) - Ui.dp(a, 44), Ui.dp(a, 420));
            w.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            android.view.WindowManager.LayoutParams attrs = w.getAttributes();
            attrs.dimAmount = 0.6f;
            w.setAttributes(attrs);
        }
        dialog.show();
        a.openUrl(url);
    }

    private void pasteToken() {
        Ui.input(a, "OAuth-токен", "", "Вставьте токен из music.yandex.ru", "Войти", false, value -> {
            if (value.isEmpty()) {
                Ui.toast(a, "Токен пустой");
                return;
            }
            Ui.toast(a, "Проверяем токен…");
            a.bg(() -> {
                App.api.setToken(value, 0);
                Models.Profile profile = App.api.profile();
                return profile;
            }, profile -> {
                Store.saveSession(value, profile);
                App.applySession();
                Ui.toast(a, "Добро пожаловать, " + (profile.name.isEmpty() ? profile.login : profile.name));
                a.startApp();
            });
        }).show();
    }

    private void finishLogin(String token) {
        a.bg(() -> {
            App.api.setToken(token, 0);
            return App.api.profile();
        }, profile -> {
            Store.saveSession(token, profile);
            App.applySession();
            Ui.toast(a, "Добро пожаловать, " + (profile.name.isEmpty() ? profile.login : profile.name));
            a.startApp();
        });
    }

    private void showTokenHelp() {
        Ui.confirm(a, "Где взять токен",
                "Откройте music.yandex.ru в браузере, войдите в аккаунт, затем перейдите по адресу music.yandex.ru/get-music-token. Скопируйте токен и вставьте сюда. Никому не передавайте его.",
                "Понятно", () -> {}).show();
    }
}
