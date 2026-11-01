package com.mashiro.mobile.screens;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mashiro.mobile.App;
import com.mashiro.mobile.ImageLoader;
import com.mashiro.mobile.MainActivity;
import com.mashiro.mobile.R;
import com.mashiro.mobile.Screen;
import com.mashiro.mobile.Store;
import com.mashiro.mobile.Theme;
import com.mashiro.mobile.Ui;
import com.mashiro.mobile.Widgets;

/** Theme, playback quality, account and maintenance settings. */
public class SettingsScreen extends Screen {
    private LinearLayout content;

    public SettingsScreen(MainActivity a) {
        super(a);
    }

    @Override
    protected View build() {
        Widgets.Page page = Widgets.page(a);
        ScrollView scroll = page.scroll;
        page.body.addView(Widgets.header(a, "Настройки", "Mashiro Mobile 1.1", true, 0, null));
        content = Ui.column(a);
        page.body.addView(content);
        render();
        return scroll;
    }

    private void render() {
        content.removeAllViews();

        // ---------------------------------------------------------- account
        content.addView(Widgets.sectionHeader(a, "Аккаунт", null, null));
        LinearLayout profile = Ui.row(a);
        profile.setBackground(Ui.strokeBg(a, Theme.surface(), 18, Theme.border(), 1));
        profile.setPadding(Ui.dp(a, 16), Ui.dp(a, 16), Ui.dp(a, 16), Ui.dp(a, 16));
        boolean guest = Store.guestMode();
        profile.addView(Widgets.avatar(a, guest ? "" : Store.avatar(), 54));
        LinearLayout copy = Ui.column(a);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = Ui.dp(a, 14);
        copy.setLayoutParams(clp);
        copy.addView(Ui.title(a, guest ? "Гостевой режим" : Store.name(), 19));
        TextView login = Ui.text(a,
                guest ? "Аккаунт не подключён"
                        : (Store.login().isEmpty() ? "Яндекс ID" : "@" + Store.login()),
                11.5f, Theme.dim(), false);
        login.setPadding(0, Ui.dp(a, 3), 0, 0);
        copy.addView(login);
        if (!guest && Store.hasPlus()) {
            TextView plus = Ui.pill(a, "Плюс активен", Theme.accent());
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            plp.topMargin = Ui.dp(a, 6);
            copy.addView(plus, plp);
        }
        profile.addView(copy);
        content.addView(profile);

        LinearLayout accountActions = Ui.row(a);
        accountActions.setPadding(0, Ui.dp(a, 8), 0, 0);
        if (guest) {
            accountActions.addView(Widgets.actionChip(a, R.drawable.ic_person, "Войти в аккаунт",
                    () -> a.showLogin()), chipLp(0));
            accountActions.addView(Widgets.actionChip(a, R.drawable.ic_note, "Что доступно",
                    () -> Ui.confirm(a, "Гостевой режим",
                            "Доступны каталог, поиск, волна и свои файлы. "
                                    + "Лайки, плейлисты и профиль требуют аккаунта Яндекса.",
                            "Понятно", () -> {}).show()), chipLp(8));
        } else {
            accountActions.addView(Widgets.actionChip(a, R.drawable.ic_person, "Профиль Яндекс",
                    () -> a.openUrl("https://passport.yandex.ru/profile")),
                    chipLp(0));
            accountActions.addView(Widgets.actionChip(a, R.drawable.ic_trash, "Выйти",
                    () -> Ui.confirm(a, "Выйти из аккаунта?", "Очередь и кэш обложек будут очищены",
                            "Выйти", () -> a.logout()).show()), chipLp(8));
        }
        content.addView(accountActions);

        // ----------------------------------------------------------- theme
        content.addView(Widgets.sectionHeader(a, "Оформление", null, null));
        LinearLayout themeRow = Ui.row(a);
        TextView dark = Ui.chip(a, "Тёмная", !Store.isLight(), v -> setTheme(false));
        TextView light = Ui.chip(a, "Светлая", Store.isLight(), v -> setTheme(true));
        themeRow.addView(dark, chipLp(0));
        themeRow.addView(light, chipLp(8));
        content.addView(themeRow);

        TextView accentLabel = Ui.text(a, "Акцентный цвет", 12, Theme.dim(), true);
        accentLabel.setPadding(0, Ui.dp(a, 14), 0, Ui.dp(a, 8));
        content.addView(accentLabel);

        String[] colors = {"#FFFFFF", "#FF6B6B", "#FFB86B", "#FFC24B", "#7BE495",
                "#3ED6A6", "#57D9D9", "#6BB8FF", "#B79CFF", "#FF7A9C", "#FF9CD0"};
        LinearLayout swatches = Ui.row(a);
        for (String hex : colors) {
            swatches.addView(swatch(hex), swatchLp());
        }
        content.addView(Widgets.horizontalScroll(a, swatches));

        content.addView(settingRow(R.drawable.ic_eye, "Меньше анимаций",
                "Отключить переходы между экранами", Store.reduceMotion() ? "Вкл" : "Выкл",
                () -> {
                    Store.setReduceMotion(!Store.reduceMotion());
                    render();
                }));

        // -------------------------------------------------------- playback
        content.addView(Widgets.sectionHeader(a, "Воспроизведение", null, null));
        LinearLayout qualityRow = Ui.row(a);
        String[][] qualities = {{"low", "Низкое"}, {"normal", "Обычное"},
                {"high", "Высокое"}, {"lossless", "Lossless"}};
        for (int i = 0; i < qualities.length; i++) {
            final String value = qualities[i][0];
            TextView chip = Ui.chip(a, qualities[i][1], value.equals(Store.quality()),
                    v -> setQuality(value));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = Ui.dp(a, 8);
            chip.setLayoutParams(lp);
            qualityRow.addView(chip);
        }
        content.addView(Widgets.horizontalScroll(a, qualityRow));

        content.addView(settingRow(R.drawable.ic_note, "Статистика прослушиваний",
                Store.totalPlays() + " " + Ui.plural(Store.totalPlays(), "прослушивание",
                        "прослушивания", "прослушиваний"), "Открыть", () -> a.openStats()));

        content.addView(settingRow(R.drawable.ic_download, "Скачанные треки",
                Store.downloadedIds().size() + " " + Ui.plural(Store.downloadedIds().size(),
                        "файл", "файла", "файлов") + " без сети", "Открыть",
                () -> a.open(new LibraryScreen(a, 2))));

        // --------------------------------------------------------- downloads
        content.addView(Widgets.sectionHeader(a, "Загрузки", null, null));
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            content.addView(settingRow(R.drawable.ic_download, "Папка на телефоне",
                    "Music/" + Store.downloadFolder(), "Изменить", () ->
                            Ui.input(a, "Папка загрузок", Store.downloadFolder(),
                                    "Mashiro", "Сохранить", false, value -> {
                                        Store.setDownloadFolder(value);
                                        render();
                                        Ui.toast(a, "Папка: Music/" + Store.downloadFolder());
                                    }).show()));
        } else {
            content.addView(settingRow(R.drawable.ic_download, "Папка на телефоне",
                    "Android/data/com.mashiro.mobile/files/Music", "", () -> {}));
        }

