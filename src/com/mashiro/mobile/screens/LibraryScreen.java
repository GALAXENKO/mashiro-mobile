package com.mashiro.mobile.screens;

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
import com.mashiro.mobile.api.Models;

import java.util.ArrayList;
import java.util.List;

/** The user's library: overview, own playlists and downloaded tracks. */
public class LibraryScreen extends Screen {
    private LinearLayout body;
    private LinearLayout content;
    private LinearLayout tabs;
    private int tab;
    private int generation = 0;
    private List<Models.Playlist> playlists = new ArrayList<>();
    private boolean loaded = false;

    public LibraryScreen(MainActivity a) {
        this(a, 0);
    }

    public LibraryScreen(MainActivity a, int tab) {
        super(a);
        this.tab = tab < 0 || tab > 2 ? 0 : tab;
    }

    @Override
    protected View build() {
        Widgets.Page page = Widgets.page(a);
        body = page.body;
        ScrollView scroll = page.scroll;

        body.addView(Widgets.header(a, "Медиатека", Store.name(), false,
                R.drawable.ic_settings, v -> a.openSettings()));

        tabs = Ui.row(a);
        tabs.setPadding(0, Ui.dp(a, 4), 0, Ui.dp(a, 6));
        body.addView(tabs);

        content = Ui.column(a);
        body.addView(content);

        renderTabs();
        render();
        return scroll;
    }

    @Override
    public void onShow() {
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
        a.bg(() -> App.api.playlists(), list -> {
            if (gen != generation) return;
            playlists = list;
            render();
        }, true);
    }

    private void setTab(int value) {
        if (tab == value) return;
        tab = value;
        renderTabs();
        render();
    }

    private void renderTabs() {
        tabs.removeAllViews();
        String[] labels = {"Обзор", "Плейлисты", "Скачано"};
        for (int i = 0; i < labels.length; i++) {
            TextView chip = Ui.chip(a, labels[i], tab == i, v -> setTab(indexOf(v)));
            chip.setTag(i);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = Ui.dp(a, 8);
            chip.setLayoutParams(lp);
            tabs.addView(chip);
        }
    }

    private int indexOf(View view) {
        Object tag = view.getTag();
        return tag instanceof Integer ? (Integer) tag : 0;
    }

    private void render() {
        if (content == null) return;
        content.removeAllViews();
        if (tab == 0) renderOverview();
        else if (tab == 1) renderPlaylists();
        else renderDownloads();
    }

    // --------------------------------------------------------------- overview

