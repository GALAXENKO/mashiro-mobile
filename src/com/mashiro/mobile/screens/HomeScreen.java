package com.mashiro.mobile.screens;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import com.mashiro.mobile.api.Models;
import com.mashiro.mobile.audio.Playback;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeScreen extends Screen {
    private LinearLayout body;
    private LinearLayout playlistsBox;
    private LinearLayout chartBox;
    private LinearLayout recentBox;
    private TextView greeting;
    private TextView nameView;
    private boolean loaded = false;
    private int generation = 0;

    public HomeScreen(MainActivity a) {
        super(a);
    }

    @Override
    protected View build() {
        Widgets.Page page = Widgets.page(a);
        body = page.body;
        ScrollView scroll = page.scroll;

        LinearLayout head = Ui.row(a);
        head.setPadding(0, Ui.dp(a, 16), 0, Ui.dp(a, 6));
        LinearLayout copy = Ui.column(a);
        copy.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        greeting = Ui.overline(a, "");
        copy.addView(greeting);
        TextView name = Ui.title(a, Store.name(), 27);
        name.setSingleLine(true);
        name.setPadding(0, Ui.dp(a, 3), 0, 0);
        nameView = name;
        copy.addView(name);
        head.addView(copy);
        ImageLoader.RoundedImageView avatar = Widgets.avatar(a, Store.avatar(), 38);
        head.addView(avatar);
        View settings = Ui.iconButton(a, R.drawable.ic_settings, 42, 20, Theme.text(),
                v -> a.openSettings());
        head.addView(settings);
        body.addView(head);

        body.addView(hero());

        LinearLayout quick = Ui.row(a);
        quick.setPadding(0, Ui.dp(a, 14), 0, 0);
        quick.addView(quickCard(R.drawable.ic_heart, "Мне нравится", () -> a.openLiked()),
                weightCard(0));
        quick.addView(quickCard(R.drawable.ic_search, "В медиатеке", () -> a.open(new LibrarySearchScreen(a))),
                weightCard(8));
        quick.addView(quickCard(R.drawable.ic_download, "Скачано", () -> a.open(new LibraryScreen(a, 2))),
                weightCard(8));
        body.addView(quick);

        body.addView(Widgets.sectionHeader(a, "Мои плейлисты", "Все",
                v -> a.open(new LibraryScreen(a, 1))));
        playlistsBox = Ui.column(a);
        body.addView(playlistsBox);

        chartBox = Ui.column(a);
        body.addView(chartBox);

        recentBox = Ui.column(a);
        body.addView(recentBox);

        TextView footer = Ui.text(a, "Mashiro Mobile · неофициальный клиент Яндекс Музыки",
                10.5f, Theme.faint(), false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, Ui.dp(a, 28), 0, 0);
        body.addView(footer);

        updateGreeting();
        return scroll;
    }

    private LinearLayout.LayoutParams weightCard(int marginLeft) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = Ui.dp(a, marginLeft);
        return lp;
    }

    private View quickCard(int icon, String text, Runnable click) {
        LinearLayout box = Ui.column(a);
        box.setBackground(Ui.strokeBg(a, Theme.surface(), 16, Theme.border(), 1));
        box.setPadding(Ui.dp(a, 12), Ui.dp(a, 13), Ui.dp(a, 10), Ui.dp(a, 13));
        box.addView(Widgets.circleIcon(a, icon, 32, Theme.surface2(), Theme.text()));
        TextView label = Ui.text(a, text, 11, Theme.dim(), true);
        label.setPadding(0, Ui.dp(a, 11), 0, 0);
        label.setSingleLine(true);
        box.addView(label);
        box.setOnClickListener(v -> click.run());
        Ui.ripple(box, Theme.light() ? 0x12101014 : 0x18FFFFFF);
        return box;
    }

    private View hero() {
        LinearLayout card = Ui.row(a);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Theme.accentSoft(54), Theme.surface2()});
        bg.setCornerRadius(Ui.dp(a, 24));
        bg.setStroke(Math.max(1, Ui.dp(a, 1)), Theme.hairline());
        card.setBackground(bg);
        card.setPadding(Ui.dp(a, 20), Ui.dp(a, 20), Ui.dp(a, 16), Ui.dp(a, 20));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = Ui.dp(a, 16);
        card.setLayoutParams(clp);
        card.setOnClickListener(v -> startWave());

        LinearLayout copy = Ui.column(a);
        copy.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        copy.addView(Ui.overline(a, "Персональная станция"));
        TextView title = Ui.title(a, "Моя волна", 25);
        title.setPadding(0, Ui.dp(a, 6), 0, Ui.dp(a, 2));
        copy.addView(title);
        TextView sub = Ui.text(a, "Бесконечный поток под ваш вкус", 12, Theme.dim(), false);
        copy.addView(sub);
        LinearLayout buttons = Ui.row(a);
        buttons.setGravity(Gravity.CENTER_VERTICAL);
        buttons.setPadding(0, Ui.dp(a, 16), 0, 0);
        TextView play = Ui.text(a, "Слушать", 13, Theme.accentText(), true);
        play.setBackground(Ui.bg(a, Theme.accent(), 30));
        play.setPadding(Ui.dp(a, 20), Ui.dp(a, 10), Ui.dp(a, 20), Ui.dp(a, 10));
        buttons.addView(play);
        TextView hint = Ui.text(a, "лайк · дизлайк · далее", 10.5f, Theme.faint(), false);
        hint.setPadding(Ui.dp(a, 12), 0, 0, 0);
        buttons.addView(hint);
        copy.addView(buttons);
        card.addView(copy);

        LinearLayout mark = Ui.row(a);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(Ui.strokeBg(a, Theme.accentSoft(26), 40, Theme.hairline(), 1));
        mark.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(a, 80), Ui.dp(a, 80)));
        mark.addView(Ui.icon(a, R.drawable.ic_wave, 34, Theme.accent()));
        card.addView(mark);
        return card;
    }

    private void startWave() {
        Ui.toast(a, "Настраиваем волну…");
        a.bg(() -> App.api.wave("user:onyourwave", "", null), wave -> {
            if (wave.tracks.isEmpty()) {
                Ui.toast(a, "Волна пока не отвечает");
                return;
            }
            Playback.get().startQueue(new ArrayList<>(wave.tracks), 0, true,
                    "user:onyourwave", wave.sessionId, wave.batchId);
            a.ensureMini();
            a.setTab(2);
        });
    }

    private void updateGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String text;
        if (hour < 5) text = "Доброй ночи";
        else if (hour < 12) text = "Доброе утро";
        else if (hour < 18) text = "Добрый день";
        else text = "Добрый вечер";
        greeting.setText(text.toUpperCase(java.util.Locale.getDefault()));
    }

    @Override
    public void onShow() {
        updateGreeting();
        if (nameView != null) nameView.setText(Store.name());
        if (!loaded) load();
    }

    @Override
    public void refresh() {
        loaded = false;
        load();
    }

    private void load() {
        loaded = true;
        final int gen = ++generation;
        if (playlistsBox != null) {
            playlistsBox.removeAllViews();
            playlistsBox.addView(Widgets.loading(a));
        }
        a.bg(() -> App.api.playlists(), playlists -> {
            if (gen != generation) return;
            renderPlaylists(playlists);
        });

        a.bg(() -> App.api.chart(), chart -> {
            if (gen != generation) return;
            renderChart(chart);
        }, true);

        List<Models.Track> recent = Store.recent();
        renderRecent(recent);
    }

    private void renderPlaylists(List<Models.Playlist> playlists) {
        playlistsBox.removeAllViews();
        LinearLayout row = Widgets.horizontal(a);
        View create = createCard();
        row.addView(create);
        int count = 0;
        for (Models.Playlist p : playlists) {
            View card = Widgets.playlistCard(a, p, 142);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    Ui.dp(a, 142), ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = Ui.dp(a, 10);
            row.addView(card, lp);
            count++;
            if (count >= 10) break;
        }
        ScrollView scroll = Widgets.horizontalScroll(a, row);
        playlistsBox.addView(scroll);
        if (count == 0) {
            TextView hint = Ui.text(a, "Плейлистов пока нет — создайте первый", 11.5f, Theme.faint(), false);
            hint.setPadding(0, Ui.dp(a, 6), 0, 0);
            playlistsBox.addView(hint);
        }
    }

    private View createCard() {
        LinearLayout card = Ui.column(a);
        card.setGravity(Gravity.CENTER);
        card.setBackground(Ui.strokeBg(a, Theme.surface(), 16, Theme.border(), 1));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                Ui.dp(a, 142), Ui.dp(a, 142));
        card.setLayoutParams(cardLp);
        View circle = Widgets.circleIcon(a, R.drawable.ic_plus, 44, Theme.surface2(), Theme.accent());
        card.addView(circle);
        TextView label = Ui.text(a, "Новый плейлист", 11.5f, Theme.dim(), true);
        label.setPadding(0, Ui.dp(a, 11), 0, 0);
        card.addView(label);
        card.setOnClickListener(v -> Ui.input(a, "Новый плейлист", "", "Название", "Создать", false, value -> {
            if (value.isEmpty()) return;
            a.bg(() -> App.api.createPlaylist(value, false), created -> {
                Ui.toast(a, "Плейлист создан");
                refresh();
            });
        }).show());
        return card;
    }

    private void renderChart(Models.Section chart) {
        chartBox.removeAllViews();
        if (chart == null || chart.tracks.isEmpty()) return;
        chartBox.addView(Widgets.sectionHeader(a,
                chart.subtitle.isEmpty() ? "Чарт" : "Чарт Яндекс Музыки", "Слушать",
                v -> a.playList(chart.tracks, 0)));
        int limit = Math.min(8, chart.tracks.size());
        for (int i = 0; i < limit; i++) {
            chartBox.addView(Widgets.trackRow(a, chart.tracks, i, null));
        }
        if (chart.tracks.size() > limit) {
            TextView more = Ui.text(a, "Показать весь чарт (" + chart.tracks.size() + ")",
                    12, Theme.accent(), true);
            more.setPadding(0, Ui.dp(a, 8), 0, 0);
            more.setOnClickListener(v -> a.playList(chart.tracks, 0));
            chartBox.addView(more);
        }
    }

    private void renderRecent(List<Models.Track> recent) {
        recentBox.removeAllViews();
        if (recent.isEmpty()) return;
        recentBox.addView(Widgets.sectionHeader(a, "Недавно слушали", null, null));
        int limit = Math.min(5, recent.size());
        for (int i = 0; i < limit; i++) {
            recentBox.addView(Widgets.trackRow(a, recent, i, null));
        }
    }
}
