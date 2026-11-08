package com.mashiro.mobile.screens;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
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
import java.util.List;

/** Catalog search: tracks, artists, albums and playlists. */
public class SearchScreen extends Screen {
    private LinearLayout body;
    private LinearLayout historyBox;
    private LinearLayout resultsBox;
    private EditText input;
    private TextView clear;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable debounce;
    private int generation = 0;
    private boolean popularLoaded = false;
    private final List<Models.Track> popular = new ArrayList<>();
    private String lastQuery = "";

    public SearchScreen(MainActivity a) {
        super(a);
    }

    @Override
    protected View build() {
        Widgets.Page page = Widgets.page(a);
        body = page.body;
        ScrollView scroll = page.scroll;

        body.addView(Widgets.header(a, "Поиск", "треки, артисты, альбомы", false,
                R.drawable.ic_settings, v -> a.openSettings()));

        LinearLayout bar = Ui.row(a);
        android.graphics.drawable.GradientDrawable barBg =
                Ui.gradient(a, Theme.surface2(), Theme.surface(), 16);
        barBg.setStroke(Math.max(1, Ui.dp(a, 1)), Theme.hairline());
        bar.setBackground(barBg);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(a, 14), Ui.dp(a, 1), Ui.dp(a, 6), Ui.dp(a, 1));
        bar.addView(Ui.icon(a, R.drawable.ic_search, 19, Theme.faint()));

        input = new EditText(a);
        input.setHint("Что послушаем?");
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
        clear.setGravity(Gravity.CENTER);
        clear.setPadding(Ui.dp(a, 10), Ui.dp(a, 8), Ui.dp(a, 12), Ui.dp(a, 8));
        clear.setVisibility(View.GONE);
        clear.setOnClickListener(v -> {
            input.setText("");
            showHistory();
        });
        bar.addView(clear);
        body.addView(bar);

        historyBox = Ui.column(a);
        body.addView(historyBox);

