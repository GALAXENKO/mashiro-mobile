package com.mashiro.mobile.screens;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mashiro.mobile.App;
import com.mashiro.mobile.MainActivity;
import com.mashiro.mobile.R;
import com.mashiro.mobile.Screen;
import com.mashiro.mobile.Store;
import com.mashiro.mobile.Theme;
import com.mashiro.mobile.Ui;
import com.mashiro.mobile.Widgets;
import com.mashiro.mobile.api.Models;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Offline-first search over downloads, recently played tracks and playlists. */
public class LibrarySearchScreen extends Screen {
    private LinearLayout content;
    private EditText input;
    private TextView clear;
    private final List<Models.Playlist> playlists = new ArrayList<>();
    private final List<Models.Track> downloaded = new ArrayList<>();
    private final List<Models.Track> recent = new ArrayList<>();
    private boolean loadedPlaylists = false;

    public LibrarySearchScreen(MainActivity a) {
        super(a);
    }

    @Override
    protected View build() {
        Widgets.Page page = Widgets.page(a);
        ScrollView scroll = page.scroll;
        page.body.addView(Widgets.header(a, "Медиатека", "поиск по своей музыке", true, 0, null));

        LinearLayout bar = Ui.row(a);
        android.graphics.drawable.GradientDrawable barBg =
                Ui.gradient(a, Theme.surface2(), Theme.surface(), 16);
        barBg.setStroke(Math.max(1, Ui.dp(a, 1)), Theme.hairline());
        bar.setBackground(barBg);
        bar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(a, 14), Ui.dp(a, 1), Ui.dp(a, 6), Ui.dp(a, 1));
        bar.addView(Ui.icon(a, R.drawable.ic_search, 19, Theme.faint()));

        input = new EditText(a);
        input.setHint("Трек, артист, плейлист");
        input.setHintTextColor(Theme.faint());
        input.setTextColor(Theme.text());
        input.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
        input.setBackground(null);
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        input.setPadding(Ui.dp(a, 11), Ui.dp(a, 14), Ui.dp(a, 8), Ui.dp(a, 14));
        input.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(input);

        clear = Ui.text(a, "✕", 15, Theme.faint(), false);
        clear.setGravity(android.view.Gravity.CENTER);
        clear.setPadding(Ui.dp(a, 10), Ui.dp(a, 8), Ui.dp(a, 12), Ui.dp(a, 8));
        clear.setVisibility(View.GONE);
        clear.setOnClickListener(v -> {
            input.setText("");
            Ui.hideKeyboard(input);
        });
        bar.addView(clear);
        page.body.addView(bar);

        content = Ui.column(a);
        page.body.addView(content);

        downloaded.clear();
        downloaded.addAll(Store.downloadedTracks());
        recent.clear();
        recent.addAll(Store.recent());

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String text = s.toString().trim();
                clear.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
                render(text);
            }
        });
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                Ui.hideKeyboard(input);
                render(input.getText().toString().trim());
                return true;
            }
            return false;
        });

        render("");
        return scroll;
    }

    @Override
    public void onShow() {
        if (loadedPlaylists) return;
        loadedPlaylists = true;
        a.bg(() -> App.api.playlists(), list -> {
            playlists.clear();
            if (list != null) playlists.addAll(list);
            render(input == null ? "" : input.getText().toString().trim());
        }, true);
    }

    private void render(String query) {
        if (content == null) return;
        content.removeAllViews();
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();

        List<Models.Track> tracks = new ArrayList<>();
        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (Models.Track t : downloaded) {
            if (q.isEmpty() || matches(t, q)) {
                if (!seen.containsKey(t.id)) {
                    seen.put(t.id, true);
                    tracks.add(t);
                }
            }
        }
        for (Models.Track t : recent) {
            if (q.isEmpty() || matches(t, q)) {
                if (!seen.containsKey(t.id)) {
                    seen.put(t.id, true);
                    tracks.add(t);
                }
            }
        }

        List<Models.Playlist> found = new ArrayList<>();
        for (Models.Playlist p : playlists) {
            if (q.isEmpty() || p.title.toLowerCase(Locale.ROOT).contains(q)) found.add(p);
        }

        if (q.isEmpty()) {
            if (!downloaded.isEmpty()) {
                content.addView(Widgets.sectionHeader(a, "Скачано", "Слушать",
                        v -> a.playList(downloaded, 0)));
                int limit = Math.min(8, downloaded.size());
                for (int i = 0; i < limit; i++) {
                    content.addView(Widgets.trackRow(a, downloaded, i, null));
                }
            }
            if (!recent.isEmpty()) {
                content.addView(Widgets.sectionHeader(a, "Недавно слушали", null, null));
                int limit = Math.min(6, recent.size());
                for (int i = 0; i < limit; i++) {
                    content.addView(Widgets.trackRow(a, recent, i, null));
                }
            }
            if (!playlists.isEmpty()) {
                content.addView(Widgets.sectionHeader(a, "Мои плейлисты", null, null));
                LinearLayout row = Widgets.horizontal(a);
                for (Models.Playlist p : playlists) {
                    View card = Widgets.playlistCard(a, p, 136);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            Ui.dp(a, 136), ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.rightMargin = Ui.dp(a, 14);
                    row.addView(card, lp);
                }
                content.addView(Widgets.horizontalScroll(a, row));
            }
            if (downloaded.isEmpty() && recent.isEmpty() && playlists.isEmpty()) {
                content.addView(Widgets.empty(a, "Медиатека пуста",
                        "Скачайте треки или начните слушать — они появятся здесь"));
            }
            return;
        }

        if (tracks.isEmpty() && found.isEmpty()) {
            content.addView(Widgets.empty(a, "Ничего не найдено",
                    "В вашей медиатеке нет совпадений с «" + query + "»"));
            return;
        }

        if (!tracks.isEmpty()) {
            content.addView(Widgets.sectionHeader(a,
                    tracks.size() + " " + Ui.plural(tracks.size(), "трек", "трека", "треков"),
                    "Слушать", v -> a.playList(tracks, 0)));
            for (int i = 0; i < tracks.size(); i++) {
                content.addView(Widgets.trackRow(a, tracks, i, null));
            }
        }

        if (!found.isEmpty()) {
            content.addView(Widgets.sectionHeader(a, "Плейлисты", null, null));
            for (Models.Playlist p : found) {
                content.addView(Widgets.actionCard(a, R.drawable.ic_music, p.title,
                        p.trackCount + " " + Ui.plural(p.trackCount, "трек", "трека", "треков"),
                        () -> a.openPlaylist(p)));
            }
        }
    }

    private boolean matches(Models.Track track, String query) {
        if (track.title != null && track.title.toLowerCase(Locale.ROOT).contains(query)) return true;
        if (track.albumTitle != null && track.albumTitle.toLowerCase(Locale.ROOT).contains(query)) return true;
        return track.artistLine().toLowerCase(Locale.ROOT).contains(query);
    }
}
