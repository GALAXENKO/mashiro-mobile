package com.mashiro.mobile.ui;

import android.app.Dialog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mashiro.mobile.ImageLoader;
import com.mashiro.mobile.MainActivity;
import com.mashiro.mobile.R;
import com.mashiro.mobile.Theme;
import com.mashiro.mobile.Ui;
import com.mashiro.mobile.Widgets;
import com.mashiro.mobile.api.Models;
import com.mashiro.mobile.audio.Playback;

import java.util.List;

/** Bottom sheet with the current playback queue and transport options. */
public final class QueueSheet {
    private QueueSheet() {}

    public static void show(MainActivity a) {
        final Playback p = Playback.get();
        LinearLayout content = Ui.column(a);
        content.setPadding(Ui.dp(a, 20), 0, Ui.dp(a, 20), Ui.dp(a, 12));

        LinearLayout header = Ui.row(a);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, Ui.dp(a, 2), 0, Ui.dp(a, 12));
        LinearLayout copy = Ui.column(a);
        copy.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        copy.addView(Ui.overline(a, "Воспроизведение"));
        TextView heading = Ui.title(a, "Очередь", 21);
        heading.setPadding(0, Ui.dp(a, 5), 0, 0);
        copy.addView(heading);
        TextView subtitle = Ui.text(a, "", 11.5f, Theme.dim(), false);
        subtitle.setPadding(0, Ui.dp(a, 4), 0, 0);
        copy.addView(subtitle);
        header.addView(copy);
        header.addView(Ui.iconButton(a, R.drawable.ic_close, 40, 18, Theme.dim(),
                v -> {
                    Dialog dialog = (Dialog) v.getTag();
                    if (dialog != null) dialog.dismiss();
                }));
        content.addView(header);

        LinearLayout controls = Ui.row(a);
        controls.setPadding(0, 0, 0, Ui.dp(a, 12));
        TextView shuffle = Ui.chip(a, "Перемешать", p.shuffle, null);
        TextView repeat = Ui.chip(a, repeatLabel(p.repeat), p.repeat != 0, null);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.rightMargin = Ui.dp(a, 8);
        shuffle.setLayoutParams(slp);
        controls.addView(shuffle);
        controls.addView(repeat);
        content.addView(controls);

        ScrollView scroll = new ScrollView(a);
        scroll.setClipToPadding(false);
        LinearLayout list = Widgets.group(a);
        scroll.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 380));
        content.addView(scroll, scrollLp);

        LinearLayout footer = Ui.row(a);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(0, Ui.dp(a, 12), 0, 0);
        TextView clear = Ui.text(a, "Очистить", 12.5f, Theme.danger(), true);
        clear.setPadding(0, Ui.dp(a, 6), Ui.dp(a, 16), Ui.dp(a, 6));
        footer.addView(clear);
        TextView sleep = Ui.text(a, "Таймер сна", 12.5f, Theme.dim(), true);
        sleep.setPadding(0, Ui.dp(a, 6), 0, Ui.dp(a, 6));
        footer.addView(sleep);
        content.addView(footer);

        final Dialog dialog = Ui.sheet(a, content);
        header.getChildAt(1).setTag(dialog);

        final Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            List<Models.Track> queue = p.queue;
            subtitle.setText(queue.size() + " " + Ui.plural(queue.size(),
                    "трек", "трека", "треков"));
            shuffle.setBackground(Ui.bg(a, p.shuffle ? Theme.accent() : Theme.surface2(), 20));
            shuffle.setTextColor(p.shuffle ? Theme.accentText() : Theme.dim());
            repeat.setText(repeatLabel(p.repeat));
            repeat.setBackground(Ui.bg(a, p.repeat != 0 ? Theme.accent() : Theme.surface2(), 20));
            repeat.setTextColor(p.repeat != 0 ? Theme.accentText() : Theme.dim());
            long remaining = p.sleepRemainingMs();
            sleep.setText(remaining > 0
                    ? "Сон: " + Ui.duration(remaining)
                    : "Таймер сна");
            sleep.setTextColor(remaining > 0 ? Theme.accent() : Theme.dim());

            list.removeAllViews();
            if (queue.isEmpty()) {
                list.addView(Widgets.empty(a, "Очередь пуста",
                        "Включите трек или волну, чтобы наполнить очередь"));
                return;
            }
            for (int i = 0; i < queue.size(); i++) {
                Models.Track track = queue.get(i);
                final int index = i;
                if (i > 0) list.addView(Widgets.rowDivider(a));
                LinearLayout row = Ui.row(a);
                row.setPadding(Ui.dp(a, 12), Ui.dp(a, 9), Ui.dp(a, 6), Ui.dp(a, 9));
                boolean active = i == p.index;
                row.setBackground(Ui.bg(a, active ? Theme.surface2() : 0x00000000, 12));
                row.addView(Widgets.cover(a, track.coverUrl, 46, 10));
                LinearLayout copyRow = Ui.column(a);
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                clp.leftMargin = Ui.dp(a, 12);
                copyRow.setLayoutParams(clp);
                TextView title = Ui.text(a, track.title, 14,
                        active ? Theme.accent() : Theme.text(), true);
                title.setSingleLine(true);
                title.setEllipsize(android.text.TextUtils.TruncateAt.END);
                copyRow.addView(title);
                TextView artist = Ui.text(a, track.artistLine(), 11.5f, Theme.dim(), false);
                artist.setSingleLine(true);
                artist.setEllipsize(android.text.TextUtils.TruncateAt.END);
                artist.setPadding(0, Ui.dp(a, 2), 0, 0);
                copyRow.addView(artist);
                row.addView(copyRow);
                row.addView(Ui.iconButton(a, R.drawable.ic_close, 34, 15, Theme.faint(),
                        v -> {
                            p.removeAt(index);
                            refresh[0].run();
                        }));
                row.setOnClickListener(v -> {
                    p.playAt(index);
                    refresh[0].run();
                });
                row.setOnLongClickListener(v -> {
                    a.openTrackMenu(track, null, null);
                    return true;
                });
                list.addView(row);
            }
        };

        Playback.Listener listener = new Playback.Listener() {
            @Override public void onPlaybackState() { refresh[0].run(); }
            @Override public void onTrackChanged() { refresh[0].run(); }
            @Override public void onProgress() {}
        };
        p.addListener(listener);
        dialog.setOnDismissListener(d -> p.removeListener(listener));

        shuffle.setOnClickListener(v -> {
            p.toggleShuffle();
            refresh[0].run();
        });
        repeat.setOnClickListener(v -> {
            p.setRepeat((p.repeat + 1) % 3);
            refresh[0].run();
        });
        clear.setOnClickListener(v -> Ui.confirm(a, "Очистить очередь?",
                "Текущий трек остановится", "Очистить", () -> {
                    p.queue.clear();
                    p.index = -1;
                    p.stop();
                    dialog.dismiss();
                }).show());
        sleep.setOnClickListener(v -> {
            long remaining = p.sleepRemainingMs();
            int next = remaining <= 0 ? 15 : remaining > 45 * 60_000L ? 0
                    : remaining > 20 * 60_000L ? 60 : 30;
            if (remaining > 0 && remaining <= 20 * 60_000L) next = 0;
            p.setSleepMinutes(next);
            Ui.toast(a, next == 0 ? "Таймер сна выключен"
                    : "Сон через " + next + " мин");
            refresh[0].run();
        });

        refresh[0].run();
        dialog.show();
    }

    private static String repeatLabel(int repeat) {
        if (repeat == 1) return "Повтор всех";
        if (repeat == 2) return "Повтор трека";
        return "Без повтора";
    }
}
