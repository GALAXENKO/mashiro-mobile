package com.mashiro.mobile.audio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;

import com.mashiro.mobile.ImageLoader;
import com.mashiro.mobile.MainActivity;
import com.mashiro.mobile.R;
import com.mashiro.mobile.Theme;
import com.mashiro.mobile.Ui;
import com.mashiro.mobile.api.Models;

/** Keeps playback alive with a media notification and lock-screen controls. */
public class PlayerService extends Service implements Playback.Listener {
    public static final String ACTION_START = "com.mashiro.mobile.START";
    public static final String ACTION_TOGGLE = "com.mashiro.mobile.TOGGLE";
    public static final String ACTION_NEXT = "com.mashiro.mobile.NEXT";
    public static final String ACTION_PREV = "com.mashiro.mobile.PREV";
    public static final String ACTION_STOP = "com.mashiro.mobile.STOP";

    private static final String CHANNEL = "playback";
    private static final int NOTIFICATION_ID = 0x4d41;
    private static volatile PlayerService instance;

    private MediaSession session;
    private Playback playback;
    private String artworkKey = "";
    private Bitmap artwork;

    public static void start(Context context) {
        Intent intent = new Intent(context, PlayerService.class).setAction(ACTION_START);
        try {
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
            else context.startService(intent);
        } catch (Exception ignored) {}
    }

    public static void refresh(Context context) {
        if (instance != null) instance.refreshInternal();
    }

    public static boolean isRunning() {
        return instance != null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        playback = Playback.get();
        createChannel();
        session = new MediaSession(this, "Mashiro");
        session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { if (playback != null) playback.play(); }
            @Override public void onPause() { if (playback != null) playback.pause(); }
            @Override public void onSkipToNext() { if (playback != null) playback.next(true); }
            @Override public void onSkipToPrevious() { if (playback != null) playback.prev(); }
            @Override public void onSeekTo(long pos) { if (playback != null) playback.seekTo(pos); }
            @Override public void onStop() { if (playback != null) playback.stop(); }
        });
        session.setActive(true);
        if (playback != null) playback.addListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_TOGGLE.equals(action)) {
            if (playback != null) playback.toggle();
        } else if (ACTION_NEXT.equals(action)) {
            if (playback != null) playback.next(true);
        } else if (ACTION_PREV.equals(action)) {
            if (playback != null) playback.prev();
        } else if (ACTION_STOP.equals(action)) {
            if (playback != null) playback.stop();
            stopForegroundCompat();
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            startForeground(NOTIFICATION_ID, buildNotification());
        } catch (Exception ignored) {}
        refreshInternal();
        return START_STICKY;
    }

    private void refreshInternal() {
        if (playback == null) return;
        try {
            startForeground(NOTIFICATION_ID, buildNotification());
        } catch (Exception ignored) {}
        updateSession();
    }

    @Override
    public void onPlaybackState() {
        refreshInternal();
    }

    @Override
    public void onTrackChanged() {
        artworkKey = "";
        artwork = null;
        refreshInternal();
    }

    @Override
    public void onProgress() {
        updateSession();
    }

    @Override
    public void onPlaybackError(String message) {
        refreshInternal();
    }

    private void updateSession() {
        if (session == null || playback == null) return;
        Models.Track track = playback.current();
        long position = playback.position;
        float speed = playback.playing ? 1f : 0f;
        int state = playback.preparing
                ? PlaybackState.STATE_BUFFERING
                : playback.playing ? PlaybackState.STATE_PLAYING
                : playback.prepared ? PlaybackState.STATE_PAUSED : PlaybackState.STATE_NONE;
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE
                | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT
                | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SEEK_TO
                | PlaybackState.ACTION_STOP;
        PlaybackState.Builder builder = new PlaybackState.Builder()
                .setActions(actions)
                .setState(state, position, speed);
        session.setPlaybackState(builder.build());
        if (track == null) return;
        MediaMetadata.Builder meta = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artistLine())
                .putString(MediaMetadata.METADATA_KEY_ALBUM, track.albumTitle)
                .putLong(MediaMetadata.METADATA_KEY_DURATION,
                        playback.duration > 0 ? playback.duration : track.durationMs);
        if (artwork != null) meta.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork);
        session.setMetadata(meta.build());
        loadArtwork(track);
    }

    private void loadArtwork(Models.Track track) {
        if (track == null || track.coverUrl.isEmpty()) return;
        if (track.coverUrl.equals(artworkKey)) return;
        artworkKey = track.coverUrl;
        ImageLoader.bitmap(track.coverUrl, bitmap -> {
            if (bitmap == null) return;
            if (!track.coverUrl.equals(artworkKey)) return;
            artwork = bitmap;
            refreshInternal();
        });
    }

    private Notification buildNotification() {
        Models.Track track = playback == null ? null : playback.current();
        String title = track == null ? "Mashiro" : track.title;
        String text = track == null ? "Яндекс Музыка" : track.artistLine();
        Intent open = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent content = PendingIntent.getActivity(this, 1, open, pendingFlags());

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            b = new Notification.Builder(this, CHANNEL);
        } else {
            b = new Notification.Builder(this);
        }
        b.setSmallIcon(R.drawable.ic_note)
                .setContentTitle(title)
                .setContentText(text)
                .setSubText(playback != null && playback.wave ? "Моя волна" : "Mashiro")
                .setContentIntent(content)
                .setOngoing(playback != null && playback.playing)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        if (artwork != null) b.setLargeIcon(artwork);
        b.addAction(new Notification.Action.Builder(
                android.R.drawable.ic_media_previous, "Назад", action(ACTION_PREV, 11)).build());
        if (playback != null && playback.playing) {
            b.addAction(new Notification.Action.Builder(
                    android.R.drawable.ic_media_pause, "Пауза", action(ACTION_TOGGLE, 12)).build());
        } else {
            b.addAction(new Notification.Action.Builder(
                    android.R.drawable.ic_media_play, "Играть", action(ACTION_TOGGLE, 12)).build());
        }
        b.addAction(new Notification.Action.Builder(
                android.R.drawable.ic_media_next, "Далее", action(ACTION_NEXT, 13)).build());
        b.addAction(new Notification.Action.Builder(
                android.R.drawable.ic_menu_close_clear_cancel, "Стоп", action(ACTION_STOP, 14)).build());
        if (Build.VERSION.SDK_INT >= 21 && session != null) {
            Notification.MediaStyle style = new Notification.MediaStyle()
                    .setMediaSession(session.getSessionToken())
                    .setShowActionsInCompactView(0, 1, 2);
            b.setStyle(style);
        }
        if (Build.VERSION.SDK_INT >= 26) b.setColorized(false);
        return b.build();
    }

    private PendingIntent action(String action, int code) {
        Intent intent = new Intent(this, PlayerService.class).setAction(action);
        return PendingIntent.getService(this, code, intent, pendingFlags());
    }

    private int pendingFlags() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return flags;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL, "Воспроизведение",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Управление плеером Mashiro");
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(channel);
    }

    private void stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE);
        else stopForeground(true);
    }

    @Override
    public void onDestroy() {
        if (playback != null) playback.removeListener(this);
        if (session != null) {
            session.setActive(false);
            session.release();
        }
        instance = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