        TextView formatLabel = Ui.text(a, "Качество загрузки", 12, Theme.dim(), true);
        formatLabel.setPadding(0, Ui.dp(a, 6), 0, Ui.dp(a, 8));
        content.addView(formatLabel);
        LinearLayout formatRow = Ui.row(a);
        TextView highFormat = Ui.chip(a, "Высокое 320", "high".equals(Store.downloadFormat()),
                v -> {
                    Store.setDownloadFormat("high");
                    render();
                });
        TextView originalFormat = Ui.chip(a, "Как в потоке",
                "original".equals(Store.downloadFormat()), v -> {
                    Store.setDownloadFormat("original");
                    render();
                });
        formatRow.addView(highFormat, chipLp(0));
        formatRow.addView(originalFormat, chipLp(8));
        content.addView(formatRow);

        // -------------------------------------------------------- maintenance
        content.addView(Widgets.sectionHeader(a, "Обслуживание", null, null));
        content.addView(settingRow(R.drawable.ic_trash, "Очистить кэш обложек",
                "Освобождает место и перезагружает изображения", "Очистить", () -> {
                    ImageLoader.clear();
                    Ui.toast(a, "Кэш обложек очищен");
                }));
        content.addView(settingRow(R.drawable.ic_search, "Очистить историю поиска",
                "Список недавних запросов", "Очистить", () -> {
                    Store.clearSearches();
                    Ui.toast(a, "История поиска очищена");
                }));
        content.addView(settingRow(R.drawable.ic_note, "Сбросить статистику",
                "Локальные счётчики прослушиваний", "Сбросить", () ->
                        Ui.confirm(a, "Сбросить статистику?", "Это действие необратимо",
                                "Сбросить", () -> {
                                    Store.clearStats();
                                    render();
                                    Ui.toast(a, "Статистика сброшена");
                                }).show()));

