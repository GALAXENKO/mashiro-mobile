package com.mashiro.mobile.ui;

import android.graphics.Color;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.mashiro.mobile.App;
import com.mashiro.mobile.Downloader;
import com.mashiro.mobile.ImageLoader;
import com.mashiro.mobile.MainActivity;
import com.mashiro.mobile.R;
import com.mashiro.mobile.Store;
import com.mashiro.mobile.Theme;
import com.mashiro.mobile.Ui;
import com.mashiro.mobile.Widgets;
import com.mashiro.mobile.api.Models;
import com.mashiro.mobile.audio.Playback;

/** Full-screen now-playing panel shown above the navigation shell. */
public class PlayerScreen {
    private final MainActivity a;
    private final LinearLayout root;
    private final FrameLayout coverBox;
    private final ImageLoader.RoundedImageView cover;
    private final TextView title;
    private final TextView artist;
    private final TextView album;
    private final TextView source;
    private final TextView elapsed;
    private final TextView total;
    private final TextView error;
    private final SeekBar seek;
    private final ImageView play;
    private final ImageView like;
    private final ImageView repeat;
    private final ImageView shuffle;
    private final ImageView download;
    private boolean shown = false;
    private boolean seeking = false;

    public PlayerScreen(MainActivity a) {
        this.a = a;
        root = Ui.column(a);
        root.setBackgroundColor(Theme.bg());
        root.setPadding(Ui.dp(a, 20), 0, Ui.dp(a, 20), Ui.dp(a, 8));

        LinearLayout header = Ui.row(a);
        header.setPadding(0, Ui.dp(a, 10), 0, Ui.dp(a, 8));
        header.addView(Ui.iconButton(a, R.drawable.ic_arrow_down, 42, 22, Theme.text(), v -> hide()));
        LinearLayout hcopy = Ui.column(a);
        hcopy.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        hcopy.setGravity(Gravity.CENTER);
        TextView label = Ui.overline(a, "Сейчас играет");
        label.setTextColor(Theme.dim());
        label.setGravity(Gravity.CENTER);
        hcopy.addView(label);
        source = Ui.text(a, "Mashiro", 10.5f, Theme.faint(), false);
        source.setGravity(Gravity.CENTER);
        source.setPadding(0, Ui.dp(a, 3), 0, 0);
        hcopy.addView(source);
        header.addView(hcopy);
        header.addView(Ui.iconButton(a, R.drawable.ic_queue, 42, 21, Theme.text(), v -> a.openQueue()));
        root.addView(header);

        coverBox = new FrameLayout(a);
        root.addView(coverBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // soft accent halo behind the artwork
        View halo = new View(a) {
            @Override
            protected void onDraw(android.graphics.Canvas canvas) {
                android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                paint.setShader(new android.graphics.RadialGradient(getWidth() / 2f, getHeight() / 2f,
                        Math.max(getWidth(), getHeight()) * 0.54f,
                        new int[]{Theme.accentSoft(56), Theme.accentSoft(0)},
                        new float[]{0.42f, 1f}, android.graphics.Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            }
        };
        coverBox.addView(halo, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        int size = Math.min(Ui.screenWidth(a) - Ui.dp(a, 58), Ui.dp(a, 380));
        cover = new ImageLoader.RoundedImageView(a);
        cover.setRadiusDp(26);
        cover.setBackground(Ui.bg(a, Theme.surface2(), 26));
        cover.setElevation(Ui.dp(a, 18));
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(size, size);
        clp.gravity = Gravity.CENTER;
        coverBox.addView(cover, clp);
        cover.setOnClickListener(v -> a.openQueue());
        coverBox.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int available = Math.min(v.getWidth(), v.getHeight()) - Ui.dp(a, 26);
            int target = Math.max(Ui.dp(a, 150), Math.min(available, Ui.dp(a, 380)));
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) cover.getLayoutParams();
            if (lp.width != target || lp.height != target) {
                lp.width = target;
                lp.height = target;
                cover.setLayoutParams(lp);
            }
        });

        LinearLayout info = Ui.column(a);
        info.setPadding(0, Ui.dp(a, 26), 0, 0);
        title = Ui.text(a, "", 25, Theme.text(), true);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
        title.setSelected(true);
        info.addView(title);
        artist = Ui.text(a, "", 14, Theme.dim(), false);
        artist.setSingleLine(true);
        artist.setEllipsize(android.text.TextUtils.TruncateAt.END);
        artist.setPadding(0, Ui.dp(a, 7), 0, 0);
        artist.setOnClickListener(v -> {
            Models.Track t = Playback.get().current();
            if (t != null && !t.artists.isEmpty() && !t.artists.get(0).id.isEmpty()) {
                a.openArtist(t.artists.get(0).id);
            }
        });
        info.addView(artist);
        album = Ui.text(a, "", 11, Theme.faint(), false);
        album.setSingleLine(true);
        album.setEllipsize(android.text.TextUtils.TruncateAt.END);
        album.setPadding(0, Ui.dp(a, 5), 0, 0);
        album.setLetterSpacing(0.02f);
        info.addView(album);
        error = Ui.text(a, "", 11.5f, Theme.danger(), false);
        error.setVisibility(View.GONE);
        error.setPadding(0, Ui.dp(a, 6), 0, 0);
        info.addView(error);
        root.addView(info);

        seek = new SeekBar(a);
        seek.setMax(1000);
        seek.setPadding(0, Ui.dp(a, 20), 0, 0);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            seek.setProgressTintList(android.content.res.ColorStateList.valueOf(Theme.accent()));
            seek.setThumbTintList(android.content.res.ColorStateList.valueOf(Theme.accent()));
            seek.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Theme.surface3()));
        }
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    long duration = Playback.get().duration;
                    elapsed.setText(Ui.duration(duration > 0 ? duration * progress / 1000 : 0));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {
                seeking = true;
            }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                seeking = false;
                long duration = Playback.get().duration;
                if (duration > 0) Playback.get().seekTo(duration * seekBar.getProgress() / 1000);
            }
        });
        root.addView(seek, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout times = Ui.row(a);
        elapsed = Ui.text(a, "0:00", 11, Theme.faint(), true);
        total = Ui.text(a, "0:00", 11, Theme.faint(), true);
        total.setGravity(Gravity.END);
        times.setPadding(0, Ui.dp(a, 2), 0, 0);
        times.addView(elapsed, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        times.addView(total);
        root.addView(times);

        LinearLayout controls = Ui.row(a);
        controls.setPadding(0, Ui.dp(a, 16), 0, Ui.dp(a, 4));
        controls.setGravity(Gravity.CENTER);
        shuffle = Ui.icon(a, R.drawable.ic_shuffle, 20, Theme.faint());
        shuffle.setOnClickListener(v -> {
            Playback.get().toggleShuffle();
            update();
        });
        repeat = Ui.icon(a, R.drawable.ic_repeat, 20, Theme.faint());
        repeat.setOnClickListener(v -> {
            Playback.get().repeat = (Playback.get().repeat + 1) % 3;
            Playback.get().setRepeat(Playback.get().repeat);
            update();
        });
        ImageView prev = Ui.icon(a, R.drawable.ic_prev, 34, Theme.text());
        prev.setOnClickListener(v -> Playback.get().prev());
        FrameLayout playBox = new FrameLayout(a);
        GradientDrawable playBg = Ui.gradient(a, Theme.accent(), Theme.accent(), 38);
        playBox.setBackground(playBg);
        playBox.setElevation(Ui.dp(a, 10));
        play = new ImageView(a);
        play.setImageResource(R.drawable.ic_play);
        play.setColorFilter(Theme.accentText());
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(Ui.dp(a, 34), Ui.dp(a, 34));
        plp.gravity = Gravity.CENTER;
        playBox.addView(play, plp);
        playBox.setOnClickListener(v -> {
            Playback.get().toggle();
            play.postDelayed(this::update, 120);
        });
        ImageView next = Ui.icon(a, R.drawable.ic_next, 34, Theme.text());
        next.setOnClickListener(v -> Playback.get().next(true));

        controls.addView(wrap(shuffle, 46));
        controls.addView(wrap(prev, 58));
        controls.addView(playBox);
        LinearLayout.LayoutParams pb = new LinearLayout.LayoutParams(Ui.dp(a, 74), Ui.dp(a, 74));
        pb.leftMargin = Ui.dp(a, 16);
        pb.rightMargin = Ui.dp(a, 16);
        playBox.setLayoutParams(pb);
        controls.addView(wrap(next, 58));
        controls.addView(wrap(repeat, 46));
        root.addView(controls);

        LinearLayout actions = Ui.row(a);
        actions.setPadding(0, Ui.dp(a, 14), 0, 0);
        like = buildAction(actions, R.drawable.ic_heart, "Нравится", v -> toggleLike());
        buildAction(actions, R.drawable.ic_lyrics, "Текст", v -> openLyrics());
        download = buildAction(actions, R.drawable.ic_download, "Скачать", v -> download());
        buildAction(actions, R.drawable.ic_share, "Поделиться", v -> share());
        root.addView(actions);
    }

    private View wrap(View icon, int sizeDp) {
        FrameLayout box = new FrameLayout(a);
        box.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(a, sizeDp), Ui.dp(a, sizeDp)));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        box.addView(icon, lp);
        box.setOnClickListener(v -> icon.performClick());
        return box;
    }

    private ImageView buildAction(LinearLayout parent, int icon, String text, View.OnClickListener click) {
        LinearLayout item = Ui.column(a);
        item.setGravity(Gravity.CENTER);
        item.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        FrameLayout box = new FrameLayout(a);
        box.setBackground(Ui.bg(a, Theme.surface2(), 20));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(Ui.dp(a, 40), Ui.dp(a, 40));
        blp.gravity = Gravity.CENTER_HORIZONTAL;
        item.addView(box, blp);
        ImageView iconView = Ui.icon(a, icon, 20, Theme.dim());
        FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(Ui.dp(a, 20), Ui.dp(a, 20));
        ilp.gravity = Gravity.CENTER;
        box.addView(iconView, ilp);
        TextView label = Ui.text(a, text, 10, Theme.faint(), false);
        label.setGravity(Gravity.CENTER);
        label.setPadding(0, Ui.dp(a, 7), 0, 0);
        item.addView(label);
        item.setOnClickListener(click);
        parent.addView(item);
        return iconView;
    }

    private void toggleLike() {
        Models.Track track = Playback.get().current();
        if (track == null) return;
        if (Store.guestMode()) {
            Ui.toast(a, "Войдите в аккаунт, чтобы ставить лайки");
            return;
        }
        if (Store.isImported(track.id)) {
            Ui.toast(a, "Локальные файлы не добавляются в избранное");
            return;
        }
        boolean liked = Store.isLiked(track.id);
        a.bg(() -> {
            App.api.setLike(track.id, false, liked);
            return true;
        }, ok -> {
            Store.setLiked(track.id, !liked);
            if (Playback.get().wave) {
                a.bg(() -> {
                    App.api.waveFeedback(Playback.get().station, liked ? "dislike" : "like",
                            track.id, Playback.get().waveBatch, 0);
                    return true;
                }, x -> {}, true);
            }
            update();
            a.updateRows();
            Ui.toast(a, liked ? "Убрали из «Мне нравится»" : "Добавили в «Мне нравится»");
        });
    }

    private void openLyrics() {
        Models.Track track = Playback.get().current();
        if (track == null) return;
        new LyricsScreen(a, track).show();
    }

    private void download() {
        Models.Track track = Playback.get().current();
        if (track == null) return;
        String existing = Store.downloadPath(track.id);
        if (!existing.isEmpty() && new java.io.File(existing).exists()) {
            Ui.toast(a, Store.isImported(track.id) ? "Этот файл уже на устройстве" : "Уже скачано");
            return;
        }
        if (Store.isImported(track.id)) {
            Ui.toast(a, "Файл недоступен — импортируйте его заново");
            return;
        }
        Ui.toast(a, "Скачиваем…");
        Downloader.download(a, App.api, track, new Downloader.Callback() {
            @Override public void onProgress(int percent) {}
            @Override public void onDone(String path) {
                Ui.toast(a, "Скачано: " + track.title);
                update();
            }
            @Override public void onError(String message) {
                Ui.toast(a, "Ошибка: " + message);
            }
        });
    }

    private void share() {
        Models.Track track = Playback.get().current();
        if (track == null) return;
        a.share(track.title + " — " + track.artistLine() + "\n" + track.shareUrl());
    }

    public void show() {
        if (Playback.get().current() == null) return;
        shown = true;
        FrameLayout host = a.overlay();
        host.removeAllViews();
        host.addView(root, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        host.setVisibility(View.VISIBLE);
        host.setAlpha(0f);
        host.setTranslationY(Ui.dp(a, 36));
        host.animate().alpha(1f).translationY(0).setDuration(Store.reduceMotion() ? 0 : 220).start();
        update();
    }

    public void hide() {
        shown = false;
        FrameLayout host = a.overlay();
        host.animate().alpha(0f).translationY(Ui.dp(a, 30))
                .setDuration(Store.reduceMotion() ? 0 : 180)
                .withEndAction(() -> {
                    host.removeAllViews();
                    host.setVisibility(View.GONE);
                }).start();
    }

    public boolean isShown() {
        return shown;
    }

    public void update() {
        Playback p = Playback.get();
        Models.Track track = p.current();
        if (track == null) {
            if (shown) hide();
            return;
        }
        title.setText(track.title);
        artist.setText(track.artistLine());
        album.setText(track.albumTitle == null ? "" : track.albumTitle);
        album.setVisibility(track.albumTitle == null || track.albumTitle.isEmpty() ? View.GONE : View.VISIBLE);
        ImageLoader.load(track.coverLarge.isEmpty() ? track.coverUrl : track.coverLarge, cover,
                Ui.dp(a, 380));
        play.setImageResource(p.playing ? R.drawable.ic_pause : R.drawable.ic_play);
        like.setImageResource(Store.isLiked(track.id) ? R.drawable.ic_heart_fill : R.drawable.ic_heart);
        like.setColorFilter(Store.isLiked(track.id) ? Theme.accent() : Theme.dim());
        shuffle.setColorFilter(p.shuffle ? Theme.accent() : Theme.faint());
        repeat.setImageResource(p.repeat == 2 ? R.drawable.ic_repeat_one : R.drawable.ic_repeat);
        repeat.setColorFilter(p.repeat == 0 ? Theme.faint() : Theme.accent());
        String localPath = Store.downloadPath(track.id);
        download.setColorFilter(!localPath.isEmpty() && new java.io.File(localPath).exists()
                ? Theme.accent() : Theme.dim());
        source.setText(p.wave ? "Моя волна" : p.queue.size() + " " + Ui.plural(p.queue.size(),
                "трек", "трека", "треков") + " в очереди");
        if (p.error != null && !p.error.isEmpty()) {
            error.setText(p.error);
            error.setVisibility(View.VISIBLE);
        } else {
            error.setVisibility(View.GONE);
        }
        total.setText(Ui.duration(p.duration > 0 ? p.duration : track.durationMs));
        updateProgress();
    }

    public void updateProgress() {
        Playback p = Playback.get();
        if (p.duration <= 0 || seeking) return;
        seek.setProgress((int) (1000L * p.position / Math.max(1, p.duration)));
        elapsed.setText(Ui.duration(p.position));
        total.setText(Ui.duration(p.duration));
    }
}
