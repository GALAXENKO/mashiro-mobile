package com.mashiro.mobile.ui;

import android.app.Dialog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mashiro.mobile.App;
import com.mashiro.mobile.Downloader;
import com.mashiro.mobile.MainActivity;
import com.mashiro.mobile.R;
import com.mashiro.mobile.Store;
import com.mashiro.mobile.Theme;
import com.mashiro.mobile.Ui;
import com.mashiro.mobile.Widgets;
import com.mashiro.mobile.api.Models;
import com.mashiro.mobile.audio.Playback;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Bottom sheet with actions for a single track. */
public final class TrackMenu {
    private TrackMenu() {}

    public static void show(MainActivity a, Models.Track track, Long playlistKind,
                            Integer playlistIndex) {
        if (track == null) return;
        LinearLayout content = Ui.column(a);
        content.setPadding(Ui.dp(a, 20), 0, Ui.dp(a, 20), Ui.dp(a, 12));

        LinearLayout head = Ui.row(a);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(0, Ui.dp(a, 2), 0, Ui.dp(a, 16));
        head.addView(Widgets.cover(a, track.coverUrl, 54, 12));
        LinearLayout copy = Ui.column(a);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = Ui.dp(a, 14);
        copy.setLayoutParams(clp);
        TextView title = Ui.title(a, track.title, 18);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        copy.addView(title);
        TextView artist = Ui.text(a, track.artistLine(), 12, Theme.dim(), false);
        artist.setSingleLine(true);
        artist.setEllipsize(android.text.TextUtils.TruncateAt.END);
        artist.setPadding(0, Ui.dp(a, 3), 0, 0);
        copy.addView(artist);
        head.addView(copy);
        content.addView(head);

        final Dialog dialog = Ui.sheet(a, content);

        Section play = new Section(a);
        play.add(a, R.drawable.ic_play, "Воспроизвести", null,
                () -> {
                    dialog.dismiss();
                    Playback.get().playTrackNow(track);
                    a.ensureMini();
                });
        play.add(a, R.drawable.ic_next, "Играть следующим", null,
                () -> {
                    Playback.get().playNext(track);
                    dialog.dismiss();
                    Ui.toast(a, "Добавлено следующим");
                });
        play.add(a, R.drawable.ic_queue, "Добавить в очередь", null,
                () -> {
                    Playback.get().enqueue(track);
                    dialog.dismiss();
                    Ui.toast(a, "Добавлено в очередь");
                });
        addSection(a, content, play.box);

        final boolean liked = Store.isLiked(track.id);
        final boolean disliked = Store.isDisliked(track.id);
        Section taste = new Section(a);
        taste.add(a, liked ? R.drawable.ic_heart_fill : R.drawable.ic_heart,
                liked ? "Убрать из «Мне нравится»" : "В «Мне нравится»",
                liked ? Theme.accent() : null, () -> {
                    setLiked(a, track, !liked);
                    dialog.dismiss();
                });
        taste.add(a, R.drawable.ic_dislike,
                disliked ? "Отменить «не нравится»" : "Не нравится", null, () -> {
                    setDisliked(a, track, !disliked);
                    dialog.dismiss();
                });
        taste.add(a, R.drawable.ic_plus, "Добавить в плейлист", null,
                () -> {
                    dialog.dismiss();
                    pickPlaylist(a, track);
                });
        if (playlistKind != null) {
            taste.add(a, R.drawable.ic_trash, "Удалить из плейлиста", null, () -> {
                dialog.dismiss();
                removeFromPlaylist(a, playlistKind, playlistIndex == null ? 0 : playlistIndex, track);
            });
        }
        addSection(a, content, taste.box);

        Section extra = new Section(a);
        if (playlistKind == null) {
            String existing = Store.downloadPath(track.id);
            boolean downloaded = !existing.isEmpty() && new File(existing).exists();
            if (Downloader.isActive(track.id)) {
                extra.add(a, R.drawable.ic_download, "Скачивается…", null, () -> dialog.dismiss());
            } else if (downloaded) {
                extra.add(a, R.drawable.ic_download, "Удалить загрузку", Theme.accent(), () -> {
                    Store.removeDownloadedTrack(track.id);
                    dialog.dismiss();
                    Ui.toast(a, "Загрузка удалена");
                });
            } else {
                extra.add(a, R.drawable.ic_download, "Скачать", null, () -> {
                    dialog.dismiss();
                    download(a, track);
                });
            }
        }
        extra.add(a, R.drawable.ic_lyrics, "Текст песни", null,
                () -> {
                    dialog.dismiss();
                    new LyricsScreen(a, track).show();
                });
        extra.add(a, R.drawable.ic_share, "Поделиться", null,
                () -> {
                    dialog.dismiss();
                    a.share(track.title + " — " + track.artistLine() + "\n" + track.shareUrl());
                });
        addSection(a, content, extra.box);

        Section open = new Section(a);
        if (!track.albumId.isEmpty()) {
            open.add(a, R.drawable.ic_music, "Открыть альбом", null,
                    () -> {
                        dialog.dismiss();
                        a.openAlbum(track.albumId);
                    });
        }
        if (!track.artists.isEmpty()) {
            open.add(a, R.drawable.ic_person, "Открыть исполнителя", null,
                    () -> {
                        dialog.dismiss();
                        a.openArtist(track.artists.get(0).id);
                    });
        }
        if (open.count > 0) addSection(a, content, open.box);

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

    // --------------------------------------------------------------- actions

    private static void setLiked(MainActivity a, Models.Track track, boolean liked) {
        Store.setLiked(track.id, liked);
        if (Playback.get().wave && Playback.get().current() != null
                && track.id.equals(Playback.get().current().id)) {
            a.bg(() -> {
                App.api.waveFeedback(Playback.get().station, liked ? "dislike" : "like",
                        track.id, Playback.get().waveBatch, 0);
                return true;
            }, x -> {}, true);
        }
        a.updateRows();
        a.bg(() -> {
            App.api.setLike(track.id, false, !liked);
            return true;
        }, x -> {}, true);
        Ui.toast(a, liked ? "В «Мне нравится»" : "Убрали из «Мне нравится»");
    }

    private static void setDisliked(MainActivity a, Models.Track track, boolean disliked) {
        Store.setDisliked(track.id, disliked);
        if (Playback.get().wave && Playback.get().current() != null
                && track.id.equals(Playback.get().current().id)) {
            a.bg(() -> {
                App.api.waveFeedback(Playback.get().station, "dislike",
                        track.id, Playback.get().waveBatch, 0);
                return true;
            }, x -> {}, true);
        }
        a.updateRows();
        a.bg(() -> {
            App.api.setLike(track.id, true, !disliked);
            return true;
        }, x -> {}, true);
        if (disliked) Playback.get().next(false);
        Ui.toast(a, disliked ? "Больше не нравится" : "Понравилось снова");
    }

    private static void removeFromPlaylist(MainActivity a, long kind, int index, Models.Track track) {
        a.bg(() -> {
            App.api.removeTrackFromPlaylist(kind, index, track);
            return true;
        }, ok -> {
            Ui.toast(a, "Удалено из плейлиста");
            a.refreshCurrent();
        });
    }

    private static void download(MainActivity a, Models.Track track) {
        Ui.toast(a, "Скачиваем…");
        Downloader.download(a, App.api, track, new Downloader.Callback() {
            @Override public void onProgress(int percent) {}
            @Override public void onDone(String path) {
                Ui.toast(a, "Скачано: " + track.title);
            }
            @Override public void onError(String message) {
                Ui.toast(a, "Ошибка: " + message);
            }
        });
    }

    // ------------------------------------------------------- playlist picker

    private static void pickPlaylist(MainActivity a, Models.Track track) {
        LinearLayout content = Ui.column(a);
        content.setPadding(Ui.dp(a, 20), 0, Ui.dp(a, 20), Ui.dp(a, 12));
        TextView heading = Ui.title(a, "Добавить в плейлист", 20);
        heading.setPadding(0, Ui.dp(a, 2), 0, Ui.dp(a, 14));
        content.addView(heading);

        final LinearLayout box = Widgets.group(a);
        content.addView(box, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout create = actionRow(a, R.drawable.ic_plus, "Новый плейлист",
                Theme.accent(), null);
        box.addView(create);

        final Dialog picker = Ui.sheet(a, content);
        create.setOnClickListener(v -> {
            picker.dismiss();
            Ui.input(a, "Новый плейлист", "", "Название плейлиста", "Создать", false,
                    name -> {
                        if (name.isEmpty()) return;
                        a.bg(() -> {
                            Models.Playlist created = App.api.createPlaylist(name, false);
                            App.api.addTrackToPlaylist(created.kind, track);
                            return created;
                        }, created -> Ui.toast(a, "Трек добавлен в «" + name + "»"));
                    }).show();
        });

        a.bg(App.api::playlists, list -> {
            if (list == null || list.isEmpty()) return;
            for (Models.Playlist p : list) {
                box.addView(Widgets.rowDivider(a));
                LinearLayout row = Ui.row(a);
                row.setPadding(Ui.dp(a, 14), Ui.dp(a, 9), Ui.dp(a, 12), Ui.dp(a, 9));
                row.addView(Widgets.circleIcon(a, R.drawable.ic_music, 34,
                        Theme.surface2(), Theme.text()));
                LinearLayout copy = Ui.column(a);
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                clp.leftMargin = Ui.dp(a, 13);
                copy.setLayoutParams(clp);
                TextView name = Ui.text(a, p.title, 14, Theme.text(), true);
                name.setSingleLine(true);
                name.setEllipsize(android.text.TextUtils.TruncateAt.END);
                copy.addView(name);
                TextView count = Ui.text(a, p.trackCount + " " + Ui.plural(p.trackCount,
                        "трек", "трека", "треков"), 11.5f, Theme.dim(), false);
                count.setPadding(0, Ui.dp(a, 2), 0, 0);
                copy.addView(count);
                row.addView(copy);
                Ui.ripple(row, Theme.light() ? 0x14101014 : 0x1AFFFFFF);
                row.setOnClickListener(v -> {
                    picker.dismiss();
                    addToPlaylist(a, p, track);
                });
                box.addView(row);
            }
        });
        picker.show();
    }

    private static void addToPlaylist(MainActivity a, Models.Playlist p, Models.Track track) {
        a.bg(() -> {
            App.api.addTrackToPlaylist(p.kind, track);
            return true;
        }, ok -> Ui.toast(a, "Трек добавлен в «" + p.title + "»"));
    }
}