package com.mashiro.mobile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Minimal cover loader with memory + disk cache. */
public final class ImageLoader {
    private static final ExecutorService pool = Executors.newFixedThreadPool(4);
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> memory =
            new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 8)) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return value.getByteCount();
                }
            };
    private static File dir;
    private static volatile boolean ready = false;

    private ImageLoader() {}

    public static void init(Context context) {
        if (ready) return;
        dir = new File(context.getApplicationContext().getCacheDir(), "covers");
        if (!dir.exists()) dir.mkdirs();
        ready = true;
    }

    public interface Callback {
        void onBitmap(Bitmap bitmap);
    }

    public static void load(String url, ImageView view, int targetPx) {
        if (view == null) return;
        if (url == null || url.isEmpty()) {
            view.setImageDrawable(null);
            return;
        }
        view.setTag(url);
        Bitmap cached = memory.get(url);
        if (cached != null && !cached.isRecycled()) {
            view.setImageBitmap(cached);
            return;
        }
        view.setImageDrawable(null);
        pool.execute(() -> {
            Bitmap bitmap = fetch(url, targetPx);
            if (bitmap == null) return;
            memory.put(url, bitmap);
            final Bitmap result = bitmap;
            main.post(() -> {
                if (url.equals(view.getTag())) view.setImageBitmap(result);
            });
        });
    }

    public static void load(String url, ImageView view) {
        load(url, view, Ui.dp(view.getContext(), 240));
    }

    public static void bitmap(String url, Callback callback) {
        if (url == null || url.isEmpty()) {
            callback.onBitmap(null);
            return;
        }
        Bitmap cached = memory.get(url);
        if (cached != null) {
            callback.onBitmap(cached);
            return;
        }
        pool.execute(() -> {
            Bitmap bitmap = fetch(url, 512);
            if (bitmap != null) memory.put(url, bitmap);
            final Bitmap result = bitmap;
            main.post(() -> callback.onBitmap(result));
        });
    }

    private static Bitmap fetch(String url, int targetPx) {
        initIfNeeded();
        File file = new File(dir, key(url) + ".img");
        if (file.exists()) {
            Bitmap cached = decodeFile(file, targetPx);
            if (cached != null) return cached;
        }
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(12000);
            c.setReadTimeout(20000);
            c.setRequestProperty("User-Agent", "Mashiro/1.0 (Android)");
            c.setRequestProperty("Accept", "image/*");
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) return null;
            InputStream in = c.getInputStream();
            Bitmap bitmap = decodeStream(in, targetPx);
            if (bitmap != null) {
                try {
                    FileOutputStream out = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out);
                    out.close();
                } catch (Exception ignored) {}
            }
            return bitmap;
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static void initIfNeeded() {
        if (!ready) {
            // Should only happen before Application.onCreate; use a temp folder.
            dir = new File(System.getProperty("java.io.tmpdir", "."), "mashiro-covers");
            if (!dir.exists()) dir.mkdirs();
            ready = true;
        }
    }

    private static Bitmap decodeFile(File file, int targetPx) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample(bounds.outWidth, bounds.outHeight, targetPx);
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
        } catch (Throwable e) {
            return null;
        }
    }

    private static Bitmap decodeStream(InputStream in, int targetPx) {
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            in.close();
            byte[] bytes = out.toByteArray();
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample(bounds.outWidth, bounds.outHeight, targetPx);
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
        } catch (Throwable e) {
            return null;
        }
    }

    private static int sample(int width, int height, int targetPx) {
        if (targetPx <= 0 || width <= 0 || height <= 0) return 1;
        int sample = 1;
        while (width / (sample * 2) >= targetPx && height / (sample * 2) >= targetPx) sample *= 2;
        return sample;
    }

    public static void clear() {
        memory.evictAll();
        final File folder = dir;
        pool.execute(() -> {
            if (folder == null) return;
            File[] files = folder.listFiles();
            if (files == null) return;
            for (File file : files) file.delete();
        });
    }

    private static String key(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(url.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format(Locale.ROOT, "%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(url.hashCode());
        }
    }

    /** ImageView that clips its content to rounded corners. */
    public static class RoundedImageView extends ImageView {
        private final Path path = new Path();
        private final RectF rect = new RectF();
        private float radiusPx = 0;

        public RoundedImageView(Context context) {
            super(context);
            setScaleType(ScaleType.CENTER_CROP);
        }

        public void setRadiusDp(float dp) {
            radiusPx = Ui.dp(getContext(), dp);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (radiusPx <= 0) {
                super.onDraw(canvas);
                return;
            }
            rect.set(0, 0, getWidth(), getHeight());
            path.reset();
            path.addRoundRect(rect, radiusPx, radiusPx, Path.Direction.CW);
            int save = canvas.save();
            canvas.clipPath(path);
            super.onDraw(canvas);
            canvas.restoreToCount(save);
        }
    }
}
