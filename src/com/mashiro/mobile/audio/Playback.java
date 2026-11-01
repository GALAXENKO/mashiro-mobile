package com.mashiro.mobile.audio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import com.mashiro.mobile.Store;
import com.mashiro.mobile.api.Models;
import com.mashiro.mobile.api.YandexApi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Single playback engine shared by the UI and the foreground service. */
public final class Playback {
    public interface Listener {
        void onPlaybackState();
        void onTrackChanged();
        void onProgress();
        default void onPlaybackError(String message) {}
    }

    private static Playback instance;

    public final List<Models.Track> queue = new ArrayList<>();
    public int index = -1;
    public int repeat = 0; // 0 off, 1 all, 2 one
    public boolean shuffle = false;
    public boolean wave = false;
    public String station = "user:onyourwave";
    public String waveSession = "";
    public String waveBatch = "";

    public volatile boolean playing = false;
    public volatile boolean preparing = false;
    public volatile boolean prepared = false;
    public volatile long position = 0;
    public volatile long duration = 0;
    public volatile int bufferPercent = 0;
    public volatile String error = "";

    private final Context context;
    private final YandexApi api;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService net = Executors.newFixedThreadPool(2);
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final List<Integer> shuffleHistory = new ArrayList<>();
    private MediaPlayer player;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private boolean pausedByFocus = false;
    private int generation = 0;
    private long pendingStart = 0;
    private boolean pendingAutoplay = true;
    private boolean counted = false;
    private boolean waveFetching = false;
    private boolean pendingAdvance = false;
    private long sleepAt = 0;
    private long lastSave = 0;
    private int playReportedPercent = -1;

