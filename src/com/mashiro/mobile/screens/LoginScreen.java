package com.mashiro.mobile.screens;

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

public class LoginScreen extends Screen {
    public LoginScreen(MainActivity a) {
        super(a);
    }

    @Override
    protected View build() {
        ScrollView scroll = new ScrollView(a);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Theme.bg());
        LinearLayout body = Ui.column(a);
        body.setPadding(Ui.dp(a, 24), Ui.dp(a, 16), Ui.dp(a, 24), Ui.dp(a, 32));
        scroll.addView(body, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // soft accent glow at the top
        View glow = new View(a) {
            @Override
            protected void onDraw(android.graphics.Canvas canvas) {
                android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                paint.setShader(new android.graphics.RadialGradient(getWidth() / 2f, 0,
                        Math.max(getWidth(), Ui.dp(getContext(), 300)),
                        new int[]{Theme.accentSoft(42), Theme.accentSoft(0)},
                        new float[]{0f, 1f}, android.graphics.Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            }
        };
        body.addView(glow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 150)));

        LinearLayout mark = Ui.row(a);
        mark.setGravity(Gravity.CENTER);
        LinearLayout logo = Ui.row(a);
        logo.setGravity(Gravity.CENTER);
        android.graphics.drawable.GradientDrawable logoBg =
                Ui.gradient(a, Theme.surface2(), Theme.surface(), 28);
        logoBg.setStroke(Math.max(1, Ui.dp(a, 1)), Theme.hairline());
        logo.setBackground(logoBg);
        android.widget.ImageView icon = Ui.icon(a, R.drawable.ic_wave, 42, Theme.accent());
        logo.addView(icon);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(Ui.dp(a, 88), Ui.dp(a, 88));
        mark.addView(logo, llp);
        body.addView(mark);

        TextView title = Ui.title(a, "Mashiro", 34);
        title.setGravity(Gravity.CENTER);
        title.setLetterSpacing(-0.02f);
        title.setPadding(0, Ui.dp(a, 22), 0, 0);
        body.addView(title);

        TextView sub = Ui.overline(a, "Неофициальный клиент Яндекс Музыки");
        sub.setTextColor(Theme.faint());
        sub.setGravity(Gravity.CENTER);
        sub.setLetterSpacing(0.14f);
        sub.setPadding(0, Ui.dp(a, 9), 0, 0);
        body.addView(sub);

        body.addView(Ui.space(a, 30));

        LinearLayout card = Ui.column(a);
        android.graphics.drawable.GradientDrawable cardBg =
                Ui.gradient(a, Theme.surface2(), Theme.surface(), 22);
        cardBg.setStroke(Math.max(1, Ui.dp(a, 1)), Theme.hairline());
        card.setBackground(cardBg);
        card.setPadding(Ui.dp(a, 18), Ui.dp(a, 18), Ui.dp(a, 18), Ui.dp(a, 6));
        card.addView(feature(R.drawable.ic_library, "Вся ваша медиатека",
                "Лайки, плейлисты и альбомы — в одном месте"));
        card.addView(Widgets.rowDivider(a));
        card.addView(feature(R.drawable.ic_wave, "Моя волна",
                "Персональная станция с лайками и дизлайками"));
        card.addView(Widgets.rowDivider(a));
        card.addView(feature(R.drawable.ic_headphones, "Фоновое воспроизведение",
                "Музыка продолжает играть на экране блокировки"));
        body.addView(card);

        body.addView(Ui.space(a, 26));

        TextView login = Ui.primaryButton(a, "Войти через Яндекс",
                v -> new OAuthLogin(a, token -> finishLogin(token)).show());
        login.setPadding(0, Ui.dp(a, 17), 0, Ui.dp(a, 17));
        body.addView(login);

        TextView loginHint = Ui.text(a,
                "Откроется страница Яндекс ID. Пароль и токен останутся на устройстве — токен подставится автоматически.",
                11, Theme.faint(), false);
        loginHint.setGravity(Gravity.CENTER);
        loginHint.setLineSpacing(0, 1.25f);
        loginHint.setPadding(Ui.dp(a, 12), Ui.dp(a, 10), Ui.dp(a, 12), 0);
        body.addView(loginHint);

        body.addView(Ui.space(a, 20));
        body.addView(orDivider());
        body.addView(Ui.space(a, 20));

        TextView token = Ui.secondaryButton(a, "Вставить OAuth-токен", v -> pasteToken());
        token.setPadding(0, Ui.dp(a, 15), 0, Ui.dp(a, 15));
        body.addView(token);

        body.addView(Ui.space(a, 10));

        TextView guest = Ui.secondaryButton(a, "Войти без аккаунта", v -> {
            Store.setGuestChosen(true);
            a.startApp();
        });
        guest.setPadding(0, Ui.dp(a, 15), 0, Ui.dp(a, 15));
        body.addView(guest);

        TextView guestHint = Ui.text(a,
                "Гостевой режим: поиск, каталог и свои файлы. Лайки и плейлисты требуют аккаунта.",
                11, Theme.faint(), false);
        guestHint.setGravity(Gravity.CENTER);
        guestHint.setLineSpacing(0, 1.25f);
        guestHint.setPadding(Ui.dp(a, 12), Ui.dp(a, 10), Ui.dp(a, 12), 0);
        body.addView(guestHint);

        TextView how = Ui.text(a, "Как получить токен?", 12, Theme.accent(), true);
        how.setGravity(Gravity.CENTER);
        how.setPadding(0, Ui.dp(a, 20), 0, 0);
        how.setOnClickListener(v -> showTokenHelp());
        body.addView(how);

        TextView note = Ui.text(a,
                "Токен хранится только на этом устройстве. Для полного воспроизведения нужна активная подписка Яндекс Плюс. Используется неофициальное API.",
                11, Theme.faint(), false);
        note.setGravity(Gravity.CENTER);
        note.setLineSpacing(0, 1.3f);
        note.setPadding(Ui.dp(a, 12), Ui.dp(a, 28), Ui.dp(a, 12), 0);
        body.addView(note);

        TextView version = Ui.text(a, "MASHIRO MOBILE 1.2.0", 9.5f, Theme.faint(), false);
        version.setGravity(Gravity.CENTER);
        version.setLetterSpacing(0.2f);
        version.setPadding(0, Ui.dp(a, 22), 0, 0);
        body.addView(version);
        return scroll;
    }

