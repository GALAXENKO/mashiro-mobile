package com.mashiro.mobile.ui;

import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mashiro.mobile.App;
import com.mashiro.mobile.MainActivity;
import com.mashiro.mobile.R;
import com.mashiro.mobile.Store;
import com.mashiro.mobile.Theme;
import com.mashiro.mobile.Ui;
import com.mashiro.mobile.Widgets;
import com.mashiro.mobile.api.Models;

/** Bottom sheet with actions for a playlist. */
public final class PlaylistMenu {
    private PlaylistMenu() {}

    public static void show(MainActivity a, Models.Playlist playlist, Runnable onChanged) {
        if (playlist == null) return;
        LinearLayout content = Ui.column(a);
        content.setPadding(Ui.dp(a, 20), 0, Ui.dp(a, 20), Ui.dp(a, 12));

        LinearLayout head = Ui.row(a);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        head.setPadding(0, Ui.dp(a, 2), 0, Ui.dp(a, 16));
        head.addView(Widgets.cover(a, playlist.coverUrl, 54, 12));
        LinearLayout copy = Ui.column(a);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = Ui.dp(a, 14);
        copy.setLayoutParams(clp);
        TextView title = Ui.title(a, playlist.title, 18);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        copy.addView(title);
        TextView sub = Ui.text(a, playlist.trackCount + " "
                + Ui.plural(playlist.trackCount, "трек", "трека", "треков"),
                12, Theme.dim(), false);
        sub.setPadding(0, Ui.dp(a, 3), 0, 0);
        copy.addView(sub);
        head.addView(copy);
        content.addView(head);

        final Dialog dialog = Ui.sheet(a, content);
        final boolean own = playlist.ownerUid <= 0 || playlist.ownerUid == Store.uid();

        Section main = new Section(a);
        main.add(a, R.drawable.ic_play, "Воспроизвести", null, () -> {
            dialog.dismiss();
            if (!playlist.tracks.isEmpty()) a.playList(playlist.tracks, 0);
        });
        main.add(a, R.drawable.ic_shuffle, "Перемешать", null, () -> {
            dialog.dismiss();
            if (!playlist.tracks.isEmpty()) a.playShuffled(playlist.tracks);
        });
        main.add(a, R.drawable.ic_share, "Поделиться", null, () -> {
            dialog.dismiss();
            long owner = playlist.ownerUid > 0 ? playlist.ownerUid : Store.uid();
            a.share(playlist.title + " — плейлист в Яндекс Музыке\n"
                    + "https://music.yandex.ru/users/" + owner + "/playlists/" + playlist.kind);
        });
        addSection(a, content, main.box);

        if (own) {
            Section edit = new Section(a);
            edit.add(a, R.drawable.ic_edit, "Переименовать", null, () -> {
                dialog.dismiss();
                Ui.input(a, "Переименовать плейлист", playlist.title, "Название",
                        "Сохранить", false, name -> {
                            if (name.isEmpty()) return;
                            a.bg(() -> {
                                App.api.renamePlaylist(playlist.kind, name);
                                return true;
                            }, ok -> {
                                Ui.toast(a, "Плейлист переименован");
                                if (onChanged != null) onChanged.run();
                            });
                        }).show();
            });
            edit.add(a, R.drawable.ic_eye,
                    playlist.isPublic ? "Сделать приватным" : "Сделать публичным", null, () -> {
                        dialog.dismiss();
                        final boolean next = !playlist.isPublic;
                        a.bg(() -> {
                            App.api.setPlaylistVisibility(playlist.kind, next);
                            return true;
                        }, ok -> {
                            Ui.toast(a, next ? "Плейлист стал приватным"
                                    : "Плейлист стал публичным");
                            if (onChanged != null) onChanged.run();
                        });
                    });
            edit.add(a, R.drawable.ic_plus, "Добавить треки", null, () -> {
                dialog.dismiss();
                Ui.toast(a, "Откройте поиск и добавьте трек через меню «⋯»");
            });
            addSection(a, content, edit.box);

            Section danger = new Section(a);
            danger.add(a, R.drawable.ic_trash, "Удалить плейлист", Theme.danger(), () -> {
                dialog.dismiss();
                Ui.confirm(a, "Удалить плейлист?",
                        "«" + playlist.title + "» будет удалён без возможности восстановления",
                        "Удалить", () -> a.bg(() -> {
                            App.api.deletePlaylist(playlist.kind);
                            return true;
                        }, ok -> {
                            Ui.toast(a, "Плейлист удалён");
                            if (onChanged != null) onChanged.run();
                            a.closeTop();
                        })).show();
            });
            addSection(a, content, danger.box);
        }

        dialog.show();
    }

    /** Grouped block of action rows with hairline separators between them. */
    private static final class Section {
        final LinearLayout box;
        int count;

        Section(MainActivity a) {
            box = Widgets.group(a);
        }

        void add(MainActivity a, int icon, String label, Integer tint, Runnable click) {
            if (count > 0) box.addView(Widgets.rowDivider(a));
            box.addView(actionRow(a, icon, label, tint, click));
            count++;
        }
    }

    private static void addSection(MainActivity a, LinearLayout content, LinearLayout box) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(a, 10);
        content.addView(box, lp);
    }

    private static LinearLayout actionRow(MainActivity a, int icon, String label,
                                          Integer tint, Runnable click) {
        LinearLayout row = Ui.row(a);
        row.setBackground(Ui.bg(a, 0x00000000, 0));
        row.setPadding(Ui.dp(a, 14), Ui.dp(a, 11), Ui.dp(a, 12), Ui.dp(a, 11));
        row.addView(Widgets.circleIcon(a, icon, 34, Theme.surface2(),
                tint != null ? tint : Theme.text()));
        LinearLayout.LayoutParams lb = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lb.leftMargin = Ui.dp(a, 13);
        TextView t = Ui.text(a, label, 14, tint != null ? tint : Theme.text(), false);
        t.setLayoutParams(lb);
        row.addView(t);
        row.setClickable(true);
        Ui.ripple(row, Theme.light() ? 0x14101014 : 0x1AFFFFFF);
        row.setOnClickListener(v -> {
            if (click != null) click.run();
        });
        return row;
    }
}