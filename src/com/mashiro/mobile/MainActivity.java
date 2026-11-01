package com.mashiro.mobile;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mashiro.mobile.api.Models;
import com.mashiro.mobile.audio.Playback;
import com.mashiro.mobile.audio.PlayerService;
import com.mashiro.mobile.screens.AlbumScreen;
import com.mashiro.mobile.screens.ArtistScreen;
import com.mashiro.mobile.screens.HomeScreen;
import com.mashiro.mobile.screens.LibraryScreen;
import com.mashiro.mobile.screens.LikedScreen;
import com.mashiro.mobile.screens.LoginScreen;
import com.mashiro.mobile.screens.PlaylistScreen;
import com.mashiro.mobile.screens.SearchScreen;
import com.mashiro.mobile.screens.SettingsScreen;
import com.mashiro.mobile.screens.StatsScreen;
import com.mashiro.mobile.screens.WaveScreen;
import com.mashiro.mobile.ui.PlayerScreen;
import com.mashiro.mobile.ui.QueueSheet;
import com.mashiro.mobile.ui.TrackMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {
    public interface Task<T> {
        T run() throws Exception;
    }

    public interface Callback<T> {
        void accept(T value);
    }

    public interface ErrorCallback {
        void accept(String message);
    }

    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicInteger taskIds = new AtomicInteger(0);
    private final List<Screen> stack = new ArrayList<>();
    private final List<RowBinding> rows = new CopyOnWriteArrayList<>();
    private FrameLayout window;
    private FrameLayout content;
    private FrameLayout overlay;
    private LinearLayout mini;
    private LinearLayout nav;
    private ImageView miniCover;
    private TextView miniTitle;
    private TextView miniArtist;
    private ImageView miniPlay;
    private View miniProgress;
    private LinearLayout navItems;
    private Screen current;
    private PlayerScreen player;
    private int tab = 1;
    private final Playback.Listener listener = new Playback.Listener() {
        @Override public void onPlaybackState() {
            updateMini();
            if (player != null) player.update();
            updateRows();
        }
        @Override public void onTrackChanged() {
            updateMini();
            if (player != null) player.update();
            updateRows();
        }
        @Override public void onProgress() {
            if (player != null) player.updateProgress();
            updateMiniProgress();
        }
        @Override public void onPlaybackError(String message) {
            if (message != null && !message.isEmpty()) Ui.toast(MainActivity.this, message);
            if (player != null) player.update();
        }
    };

    private static class RowBinding {
        final String id;
        final View row;
        final TextView title;
        RowBinding(String id, View row, TextView title) {
            this.id = id;
            this.row = row;
            this.title = title;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Theme.apply(this);
        setContentView(buildShell());
        Playback.get().addListener(listener);
        askNotifications();
        if (Store.token().isEmpty() && !Store.guestChosen()) {
            showLogin();
        } else {
            startApp();
        }
    }

    private View buildShell() {
        window = new FrameLayout(this);
        window.setBackgroundColor(Theme.bg());

        LinearLayout shell = Ui.column(this);
        shell.setBackgroundColor(Theme.bg());
        shell.setPadding(0, Ui.statusBar(this), 0, Ui.navBar(this));
        window.addView(shell, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        content = new FrameLayout(this);
        content.setBackgroundColor(Theme.bg());
        shell.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        mini = buildMini();
        shell.addView(mini, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 62)));
        mini.setVisibility(View.GONE);

        nav = buildNav();
        shell.addView(nav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 58)));

        overlay = new FrameLayout(this);
        overlay.setPadding(0, Ui.statusBar(this), 0, Ui.navBar(this));
        overlay.setBackgroundColor(Theme.bg());
        overlay.setVisibility(View.GONE);
        window.addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return window;
    }

    private LinearLayout buildNav() {
        LinearLayout bar = Ui.column(this);
        bar.setBackgroundColor(Theme.surface());
        View top = new View(this);
        top.setBackgroundColor(Theme.border());
        bar.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        navItems = Ui.row(this);
        navItems.setPadding(Ui.dp(this, 6), 0, Ui.dp(this, 6), 0);
        bar.addView(navItems, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        navItems.addView(navItem(1, R.drawable.ic_home, "Главная"));
        navItems.addView(navItem(2, R.drawable.ic_wave, "Волна"));
        navItems.addView(navItem(3, R.drawable.ic_search, "Поиск"));
        navItems.addView(navItem(4, R.drawable.ic_library, "Медиатека"));
        return bar;
    }

    private View navItem(int id, int iconRes, String title) {
        LinearLayout item = Ui.column(this);
        item.setGravity(Gravity.CENTER);
        item.setTag("nav" + id);
        item.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        ImageView icon = Ui.icon(this, iconRes, 22, Theme.dim());
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(Ui.dp(this, 22), Ui.dp(this, 22));
        ilp.gravity = Gravity.CENTER_HORIZONTAL;
        item.addView(icon, ilp);
        TextView label = Ui.text(this, title, 10.5f, Theme.dim(), true);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = Ui.dp(this, 4);
        tlp.gravity = Gravity.CENTER_HORIZONTAL;
        item.addView(label, tlp);
        item.setOnClickListener(v -> setTab(id));
        Ui.ripple(item, 0x12FFFFFF);
        return item;
    }

    private LinearLayout buildMini() {
        LinearLayout bar = Ui.row(this);
        bar.setBackgroundColor(Theme.surface());
        bar.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 8), 0);
        View top = new View(this);
        top.setBackgroundColor(Theme.border());
        // mini uses a wrapper so the hairline can sit above the content
        LinearLayout wrap = Ui.column(this);
        wrap.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        wrap.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        ImageLoader.RoundedImageView cover = new ImageLoader.RoundedImageView(this);
        cover.setRadiusDp(7);
        cover.setBackground(Ui.bg(this, Theme.surface2(), 7));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44));
        clp.rightMargin = Ui.dp(this, 12);
        cover.setOnClickListener(v -> openPlayer());
        bar.addView(cover, clp);
        miniCover = cover;

        LinearLayout copy = Ui.column(this);
        copy.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        copy.setOnClickListener(v -> openPlayer());
        miniTitle = Ui.text(this, "", 14, Theme.text(), true);
        miniTitle.setSingleLine(true);
        miniTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        miniArtist = Ui.text(this, "", 11.5f, Theme.dim(), false);
        miniArtist.setSingleLine(true);
        miniArtist.setEllipsize(android.text.TextUtils.TruncateAt.END);
        copy.addView(miniTitle);
        copy.addView(miniArtist);
        bar.addView(copy);

        LinearLayout controls = Ui.row(this);
        miniPlay = Ui.icon(this, R.drawable.ic_play, 22, Theme.text());
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42));
        plp.gravity = Gravity.CENTER_VERTICAL;
        View playBtn = wrapIcon(miniPlay, plp);
        playBtn.setOnClickListener(v -> Playback.get().toggle());
        LinearLayout nextBtn = Ui.row(this);
        nextBtn.addView(Ui.icon(this, R.drawable.ic_next, 20, Theme.text()));
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42));
        nlp.gravity = Gravity.CENTER_VERTICAL;
        nextBtn.setLayoutParams(nlp);
        nextBtn.setGravity(Gravity.CENTER);
        nextBtn.setOnClickListener(v -> Playback.get().next(true));
        controls.addView(playBtn);
        controls.addView(nextBtn);
        bar.addView(controls);

        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Theme.surface());
        LinearLayout.LayoutParams wrapLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(wrap, wrapLp);
        miniProgress = new View(this);
        miniProgress.setBackgroundColor(Theme.accent());
        root.addView(miniProgress, new LinearLayout.LayoutParams(0, Ui.dp(this, 2)));
        return root;
    }

    private View wrapIcon(ImageView icon, LinearLayout.LayoutParams lp) {
        FrameLayout box = new FrameLayout(this);
        box.setLayoutParams(lp);
        FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ilp.gravity = Gravity.CENTER;
        box.addView(icon, ilp);
        return box;
    }

    // ------------------------------------------------------------ navigation

    public void setTab(int id) {
        tab = id;
        updateNav();
        if (id == 1) showRoot(new HomeScreen(this));
        else if (id == 2) showRoot(new WaveScreen(this));
        else if (id == 3) showRoot(new SearchScreen(this));
        else showRoot(new LibraryScreen(this));
    }

    private void updateNav() {
        if (navItems == null) return;
        for (int i = 0; i < navItems.getChildCount(); i++) {
            View item = navItems.getChildAt(i);
            LinearLayout layout = (LinearLayout) item;
            boolean active = i + 1 == tab;
            ImageView icon = (ImageView) layout.getChildAt(0);
            TextView label = (TextView) layout.getChildAt(1);
            icon.setColorFilter(active ? Theme.accent() : Theme.dim());
            label.setTextColor(active ? Theme.accent() : Theme.dim());
        }
    }

    public void open(Screen screen) {
        if (current != null) {
            current.onHide();
            stack.add(current);
        }
        show(screen);
    }

    public void showRoot(Screen screen) {
        if (current != null) current.onHide();
        stack.clear();
        show(screen);
    }

    private void show(Screen screen) {
        clearRows();
        current = screen;
        content.removeAllViews();
        View view = screen.view();
        content.addView(view, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        screen.onShow();
        if (!Store.reduceMotion()) {
            view.setAlpha(0f);
            view.animate().alpha(1f).setDuration(180).start();
        }
        updateNav();
        updateRows();
    }

    public void refreshCurrent() {
        if (current != null) {
            clearRows();
            current.refresh();
            updateRows();
        }
    }

    /** Pops the top pushed screen (like the back button, but always pops). */
    public void closeTop() {
        if (player != null && player.isShown()) {
            player.hide();
            return;
        }
        if (!stack.isEmpty()) {
            Screen previous = stack.remove(stack.size() - 1);
            clearRows();
            current = previous;
            content.removeAllViews();
            content.addView(previous.view(), new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            previous.onShow();
            updateRows();
        } else {
            onBackPressed();
        }
    }

    @Override
    public void onBackPressed() {
        if (player != null && player.isShown()) {
            player.hide();
            return;
        }
        if (!stack.isEmpty()) {
            Screen previous = stack.remove(stack.size() - 1);
            clearRows();
            current = previous;
            content.removeAllViews();
            content.addView(previous.view(), new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            previous.onShow();
            updateRows();
            return;
        }
        if (tab != 1) {
            setTab(1);
            return;
        }
        super.onBackPressed();
    }

    // ---------------------------------------------------------------- screens

    public void showLogin() {
        tab = 0;
        stack.clear();
        current = null;
        nav.setVisibility(View.GONE);
        mini.setVisibility(View.GONE);
        content.removeAllViews();
        LoginScreen screen = new LoginScreen(this);
        content.addView(screen.view(), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        screen.onShow();
    }

    public void startApp() {
        nav.setVisibility(View.VISIBLE);
        tab = 1;
        updateNav();
        showRoot(new HomeScreen(this));
        updateMini();
        if (Store.loggedIn()) {
            bg(() -> {
                Models.Profile profile = App.api.profile();
                Store.setPlus(profile.hasPlus);
                if (!profile.name.isEmpty()) {
                    Store.saveSession(Store.token(), profile);
                }
                return profile;
            }, profile -> { if (current instanceof HomeScreen) current.refresh(); });
        }
        refreshLikes();
    }

    public void refreshLikes() {
        if (Store.uid() <= 0 || Store.token().isEmpty()) return;
        bg(() -> {
            java.util.Set<String> liked = App.api.likedIds();
            java.util.Set<String> disliked = App.api.dislikedIds();
            return new java.util.Set[]{liked, disliked};
        }, sets -> {
            Store.saveLikedSet(sets[0]);
            Store.saveDislikedSet(sets[1]);
            updateRows();
        });
    }

    public void openAlbum(String id) {
        if (id == null || id.isEmpty()) return;
        open(new AlbumScreen(this, id));
    }

    public void openArtist(String id) {
        if (id == null || id.isEmpty()) return;
        open(new ArtistScreen(this, id));
    }

    public void openPlaylist(long kind) {
        open(new PlaylistScreen(this, kind));
    }

    public void openPlaylist(Models.Playlist playlist) {
        if (playlist == null) return;
        open(new PlaylistScreen(this, playlist));
    }

    public void openLiked() {
        open(new LikedScreen(this));
    }

    public void openSettings() {
        open(new SettingsScreen(this));
    }

    public void openStats() {
        open(new StatsScreen(this));
    }

    public void openPlayer() {
        if (Playback.get().current() == null) return;
        if (player == null) player = new PlayerScreen(this);
        player.show();
    }

    public void openQueue() {
        QueueSheet.show(this);
    }

    public void openTrackMenu(Models.Track track, Long playlistKind, Integer playlistIndex) {
        TrackMenu.show(this, track, playlistKind, playlistIndex);
    }

    public void openPlaylistMenu(Models.Playlist playlist, Runnable onChanged) {
        com.mashiro.mobile.ui.PlaylistMenu.show(this, playlist, onChanged);
    }

    // --------------------------------------------------------------- playback

    public void playList(List<Models.Track> tracks, int index) {
        if (tracks == null || tracks.isEmpty()) return;
        int start = Math.max(0, Math.min(index, tracks.size() - 1));
        Playback.get().startQueue(new ArrayList<>(tracks), start, false, "", "", "");
        ensureMini();
    }

    public void playShuffled(List<Models.Track> tracks) {
        if (tracks == null || tracks.isEmpty()) return;
        List<Models.Track> copy = new ArrayList<>(tracks);
        java.util.Collections.shuffle(copy);
        Playback.get().startQueue(copy, 0, false, "", "", "");
        ensureMini();
    }

    public void ensureMini() {
        updateMini();
        if (Playback.get().current() != null) mini.setVisibility(View.VISIBLE);
    }

    private void updateMini() {
        if (mini == null) return;
        Playback p = Playback.get();
        Models.Track t = p.current();
        if (t == null) {
            mini.setVisibility(View.GONE);
            return;
        }
        mini.setVisibility(View.VISIBLE);
        miniTitle.setText(t.title);
        miniArtist.setText(t.artistLine());
        ImageLoader.load(t.coverUrl, miniCover, Ui.dp(this, 120));
        miniPlay.setImageResource(p.playing ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    private void updateMiniProgress() {
        if (miniProgress == null) return;
        Playback p = Playback.get();
        if (p.duration <= 0) {
            miniProgress.setVisibility(View.GONE);
            return;
        }
        miniProgress.setVisibility(View.VISIBLE);
        int width = mini.getWidth();
        if (width <= 0) return;
        int progress = (int) (width * (p.position / (float) p.duration));
        ViewGroup.LayoutParams lp = miniProgress.getLayoutParams();
        lp.width = Math.max(0, Math.min(width, progress));
        miniProgress.setLayoutParams(lp);
    }

    // ------------------------------------------------------------- row states

    public void registerRow(String trackId, View row, TextView title) {
        rows.add(new RowBinding(trackId, row, title));
    }

    public void clearRows() {
        rows.clear();
    }

    public void updateRows() {
        Models.Track current = Playback.get().current();
        String id = current == null ? "" : current.id;
        for (RowBinding binding : rows) {
            boolean active = binding.id.equals(id);
            binding.title.setTextColor(active ? Theme.accent() : Theme.text());
            binding.row.setBackground(active
                    ? Ui.bg(this, Theme.surface2(), 12)
                    : Ui.bg(this, Color.TRANSPARENT, 12));
        }
    }

    // ------------------------------------------------------------------- async

    public <T> void bg(Task<T> task, Callback<T> callback) {
        bg(task, callback, false);
    }

    public <T> void bg(Task<T> task, Callback<T> callback, boolean silent) {
        final int id = taskIds.incrementAndGet();
        executor.execute(() -> {
            try {
                T value = task.run();
                main.post(() -> callback.accept(value));
            } catch (Exception e) {
                final String message = e.getMessage() == null ? "Ошибка" : e.getMessage();
                main.post(() -> {
                    if (!silent) Ui.toast(MainActivity.this, message);
                    if (e instanceof com.mashiro.mobile.api.YandexApi.ApiException
                            && ((com.mashiro.mobile.api.YandexApi.ApiException) e).code == 401) {
                        handleAuthError();
                    }
                });
            }
        });
    }

    public void handleAuthError() {
        if (Store.guestMode()) return;
        Store.logout();
        App.applySession();
        showLogin();
    }

    public void logout() {
        Playback.get().stop();
        Store.logout();
        Store.setGuestChosen(false);
        App.applySession();
        if (player != null) {
            player.hide();
            player = null;
        }
        showLogin();
    }

    // ------------------------------------------------------------------- misc

    public void share(String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, "Поделиться"));
    }

    public void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)));
        } catch (Exception e) {
            Ui.toast(this, "Нет приложения для ссылки");
        }
    }

    private void askNotifications() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 41);
        }
    }

    public FrameLayout overlay() {
        return overlay;
    }

    public FrameLayout content() {
        return content;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        AudioImporter.handleResult(this, requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AudioImporter.REQ_PERM) {
            AudioImporter.handlePermissions(this, grantResults);
        }
    }

    @Override
    protected void onDestroy() {
        Playback.get().removeListener(listener);
        super.onDestroy();
    }
}