    private void renderOverview() {
        LinearLayout profile = Ui.row(a);
        profile.setBackground(Ui.strokeBg(a, Theme.surface(), 18, Theme.border(), 1));
        profile.setPadding(Ui.dp(a, 16), Ui.dp(a, 16), Ui.dp(a, 16), Ui.dp(a, 16));
        profile.addView(Widgets.avatar(a, Store.avatar(), 58));
        LinearLayout copy = Ui.column(a);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = Ui.dp(a, 14);
        copy.setLayoutParams(clp);
        TextView name = Ui.title(a, Store.name(), 20);
        name.setSingleLine(true);
        copy.addView(name);
        TextView sub = Ui.text(a, Store.liked().size() + " "
                + Ui.plural(Store.liked().size(), "любимый трек", "любимых трека", "любимых треков")
                + " · " + playlists.size() + " " + Ui.plural(playlists.size(), "плейлист", "плейлиста", "плейлистов"),
                11.5f, Theme.dim(), false);
        sub.setPadding(0, Ui.dp(a, 3), 0, 0);
        copy.addView(sub);
        if (Store.hasPlus()) {
            TextView plus = Ui.pill(a, "Плюс", Theme.accent());
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            plp.topMargin = Ui.dp(a, 6);
            copy.addView(plus, plp);
        }
        profile.addView(copy);
        profile.setOnClickListener(v -> a.openSettings());
        content.addView(profile);

        LinearLayout quick = Ui.row(a);
        quick.setPadding(0, Ui.dp(a, 14), 0, 0);
        quick.addView(quickCard(R.drawable.ic_heart, "Мне нравится", Store.liked().size(),
                () -> a.openLiked()), weight(0));
        quick.addView(quickCard(R.drawable.ic_download, "Скачано", Store.downloadedIds().size(),
                () -> setTab(2)), weight(8));
        quick.addView(quickCard(R.drawable.ic_note, "Статистика", Store.totalPlays(),
                () -> a.openStats()), weight(8));
        content.addView(quick);

        List<Models.Track> recent = Store.recent();
        if (!recent.isEmpty()) {
            content.addView(Widgets.sectionHeader(a, "Недавно слушали", "История", v -> {
                if (!recent.isEmpty()) a.playList(recent, 0);
            }));
            int limit = Math.min(5, recent.size());
            for (int i = 0; i < limit; i++) {
                content.addView(Widgets.trackRow(a, recent, i, null));
            }
        }

        content.addView(Widgets.sectionHeader(a, "Мои плейлисты", "Все", v -> setTab(1)));
        if (playlists.isEmpty()) {
            TextView hint = Ui.text(a, "Плейлистов пока нет", 11.5f, Theme.faint(), false);
            content.addView(hint);
        } else {
            LinearLayout row = Widgets.horizontal(a);
            int count = 0;
            for (Models.Playlist p : playlists) {
                View card = Widgets.playlistCard(a, p, 132);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        Ui.dp(a, 132), ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.rightMargin = Ui.dp(a, 10);
                row.addView(card, lp);
                if (++count >= 8) break;
            }
            content.addView(Widgets.horizontalScroll(a, row));
        }

        content.addView(Widgets.sectionHeader(a, "Ещё", null, null));
        content.addView(Widgets.actionCard(a, R.drawable.ic_search, "Поиск в медиатеке",
                "Локальный поиск по скачанному и плейлистам", () -> a.open(new LibrarySearchScreen(a))));
        content.addView(Widgets.actionCard(a, R.drawable.ic_wave, "Моя волна",
                "Персональная станция", () -> a.setTab(2)));
        content.addView(Widgets.actionCard(a, R.drawable.ic_settings, "Настройки",
                "Тема, качество звука, аккаунт", () -> a.openSettings()));
    }

    private LinearLayout.LayoutParams weight(int leftMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = Ui.dp(a, leftMargin);
        return lp;
    }

    private View quickCard(int icon, String title, int value, Runnable click) {
        LinearLayout box = Ui.column(a);
        box.setBackground(Ui.strokeBg(a, Theme.surface(), 15, Theme.border(), 1));
        box.setPadding(Ui.dp(a, 12), Ui.dp(a, 12), Ui.dp(a, 10), Ui.dp(a, 12));
        box.addView(Ui.icon(a, icon, 20, Theme.text()));
        TextView number = Ui.text(a, String.valueOf(value), 18, Theme.text(), true);
        number.setPadding(0, Ui.dp(a, 10), 0, 0);
        box.addView(number);
        TextView label = Ui.text(a, title, 10.5f, Theme.dim(), true);
        label.setSingleLine(true);
        box.addView(label);
        box.setOnClickListener(v -> click.run());
        Ui.ripple(box, 0x1AFFFFFF);
        return box;
    }

    // -------------------------------------------------------------- playlists

