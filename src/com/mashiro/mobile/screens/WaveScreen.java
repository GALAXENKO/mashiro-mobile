package com.mashiro.mobile.screens;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.FrameLayout;
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
import java.util.List;

public class WaveScreen extends Screen {
    private LinearLayout body;
    private LinearLayout currentBox;
    private LinearLayout stationsBox;
    private LinearLayout upcomingBox;
    private TextView status;
    private boolean busy = false;
    private String station = "user:onyourwave";
    private boolean visible = false;

    private final Playback.Listener listener = new Playback.Listener() {
        @Override public void onPlaybackState() { if (visible) updateCurrent(); }
        @Override public void onTrackChanged() { if (visible) { updateCurrent(); updateUpcoming(); } }
        @Override public void onProgress() {}
        @Override public void onPlaybackError(String message) { if (visible) updateCurrent(); }
    };

    public WaveScreen(MainActivity a) {
        super(a);
    }

    @Override
    protected View build() {
        Widgets.Page page = Widgets.page(a);
        body = page.body;
        ScrollView scroll = page.scroll;
        body.addView(Widgets.header(a, "Моя волна", "персональная радиостанция", false,
                R.drawable.ic_settings, v -> a.openSettings()));

        currentBox = Ui.column(a);
        body.addView(currentBox);

        stationsBox = Ui.column(a);
        body.addView(stationsBox);
        stationsBox.addView(Widgets.sectionHeader(a, "Станции", null, null));
        LinearLayout stations = Widgets.horizontal(a);
        stations.addView(stationChip("Моя волна", "user:onyourwave", true));
        stations.addView(stationChip("Для вас", "personal:collection", false));
        stations.addView(stationChip("Настроение", "mood:all", false));
        stations.addView(stationChip("Жанры", "genre:all", false));
        stations.addView(stationChip("Активность", "activity:all", false));
        stationsBox.addView(Widgets.horizontalScroll(a, stations));

        status = Ui.text(a, "Волна подстраивается под ваши лайки и дизлайки", 11.5f,
                Theme.faint(), false);
        status.setPadding(0, Ui.dp(a, 14), 0, 0);
        body.addView(status);

        upcomingBox = Ui.column(a);
        body.addView(upcomingBox);

        updateCurrent();
        updateUpcoming();
        return scroll;
    }

    private View stationChip(String title, String id, boolean active) {
        TextView chip = Ui.chip(a, title, active, v -> startStation(id));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = Ui.dp(a, 8);
        chip.setLayoutParams(lp);
        return chip;
    }

    @Override
    public void onShow() {
        visible = true;
        Playback.get().addListener(listener);
        updateCurrent();
        updateUpcoming();
    }

    @Override
    public void onHide() {
        visible = false;
        Playback.get().removeListener(listener);
    }

    @Override
    public void refresh() {
        updateCurrent();
        updateUpcoming();
    }

    private void startStation(String id) {
        if (busy) return;
        busy = true;
        station = id;
        status.setText("Настраиваем волну…");
        a.bg(() -> App.api.wave(id, "", null), wave -> {
            busy = false;
            if (wave.tracks.isEmpty()) {
                status.setText("Волна пока не отвечает. Попробуйте ещё раз.");
                return;
            }
            Playback.get().startQueue(new ArrayList<>(wave.tracks), 0, true, id,
                    wave.sessionId, wave.batchId);
            a.ensureMini();
            status.setText("Волна подстраивается под ваши лайки и дизлайки");
            updateCurrent();
            updateUpcoming();
        });
    }

