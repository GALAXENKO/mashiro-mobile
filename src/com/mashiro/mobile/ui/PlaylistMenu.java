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
        content.setPadding(Ui.dp(a, 16), 0, Ui.dp(a, 8), Ui.dp(a, 8));

        LinearLayout head = Ui.row(a);
        head.setPadding(0, 0, 0, Ui.dp(a, 8));
        head.addView(Widgets.cover(a, playlist.coverUrl, 52, 9));
        LinearLayout copy = Ui.column(a);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = Ui.dp(a, 12);
        copy.setLayoutParams(clp);
        TextView title = Ui.title(a, playlist.title, 17);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        copy.addView(title);
        TextView sub = Ui.text(a, playlist.trackCount + " "
                + Ui.plural(playlist.trackCount, "трек", "трека", "треков"),
                12, Theme.dim(), false);
        sub.setPadding(0, Ui.dp(a, 2), 0, 0);
        copy.addView(sub);
        head.addView(copy);
        content.addView(head);

        final Dialog dialog = Ui.sheet(a, content);
        final boolean own = playlist.ownerUid <= 0 || playlist.ownerUid == Store.uid();

        addRow(a, content, R.drawable.ic_play, "Воспроизвести", () -> {
            dialog.dismiss();
            if (!playlist.tracks.isEmpty()) a.playList(playlist.tracks, 0);
        });
        addRow(a, content, R.drawable.ic_shuffle, "Перемешать", () -> {
            dialog.dismiss();
            if (!playlist.tracks.isEmpty()) a.playShuffled(playlist.tracks);
        });
        addRow(a, content, R.drawable.ic_share, "Поделиться", () -> {
            dialog.dismiss();
            long owner = playlist.ownerUid > 0 ? playlist.ownerUid : Store.uid();
            a.share(playlist.title + " — плейлист в Яндекс Музыке\n"
                    + "https://music.yandex.ru/users/" + owner + "/playlists/" + playlist.kind);
        });

        if (own) {
            content.addView(Ui.divider(a));
            addRow(a, content, R.drawable.ic_edit, "Переименовать", () -> {
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
            addRow(a, content, R.drawable.ic_eye,
                    playlist.isPublic ? "Сделать приватным" : "Сделать публичным", () -> {
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
            addRow(a, content, R.drawable.ic_plus, "Добавить треки", () -> {
                dialog.dismiss();
                Ui.toast(a, "Откройте поиск и добавьте трек через меню «⋯»");
            });
            addRow(a, content, R.drawable.ic_trash, "Удалить плейлист", () -> {
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
        }

        dialog.show();
    }

    private static void addRow(MainActivity a, LinearLayout parent, int icon, String label,
                               Runnable click) {
        LinearLayout row = Ui.row(a);
        row.setBackground(Ui.bg(a, 0x00000000, 10));
        row.setPadding(Ui.dp(a, 10), Ui.dp(a, 10), Ui.dp(a, 8), Ui.dp(a, 10));
        int tint = label.equals("Удалить плейлист") ? Theme.danger() : Theme.text();
        row.addView(Widgets.circleIcon(a, icon, 32, Theme.surface2(), tint));
        LinearLayout.LayoutParams lb = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lb.leftMargin = Ui.dp(a, 12);
        TextView t = Ui.text(a, label, 14, tint, false);
        t.setLayoutParams(lb);
        row.addView(t);
        row.setClickable(true);
        Ui.ripple(row, Theme.light() ? 0x14101014 : 0x1AFFFFFF);
        row.setOnClickListener(v -> {
            if (click != null) click.run();
        });
        parent.addView(row);
    }
}