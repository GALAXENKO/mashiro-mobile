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
import com.mashiro.mobile.Theme;
import com.mashiro.mobile.Ui;
import com.mashiro.mobile.Widgets;
import com.mashiro.mobile.api.Models;

import java.util.ArrayList;
import java.util.List;

public class ArtistScreen extends Screen {
    private final String id;
    private LinearLayout content;
    private Models.ArtistPage page;
    private List<Models.Track> allTracks = new ArrayList<>();
    private boolean allLoaded = false;
    private boolean loadingAll = false;
    private boolean expanded = false;
    private int generation = 0;

    public ArtistScreen(MainActivity a, String id) {
        super(a);
        this.id = id;
    }

    @Override
    protected View build() {
        Widgets.Page p = Widgets.page(a);
        ScrollView scroll = p.scroll;
        p.body.addView(Widgets.header(a, "Артист", "", true, R.drawable.ic_share,
                v -> share()));
        content = Ui.column(a);
        p.body.addView(content);
        content.addView(Widgets.loading(a));
        return scroll;
    }

    @Override
    public void onShow() {
        if (page != null) return;
        final int gen = ++generation;
        a.bg(() -> App.api.artist(id), value -> {
            if (gen != generation) return;
            page = value;
            render();
        });
    }

    private void share() {
        if (page == null) return;
        a.share(page.artist.name + " в Яндекс Музыке\nhttps://music.yandex.ru/artist/" + id);
    }

    private List<Models.Track> visibleTracks() {
        if (allLoaded) return allTracks;
        return page == null ? new ArrayList<>() : page.tracks;
    }

    private void render() {
        content.removeAllViews();
        if (page == null) return;
        Models.Artist artist = page.artist;

        LinearLayout hero = Ui.column(a);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(0, Ui.dp(a, 4), 0, Ui.dp(a, 4));
        LinearLayout avatarBox = Ui.row(a);
        avatarBox.setGravity(Gravity.CENTER);
        avatarBox.addView(Widgets.avatar(a, artist.coverUrl, 118));
        hero.addView(avatarBox);

        TextView name = Ui.title(a, artist.name, 26);
        name.setGravity(Gravity.CENTER);
        name.setPadding(0, Ui.dp(a, 16), 0, 0);
        hero.addView(name);

        String stats = "";
        if (page.listeners > 0) {
            stats = Ui.count(page.listeners) + " " + Ui.plural((int) Math.min(Integer.MAX_VALUE, page.listeners),
                    "слушатель", "слушателя", "слушателей");
        }
        if (page.tracksCount > 0) {
            String tracksText = page.tracksCount + " " + Ui.plural((int) Math.min(Integer.MAX_VALUE, page.tracksCount),
                    "трек", "трека", "треков");
            stats = stats.isEmpty() ? tracksText : stats + " · " + tracksText;
        }
        if (!stats.isEmpty()) {
            TextView sub = Ui.text(a, stats, 11.5f, Theme.dim(), false);
            sub.setGravity(Gravity.CENTER);
            sub.setPadding(0, Ui.dp(a, 6), 0, 0);
            hero.addView(sub);
        }
        content.addView(hero);

        if (!page.description.isEmpty()) {
            TextView description = Ui.text(a, page.description, 12.5f, Theme.dim(), false);
            description.setLineSpacing(0, 1.25f);
            description.setPadding(0, Ui.dp(a, 14), 0, 0);
            description.setMaxLines(expanded ? Integer.MAX_VALUE : 4);
            description.setEllipsize(expanded ? null : android.text.TextUtils.TruncateAt.END);
            description.setOnClickListener(v -> {
                expanded = !expanded;
                render();
            });
            content.addView(description);
        }

        LinearLayout actions = Ui.row(a);
        actions.setPadding(0, Ui.dp(a, 16), 0, Ui.dp(a, 4));
        TextView play = Ui.primaryButton(a, "Слушать", v -> {
            List<Models.Track> tracks = visibleTracks();
            if (!tracks.isEmpty()) a.playList(tracks, 0);
        });
        play.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView shuffle = Ui.secondaryButton(a, "Перемешать", v -> {
            List<Models.Track> tracks = visibleTracks();
            if (!tracks.isEmpty()) a.playShuffled(tracks);
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        slp.leftMargin = Ui.dp(a, 8);
        shuffle.setLayoutParams(slp);
        actions.addView(play);
        actions.addView(shuffle);
        content.addView(actions);

        List<Models.Track> tracks = visibleTracks();
        content.addView(Widgets.sectionHeader(a,
                allLoaded ? "Все треки" : "Популярные треки", null, null));
        if (tracks.isEmpty()) {
            content.addView(Ui.text(a, "Нет доступных треков", 12, Theme.faint(), false));
        } else {
            for (int i = 0; i < tracks.size(); i++) {
                content.addView(Widgets.numberedRow(a, tracks, i, null));
            }
        }

        if (!allLoaded) {
            TextView more = Ui.text(a, loadingAll ? "Загружаем…" : "Показать все треки",
                    13, Theme.accent(), true);
            more.setGravity(Gravity.CENTER);
            more.setPadding(0, Ui.dp(a, 14), 0, Ui.dp(a, 6));
            more.setOnClickListener(v -> loadAll());
            content.addView(more);
        }

        if (!page.albums.isEmpty()) {
            content.addView(Widgets.sectionHeader(a, "Альбомы", null, null));
            LinearLayout row = Widgets.horizontal(a);
            for (Models.Album album : page.albums) {
                View card = Widgets.albumCard(a, album, 132);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        Ui.dp(a, 132), ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.rightMargin = Ui.dp(a, 12);
                row.addView(card, lp);
            }
            content.addView(Widgets.horizontalScroll(a, row));
        }
    }

    private void loadAll() {
        if (loadingAll || allLoaded) return;
        loadingAll = true;
        render();
        final int gen = generation;
        a.bg(() -> App.api.artistTracks(id), list -> {
            if (gen != generation) return;
            loadingAll = false;
            allLoaded = true;
            allTracks = list;
            render();
        }, true);
    }
}