        resultsBox = Ui.column(a);
        body.addView(resultsBox);

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String text = s.toString().trim();
                clear.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
                if (text.isEmpty()) {
                    showHistory();
                    return;
                }
                suggest(text);
            }
        });
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                String text = input.getText().toString().trim();
                if (!text.isEmpty()) {
                    Ui.hideKeyboard(input);
                    search(text);
                }
                return true;
            }
            return false;
        });

        showHistory();
        return scroll;
    }

    @Override
    public void onShow() {
        if (!popularLoaded) loadPopular();
    }

    private void loadPopular() {
        popularLoaded = true;
        a.bg(() -> App.api.chart(), chart -> {
            popular.clear();
            popular.addAll(chart.tracks);
            if (input != null && input.getText().toString().trim().isEmpty()) showHistory();
        }, true);
    }

    // -------------------------------------------------------------- suggestions

    private void suggest(String text) {
        if (debounce != null) handler.removeCallbacks(debounce);
        debounce = () -> {
            final String query = text;
            a.bg(() -> App.api.suggest(query), list -> {
                String current = input.getText().toString().trim();
                if (!current.equalsIgnoreCase(query)) return;
                if (current.isEmpty()) return;
                renderSuggestions(list);
            }, true);
        };
        handler.postDelayed(debounce, 380);
    }

    private void renderSuggestions(List<String> list) {
        historyBox.removeAllViews();
        historyBox.setVisibility(View.VISIBLE);
        resultsBox.removeAllViews();
        if (list == null || list.isEmpty()) return;
        LinearLayout row = Widgets.horizontal(a);
        for (String value : list) {
            TextView chip = Ui.chip(a, value, false, v -> {
                input.setText(value);
                input.setSelection(value.length());
                Ui.hideKeyboard(input);
                search(value);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = Ui.dp(a, 8);
            chip.setLayoutParams(lp);
            row.addView(chip);
        }
        historyBox.addView(row);
    }

    // ----------------------------------------------------------------- history

    private void showHistory() {
        generation++;
        historyBox.setVisibility(View.VISIBLE);
        historyBox.removeAllViews();
        resultsBox.removeAllViews();
        lastQuery = "";

        List<String> searches = Store.searches();
        if (!searches.isEmpty()) {
            LinearLayout head = Widgets.sectionHeader(a, "Недавние запросы", "Очистить", v -> {
                Store.clearSearches();
                showHistory();
            });
            head.setPadding(0, Ui.dp(a, 22), 0, Ui.dp(a, 6));
            historyBox.addView(head);
            LinearLayout row = Widgets.horizontal(a);
            for (String value : searches) {
                TextView chip = Ui.chip(a, value, false, v -> {
                    input.setText(value);
                    input.setSelection(value.length());
                    search(value);
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.rightMargin = Ui.dp(a, 8);
                lp.bottomMargin = Ui.dp(a, 8);
                chip.setLayoutParams(lp);
                row.addView(chip);
            }
            historyBox.addView(Widgets.horizontalScroll(a, row));
        }

        if (!popular.isEmpty()) {
            historyBox.addView(Widgets.sectionHeader(a, "Популярное сейчас", null, null));
            int limit = Math.min(8, popular.size());
            for (int i = 0; i < limit; i++) {
                historyBox.addView(Widgets.trackRow(a, popular, i, null));
            }
        } else {
            historyBox.addView(Widgets.empty(a, "Начните с поиска",
                    "Найдите трек, артиста или плейлист"));
        }
    }

    // ----------------------------------------------------------------- results

    private void search(String text) {
        final int gen = ++generation;
        lastQuery = text;
        Store.addSearch(text);
        historyBox.setVisibility(View.GONE);
        resultsBox.removeAllViews();
        resultsBox.addView(Widgets.loading(a));
        a.bg(() -> App.api.search(text), result -> {
            if (gen != generation) return;
            renderResults(result);
        }, false);
    }

    private void renderResults(Models.SearchResult r) {
        resultsBox.removeAllViews();
        if (r == null) return;
        boolean empty = r.tracks.isEmpty() && r.artists.isEmpty()
                && r.albums.isEmpty() && r.playlists.isEmpty();
        if (empty) {
            resultsBox.addView(Widgets.empty(a, "Ничего не нашли",
                    "Попробуйте изменить запрос: «" + lastQuery + "»"));
            return;
        }

        if (!r.tracks.isEmpty()) {
            resultsBox.addView(Widgets.sectionHeader(a, "Треки", "Слушать",
                    v -> a.playList(r.tracks, 0)));
            int limit = Math.min(6, r.tracks.size());
            for (int i = 0; i < limit; i++) {
                resultsBox.addView(Widgets.trackRow(a, r.tracks, i, null));
            }
            if (r.tracks.size() > limit) {
                TextView more = Ui.text(a, "Ещё " + (r.tracks.size() - limit) + " "
                        + Ui.plural(r.tracks.size() - limit, "трек", "трека", "треков")
                        + "  ›", 12, Theme.accent(), true);
                more.setPadding(0, Ui.dp(a, 12), 0, Ui.dp(a, 2));
                more.setOnClickListener(v -> a.playList(r.tracks, 0));
                resultsBox.addView(more);
            }
        }

        if (!r.artists.isEmpty()) {
            resultsBox.addView(Widgets.sectionHeader(a, "Артисты", null, null));
            LinearLayout row = Widgets.horizontal(a);
            for (Models.Artist artist : r.artists) {
                View card = Widgets.artistCard(a, artist, 92);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        Ui.dp(a, 92), ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.rightMargin = Ui.dp(a, 12);
                row.addView(card, lp);
            }
            resultsBox.addView(Widgets.horizontalScroll(a, row));
        }

        if (!r.albums.isEmpty()) {
            resultsBox.addView(Widgets.sectionHeader(a, "Альбомы", null, null));
            LinearLayout row = Widgets.horizontal(a);
            for (Models.Album album : r.albums) {
                View card = Widgets.albumCard(a, album, 136);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        Ui.dp(a, 136), ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.rightMargin = Ui.dp(a, 12);
                row.addView(card, lp);
            }
            resultsBox.addView(Widgets.horizontalScroll(a, row));
        }

        if (!r.playlists.isEmpty()) {
            resultsBox.addView(Widgets.sectionHeader(a, "Плейлисты", null, null));
            LinearLayout row = Widgets.horizontal(a);
            for (Models.Playlist playlist : r.playlists) {
                View card = Widgets.playlistCard(a, playlist, 136);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        Ui.dp(a, 136), ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.rightMargin = Ui.dp(a, 12);
                row.addView(card, lp);
            }
            resultsBox.addView(Widgets.horizontalScroll(a, row));
        }

        TextView hint = Ui.overline(a, "Яндекс Музыка");
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, Ui.dp(a, 28), 0, 0);
        resultsBox.addView(hint);
    }
}