    /** Thin rule with a centered caption used to separate login options. */
    private View orDivider() {
        LinearLayout row = Ui.row(a);
        View left = new View(a);
        left.setBackgroundColor(Theme.hairline());
        row.addView(left, new LinearLayout.LayoutParams(0, Math.max(1, Ui.dp(a, 1)), 1f));
        TextView caption = Ui.text(a, "или", 10.5f, Theme.faint(), false);
        caption.setLetterSpacing(0.14f);
        caption.setPadding(Ui.dp(a, 12), 0, Ui.dp(a, 12), 0);
        row.addView(caption);
        View right = new View(a);
        right.setBackgroundColor(Theme.hairline());
        row.addView(right, new LinearLayout.LayoutParams(0, Math.max(1, Ui.dp(a, 1)), 1f));
        return row;
    }

    private View feature(int icon, String title, String subtitle) {
        LinearLayout row = Ui.row(a);
        row.setPadding(0, Ui.dp(a, 12), 0, Ui.dp(a, 12));
        row.addView(Widgets.circleIcon(a, icon, 38, Theme.surface3(), Theme.text()));
        LinearLayout copy = Ui.column(a);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = Ui.dp(a, 13);
        copy.setLayoutParams(clp);
        copy.addView(Ui.text(a, title, 13.5f, Theme.text(), true));
        TextView sub = Ui.text(a, subtitle, 11.5f, Theme.dim(), false);
        sub.setPadding(0, Ui.dp(a, 3), 0, 0);
        sub.setLineSpacing(0, 1.2f);
        copy.addView(sub);
        row.addView(copy);
        return row;
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
                Store.setGuestChosen(false);
                App.applySession();
                Ui.toast(a, "Добро пожаловать, " + (profile.name.isEmpty() ? profile.login : profile.name));
                a.startApp();
            });
        }).show();
    }

    private void finishLogin(String token) {
        Ui.toast(a, "Входим в аккаунт…");
        a.bg(() -> {
            App.api.setToken(token, 0);
            return App.api.profile();
        }, profile -> {
            Store.saveSession(token, profile);
            Store.setGuestChosen(false);
            App.applySession();
            Ui.toast(a, "Добро пожаловать, " + (profile.name.isEmpty() ? profile.login : profile.name));
            a.startApp();
        });
    }

    private void showTokenHelp() {
        Ui.confirm(a, "Где взять токен",
                "Проще всего — кнопкой «Войти через Яндекс»: приложение откроет страницу Яндекс ID и само заберёт токен. "
                        + "Вручную: войдите на music.yandex.ru, откройте music.yandex.ru/get-music-token и скопируйте токен. "
                        + "Никому не передавайте его.",
                "Понятно", () -> {}).show();
    }
}