    private void updateCurrent() {
        if (currentBox == null) return;
        currentBox.removeAllViews();
        Playback p = Playback.get();
        Models.Track track = p.current();
        if (!p.wave || track == null) {
            currentBox.addView(hero("Включить волну", v -> startStation("user:onyourwave")));
            if (track != null && !p.wave) {
                currentBox.addView(Widgets.sectionHeader(a, "Сейчас играет из очереди", null, null));
                LinearLayout row = Ui.row(a);
                row.setPadding(0, 0, 0, Ui.dp(a, 6));
                row.addView(Widgets.cover(a, track.coverUrl, 52, 8));
                LinearLayout copy = Ui.column(a);
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                clp.leftMargin = Ui.dp(a, 12);
                copy.setLayoutParams(clp);
                copy.addView(Ui.text(a, track.title, 14, Theme.text(), true));
                copy.addView(Ui.text(a, track.artistLine(), 11.5f, Theme.dim(), false));
                row.addView(copy);
                row.addView(Ui.iconButton(a, R.drawable.ic_play, 40, 22, Theme.accent(),
                        v -> a.openPlayer()));
                currentBox.addView(row);
            }
            return;
        }

        LinearLayout card = Ui.column(a);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Theme.accentSoft(24), Theme.surface()});
        bg.setCornerRadius(Ui.dp(a, 22));
        card.setBackground(bg);
        card.setPadding(Ui.dp(a, 18), Ui.dp(a, 18), Ui.dp(a, 18), Ui.dp(a, 16));

        TextView eyebrow = Ui.text(a, "СЕЙЧАС В ВОЛНЕ", 9.5f, Theme.dim(), true);
        eyebrow.setLetterSpacing(0.14f);
        eyebrow.setGravity(Gravity.CENTER);
        card.addView(eyebrow);

        ImageLoader.RoundedImageView art = Widgets.cover(a, track.coverLarge.isEmpty()
                ? track.coverUrl : track.coverLarge, 210, 16);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(Ui.dp(a, 210), Ui.dp(a, 210));
        alp.gravity = Gravity.CENTER_HORIZONTAL;
        alp.topMargin = Ui.dp(a, 12);
        art.setLayoutParams(alp);
        art.setOnClickListener(v -> a.openPlayer());
        card.addView(art);

        TextView title = Ui.text(a, track.title, 20, Theme.text(), true);
        title.setGravity(Gravity.CENTER);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setPadding(0, Ui.dp(a, 14), 0, 0);
        card.addView(title);
        TextView artist = Ui.text(a, track.artistLine(), 12.5f, Theme.dim(), false);
        artist.setGravity(Gravity.CENTER);
        artist.setPadding(0, Ui.dp(a, 3), 0, 0);
        card.addView(artist);

        LinearLayout controls = Ui.row(a);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(0, Ui.dp(a, 14), 0, 0);
        boolean liked = Store.isLiked(track.id);
        View like = waveButton(R.drawable.ic_heart_fill, "Нравится",
                liked ? Theme.accent() : Theme.dim(), v -> likeCurrent(true));
        View dislike = waveButton(R.drawable.ic_dislike, "Не очень",
                Store.isDisliked(track.id) ? Theme.danger() : Theme.dim(), v -> likeCurrent(false));
        FrameLayoutShim shim = null;
        View playBox = playButton(p.playing);
        View next = waveButton(R.drawable.ic_next, "Далее", Theme.text(), v -> Playback.get().next(true));
        controls.addView(like, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        controls.addView(dislike, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        controls.addView(playBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        controls.addView(next, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(controls);
        currentBox.addView(card);

        TextView hint = Ui.text(a, "Оценки уходят в Яндекс и меняют поток", 10.5f, Theme.faint(), false);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, Ui.dp(a, 10), 0, 0);
        currentBox.addView(hint);
    }

    private static class FrameLayoutShim {}

    private View waveButton(int icon, String label, int tint, View.OnClickListener click) {
        LinearLayout box = Ui.column(a);
        box.setGravity(Gravity.CENTER);
        box.setOnClickListener(click);
        ImageView iv = Ui.icon(a, icon, 24, tint);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(Ui.dp(a, 24), Ui.dp(a, 24));
        ilp.gravity = Gravity.CENTER_HORIZONTAL;
        box.addView(iv, ilp);
        TextView t = Ui.text(a, label, 9.5f, Theme.faint(), false);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, Ui.dp(a, 5), 0, 0);
        box.addView(t);
        return box;
    }

    private View playButton(boolean playing) {
        LinearLayout box = Ui.column(a);
        box.setGravity(Gravity.CENTER);
        FrameLayout circle = new FrameLayout(a);
        circle.setBackground(Ui.bg(a, Theme.accent(), 26));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(Ui.dp(a, 52), Ui.dp(a, 52));
        clp.gravity = Gravity.CENTER_HORIZONTAL;
        circle.setLayoutParams(clp);
        ImageView icon = Ui.icon(a, playing ? R.drawable.ic_pause : R.drawable.ic_play,
                26, Theme.accentText());
        FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(Ui.dp(a, 26), Ui.dp(a, 26));
        ilp.gravity = Gravity.CENTER;
        circle.addView(icon, ilp);
        circle.setOnClickListener(v -> {
            Playback.get().toggle();
            updateCurrent();
        });
        box.addView(circle);
        TextView t = Ui.text(a, playing ? "Пауза" : "Слушать", 9.5f, Theme.faint(), false);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, Ui.dp(a, 5), 0, 0);
        box.addView(t);
        return box;
    }

    private void likeCurrent(boolean like) {
        Models.Track track = Playback.get().current();
        if (track == null) return;
        boolean remove = like ? Store.isLiked(track.id) : Store.isDisliked(track.id);
        a.bg(() -> {
            App.api.setLike(track.id, !like, remove);
            return true;
        }, ok -> {
            if (like) {
                Store.setLiked(track.id, !remove);
                Store.setDisliked(track.id, false);
            } else {
                Store.setDisliked(track.id, !remove);
                Store.setLiked(track.id, false);
            }
            a.bg(() -> {
                App.api.waveFeedback(Playback.get().station,
                        like ? (remove ? "dislike" : "like") : "dislike",
                        track.id, Playback.get().waveBatch, 0);
                return true;
            }, x -> {}, true);
            updateCurrent();
            a.updateRows();
            if (!like && !remove) Playback.get().next(true);
        });
    }

    private View hero(String buttonText, View.OnClickListener click) {
        LinearLayout card = Ui.column(a);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Theme.surface2(), Theme.accentSoft(26)});
        bg.setCornerRadius(Ui.dp(a, 20));
        card.setBackground(bg);
        card.setPadding(Ui.dp(a, 18), Ui.dp(a, 20), Ui.dp(a, 18), Ui.dp(a, 18));
        LinearLayout mark = Ui.row(a);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(Ui.bg(a, Theme.accentSoft(22), 34));
        mark.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(a, 68), Ui.dp(a, 68)));
        mark.addView(Ui.icon(a, R.drawable.ic_wave, 30, Theme.accent()));
        card.addView(mark);
        TextView title = Ui.title(a, "Моя волна", 23);
        title.setPadding(0, Ui.dp(a, 14), 0, 0);
        card.addView(title);
        card.addView(Ui.text(a, "Бесконечная станция, которая учится на ваших оценках.",
                12, Theme.dim(), false));
        TextView button = Ui.primaryButton(a, buttonText, click);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = Ui.dp(a, 16);
        button.setLayoutParams(blp);
        card.addView(button);
        return card;
    }

    private void updateUpcoming() {
        if (upcomingBox == null) return;
        upcomingBox.removeAllViews();
        Playback p = Playback.get();
        if (!p.wave || p.queue.isEmpty()) return;
        List<Models.Track> upcoming = new ArrayList<>();
        int start = Math.max(0, p.index + 1);
        for (int i = start; i < p.queue.size() && upcoming.size() < 12; i++) {
            upcoming.add(p.queue.get(i));
        }
        if (upcoming.isEmpty()) return;
        upcomingBox.addView(Widgets.sectionHeader(a, "Дальше в волне", null, null));
        for (int i = 0; i < upcoming.size(); i++) {
            Models.Track t = upcoming.get(i);
            final int target = start + i;
            LinearLayout row = Ui.row(a);
            row.setPadding(Ui.dp(a, 4), Ui.dp(a, 6), Ui.dp(a, 2), Ui.dp(a, 6));
            row.setBackground(Ui.bg(a, android.graphics.Color.TRANSPARENT, 10));
            row.addView(Widgets.cover(a, t.coverUrl, 42, 7));
            LinearLayout copy = Ui.column(a);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            clp.leftMargin = Ui.dp(a, 10);
            copy.setLayoutParams(clp);
            TextView title = Ui.text(a, t.title, 13.5f, Theme.text(), true);
            title.setSingleLine(true);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            copy.addView(title);
            copy.addView(Ui.text(a, t.artistLine(), 11, Theme.dim(), false));
            row.addView(copy);
            row.addView(Ui.iconButton(a, R.drawable.ic_more, 36, 18, Theme.faint(),
                    v -> a.openTrackMenu(t, null, null)));
            row.setOnClickListener(v -> Playback.get().playAt(target));
            a.registerRow(t.id, row, title);
            upcomingBox.addView(row);
        }
    }
}
