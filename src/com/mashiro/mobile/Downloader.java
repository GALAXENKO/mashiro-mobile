package com.mashiro.mobile;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.mashiro.mobile.api.Models;
import com.mashiro.mobile.api.YandexApi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Downloads tracks into the app's private music folder; the player prefers local files. */
public final class Downloader {
    public interface Callback {
        void onProgress(int percent);
        void onDone(String path);
        void onError(String message);
    }

    private static final ExecutorService pool = Executors.newSingleThreadExecutor();
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static final Set<String> active = ConcurrentHashMap.newKeySet();

    private Downloader() {}

    public static boolean isActive(String trackId) {
        return active.contains(trackId);
    }

    public static void download(Context context, YandexApi api, Models.Track track, Callback callback) {
        if (track == null || track.id.isEmpty()) {
            callback.onError("Трек недоступен");
            return;
        }
        String existing = Store.downloadPath(track.id);
        if (!existing.isEmpty() && new File(existing).exists()) {
            callback.onDone(existing);
            return;
        }
        if (!active.add(track.id)) {
            callback.onError("Уже скачивается");
            return;
        }
        final Context app = context.getApplicationContext();
        pool.execute(() -> {
            HttpURLConnection connection = null;
            File temp = null;
            try {
                String url = api.streamUrl(track.id, Store.quality());
                File folder = app.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
                if (folder == null) folder = new File(app.getFilesDir(), "Music");
                if (!folder.exists()) folder.mkdirs();
                String name = safe(track.artistLine() + " - " + track.title);
                File target = new File(folder, name + ".mp3");
                if (target.exists()) target = new File(folder, name + " (" + track.id + ").mp3");
                temp = new File(target.getAbsolutePath() + ".part");
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setRequestProperty("X-Yandex-Music-Client", YandexApi.MUSIC_CLIENT);
                connection.setRequestProperty("User-Agent", "Mashiro/1.0 (Android)");
                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) throw new Exception("CDN ответил " + code);
                long total = connection.getContentLengthLong();
                InputStream in = connection.getInputStream();
                FileOutputStream out = new FileOutputStream(temp);
                byte[] buffer = new byte[16384];
                long written = 0;
                int read;
                int lastPercent = -1;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    written += read;
                    if (total > 0) {
                        int percent = (int) (written * 100 / total);
                        if (percent != lastPercent) {
                            lastPercent = percent;
                            final int p = percent;
                            main.post(() -> callback.onProgress(p));
                        }
                    }
                }
                out.close();
                in.close();
                if (!temp.renameTo(target)) throw new Exception("Не удалось сохранить файл");
                Store.saveDownloadedTrack(track, target.getAbsolutePath());
                temp = null;
                final String path = target.getAbsolutePath();
                main.post(() -> callback.onDone(path));
            } catch (Exception e) {
                final String message = e.getMessage() == null ? "Ошибка загрузки" : e.getMessage();
                main.post(() -> callback.onError(message));
            } finally {
                active.remove(track.id);
                if (connection != null) connection.disconnect();
                if (temp != null && temp.exists()) temp.delete();
            }
        });
    }

    private static String safe(String value) {
        String out = value.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", " ").trim();
        if (out.length() > 90) out = out.substring(0, 90).trim();
        return out.isEmpty() ? "track" : out;
    }
}
