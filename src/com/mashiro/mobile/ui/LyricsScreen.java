package com.mashiro.mobile.ui;

import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mashiro.mobile.App;
import com.mashiro.mobile.MainActivity;
import com.mashiro.mobile.Theme;
import com.mashiro.mobile.Ui;
import com.mashiro.mobile.api.Models;
import com.mashiro.mobile.audio.Playback;

import java.util.ArrayList;
import java.util.List;

/** Bottom sheet with the track's lyrics; synced lines follow playback. */
public final class LyricsScreen {
    private final MainActivity a;
    private final Models.Track track;
    private final LinearLayout list;
    private final List<Models.LyricLine> lines = new ArrayList<>();
    private final List<TextView> lineViews = new ArrayList<>();
    private Dialog dialog;
    private Playback.Listener listener;
    private boolean synced = false;
    private ScrollView scroll;

    public LyricsScreen(MainActivity a, Models.Track track) {
        this.a = a;
        this.track = track;
        this.list = Ui.column(a);
    }

    public void show() {
        LinearLayout content = Ui.column(a);
        content.setPadding(Ui.dp(a, 18), 0, Ui.dp(a, 12), Ui.dp(a, 10));

        LinearLayout head = Ui.row(a);
        head.setPadding(0, 0, 0, Ui.dp(a, 6));
        LinearLayout copy = Ui.column(a);
        copy.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        copy.addView(Ui.title(a, "Текст песни", 19));
        TextView sub = Ui.text(a, track.title + " — " + track.artistLine(), 11.5f,
                Theme.dim(), false);
        sub.setSingleLine(true);
        sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        sub.setPadding(0, Ui.dp(a, 2), 0, 0);
        copy.addView(sub);
        head.addView(copy);
        content.addView(head);

        LinearLayout loading = Ui.column(a);
        loading.setGravity(android.view.Gravity.CENTER);
        loading.setPadding(0, Ui.dp(a, 80), 0, Ui.dp(a, 80));
        loading.addView(Ui.progress(a, true));
        TextView loadingText = Ui.text(a, "Загружаем текст…", 13, Theme.faint(), false);
        loadingText.setPadding(0, Ui.dp(a, 12), 0, 0);
        loading.addView(loadingText);
        list.addView(loading);

        ScrollView scrollView = new ScrollView(a);
        scrollView.setClipToPadding(false);
        scrollView.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scroll = scrollView;
        int height = (int) (Ui.screenWidth(a) * 0.8f);
        content.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height));

        dialog = Ui.sheet(a, content);
        dialog.setOnDismissListener(d -> {
            if (listener != null) Playback.get().removeListener(listener);
            dialog = null;
        });
        dialog.show();

        a.bg(() -> App.api.lyrics(track), lyrics -> {
            if (dialog == null || !dialog.isShowing()) return;
            render(lyrics);
        });
    }

    private void render(Models.Lyrics lyrics) {
        if (lyrics == null || lyrics.lines.isEmpty()) {
            empty("Текста нет",
                    "Для этого трека не удалось найти текст песни");
            return;
        }
        lines.clear();
        lines.addAll(lyrics.lines);
        synced = lyrics.synced;
        lineViews.clear();
        list.removeAllViews();
        for (Models.LyricLine line : lines) {
            TextView t = Ui.text(a, line.text == null || line.text.isEmpty()
                    ? "\u00A0" : line.text, 15.5f, Theme.dim(), false);
            t.setLineSpacing(0, 1.35f);
            t.setPadding(0, Ui.dp(a, 5), 0, Ui.dp(a, 5));
            t.setVisibility(View.VISIBLE);
            list.addView(t, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            lineViews.add(t);
        }
        if (synced) {
            listener = new Playback.Listener() {
                @Override public void onPlaybackState() { highlight(); }
                @Override public void onTrackChanged() {}
                @Override public void onProgress() { highlight(); }
            };
            Playback.get().addListener(listener);
            highlight();
        }
    }

    private void highlight() {
        if (!synced || lineViews.isEmpty()) return;
        Playback p = Playback.get();
        Models.Track current = p.current();
        if (current == null || !track.id.equals(current.id)) return;
        int active = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).timeMs <= p.position) active = i;
        }
        for (int i = 0; i < lineViews.size(); i++) {
            TextView t = lineViews.get(i);
            t.setTextColor(i == active ? Theme.accent() : Theme.dim());
            t.setTypeface(t.getTypeface(), i == active
                    ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
        final View view = lineViews.get(active);
        view.post(() -> {
            if (scroll != null) {
                scroll.smoothScrollTo(0, Math.max(0,
                        view.getTop() - Ui.dp(a, 140)));
            }
        });
    }

    private void empty(String title, String message) {
        list.removeAllViews();
        lineViews.clear();
        lines.clear();
        synced = false;
        LinearLayout box = Ui.column(a);
        box.setGravity(android.view.Gravity.CENTER);
        box.setPadding(0, Ui.dp(a, 90), 0, Ui.dp(a, 90));
        box.addView(Ui.title(a, title, 18));
        TextView sub = Ui.text(a, message, 12.5f, Theme.dim(), false);
        sub.setGravity(android.view.Gravity.CENTER);
        sub.setPadding(Ui.dp(a, 12), Ui.dp(a, 10), Ui.dp(a, 12), 0);
        box.addView(sub);
        list.addView(box);
    }
}