    private void renderPlaylists() {
        LinearLayout actions = Ui.row(a);
        TextView create = Ui.secondaryButton(a, "Новый плейлист", v -> createPlaylist());
        actions.addView(create);
        TextView search = Ui.secondaryButton(a, "Поиск", v -> a.open(new LibrarySearchScreen(a)));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.leftMargin = Ui.dp(a, 8);
        actions.addView(search, slp);
        content.addView(actions);

        if (playlists.isEmpty()) {
            content.addView(Widgets.empty(a, "Плейлистов пока нет",
                    "Создайте первый плейлист и добавляйте в него треки"));
            return;
        }

        int gap = Ui.dp(a, 12);
        int width = (Ui.screenWidth(a) - Ui.dp(a, 36) - gap) / 2;
        LinearLayout row = null;
        for (int i = 0; i < playlists.size(); i++) {
            if (i % 2 == 0) {
                row = Ui.row(a);
                row.setPadding(0, Ui.dp(a, 10), 0, 0);
                content.addView(row);
            }
            Models.Playlist p = playlists.get(i);
            LinearLayout cell = Ui.column(a);
            cell.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            cell.addView(Widgets.playlistGridCard(a, p, width));
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            clp.rightMargin = i % 2 == 0 ? gap : 0;
            clp.leftMargin = i % 2 == 0 ? 0 : gap;
            row.addView(cell, clp);
            if (i == playlists.size() - 1 && i % 2 == 0) {
                View filler = new View(a);
                row.addView(filler, new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            }
        }
    }

    private void createPlaylist() {
        Ui.input(a, "Новый плейлист", "", "Название", "Создать", false, value -> {
            if (value.isEmpty()) return;
            a.bg(() -> App.api.createPlaylist(value, false), created -> {
                Ui.toast(a, "Плейлист создан");
                refresh();
            });
        }).show();
    }

    // -------------------------------------------------------------- downloads

    private void renderDownloads() {
        List<Models.Track> tracks = Store.downloadedTracks();
        if (tracks.isEmpty()) {
            content.addView(Widgets.empty(a, "Нет скачанных треков",
                    "Скачайте трек из меню или из плеера — он будет доступен без сети"));
            return;
        }

        long bytes = 0;
        for (Models.Track t : tracks) {
            String path = Store.downloadPath(t.id);
            if (!path.isEmpty()) bytes += new java.io.File(path).length();
        }
        LinearLayout summary = Ui.row(a);
        summary.setBackground(Ui.strokeBg(a, Theme.surface(), 16, Theme.border(), 1));
        summary.setPadding(Ui.dp(a, 15), Ui.dp(a, 14), Ui.dp(a, 15), Ui.dp(a, 14));
        summary.addView(Widgets.circleIcon(a, R.drawable.ic_download, 40,
                Theme.accentSoft(20), Theme.accent()));
        LinearLayout copy = Ui.column(a);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = Ui.dp(a, 12);
        copy.setLayoutParams(clp);
        copy.addView(Ui.text(a, tracks.size() + " " + Ui.plural(tracks.size(),
                "трек", "трека", "треков"), 14, Theme.text(), true));
        copy.addView(Ui.text(a, size(bytes) + " · доступно без сети", 11.5f, Theme.dim(), false));
        summary.addView(copy);
        TextView play = Ui.primaryButton(a, "Слушать", v -> a.playList(tracks, 0));
        summary.addView(play);
        content.addView(summary);

        content.addView(Widgets.sectionHeader(a, "Файлы", null, null));
        for (int i = 0; i < tracks.size(); i++) {
            Models.Track track = tracks.get(i);
            LinearLayout row = Ui.row(a);
            row.setPadding(Ui.dp(a, 4), Ui.dp(a, 6), Ui.dp(a, 4), Ui.dp(a, 6));
            row.setBackground(Ui.bg(a, android.graphics.Color.TRANSPARENT, 10));
            row.addView(Widgets.cover(a, track.coverUrl, 44, 7));
            LinearLayout copyRow = Ui.column(a);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            rlp.leftMargin = Ui.dp(a, 11);
            copyRow.setLayoutParams(rlp);
            TextView title = Ui.text(a, track.title, 14, Theme.text(), true);
            title.setSingleLine(true);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            copyRow.addView(title);
            copyRow.addView(Ui.text(a, track.artistLine(), 11.5f, Theme.dim(), false));
            row.addView(copyRow);
            row.addView(Ui.iconButton(a, R.drawable.ic_trash, 36, 18, Theme.faint(),
                    v -> removeDownload(track)));
            final int index = i;
            row.setOnClickListener(v -> a.playList(tracks, index));
            a.registerRow(track.id, row, title);
            content.addView(row);
        }
    }

    private void removeDownload(Models.Track track) {
        Ui.confirm(a, "Удалить файл?", track.title + " — " + track.artistLine(),
                "Удалить", () -> {
                    Store.removeDownloadedTrack(track.id);
                    render();
                    Ui.toast(a, "Файл удалён");
                }).show();
    }

    private String size(long bytes) {
        if (bytes >= 1024L * 1024L * 1024L) {
            return String.format(java.util.Locale.ROOT, "%.1f ГБ", bytes / (1024.0 * 1024 * 1024));
        }
        if (bytes >= 1024L * 1024L) {
            return String.format(java.util.Locale.ROOT, "%.0f МБ", bytes / (1024.0 * 1024));
        }
        return Math.max(1, bytes / 1024) + " КБ";
    }
}
