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
import com.mashiro.mobile.Theme;
import com.mashiro.mobile.Ui;
import com.mashiro.mobile.Widgets;
import com.mashiro.mobile.api.Models;

import java.util.List;

public class AlbumScreen extends Screen {
    private final String id;
    private LinearLayout content;
    private Models.Album album;
    private int generation = 0;

    public AlbumScreen(MainActivity a, String id) {
        super(a);
        this.id = id;
    }

    @Override
    protected View build() {
        Widgets.Page page = Widgets.page(a);
        ScrollView scroll = page.scroll;
        page.body.addView(Widgets.header(a, "Альбом", "", true, R.drawable.ic_share,
                v -> share()));
        content = Ui.column(a);
        page.body.addView(content);
        content.addView(Widgets.loading(a));
        return scroll;
    }

    @Override
    public void onShow() {
        if (album != null) return;
        final int gen = ++generation;
        a.bg(() -> App.api.album(id), value -> {
            if (gen != generation) return;
            album = value;
            render();
        });
    }

    private void share() {
        if (album == null) return;
        a.share(album.title + " — " + artists(album) + "\nhttps://music.yandex.ru/album/" + album.id);
    }

    private String artists(Models.Album album) {
        StringBuilder sb = new StringBuilder();
        for (Models.Artist artist : album.artists) {
            if (artist.name == null || artist.name.isEmpty()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(artist.name);
        }
        return sb.length() == 0 ? "Неизвестный исполнитель" : sb.toString();
    }

    private void render() {
        content.removeAllViews();
        if (album == null) return;

        LinearLayout hero = Ui.row(a);
        hero.setPadding(0, Ui.dp(a, 8), 0, Ui.dp(a, 4));
        int size = Math.min(Ui.screenWidth(a) - Ui.dp(a, 148), Ui.dp(a, 164));
        ImageLoader.RoundedImageView cover = Widgets.cover(a, album.coverUrl, 156, 20);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(size, size);
        clp.rightMargin = Ui.dp(a, 18);
        cover.setElevation(Ui.dp(a, 10));
        hero.addView(cover, clp);

        LinearLayout copy = Ui.column(a);
        copy.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        copy.addView(Ui.overline(a, "Альбом"));
        TextView title = Ui.title(a, album.title, 22);
        title.setMaxLines(3);
        title.setPadding(0, Ui.dp(a, 7), 0, Ui.dp(a, 7));
        copy.addView(title);

        LinearLayout artistRow = Ui.row(a);
        artistRow.setPadding(0, Ui.dp(a, 2), 0, 0);
        for (int i = 0; i < album.artists.size(); i++) {
            Models.Artist artist = album.artists.get(i);
            if (artist.name.isEmpty()) continue;
            TextView name = Ui.text(a, artist.name, 12.5f, Theme.dim(), true);
            name.setOnClickListener(v -> a.openArtist(artist.id));
            artistRow.addView(name);
            if (i < album.artists.size() - 1) {
                TextView comma = Ui.text(a, ", ", 12.5f, Theme.dim(), false);
                artistRow.addView(comma);
            }
        }
        copy.addView(artistRow);

        String meta = album.year > 0 ? String.valueOf(album.year) : "";
        if (!album.genre.isEmpty()) meta = meta.isEmpty() ? album.genre : meta + " · " + album.genre;
        if (meta.isEmpty()) meta = "Альбом";
        TextView metaView = Ui.text(a, meta, 11, Theme.faint(), false);
        metaView.setLetterSpacing(0.04f);
        metaView.setPadding(0, Ui.dp(a, 7), 0, 0);
        copy.addView(metaView);
        hero.addView(copy);
        content.addView(hero);

        LinearLayout actions = Ui.row(a);
        actions.setPadding(0, Ui.dp(a, 16), 0, Ui.dp(a, 4));
        TextView play = Ui.primaryButton(a, "Слушать", v -> a.playList(album.tracks, 0));
        play.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView shuffle = Ui.secondaryButton(a, "Перемешать", v -> a.playShuffled(album.tracks));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        slp.leftMargin = Ui.dp(a, 9);
        shuffle.setLayoutParams(slp);
        actions.addView(play);
        actions.addView(shuffle);
        content.addView(actions);

        content.addView(Widgets.sectionHeader(a,
                album.tracks.size() + " " + Ui.plural(album.tracks.size(),
                        "трек", "трека", "треков"), null, null));

        List<Models.Track> tracks = album.tracks;
        for (int i = 0; i < tracks.size(); i++) {
            content.addView(Widgets.numberedRow(a, tracks, i, null));
        }

        if (!album.artists.isEmpty() && !album.artists.get(0).id.isEmpty()) {
            Models.Artist first = album.artists.get(0);
            content.addView(Widgets.sectionHeader(a, "Исполнитель", null, null));
            content.addView(Widgets.actionCard(a, R.drawable.ic_person, first.name,
                    "Все треки и альбомы", () -> a.openArtist(first.id)));
        }

        TextView footer = Ui.text(a, "Неофициальный клиент · данные Яндекс Музыки", 10,
                Theme.faint(), false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, Ui.dp(a, 22), 0, 0);
        content.addView(footer);
    }
}
