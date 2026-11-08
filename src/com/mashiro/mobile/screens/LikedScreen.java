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

import java.util.ArrayList;
import java.util.List;

/** Tracks the user liked, in the order the API returns them. */
public class LikedScreen extends Screen {
    private LinearLayout body;
    private LinearLayout content;
    private final List<Models.Track> tracks = new ArrayList<>();
    private boolean loaded = false;
    private int generation = 0;

    public LikedScreen(MainActivity a) {
        super(a);
    }

    @Override
    protected View build() {
        Widgets.Page page = Widgets.page(a);
        body = page.body;
        ScrollView scroll = page.scroll;

        body.addView(Widgets.header(a, "Мне нравится", "любимые треки", true, 0, null));
        content = Ui.column(a);
        body.addView(content);
        content.addView(Widgets.loading(a));
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
        a.bg(() -> App.api.likedTracks(), list -> {
            if (gen != generation) return;
            tracks.clear();
            tracks.addAll(list);
            render();
        }, true);
    }

    private void render() {
        content.removeAllViews();
        if (tracks.isEmpty()) {
            content.addView(Widgets.empty(a, "Пока пусто",
                    "Ставьте лайки трекам — они появятся здесь"));
            return;
        }

        LinearLayout hero = Ui.row(a);
        android.graphics.drawable.GradientDrawable heroBg =
                Ui.gradient(a, Theme.accentSoft(26), Theme.surface(), 20);
        heroBg.setStroke(Math.max(1, Ui.dp(a, 1)), Theme.hairline());
        hero.setBackground(heroBg);
        hero.setPadding(Ui.dp(a, 18), Ui.dp(a, 18), Ui.dp(a, 18), Ui.dp(a, 18));
        hero.addView(Widgets.circleIcon(a, R.drawable.ic_heart_fill, 58,
                Theme.accentSoft(26), Theme.accent()));
        LinearLayout copy = Ui.column(a);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = Ui.dp(a, 16);
        copy.setLayoutParams(clp);
        TextView eyebrow = Ui.overline(a, "Коллекция");
        copy.addView(eyebrow);
        TextView title = Ui.title(a, "Мне нравится", 21);
        title.setPadding(0, Ui.dp(a, 5), 0, 0);
        copy.addView(title);
        TextView count = Ui.text(a, tracks.size() + " " + Ui.plural(tracks.size(),
                "трек", "трека", "треков"), 11.5f, Theme.dim(), false);
        count.setPadding(0, Ui.dp(a, 4), 0, 0);
        copy.addView(count);
        hero.addView(copy);
        content.addView(hero);

        LinearLayout actions = Ui.row(a);
        actions.setPadding(0, Ui.dp(a, 14), 0, Ui.dp(a, 6));
        TextView play = Ui.primaryButton(a, "Слушать", v -> a.playList(tracks, 0));
        play.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView shuffle = Ui.secondaryButton(a, "Перемешать", v -> a.playShuffled(tracks));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        slp.leftMargin = Ui.dp(a, 9);
        shuffle.setLayoutParams(slp);
        actions.addView(play);
        actions.addView(shuffle);
        content.addView(actions);

        for (int i = 0; i < tracks.size(); i++) {
            content.addView(Widgets.trackRow(a, tracks, i, null));
        }

        TextView hint = Ui.overline(a, "Синхронизировано с Яндекс Музыкой");
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, Ui.dp(a, 26), 0, 0);
        content.addView(hint);
    }
}
