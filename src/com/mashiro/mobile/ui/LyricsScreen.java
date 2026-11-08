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
import com.mashiro.mobile.Widgets;
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
    private TextView sourceView;

    public LyricsScreen(MainActivity a, Models.Track track) {
        this.a = a;
        this.track = track;
        this.list = Ui.column(a);
    }

    public void show() {
        LinearLayout content = Ui.column(a);
        content.setPadding(Ui.dp(a, 20), 0, Ui.dp(a, 16), Ui.dp(a, 12));

        LinearLayout head = Ui.row(a);
        head.setPadding(0, 0, 0, Ui.dp(a, 12));
        LinearLayout copy = Ui.column(a);
        copy.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView overline = Ui.overline(a, "Текст песни");
        overline.setTextColor(Theme.accent());
        copy.addView(overline);
        TextView heading = Ui.title(a, track.title, 20);
        heading.setSingleLine(true);
        heading.setEllipsize(android.text.TextUtils.TruncateAt.END);
        heading.setPadding(0, Ui.dp(a, 5), 0, 0);
        copy.addView(heading);
        TextView sub = Ui.text(a, track.artistLine(), 12, Theme.dim(), false);
        sub.setSingleLine(true);
        sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        sub.setPadding(0, Ui.dp(a, 3), 0, 0);
        copy.addView(sub);

        sourceView = Ui.text(a, "Ищем в открытых источниках…", 10, Theme.faint(), false);
        sourceView.setSingleLine(true);
        sourceView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        sourceView.setLetterSpacing(0.03f);
        sourceView.setPadding(0, Ui.dp(a, 8), 0, 0);
        copy.addView(sourceView);
        head.addView(copy);
        content.addView(head);

        content.addView(Widgets.rowDivider(a));

        LinearLayout loading = Ui.column(a);
        loading.setGravity(android.view.Gravity.CENTER);
        loading.setPadding(0, Ui.dp(a, 80), 0, Ui.dp(a, 80));
        loading.addView(Ui.progress(a, true));
        TextView loadingText = Ui.text(a, "Загружаем текст…", 12.5f, Theme.faint(), false);
        loadingText.setLetterSpacing(0.04f);
        loadingText.setPadding(0, Ui.dp(a, 14), 0, 0);
        loading.addView(loadingText);
        list.addView(loading);

        ScrollView scrollView = new ScrollView(a);
        scrollView.setClipToPadding(false);
        scrollView.setPadding(0, Ui.dp(a, 6), 0, 0);
        scrollView.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scroll = scrollView;
        int height = (int) (Ui.screenWidth(a) * 0.86f);
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
            if (sourceView != null) sourceView.setText("Ничего не нашлось");
            empty("Текста нет",
                    "Мы поискали в LRCLIB, Netease, Яндекс Музыке и lyrics.ovh — "
                            + "для этого трека текст не опубликован");
            return;
        }
        if (sourceView != null) {
            StringBuilder label = new StringBuilder()
                    .append(lyrics.source == null || lyrics.source.isEmpty()
                            ? "Открытый каталог" : lyrics.source);
            label.append(lyrics.synced ? "  ·  синхронизированный текст" : "  ·  обычный текст");
            if (!lyrics.writers.isEmpty()) {
                label.append("  ·  ").append(android.text.TextUtils.join(", ", lyrics.writers));
            }
            sourceView.setText(label.toString());
        }
        lines.clear();
        lines.addAll(lyrics.lines);
        synced = lyrics.synced;
        lineViews.clear();
        list.removeAllViews();
        for (Models.LyricLine line : lines) {
            TextView t = Ui.text(a, line.text == null || line.text.isEmpty()
                    ? "\u00A0" : line.text, 16, Theme.dim(), false);
            t.setLineSpacing(0, 1.42f);
            t.setPadding(0, Ui.dp(a, 6), 0, Ui.dp(a, 6));
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
        box.setPadding(Ui.dp(a, 16), Ui.dp(a, 72), Ui.dp(a, 16), Ui.dp(a, 72));
        box.addView(Widgets.circleIcon(a, com.mashiro.mobile.R.drawable.ic_lyrics, 54,
                Theme.surface2(), Theme.faint()));
        TextView heading = Ui.title(a, title, 17);
        heading.setGravity(android.view.Gravity.CENTER);
        heading.setPadding(0, Ui.dp(a, 16), 0, 0);
        box.addView(heading);
        TextView sub = Ui.text(a, message, 12.5f, Theme.dim(), false);
        sub.setGravity(android.view.Gravity.CENTER);
        sub.setLineSpacing(0, 1.3f);
        sub.setPadding(Ui.dp(a, 12), Ui.dp(a, 8), Ui.dp(a, 12), 0);
        box.addView(sub);
        list.addView(box);
    }
}