    private final BroadcastReceiver noisy = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) pause();
        }
    };

    private final AudioManager.OnAudioFocusChangeListener focusListener = change -> {
        if (change == AudioManager.AUDIOFOCUS_LOSS) {
            pausedByFocus = true;
            pauseInternal();
        } else if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            pausedByFocus = true;
            pauseInternal();
        } else if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            if (player != null) player.setVolume(0.25f, 0.25f);
        } else if (change == AudioManager.AUDIOFOCUS_GAIN) {
            if (player != null) player.setVolume(1f, 1f);
            if (pausedByFocus) {
                pausedByFocus = false;
                startPlaying();
            }
        }
    };

    private Playback(Context context, YandexApi api) {
        this.context = context.getApplicationContext();
        this.api = api;
        this.audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        createPlayer();
        restore();
        try {
            this.context.registerReceiver(noisy,
                    new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        } catch (Exception ignored) {}
        handler.post(ticker);
    }

    public static synchronized void init(Context context, YandexApi api) {
        if (instance == null) instance = new Playback(context, api);
    }

    public static Playback get() {
        return instance;
    }

    private void createPlayer() {
        player = new MediaPlayer();
        if (Build.VERSION.SDK_INT >= 21) {
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
        } else {
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }
        try {
            player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        } catch (Exception ignored) {}
        player.setOnPreparedListener(mp -> {
            prepared = true;
            preparing = false;
            bufferPercent = 0;
            duration = mp.getDuration();
            if (pendingStart > 0) {
                mp.seekTo((int) Math.min(pendingStart, Math.max(0, duration - 1000)));
                position = pendingStart;
            }
            notifyState();
            notifyTrack();
            if (pendingAutoplay) {
                startPlaying();
            }
        });
        player.setOnCompletionListener(mp -> {
            if (counted) reportWaveFinished();
            counted = false;
            if (repeat == 2) {
                position = 0;
                mp.seekTo(0);
                startPlaying();
            } else {
                next(false);
            }
        });
        player.setOnErrorListener((mp, what, extra) -> {
            preparing = false;
            prepared = false;
            playing = false;
            error = "Не удалось воспроизвести трек";
            notifyState();
            notifyError(error);
            return true;
        });
        player.setOnBufferingUpdateListener((mp, percent) -> {
            bufferPercent = percent;
            if (percent != playReportedPercent) {
                playReportedPercent = percent;
                notifyState();
            }
        });
    }

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (player != null && prepared) {
                try {
                    position = player.getCurrentPosition();
                    duration = player.getDuration();
                } catch (Exception ignored) {}
                if (playing) notifyProgress();
            }
            if (playing && !preparing) {
                long now = System.currentTimeMillis();
                if (now - lastSave > 5000) {
                    lastSave = now;
                    save();
                }
            }
            if (sleepAt > 0 && System.currentTimeMillis() >= sleepAt) {
                sleepAt = 0;
                pause();
            }
            if (wave && playing && !waveFetching && queue.size() > 0 && index >= queue.size() - 2) {
                extendWave(false);
            }
            handler.postDelayed(this, 500);
        }
    };

    // ------------------------------------------------------------- listeners

    public void addListener(Listener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void notifyState() {
        for (Listener l : listeners) l.onPlaybackState();
    }

    private void notifyTrack() {
        for (Listener l : listeners) l.onTrackChanged();
    }

    private void notifyProgress() {
        for (Listener l : listeners) l.onProgress();
    }

    private void notifyError(String message) {
        for (Listener l : listeners) l.onPlaybackError(message);
    }

    // -------------------------------------------------------------- accessors

    public Models.Track current() {
        if (index < 0 || index >= queue.size()) return null;
        return queue.get(index);
    }

    public List<Models.Track> queue() {
        return queue;
    }

    public YandexApi api() {
        return api;
    }

    public MediaPlayer rawPlayer() {
        return player;
    }

    // ------------------------------------------------------------- queue api

    public void startQueue(List<Models.Track> tracks, int start, boolean isWave,
                           String waveStation, String session, String batch) {
        if (tracks == null || tracks.isEmpty()) return;
        queue.clear();
        queue.addAll(tracks);
        index = Math.max(0, Math.min(start, queue.size() - 1));
        wave = isWave;
        if (isWave) {
            station = waveStation == null || waveStation.isEmpty() ? "user:onyourwave" : waveStation;
            waveSession = session == null ? "" : session;
            waveBatch = batch == null ? "" : batch;
        } else {
            station = "";
            waveSession = "";
            waveBatch = "";
        }
        shuffleHistory.clear();
        error = "";
        position = 0;
        loadCurrent(0, true);
    }

    public void playAt(int position) {
        if (position < 0 || position >= queue.size()) return;
        index = position;
        loadCurrent(0, true);
    }

    public void playTrackNow(Models.Track track) {
        int found = find(track.id);
        if (found >= 0) {
            playAt(found);
            return;
        }
        queue.add(index + 1, track);
        index = index + 1;
        loadCurrent(0, true);
    }

    public void playNext(Models.Track track) {
        if (track == null) return;
        int at = Math.max(0, Math.min(index + 1, queue.size()));
        queue.add(at, track);
        notifyState();
        save();
    }

    public void enqueue(Models.Track track) {
        if (track == null) return;
        int existing = find(track.id);
        if (existing >= 0) {
            queue.remove(existing);
            if (existing < index) index--;
        }
        queue.add(track);
        notifyState();
        save();
    }

    public void addAll(List<Models.Track> tracks) {
        if (tracks == null) return;
        for (Models.Track t : tracks) {
            if (find(t.id) < 0) queue.add(t);
        }
        notifyState();
        save();
    }

    public void removeAt(int at) {
        if (at < 0 || at >= queue.size()) return;
        queue.remove(at);
        if (at < index) index--;
        if (index >= queue.size()) index = queue.size() - 1;
        if (index < 0) {
            stop();
            return;
        }
        notifyState();
        save();
    }

    public int find(String id) {
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).id.equals(id)) return i;
        }
        return -1;
    }

    // ------------------------------------------------------------- transport

    public void toggle() {
        if (preparing) return;
        if (!prepared) {
            if (current() == null) return;
            loadCurrent(position, true);
            return;
        }
        if (playing) pause();
        else startPlaying();
    }

    public void play() {
        if (current() == null && !queue.isEmpty()) index = 0;
        if (current() == null) return;
        if (!prepared) loadCurrent(position, true);
        else startPlaying();
    }

    private void startPlaying() {
        if (!prepared || player == null) return;
        if (!requestFocus()) return;
        try {
            player.start();
            playing = true;
            error = "";
            PlayerService.start(context);
            if (!counted) {
                counted = true;
                Store.countPlay(current());
                Store.addRecent(current());
            }
            notifyState();
            notifyTrack();
            save();
        } catch (Exception e) {
            error = "Ошибка воспроизведения";
            notifyError(error);
        }
    }

    public void pause() {
        pauseInternal();
        abandonFocus();
        save();
    }

    private void pauseInternal() {
        if (player != null && prepared) {
            try {
                player.pause();
            } catch (Exception ignored) {}
        }
        playing = false;
        notifyState();
    }

    public void stop() {
        generation++;
        if (player != null) {
            try {
                player.reset();
            } catch (Exception ignored) {}
        }
        prepared = false;
        preparing = false;
        playing = false;
        position = 0;
        duration = current() == null ? 0 : current().durationMs;
        notifyState();
        save();
        PlayerService.refresh(context);
    }

    public void next(boolean user) {
        if (queue.isEmpty()) return;
        if (user && wave) {
            api.waveFeedback(station, "skip", current() == null ? null : current().id, waveBatch,
                    position / 1000.0);
        }
        if (shuffle && queue.size() > 1) {
            int nextIndex = randomOther(index);
            if (nextIndex >= 0) {
                index = nextIndex;
                loadCurrent(0, true);
                return;
            }
        }
        int target = index + 1;
        if (target >= queue.size()) {
            if (wave) {
                pendingAdvance = true;
                extendWave(true);
                return;
            }
            if (repeat == 1) target = 0;
            else {
                pauseInternal();
                seekTo(0);
                return;
            }
        }
        index = target;
        loadCurrent(0, true);
    }

    public void prev() {
        if (queue.isEmpty()) return;
        if (position > 3000 && prepared) {
            seekTo(0);
            return;
        }
        int target = index - 1;
        if (shuffle && shuffleHistory.size() > 0) {
            target = shuffleHistory.remove(shuffleHistory.size() - 1);
        } else if (target < 0) {
            if (repeat == 1) target = queue.size() - 1;
            else {
                seekTo(0);
                return;
            }
        }
        index = Math.max(0, Math.min(target, queue.size() - 1));
        loadCurrent(0, true);
    }

    private int randomOther(int avoid) {
        if (queue.size() <= 1) return -1;
        int value = avoid;
        for (int i = 0; i < 8 && value == avoid; i++) {
            value = (int) (Math.random() * queue.size());
        }
        if (value == avoid) value = (avoid + 1) % queue.size();
        shuffleHistory.add(avoid);
        if (shuffleHistory.size() > 100) shuffleHistory.remove(0);
        return value;
    }

    public void seekTo(long ms) {
        long target = Math.max(0, Math.min(ms, duration > 0 ? duration : ms));
        position = target;
        if (player != null && prepared) {
            try {
                player.seekTo((int) target);
            } catch (Exception ignored) {}
        }
        notifyProgress();
        PlayerService.refresh(context);
    }

    public void setRepeat(int value) {
        repeat = value % 3;
        notifyState();
        save();
    }

    public void toggleShuffle() {
        shuffle = !shuffle;
        shuffleHistory.clear();
        notifyState();
        save();
    }

    public void setSleepMinutes(int minutes) {
        sleepAt = minutes <= 0 ? 0 : System.currentTimeMillis() + minutes * 60_000L;
        notifyState();
    }

    public long sleepRemainingMs() {
        return sleepAt <= 0 ? 0 : Math.max(0, sleepAt - System.currentTimeMillis());
    }

    // --------------------------------------------------------------- loading

    private void loadCurrent(long startAt, boolean autoplay) {
        Models.Track track = current();
        if (track == null) return;
        generation++;
        final int gen = generation;
        pendingStart = startAt;
        pendingAutoplay = autoplay;
        prepared = false;
        preparing = true;
        playing = false;
        counted = false;
        position = startAt;
        duration = track.durationMs;
        bufferPercent = 0;
        error = "";
        notifyState();
        notifyTrack();
        PlayerService.start(context);
        save();

        final String local = Store.downloadPath(track.id);
        if (!local.isEmpty() && new java.io.File(local).exists()) {
            mainLoad(gen, local, true);
            return;
        }
        net.execute(() -> {
            try {
                String url = api.streamUrl(track.id, Store.quality());
                mainLoad(gen, url, false);
            } catch (Exception e) {
                int code = e instanceof YandexApi.ApiException ? ((YandexApi.ApiException) e).code : 0;
                final String message = code == 403
                        ? "Трек доступен только с Яндекс Плюс"
                        : e.getMessage() == null ? "Не удалось получить поток" : e.getMessage();
                handler.post(() -> {
                    if (gen != generation) return;
                    preparing = false;
                    playing = false;
                    prepared = false;
                    error = message;
                    notifyState();
                    notifyError(message);
                });
            }
        });
    }

    private void mainLoad(int gen, String source, boolean local) {
        handler.post(() -> {
            if (gen != generation) return;
            try {
                if (player == null) createPlayer();
                player.reset();
                if (local) player.setDataSource(source);
                else player.setDataSource(source);
                player.prepareAsync();
            } catch (Exception e) {
                preparing = false;
                error = "Ошибка подготовки трека";
                notifyState();
                notifyError(error);
            }
        });
    }

    // ------------------------------------------------------------------ wave

    private void extendWave(boolean advance) {
        if (waveFetching || !wave) return;
        waveFetching = true;
        final List<String> ids = new ArrayList<>();
        for (Models.Track t : queue) ids.add(t.id);
        final String session = waveSession;
        final String stationId = station;
        net.execute(() -> {
            try {
                Models.Wave result = api.wave(stationId, session, ids);
                handler.post(() -> {
                    waveFetching = false;
                    if (!result.sessionId.isEmpty()) waveSession = result.sessionId;
                    if (!result.batchId.isEmpty()) waveBatch = result.batchId;
                    int added = 0;
                    for (Models.Track t : result.tracks) {
                        if (find(t.id) < 0) {
                            queue.add(t);
                            added++;
                        }
                    }
                    notifyState();
                    save();
                    if (advance && pendingAdvance) {
                        pendingAdvance = false;
                        if (added > 0 && index + 1 < queue.size()) {
                            index++;
                            loadCurrent(0, true);
                        } else {
                            pauseInternal();
                        }
                    }
                });
            } catch (Exception e) {
                handler.post(() -> {
                    waveFetching = false;
                    pendingAdvance = false;
                    notifyError("Волна не отвечает: " + e.getMessage());
                });
            }
        });
    }

    private void reportWaveFinished() {
        if (!wave) return;
        Models.Track t = current();
        if (t == null) return;
        api.waveFeedback(station, "trackFinished", t.id, waveBatch, duration / 1000.0);
    }

    // ------------------------------------------------------------- persistence

    private void save() {
        try {
            Store.saveQueue(queue, index, position, repeat, shuffle, wave, station, waveSession, waveBatch);
        } catch (Exception ignored) {}
    }

    private void restore() {
        try {
            JSONObject o = Store.queue();
            if (o.length() == 0) return;
            JSONArray arr = o.optJSONArray("queue");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    Models.Track t = Models.Track.fromJson(arr.optJSONObject(i));
                    if (!t.id.isEmpty()) queue.add(t);
                }
            }
            index = o.optInt("index", -1);
            if (index >= queue.size()) index = queue.size() - 1;
            position = o.optLong("pos", 0);
            repeat = o.optInt("repeat", 0);
            shuffle = o.optBoolean("shuffle", false);
            wave = o.optBoolean("wave", false);
            station = o.optString("station", "user:onyourwave");
            waveSession = o.optString("waveSession", "");
            waveBatch = o.optString("waveBatch", "");
            Models.Track track = current();
            duration = track == null ? 0 : track.durationMs;
        } catch (Exception ignored) {}
    }

    // ---------------------------------------------------------------- focus

    private boolean requestFocus() {
        if (audioManager == null) return true;
        int result;
        if (Build.VERSION.SDK_INT >= 26) {
            if (focusRequest == null) {
                focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build())
                        .setOnAudioFocusChangeListener(focusListener)
                        .setWillPauseWhenDucked(false)
                        .build();
            }
            result = audioManager.requestAudioFocus(focusRequest);
        } else {
            result = audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void abandonFocus() {
        if (audioManager == null) return;
        if (Build.VERSION.SDK_INT >= 26 && focusRequest != null) {
            audioManager.abandonAudioFocusRequest(focusRequest);
        } else {
            audioManager.abandonAudioFocus(focusListener);
        }
    }

    public boolean isSleepActive() {
        return sleepAt > 0;
    }
}
