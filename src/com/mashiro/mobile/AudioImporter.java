package com.mashiro.mobile;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import com.mashiro.mobile.api.Models;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Lets the user import their own audio files from the device into Mashiro. */
public final class AudioImporter {
    /** Activity result request code for the file picker (ACTION_OPEN_DOCUMENT). */
    public static final int REQ_PICK = 501;
    /** Permission request code used when scanning the device for audio. */
    public static final int REQ_PERM = 552;

    private static final String PERM_READ = Build.VERSION.SDK_INT >= 33
            ? "android.permission.READ_MEDIA_AUDIO"
            : "android.permission.READ_EXTERNAL_STORAGE";

    private static Runnable pending;

    private AudioImporter() {}

    /** Opens the system file picker for audio files (multiple selection). */
    public static void pick(MainActivity a, Runnable onDone) {
        pending = onDone;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        try {
            a.startActivityForResult(intent, REQ_PICK);
        } catch (Exception e) {
            pending = null;
            Ui.toast(a, "Не удалось открыть выбор файлов");
        }
    }

    /** Scans the device's audio library and imports everything found. */
    public static void scanDevice(MainActivity a, Runnable onDone) {
        pending = onDone;
        if (a.checkSelfPermission(PERM_READ) == PackageManager.PERMISSION_GRANTED) {
            runScan(a);
        } else {
            a.requestPermissions(new String[]{PERM_READ}, REQ_PERM);
        }
    }

    /** Called from MainActivity.onRequestPermissionsResult for our scan permission. */
    public static void handlePermissions(MainActivity a, int[] grantResults) {
        final Runnable done = pending;
        pending = null;
        if (grantResults != null && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            runScan(a);
        } else {
            Ui.toast(a, "Нет доступа к файлам");
            if (done != null) done.run();
        }
    }

    /** Called from MainActivity.onActivityResult for the file picker. */
    public static void handleResult(MainActivity a, int requestCode, int resultCode, Intent data) {
        final Runnable done = pending;
        pending = null;
        if (requestCode != REQ_PICK || resultCode != Activity.RESULT_OK || data == null) {
            if (done != null) done.run();
            return;
        }
        final List<Uri> uris = new ArrayList<>();
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                uris.add(data.getClipData().getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }
        a.bg(() -> {
            int count = 0;
            for (Uri uri : uris) {
                if (copyAndImport(a, uri)) count++;
            }
            return count;
        }, count -> {
            Ui.toast(a, "Импортировано: " + count);
            if (done != null) done.run();
        });
    }

    private static void runScan(MainActivity a) {
        final Runnable done = pending;
        pending = null;
        a.bg(() -> queryAndImport(a), count -> {
            Ui.toast(a, "Найдено файлов: " + count);
            if (done != null) done.run();
        });
    }

    /** Copies a picked (content://) file into private storage and imports it. */
    private static boolean copyAndImport(MainActivity a, Uri uri) {
        try {
            File dir = new File(a.getFilesDir(), "imported");
            if (!dir.exists()) dir.mkdirs();
            String name = displayName(a, uri);
            String base = stripExt(name);
            if (base.isEmpty()) base = "track";
            File out = new File(dir, sanitize(base) + "_" + (System.currentTimeMillis() % 100000) + extFor(a, uri));
            try (InputStream is = a.getContentResolver().openInputStream(uri);
                 FileOutputStream fos = new FileOutputStream(out)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = is.read(buf)) != -1) fos.write(buf, 0, r);
            }
            if (out.length() == 0) {
                out.delete();
                return false;
            }
            importFromPath(a, out.getAbsolutePath(), base);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static int queryAndImport(MainActivity a) {
        int count = 0;
        Cursor c = null;
        try {
            c = a.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DISPLAY_NAME},
                    null, null, null);
            if (c != null) {
                int dataIdx = c.getColumnIndex(MediaStore.Audio.Media.DATA);
                int nameIdx = c.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
                while (c.moveToNext()) {
                    String path = dataIdx >= 0 ? c.getString(dataIdx) : null;
                    if (path == null || path.isEmpty()) continue;
                    if (path.contains("com.mashiro.mobile")) continue;
                    String name = nameIdx >= 0 ? c.getString(nameIdx) : null;
                    if (name == null) name = new File(path).getName();
                    importFromPath(a, path, stripExt(name));
                    count++;
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return count;
    }

    private static void importFromPath(MainActivity a, String realPath, String fallbackTitle) {
        Models.Track t = new Models.Track();
        t.id = Store.localId(realPath);
        if (Store.isImported(t.id) && !Store.downloadPath(t.id).isEmpty()) {
            // already imported from this path — refresh metadata below
        }
        String title = "";
        String artist = "";
        long durMs = 0;
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(realPath);
            title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String d = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (d != null) {
                try {
                    durMs = Long.parseLong(d);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {
        } finally {
            try {
                mmr.release();
            } catch (Exception ignored) {}
        }
        t.title = (title == null || title.isEmpty()) ? fallbackTitle : title;
        if (artist != null && !artist.isEmpty()) {
            Models.Artist ar = new Models.Artist();
            ar.name = artist;
            t.artists.add(ar);
        }
        t.durationMs = durMs;
        Store.saveImportedTrack(t, realPath);
    }

    private static String displayName(MainActivity a, Uri uri) {
        String name = "";
        Cursor c = null;
        try {
            c = a.getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = c.getString(idx);
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return name == null ? "" : name;
    }

    private static String extFor(MainActivity a, Uri uri) {
        String mime = a.getContentResolver().getType(uri);
        if (mime == null) mime = "";
        switch (mime) {
            case "audio/mpeg": return ".mp3";
            case "audio/mp4": case "audio/x-m4a": return ".m4a";
            case "audio/ogg": case "application/ogg": return ".ogg";
            case "audio/flac": case "audio/x-flac": return ".flac";
            case "audio/wav": case "audio/x-wav": case "audio/wave": return ".wav";
            case "audio/aac": case "audio/x-aac": return ".aac";
            case "audio/opus": case "audio/ogg; codecs=opus": return ".opus";
            default: return ".mp3";
        }
    }

    private static String stripExt(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String sanitize(String value) {
        String out = value.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", " ").trim();
        if (out.length() > 80) out = out.substring(0, 80).trim();
        return out.isEmpty() ? "track" : out;
    }
}
