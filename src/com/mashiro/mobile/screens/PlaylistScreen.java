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

import java.util.List;

/** A playlist page; works for both the user's own and foreign (public) playlists. */
public class PlaylistScreen extends Screen {
    private final long kind;
    private final Models.Playlist initial;
    private LinearLayout content;
    private Models.Playlist playlist;
    private int generation = 0;

    public PlaylistScreen(MainActivity a, long kind) {
        super(a);
        this.kind = kind;
        this.initial = null;
    }

    public PlaylistScreen(MainActivity a, Models.Playlist playlist) {
        super(a);
        this.initial = playlist;
        this.kind = playlist == null ? 0 : playlist.kind;
    }

    @Override
    protected View build() {
        Widgets.Page page = Widgets.page(a);
        ScrollView scroll = page.scroll;
        page.body.addView(Widgets.header(a, "Плейлист", "", true, R.drawable.ic_share,
                v -> share()));
        content = Ui.column(a);
        page.body.addView(content);
        content.addView(Widgets.loading(a));
        return scroll;
    }

    @Override
    public void onShow() {
        if (playlist != null) return;
        load();
    }

    @Override
    public void refresh() {
        load();
    }

    private void load() {
        final int gen = ++generation;
        long owner = initial != null && initial.ownerUid > 0 ? initial.ownerUid : Store.uid();
        a.bg(() -> App.api.playlistOf(owner, kind), value -> {
            if (gen != generation) return;
            playlist = value;
            render();
        });
    }

    private boolean isOwn() {
        return playlist == null || playlist.ownerUid <= 0 || playlist.ownerUid == Store.uid();
    }

    private void share() {
        Models.Playlist p = playlist != null ? playlist : initial;
        if (p == null) return;
        long owner = p.ownerUid > 0 ? p.ownerUid : Store.uid();
        a.share(p.title + " — плейлист в Яндекс Музыке\nhttps://music.yandex.ru/users/"
                + owner + "/playlists/" + p.kind);
    }

    private void render() {
        content.removeAllViews();
        if (playlist == null) return;

        LinearLayout hero = Ui.column(a);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(0, Ui.dp(a, 4), 0, 0);
        int size = Math.min(Ui.screenWidth(a) - Ui.dp(a, 150), Ui.dp(a, 160));
        com.mashiro.mobile.ImageLoader.RoundedImageView cover =
                Widgets.cover(a, playlist.coverUrl, 156, 16);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(size, size);
        clp.gravity = Gravity.CENTER_HORIZONTAL;
        cover.setLayoutParams(clp);
        hero.addView(cover);

        TextView title = Ui.title(a, playlist.title, 23);
        title.setGravity(Gravity.CENTER);
        title.setMaxLines(3);
        title.setPadding(0, Ui.dp(a, 14), 0, 0);
        hero.addView(title);

        String ownerName = isOwn() ? Store.name() : "Плейлист сообщества";
        TextView sub = Ui.text(a, ownerName + " · " + playlist.trackCount + " "
                + Ui.plural(playlist.trackCount, "трек", "трека", "треков"), 11.5f,
                Theme.dim(), false);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, Ui.dp(a, 5), 0, 0);
        hero.addView(sub);

        if (!playlist.isPublic) {
            TextView pill = Ui.pill(a, "Приватный", Theme.faint());
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            plp.topMargin = Ui.dp(a, 6);
            hero.addView(pill, plp);
        }
        content.addView(hero);

        if (!playlist.description.isEmpty()) {
            TextView description = Ui.text(a, playlist.description, 12.5f, Theme.dim(), false);
            description.setLineSpacing(0, 1.25f);
            description.setPadding(0, Ui.dp(a, 12), 0, 0);
            content.addView(description);
        }

        LinearLayout actions = Ui.row(a);
        actions.setPadding(0, Ui.dp(a, 14), 0, Ui.dp(a, 4));
        TextView play = Ui.primaryButton(a, "Слушать", v -> {
            if (!playlist.tracks.isEmpty()) a.playList(playlist.tracks, 0);
        });
        play.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView shuffle = Ui.secondaryButton(a, "Перемешать", v -> {
            if (!playlist.tracks.isEmpty()) a.playShuffled(playlist.tracks);
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        slp.leftMargin = Ui.dp(a, 8);
        shuffle.setLayoutParams(slp);
        actions.addView(play);
        actions.addView(shuffle);
        actions.addView(Ui.iconButton(a, R.drawable.ic_more, 44, 20, Theme.text(),
                v -> a.openPlaylistMenu(playlist, this::refresh)));
        content.addView(actions);

        if (isOwn()) {
            TextView add = Ui.text(a, "+ Добавить треки из поиска", 12.5f, Theme.accent(), true);
            add.setPadding(0, Ui.dp(a, 8), 0, Ui.dp(a, 4));
            add.setOnClickListener(v -> Ui.toast(a,
                    "Откройте поиск и добавьте трек через меню «⋯»"));
            content.addView(add);
        }

        content.addView(Widgets.sectionHeader(a,
                playlist.tracks.size() + " " + Ui.plural(playlist.tracks.size(),
                        "трек", "трека", "треков"), null, null));

        if (playlist.tracks.isEmpty()) {
            content.addView(Widgets.empty(a, "Плейлист пуст",
                    isOwn() ? "Добавьте треки через меню трека" : "В этом плейлисте пока нет треков"));
            return;
        }

        List<Models.Track> tracks = playlist.tracks;
        Long ownerKind = isOwn() ? kind : null;
        for (int i = 0; i < tracks.size(); i++) {
            content.addView(Widgets.numberedRow(a, tracks, i, ownerKind));
        }

        TextView footer = Ui.text(a, "Неофициальный клиент · данные Яндекс Музыки", 10,
                Theme.faint(), false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, Ui.dp(a, 22), 0, 0);
        content.addView(footer);
    }
}
