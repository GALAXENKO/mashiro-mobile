package com.mashiro.mobile.screens;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mashiro.mobile.MainActivity;
import com.mashiro.mobile.R;
import com.mashiro.mobile.Screen;
import com.mashiro.mobile.Store;
import com.mashiro.mobile.Theme;
import com.mashiro.mobile.Ui;
import com.mashiro.mobile.Widgets;

import java.util.List;

/** Local listening statistics collected while the app is used. */
public class StatsScreen extends Screen {
    private LinearLayout content;

    public StatsScreen(MainActivity a) {
        super(a);
    }

    @Override
    protected View build() {
        Widgets.Page page = Widgets.page(a);
        ScrollView scroll = page.scroll;
        page.body.addView(Widgets.header(a, "Статистика", "ваши прослушивания", true, 0, null));
        content = Ui.column(a);
        page.body.addView(content);
        render();
        return scroll;
    }

    @Override
    public void refresh() {
        render();
    }

    private void render() {
        content.removeAllViews();
        int total = Store.totalPlays();
        List<Store.StatItem> tracks = Store.topStats();
        List<Store.StatItem> artists = Store.topArtists();

        LinearLayout summary = Ui.row(a);
        summary.setBackground(Ui.strokeBg(a, Theme.surface(), 18, Theme.border(), 1));
        summary.setPadding(Ui.dp(a, 18), Ui.dp(a, 18), Ui.dp(a, 18), Ui.dp(a, 18));
        LinearLayout copy = Ui.column(a);
        copy.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        copy.addView(Ui.text(a, "ВСЕГО ПРОСЛУШИВАНИЙ", 9.5f, Theme.dim(), true));
        TextView number = Ui.title(a, String.valueOf(total), 40);
        number.setPadding(0, Ui.dp(a, 4), 0, 0);
        copy.addView(number);
        TextView sub = Ui.text(a, tracks.size() + " "
                + Ui.plural(tracks.size(), "трек", "трека", "треков") + " · "
                + artists.size() + " " + Ui.plural(artists.size(), "артист", "артиста", "артистов"),
                11.5f, Theme.dim(), false);
        copy.addView(sub);
        summary.addView(copy);
        summary.addView(Widgets.circleIcon(a, R.drawable.ic_headphones, 56,
                Theme.accentSoft(20), Theme.accent()));
        content.addView(summary);

        if (total == 0) {
            content.addView(Widgets.empty(a, "Пока нет статистики",
                    "Слушайте треки — здесь появятся ваши любимые исполнители и треки"));
            return;
        }

        content.addView(Widgets.sectionHeader(a, "Любимые треки", null, null));
        int trackLimit = Math.min(10, tracks.size());
        for (int i = 0; i < trackLimit; i++) {
            content.addView(statRow(tracks.get(i), i + 1));
        }

        if (!artists.isEmpty()) {
            content.addView(Widgets.sectionHeader(a, "Любимые исполнители", null, null));
            int artistLimit = Math.min(8, artists.size());
            for (int i = 0; i < artistLimit; i++) {
                Store.StatItem item = artists.get(i);
                LinearLayout row = Ui.row(a);
                row.setPadding(Ui.dp(a, 4), Ui.dp(a, 7), Ui.dp(a, 4), Ui.dp(a, 7));
                TextView num = Ui.text(a, String.valueOf(i + 1), 12.5f, Theme.faint(), true);
                num.setGravity(Gravity.CENTER);
                num.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(a, 28),
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                row.addView(num);
                row.addView(Widgets.circleIcon(a, R.drawable.ic_person, 40,
                        Theme.surface2(), Theme.dim()));
                LinearLayout copyRow = Ui.column(a);
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                clp.leftMargin = Ui.dp(a, 12);
                copyRow.setLayoutParams(clp);
                TextView name = Ui.text(a, item.title, 14, Theme.text(), true);
                name.setSingleLine(true);
                copyRow.addView(name);
                copyRow.addView(Ui.text(a, item.count + " "
                        + Ui.plural(item.count, "прослушивание", "прослушивания", "прослушиваний"),
                        11, Theme.dim(), false));
                row.addView(copyRow);
                content.addView(row);
            }
        }

        TextView hint = Ui.text(a, "Статистика хранится только на этом устройстве", 10.5f,
                Theme.faint(), false);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, Ui.dp(a, 22), 0, 0);
        content.addView(hint);

        TextView reset = Ui.secondaryButton(a, "Сбросить статистику", v ->
                Ui.confirm(a, "Сбросить статистику?", "Локальные счётчики будут удалены",
                        "Сбросить", () -> {
                            Store.clearStats();
                            render();
                            Ui.toast(a, "Статистика сброшена");
                        }).show());
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = Ui.dp(a, 14);
        reset.setLayoutParams(rlp);
        content.addView(reset);
    }

    private View statRow(Store.StatItem item, int position) {
        LinearLayout row = Ui.row(a);
        row.setPadding(Ui.dp(a, 4), Ui.dp(a, 6), Ui.dp(a, 4), Ui.dp(a, 6));
        TextView num = Ui.text(a, String.valueOf(position), 12.5f, Theme.faint(), true);
        num.setGravity(Gravity.CENTER);
        num.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(a, 26),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(num);
        row.addView(Widgets.cover(a, item.cover, 44, 8));
        LinearLayout copy = Ui.column(a);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = Ui.dp(a, 11);
        copy.setLayoutParams(clp);
        TextView title = Ui.text(a, item.title, 14, Theme.text(), true);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        copy.addView(title);
        TextView artist = Ui.text(a, item.artist, 11.5f, Theme.dim(), false);
        artist.setSingleLine(true);
        artist.setEllipsize(android.text.TextUtils.TruncateAt.END);
        copy.addView(artist);
        row.addView(copy);
        TextView count = Ui.text(a, "×" + item.count, 12, Theme.accent(), true);
        row.addView(count);
        return row;
    }
}
