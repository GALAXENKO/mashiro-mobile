package com.mashiro.mobile.screens;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
    private static final String[] ACCENTS = {"#FFFFFF", "#FF6B6B", "#FFB86B", "#FFC24B",
            "#7BE495", "#3ED6A6", "#57D9D9", "#6BB8FF", "#B79CFF", "#FF7A9C", "#FF9CD0"};

    private interface Pick {
        void on(String value);
    }

    private LinearLayout content;

    public SettingsScreen(MainActivity a) {
        super(a);
    }

    @Override
    protected View build() {
        Widgets.Page page = Widgets.page(a);
        ScrollView scroll = page.scroll;
        page.body.addView(Widgets.header(a, "Настройки", "Mashiro Mobile 1.2", true, 0, null));
        content = Ui.column(a);
        page.body.addView(content);
        render();
        return scroll;
    }

    private void render() {
        content.removeAllViews();

        // ---------------------------------------------------------- account
        content.addView(Widgets.sectionHeader(a, "Аккаунт", null, null));
        content.addView(profileCard());
        content.addView(accountActions());

        // ----------------------------------------------------------- theme
        LinearLayout design = section("Оформление");
        addRow(design, choiceRow(R.drawable.ic_eye, "Тема", null,
                new String[][]{{"dark", "Тёмная"}, {"light", "Светлая"}},
                Store.isLight() ? "light" : "dark",
                value -> setTheme("light".equals(value))));
        addRow(design, accentRow());
        addRow(design, Widgets.listRow(a, R.drawable.ic_wave, "Меньше анимаций",
                "Отключить переходы между экранами",
                Store.reduceMotion() ? "Вкл" : "Выкл", () -> {
                    Store.setReduceMotion(!Store.reduceMotion());
                    render();
                }));

        // -------------------------------------------------------- playback
        LinearLayout playback = section("Воспроизведение");
        addRow(playback, choiceRow(R.drawable.ic_headphones, "Качество потока",
                "Битрейт для онлайн-воспроизведения",
                new String[][]{{"low", "Низкое"}, {"normal", "Обычное"},
                        {"high", "Высокое"}, {"lossless", "Lossless"}},
                Store.quality(), this::setQuality));
        addRow(playback, Widgets.listRow(a, R.drawable.ic_note, "Статистика прослушиваний",
                Store.totalPlays() + " " + Ui.plural(Store.totalPlays(), "прослушивание",
                        "прослушивания", "прослушиваний"), "Открыть", () -> a.openStats()));
        addRow(playback, Widgets.listRow(a, R.drawable.ic_download, "Скачанные треки",
                Store.downloadedIds().size() + " " + Ui.plural(Store.downloadedIds().size(),
                        "файл", "файла", "файлов") + " без сети", "Открыть",
                () -> a.open(new LibraryScreen(a, 2))));

        // ----------------------------------------------------------- lyrics
        LinearLayout lyrics = section("Тексты песен");
        addRow(lyrics, Widgets.listRow(a, R.drawable.ic_lyrics, "Источники текстов",
                "LRCLIB · Netease · Яндекс · lyrics.ovh", "Порядок", () ->
                        Ui.confirm(a, "Откуда берутся тексты",
                                "Сначала ищем синхронизированный текст в LRCLIB, затем в Netease. "
                                        + "Если там ничего нет — используем текст из Яндекса, "
                                        + "а в конце пробуем lyrics.ovh. Первый найденный вариант "
                                        + "и показывается, с подписью источника.",
                                "Понятно", () -> {}).show()));
        addRow(lyrics, Widgets.listRow(a, R.drawable.ic_note, "Синхронный текст",
                "Подсветка текущей строки и автопрокрутка", "Авто", null));

        // --------------------------------------------------------- downloads
        LinearLayout downloads = section("Загрузки");
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            addRow(downloads, Widgets.listRow(a, R.drawable.ic_download, "Папка на телефоне",
                    "Music/" + Store.downloadFolder(), "Изменить", () ->
                            Ui.input(a, "Папка загрузок", Store.downloadFolder(),
                                    "Mashiro", "Сохранить", false, value -> {
                                        Store.setDownloadFolder(value);
                                        render();
                                        Ui.toast(a, "Папка: Music/" + Store.downloadFolder());
                                    }).show()));
        } else {
            addRow(downloads, Widgets.listRow(a, R.drawable.ic_download, "Папка на телефоне",
                    "Android/data/com.mashiro.mobile/files/Music", null, null));
        }
        addRow(downloads, choiceRow(R.drawable.ic_arrow_down, "Качество загрузки", null,
                new String[][]{{"high", "Высокое 320"}, {"original", "Как в потоке"}},
                Store.downloadFormat(), value -> {
                    Store.setDownloadFormat(value);
                    render();
                }));

        // -------------------------------------------------------- maintenance
        LinearLayout care = section("Обслуживание");
        addRow(care, Widgets.listRow(a, R.drawable.ic_trash, "Кэш обложек",
                "Освобождает место и перезагружает изображения", "Очистить", () -> {
                    ImageLoader.clear();
                    Ui.toast(a, "Кэш обложек очищен");
                }));
        addRow(care, Widgets.listRow(a, R.drawable.ic_search, "История поиска",
                "Список недавних запросов", "Очистить", () -> {
                    Store.clearSearches();
                    Ui.toast(a, "История поиска очищена");
                }));
        addRow(care, Widgets.listRow(a, R.drawable.ic_note, "Статистика",
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
                "Неофициальный клиент Яндекс Музыки · 1.2.0",
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

        TextView version = Ui.text(a, "Mashiro Mobile 1.2.0 · Android", 10, Theme.faint(), false);
        version.setGravity(Gravity.CENTER);
        version.setPadding(0, Ui.dp(a, 18), 0, 0);
        content.addView(version);
    }

    private View profileCard() {
        LinearLayout profile = Ui.row(a);
        GradientDrawable bg = Ui.gradient(a, Theme.surface2(), Theme.surface(), 20);
        bg.setStroke(Math.max(1, Ui.dp(a, 1)), Theme.hairline());
        profile.setBackground(bg);
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
        return profile;
    }

    private View accountActions() {
        boolean guest = Store.guestMode();
        LinearLayout actions = Ui.row(a);
        actions.setPadding(0, Ui.dp(a, 8), 0, 0);
        if (guest) {
            actions.addView(Widgets.actionChip(a, R.drawable.ic_person, "Войти в аккаунт",
                    () -> a.showLogin()), chipLp(0));
            actions.addView(Widgets.actionChip(a, R.drawable.ic_note, "Что доступно",
                    () -> Ui.confirm(a, "Гостевой режим",
                            "Доступны каталог, поиск, волна и свои файлы. "
                                    + "Лайки, плейлисты и профиль требуют аккаунта Яндекса.",
                            "Понятно", () -> {}).show()), chipLp(8));
        } else {
            actions.addView(Widgets.actionChip(a, R.drawable.ic_person, "Профиль Яндекс",
                    () -> a.openUrl("https://passport.yandex.ru/profile")), chipLp(0));
            actions.addView(Widgets.actionChip(a, R.drawable.ic_trash, "Выйти",
                    () -> Ui.confirm(a, "Выйти из аккаунта?",
                            "Очередь и кэш обложек будут очищены",
                            "Выйти", () -> a.logout()).show()), chipLp(8));
        }
        return actions;
    }

    private LinearLayout section(String title) {
        content.addView(Widgets.sectionHeader(a, title, null, null));
        LinearLayout group = Widgets.group(a);
        content.addView(group);
        return group;
    }

    private void addRow(LinearLayout group, View row) {
        if (group.getChildCount() > 0) group.addView(Widgets.rowDivider(a));
        group.addView(row);
    }

    private View choiceRow(int icon, String title, String subtitle, String[][] options,
                           String current, Pick pick) {
        LinearLayout row = Ui.row(a);
        row.setPadding(Ui.dp(a, 14), Ui.dp(a, 12), Ui.dp(a, 12), Ui.dp(a, 12));
        row.addView(Widgets.circleIcon(a, icon, 36, Theme.surface2(), Theme.text()));

        LinearLayout copy = Ui.column(a);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = Ui.dp(a, 12);
        copy.setLayoutParams(clp);
        copy.addView(Ui.text(a, title, 14, Theme.text(), true));
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView sub = Ui.text(a, subtitle, 11.5f, Theme.dim(), false);
            sub.setMaxLines(2);
            sub.setPadding(0, Ui.dp(a, 1), 0, 0);
            copy.addView(sub);
        }

        LinearLayout chips = Ui.row(a);
        chips.setPadding(0, Ui.dp(a, 9), 0, 0);
        for (String[] option : options) {
            TextView chip = Ui.chip(a, option[1], option[0].equals(current),
                    v -> pick.on(option[0]));
            chips.addView(chip, chipLp(chips.getChildCount() == 0 ? 0 : 8));
        }
        copy.addView(Widgets.horizontalScroll(a, chips));
        row.addView(copy);
        return row;
    }

    private View accentRow() {
        LinearLayout row = Ui.row(a);
        row.setPadding(Ui.dp(a, 14), Ui.dp(a, 12), Ui.dp(a, 12), Ui.dp(a, 12));
        row.addView(Widgets.circleIcon(a, R.drawable.ic_heart, 36, Theme.surface2(), Theme.text()));

        LinearLayout copy = Ui.column(a);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = Ui.dp(a, 12);
        copy.setLayoutParams(clp);
        copy.addView(Ui.text(a, "Акцентный цвет", 14, Theme.text(), true));

        LinearLayout swatches = Ui.row(a);
        swatches.setPadding(0, Ui.dp(a, 10), 0, 0);
        for (String hex : ACCENTS) {
            swatches.addView(swatch(hex), swatchLp());
        }
        copy.addView(Widgets.horizontalScroll(a, swatches));
        row.addView(copy);
        return row;
    }

    private LinearLayout.LayoutParams chipLp(int left) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = Ui.dp(a, left);
        return lp;
    }

    private LinearLayout.LayoutParams swatchLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                Ui.dp(a, 34), Ui.dp(a, 34));
        lp.rightMargin = Ui.dp(a, 10);
        return lp;
    }

    private View swatch(String hex) {
        android.widget.FrameLayout box = new android.widget.FrameLayout(a);
        box.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(a, 34), Ui.dp(a, 34)));
        int color;
        try {
            color = Color.parseColor(hex);
        } catch (Exception e) {
            color = Color.WHITE;
        }
        boolean active = Store.accent().equalsIgnoreCase(hex);
        box.setBackground(Ui.strokeBg(a, color, 17,
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