        // -------------------------------------------------------------- about
        content.addView(Widgets.sectionHeader(a, "О приложении", null, null));
        content.addView(Widgets.actionCard(a, R.drawable.ic_music, "Mashiro Mobile",
                "Неофициальный клиент Яндекс Музыки · 1.1.0",
                () -> a.openUrl("https://github.com/aeviww/yandex-music-client")));
        content.addView(Widgets.actionCard(a, R.drawable.ic_person, "Исходный проект",
                "aeviww/yandex-music-client на GitHub",
                () -> a.openUrl("https://github.com/aeviww/yandex-music-client")));

        TextView disclaimer = Ui.text(a,
                "Приложение не связано с Яндексом. Все треки, обложки и тексты принадлежат "
                        + "их правообладателям и загружаются через официальный API сервиса. "
                        + "Используйте свой аккаунт и активную подписку.",
                11, Theme.faint(), false);
        disclaimer.setLineSpacing(0, 1.3f);
        disclaimer.setPadding(0, Ui.dp(a, 10), 0, Ui.dp(a, 4));
        content.addView(disclaimer);

        TextView version = Ui.text(a, "Mashiro Mobile 1.1.0 · Android", 10, Theme.faint(), false);
        version.setGravity(Gravity.CENTER);
        version.setPadding(0, Ui.dp(a, 18), 0, 0);
        content.addView(version);
    }

    private LinearLayout.LayoutParams chipLp(int left) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = Ui.dp(a, left);
        return lp;
    }

    private LinearLayout.LayoutParams swatchLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                Ui.dp(a, 36), Ui.dp(a, 36));
        lp.rightMargin = Ui.dp(a, 10);
        return lp;
    }

    private View swatch(String hex) {
        android.widget.FrameLayout box = new android.widget.FrameLayout(a);
        box.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(a, 36), Ui.dp(a, 36)));
        int color;
        try {
            color = Color.parseColor(hex);
        } catch (Exception e) {
            color = Color.WHITE;
        }
        boolean active = Store.accent().equalsIgnoreCase(hex);
        box.setBackground(Ui.strokeBg(a, color, 18,
                active ? Theme.text() : Theme.border(), active ? 2 : 1));
        if (active) {
            View check = Ui.icon(a, R.drawable.ic_check, 16,
                    Theme.light() && hex.equalsIgnoreCase("#FFFFFF") ? Color.BLACK : Theme.accentText());
            android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(
                    Ui.dp(a, 16), Ui.dp(a, 16));
            lp.gravity = Gravity.CENTER;
            box.addView(check, lp);
        }
        box.setOnClickListener(v -> {
            Store.setAccent(hex);
            Theme.apply(a);
            a.recreate();
        });
        return box;
    }

    private View settingRow(int icon, String title, String subtitle, String value, Runnable click) {
        LinearLayout row = Ui.row(a);
        row.setBackground(Ui.strokeBg(a, Theme.surface(), 15, Theme.border(), 1));
        row.setPadding(Ui.dp(a, 14), Ui.dp(a, 13), Ui.dp(a, 14), Ui.dp(a, 13));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(a, 8);
        row.setLayoutParams(lp);
        row.addView(Widgets.circleIcon(a, icon, 38, Theme.surface2(), Theme.text()));
        LinearLayout copy = Ui.column(a);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = Ui.dp(a, 12);
        copy.setLayoutParams(clp);
        copy.addView(Ui.text(a, title, 14, Theme.text(), true));
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView sub = Ui.text(a, subtitle, 11.5f, Theme.dim(), false);
            sub.setMaxLines(2);
            copy.addView(sub);
        }
        row.addView(copy);
        TextView action = Ui.text(a, value, 12, Theme.accent(), true);
        action.setPadding(Ui.dp(a, 8), Ui.dp(a, 4), 0, Ui.dp(a, 4));
        row.addView(action);
        row.setOnClickListener(v -> click.run());
        Ui.ripple(row, 0x1AFFFFFF);
        return row;
    }

    private void setTheme(boolean light) {
        if (Store.isLight() == light) return;
        Store.setLight(light);
        Theme.apply(a);
        a.recreate();
    }

    private void setQuality(String value) {
        Store.setQuality(value);
        if (App.api != null) App.api.quality = value;
        render();
        Ui.toast(a, "Качество: " + qualityLabel(value));
    }

    private String qualityLabel(String value) {
        if ("low".equals(value)) return "низкое";
        if ("normal".equals(value)) return "обычное";
        if ("lossless".equals(value)) return "lossless";
        return "высокое";
    }
}
