package com.mashiro.mobile;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;

import com.mashiro.mobile.api.Models;
import com.mashiro.mobile.api.YandexApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Downloads tracks to the phone. On Android 10+ files go to the public Music collection. */
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
            File cache = null;
            try {
                String url = api.streamUrl(track.id, Store.quality());
                String name = safe(track.artistLine() + " - " + track.title) + ".mp3";

                cache = File.createTempFile("download", ".part", app.getCacheDir());
                FileOutputStream out = new FileOutputStream(cache);
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setRequestProperty("X-Yandex-Music-Client", YandexApi.MUSIC_CLIENT);
                connection.setRequestProperty("User-Agent", "Mashiro/1.1 (Android)");
                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) throw new Exception("CDN ответил " + code);
                long total = connection.getContentLengthLong();
                InputStream in = connection.getInputStream();
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

                String finalPath;
                if (Build.VERSION.SDK_INT >= 29) {
                    finalPath = publishMedia(app, cache, name);
                } else {
                    finalPath = saveToAppDir(app, cache, name);
                }
                if (finalPath == null || finalPath.isEmpty()) throw new Exception("Не удалось сохранить файл");
                cache.delete();
                cache = null;

                Store.saveDownloadedTrack(track, finalPath);
                final String path = finalPath;
                main.post(() -> callback.onDone(path));
            } catch (Exception e) {
                final String message = e.getMessage() == null ? "Ошибка загрузки" : e.getMessage();
                main.post(() -> callback.onError(message));
            } finally {
                active.remove(track.id);
                if (connection != null) connection.disconnect();
                if (cache != null && cache.exists()) cache.delete();
            }
        });
    }

    /** Writes the finished download into the public Music collection via MediaStore. */
    private static String publishMedia(Context context, File cache, String name) throws Exception {
        ContentResolver cr = context.getContentResolver();
        String folder = Store.downloadFolder();
        if (folder == null || folder.isEmpty()) folder = "Mashiro";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, name);
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg");
        values.put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/" + folder + "/");
        values.put(MediaStore.Audio.Media.IS_PENDING, 1);
        Uri uri = cr.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new Exception("Не удалось создать файл");

        try (OutputStream os = cr.openOutputStream(uri);
             FileInputStream fis = new FileInputStream(cache)) {
            copy(fis, os);
        }

        values.clear();
        values.put(MediaStore.Audio.Media.IS_PENDING, 0);
        cr.update(uri, values, null, null);

        String data = "";
        try (Cursor c = cr.query(uri, new String[]{MediaStore.Audio.Media.DATA}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(MediaStore.Audio.Media.DATA);
                if (idx >= 0) data = c.getString(idx);
            }
        }
        if (data != null && !data.isEmpty()) return data;

        // MediaStore file can't be referenced by path — keep a private copy.
        return saveToAppDir(context, cache, name);
    }

    /** App-private music folder (only used below Android 10, or as a fallback). */
    private static String saveToAppDir(Context context, File cache, String name) throws Exception {
        File folder = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (folder == null) folder = new File(context.getFilesDir(), "Music");
        if (!folder.exists()) folder.mkdirs();
        String base = name;
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);
        File target = new File(folder, name);
        int i = 1;
        while (target.exists()) {
            target = new File(folder, base + " (" + i + ").mp3");
            i++;
        }
        try (FileInputStream fis = new FileInputStream(cache);
             FileOutputStream fos = new FileOutputStream(target)) {
            copy(fis, fos);
        }
        return target.getAbsolutePath();
    }

    /** Deletes a downloaded track. Uses MediaStore on Android 10+, falls back to direct delete. */
    public static void deleteFile(Context context, String path) {
        if (path == null || path.isEmpty()) return;
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                ContentResolver cr = context.getContentResolver();
                long id = -1;
                try (Cursor c = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        new String[]{MediaStore.Audio.Media._ID},
                        MediaStore.Audio.Media.DATA + "=?", new String[]{path}, null)) {
                    if (c != null && c.moveToFirst()) id = c.getLong(0);
                }
                if (id >= 0) {
                    Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                    if (cr.delete(uri, null, null) > 0) return;
                }
            }
        } catch (Exception ignored) {}
        try {
            new File(path).delete();
        } catch (Exception ignored) {}
    }

    private static void copy(InputStream in, OutputStream out) throws Exception {
        byte[] buf = new byte[16384];
        int r;
        while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
    }

    private static String safe(String value) {
        String out = value.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", " ").trim();
        if (out.length() > 90) out = out.substring(0, 90).trim();
        return out.isEmpty() ? "track" : out;
    }
}
