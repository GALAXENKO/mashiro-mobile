package com.mashiro.mobile.ui;

import android.app.Dialog;
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
        content.setPadding(Ui.dp(a, 16), 0, Ui.dp(a, 8), Ui.dp(a, 8));

        LinearLayout head = Ui.row(a);
        head.setPadding(0, 0, 0, Ui.dp(a, 8));
        head.addView(Widgets.cover(a, track.coverUrl, 52, 9));
        LinearLayout copy = Ui.column(a);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = Ui.dp(a, 12);
        copy.setLayoutParams(clp);
        TextView title = Ui.title(a, track.title, 17);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        copy.addView(title);
        TextView artist = Ui.text(a, track.artistLine(), 12, Theme.dim(), false);
        artist.setSingleLine(true);
        artist.setEllipsize(android.text.TextUtils.TruncateAt.END);
        artist.setPadding(0, Ui.dp(a, 2), 0, 0);
        copy.addView(artist);
        head.addView(copy);
        content.addView(head);

        final Dialog dialog = Ui.sheet(a, content);
        addRow(a, content, R.drawable.ic_play, "Воспроизвести", null,
                () -> {
                    dialog.dismiss();
                    Playback.get().playTrackNow(track);
                    a.ensureMini();
                });
        addRow(a, content, R.drawable.ic_next, "Играть следующим", null,
                () -> {
                    Playback.get().playNext(track);
                    dialog.dismiss();
                    Ui.toast(a, "Добавлено следующим");
                });
        addRow(a, content, R.drawable.ic_queue, "Добавить в очередь", null,
                () -> {
                    Playback.get().enqueue(track);
                    dialog.dismiss();
                    Ui.toast(a, "Добавлено в очередь");
                });

        content.addView(Ui.divider(a));

        final boolean liked = Store.isLiked(track.id);
        addRow(a, content, liked ? R.drawable.ic_heart_fill : R.drawable.ic_heart,
                liked ? "Убрать из «Мне нравится»" : "В «Мне нравится»",
                liked ? Theme.accent() : null, () -> {
                    setLiked(a, track, !liked);
                    dialog.dismiss();
                });

        final boolean disliked = Store.isDisliked(track.id);
        addRow(a, content, R.drawable.ic_dislike,
                disliked ? "Отменить «не нравится»" : "Не нравится", null, () -> {
                    setDisliked(a, track, !disliked);
                    dialog.dismiss();
                });

        addRow(a, content, R.drawable.ic_plus, "Добавить в плейлист", null,
                () -> {
                    dialog.dismiss();
                    pickPlaylist(a, track);
                });

        if (playlistKind != null) {
            addRow(a, content, R.drawable.ic_trash, "Удалить из плейлиста", null, () -> {
                dialog.dismiss();
                removeFromPlaylist(a, playlistKind, playlistIndex == null ? 0 : playlistIndex, track);
            });
        }

        if (playlistKind == null) {
            String existing = Store.downloadPath(track.id);
            boolean downloaded = !existing.isEmpty() && new File(existing).exists();
            if (Downloader.isActive(track.id)) {
                addRow(a, content, R.drawable.ic_download, "Скачивается…", null, () -> dialog.dismiss());
            } else if (downloaded) {
                addRow(a, content, R.drawable.ic_download, "Удалить загрузку", Theme.accent(), () -> {
                    Store.removeDownloadedTrack(track.id);
                    dialog.dismiss();
                    Ui.toast(a, "Загрузка удалена");
                });
            } else {
                addRow(a, content, R.drawable.ic_download, "Скачать", null, () -> {
                    dialog.dismiss();
                    download(a, track);
                });
            }
        }

        addRow(a, content, R.drawable.ic_lyrics, "Текст песни", null,
                () -> {
                    dialog.dismiss();
                    new LyricsScreen(a, track).show();
                });
        addRow(a, content, R.drawable.ic_share, "Поделиться", null,
                () -> {
                    dialog.dismiss();
                    a.share(track.title + " — " + track.artistLine() + "\n" + track.shareUrl());
                });

        if (!track.albumId.isEmpty()) {
            addRow(a, content, R.drawable.ic_music, "Открыть альбом", null,
                    () -> {
                        dialog.dismiss();
                        a.openAlbum(track.albumId);
                    });
        }
        if (!track.artists.isEmpty()) {
            addRow(a, content, R.drawable.ic_person, "Открыть исполнителя", null,
                    () -> {
                        dialog.dismiss();
                        a.openArtist(track.artists.get(0).id);
                    });
        }

        dialog.show();
    }

    private static void addRow(MainActivity a, LinearLayout parent, int icon, String label,
                               Integer tint, Runnable click) {
        LinearLayout row = Ui.row(a);
        row.setBackground(Ui.bg(a, 0x00000000, 10));
        row.setPadding(Ui.dp(a, 10), Ui.dp(a, 10), Ui.dp(a, 8), Ui.dp(a, 10));
        row.addView(Widgets.circleIcon(a, icon, 32, Theme.surface2(),
                tint != null ? tint : Theme.text()));
        LinearLayout.LayoutParams lb = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lb.leftMargin = Ui.dp(a, 12);
        TextView t = Ui.text(a, label, 14, tint != null ? tint : Theme.text(), false);
        t.setLayoutParams(lb);
        row.addView(t);
        row.setClickable(true);
        Ui.ripple(row, Theme.light() ? 0x14101014 : 0x1AFFFFFF);
        View.OnClickListener onClick = v -> {
            if (click != null) click.run();
        };
        row.setOnClickListener(onClick);
        parent.addView(row);
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
        content.setPadding(Ui.dp(a, 16), 0, Ui.dp(a, 8), Ui.dp(a, 8));
        content.addView(Ui.title(a, "Добавить в плейлист", 18));

        LinearLayout create = Ui.row(a);
        create.setPadding(Ui.dp(a, 10), Ui.dp(a, 10), Ui.dp(a, 8), Ui.dp(a, 10));
        create.addView(Widgets.circleIcon(a, R.drawable.ic_plus, 32, Theme.surface2(), Theme.accent()));
        TextView createLabel = Ui.text(a, "Новый плейлист", 14, Theme.accent(), true);
        createLabel.setPadding(Ui.dp(a, 12), 0, 0, 0);
        create.addView(createLabel);
        content.addView(create);

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
                LinearLayout row = Ui.row(a);
                row.setPadding(Ui.dp(a, 10), Ui.dp(a, 9), Ui.dp(a, 6), Ui.dp(a, 9));
                row.setBackground(Ui.bg(a, 0x00000000, 10));
                row.addView(Widgets.circleIcon(a, R.drawable.ic_music, 32,
                        Theme.surface2(), Theme.text()));
                LinearLayout copy = Ui.column(a);
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                clp.leftMargin = Ui.dp(a, 12);
                copy.setLayoutParams(clp);
                copy.addView(Ui.text(a, p.title, 14, Theme.text(), true));
                copy.addView(Ui.text(a, p.trackCount + " " + Ui.plural(p.trackCount,
                        "трек", "трека", "треков"), 11, Theme.dim(), false));
                row.addView(copy);
                Ui.ripple(row, Theme.light() ? 0x14101014 : 0x1AFFFFFF);
                row.setOnClickListener(v -> {
                    picker.dismiss();
                    addToPlaylist(a, p, track);
                });
                content.addView(row);
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