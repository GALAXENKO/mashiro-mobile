package com.mashiro.mobile;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mashiro.mobile.api.Models;

import java.util.List;

/** Reusable cards and list rows in the Mashiro visual language. */
public final class Widgets {
    private Widgets() {}

    public static class Page {
        public ScrollView scroll;
        public LinearLayout body;
    }

    public static Page page(MainActivity a) {
        Page page = new Page();
        page.scroll = new ScrollView(a);
        page.scroll.setFillViewport(true);
        page.scroll.setBackgroundColor(Theme.bg());
        page.scroll.setClipToPadding(false);
        page.body = Ui.column(a);
        page.body.setPadding(Ui.dp(a, 18), Ui.dp(a, 6), Ui.dp(a, 18), Ui.dp(a, 34));
        page.scroll.addView(page.body, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return page;
    }

    public static LinearLayout header(MainActivity a, String title, String subtitle,
                                      boolean back, int rightIcon, View.OnClickListener right) {
        LinearLayout row = Ui.row(a);
        row.setPadding(0, Ui.dp(a, 10), 0, Ui.dp(a, 12));
        if (back) {
            View b = Ui.iconButton(a, R.drawable.ic_back, 40, 21, Theme.text(),
                    v -> a.onBackPressed());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(a, 40), Ui.dp(a, 40));
            lp.rightMargin = Ui.dp(a, 8);
            row.addView(b, lp);
        }
        LinearLayout copy = Ui.column(a);
        copy.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView t = Ui.title(a, title, 25);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(t);
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView s = Ui.text(a, subtitle, 12, Theme.dim(), false);
            s.setSingleLine(true);
            s.setEllipsize(TextUtils.TruncateAt.END);
            copy.addView(s);
        }
        row.addView(copy);
        if (rightIcon != 0) {
            row.addView(Ui.iconButton(a, rightIcon, 40, 20, Theme.text(), right));
        }
        return row;
    }

    public static LinearLayout sectionHeader(MainActivity a, String title, String action,
                                             View.OnClickListener click) {
        LinearLayout row = Ui.row(a);
        row.setPadding(0, Ui.dp(a, 22), 0, Ui.dp(a, 10));
        TextView t = Ui.text(a, title, 18, Theme.text(), true);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(t);
        if (action != null && !action.isEmpty()) {
            TextView more = Ui.text(a, action, 12, Theme.dim(), true);
            more.setPadding(Ui.dp(a, 8), Ui.dp(a, 4), 0, Ui.dp(a, 4));
            more.setOnClickListener(click);
            row.addView(more);
        }
        return row;
    }

    public static ImageLoader.RoundedImageView cover(Context c, String url, int sizeDp, float radiusDp) {
        ImageLoader.RoundedImageView view = new ImageLoader.RoundedImageView(c);
        view.setRadiusDp(radiusDp);
        view.setBackground(Ui.bg(c, Theme.surface2(), radiusDp));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(c, sizeDp), Ui.dp(c, sizeDp));
        view.setLayoutParams(lp);
        ImageLoader.load(url, view, Ui.dp(c, sizeDp));
        return view;
    }

    public static ImageLoader.RoundedImageView avatar(Context c, String url, int sizeDp) {
        return cover(c, url, sizeDp, sizeDp / 2f);
    }

    public static View circleIcon(Context c, int res, int sizeDp, int bg, int tint) {
        FrameLayout box = new FrameLayout(c);
        box.setBackground(Ui.bg(c, bg, sizeDp / 2f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(c, sizeDp), Ui.dp(c, sizeDp));
        box.setLayoutParams(lp);
        ImageView icon = Ui.icon(c, res, sizeDp * 0.44f, tint);
        FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(
                Ui.dp(c, sizeDp * 0.44f), Ui.dp(c, sizeDp * 0.44f));
        ilp.gravity = Gravity.CENTER;
        box.addView(icon, ilp);
        return box;
    }

    public static LinearLayout trackRow(MainActivity a, List<Models.Track> list, int i,
                                        Long playlistKind) {
        Models.Track track = list.get(i);
        LinearLayout row = Ui.row(a);
        row.setPadding(Ui.dp(a, 6), Ui.dp(a, 6), Ui.dp(a, 2), Ui.dp(a, 6));
        row.setBackground(Ui.bg(a, Color.TRANSPARENT, 12));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ImageLoader.RoundedImageView art = cover(a, track.coverUrl, 46, 8);
        LinearLayout.LayoutParams artLp = new LinearLayout.LayoutParams(Ui.dp(a, 46), Ui.dp(a, 46));
        artLp.rightMargin = Ui.dp(a, 12);
        row.addView(art, artLp);

        LinearLayout copy = Ui.column(a);
        copy.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView title = Ui.text(a, track.title, 14.5f, Theme.text(), true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        TextView sub = Ui.text(a, track.artistLine(), 11.5f, Theme.dim(), false);
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(title);
        copy.addView(sub);
        row.addView(copy);

        TextView time = Ui.text(a, Ui.duration(track.durationMs), 11, Theme.faint(), false);
        LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        timeLp.rightMargin = Ui.dp(a, 2);
        row.addView(time, timeLp);

        View more = Ui.iconButton(a, R.drawable.ic_more, 36, 19, Theme.faint(),
                v -> a.openTrackMenu(track, playlistKind, playlistKind == null ? null : i));
        row.addView(more);

        row.setOnClickListener(v -> a.playList(list, i));
        row.setOnLongClickListener(v -> {
            a.openTrackMenu(track, playlistKind, playlistKind == null ? null : i);
            return true;
        });
        a.registerRow(track.id, row, title);
        return row;
    }

    /** Compact numbered row for album, playlist and artist track lists. */
    public static LinearLayout numberedRow(MainActivity a, List<Models.Track> list, int i,
                                           Long playlistKind) {
        Models.Track track = list.get(i);
        LinearLayout row = Ui.row(a);
        row.setPadding(Ui.dp(a, 2), Ui.dp(a, 7), Ui.dp(a, 2), Ui.dp(a, 7));
        row.setBackground(Ui.bg(a, Color.TRANSPARENT, 10));

        TextView num = Ui.text(a, String.valueOf(i + 1), 12.5f, Theme.faint(), true);
        num.setGravity(Gravity.CENTER);
        num.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(a, 28),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(num);

        LinearLayout copy = Ui.column(a);
        copy.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView title = Ui.text(a, track.title, 14, Theme.text(), true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(title);
        TextView artist = Ui.text(a, track.artistLine(), 11.5f, Theme.dim(), false);
        artist.setSingleLine(true);
        artist.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(artist);
        row.addView(copy);

        if (!track.available) {
            TextView badge = Ui.pill(a, "нет", Theme.faint());
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            blp.rightMargin = Ui.dp(a, 6);
            row.addView(badge, blp);
        }

        TextView time = Ui.text(a, Ui.duration(track.durationMs), 11, Theme.faint(), false);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.rightMargin = Ui.dp(a, 2);
        row.addView(time, tlp);

        View more = Ui.iconButton(a, R.drawable.ic_more, 34, 18, Theme.faint(),
                v -> a.openTrackMenu(track, playlistKind, playlistKind == null ? null : i));
        row.addView(more);

        row.setOnClickListener(v -> a.playList(list, i));
        row.setOnLongClickListener(v -> {
            a.openTrackMenu(track, playlistKind, playlistKind == null ? null : i);
            return true;
        });
        a.registerRow(track.id, row, title);
        return row;
    }

    public static LinearLayout simpleTrackRow(MainActivity a, Models.Track track, int index,
                                              Runnable onClick) {        LinearLayout row = Ui.row(a);
        row.setPadding(Ui.dp(a, 4), Ui.dp(a, 6), Ui.dp(a, 4), Ui.dp(a, 6));
        TextView num = Ui.text(a, String.valueOf(index + 1), 13, Theme.faint(), true);
        num.setGravity(Gravity.CENTER);
        num.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(a, 26), ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(num);
        ImageLoader.RoundedImageView art = cover(a, track.coverUrl, 42, 7);
        LinearLayout.LayoutParams artLp = new LinearLayout.LayoutParams(Ui.dp(a, 42), Ui.dp(a, 42));
        artLp.rightMargin = Ui.dp(a, 10);
        row.addView(art, artLp);
        LinearLayout copy = Ui.column(a);
        copy.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView title = Ui.text(a, track.title, 14, Theme.text(), true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(title);
        copy.addView(Ui.text(a, track.artistLine(), 11.5f, Theme.dim(), false));
        row.addView(copy);
        if (onClick != null) row.setOnClickListener(v -> onClick.run());
        a.registerRow(track.id, row, title);
        return row;
    }

    public static View playlistCard(MainActivity a, Models.Playlist playlist, int widthDp) {
        LinearLayout card = Ui.column(a);
        card.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(a, widthDp),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout artBox = new FrameLayout(a);
        ImageLoader.RoundedImageView art = cover(a, playlist.coverUrl, widthDp, 12);
        artBox.addView(art);
        View play = circleIcon(a, R.drawable.ic_play, 30, Theme.accent(), Theme.accentText());
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(Ui.dp(a, 30), Ui.dp(a, 30));
        plp.gravity = Gravity.BOTTOM | Gravity.END;
        plp.setMargins(0, 0, Ui.dp(a, 8), Ui.dp(a, 8));
        play.setOnClickListener(v -> a.bg(() -> App.api.playlistOf(playlist.ownerUid, playlist.kind),
                loaded -> a.playList(loaded.tracks, 0)));
        artBox.addView(play, plp);
        card.addView(artBox);
        TextView t = Ui.text(a, playlist.title, 13, Theme.text(), true);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = Ui.dp(a, 8);
        card.addView(t, tlp);
        card.addView(Ui.text(a, playlist.trackCount + " " + Ui.plural(playlist.trackCount,
                "трек", "трека", "треков"), 11, Theme.faint(), false));
        card.setOnClickListener(v -> a.openPlaylist(playlist));
        card.setOnLongClickListener(v -> {
            a.openPlaylistMenu(playlist, a::refreshCurrent);
            return true;
        });
        return card;
    }

    /** Square card sized in pixels, used by two-column library grids. */
    public static View playlistGridCard(MainActivity a, Models.Playlist playlist, int sizePx) {
        LinearLayout card = Ui.column(a);
        card.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout artBox = new FrameLayout(a);
        ImageLoader.RoundedImageView art = new ImageLoader.RoundedImageView(a);
        art.setRadiusDp(12);
        art.setBackground(Ui.bg(a, Theme.surface2(), 12));
        FrameLayout.LayoutParams alp = new FrameLayout.LayoutParams(sizePx, sizePx);
        artBox.addView(art, alp);
        ImageLoader.load(playlist.coverUrl, art, sizePx);
        View play = circleIcon(a, R.drawable.ic_play, 30, Theme.accent(), Theme.accentText());
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(Ui.dp(a, 30), Ui.dp(a, 30));
        plp.gravity = Gravity.BOTTOM | Gravity.END;
        plp.setMargins(0, 0, Ui.dp(a, 8), Ui.dp(a, 8));
        play.setOnClickListener(v -> a.bg(() -> App.api.playlistOf(playlist.ownerUid, playlist.kind),
                loaded -> a.playList(loaded.tracks, 0)));
        artBox.addView(play, plp);
        card.addView(artBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView t = Ui.text(a, playlist.title, 13, Theme.text(), true);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = Ui.dp(a, 8);
        card.addView(t, tlp);
        card.addView(Ui.text(a, playlist.trackCount + " " + Ui.plural(playlist.trackCount,
                "трек", "трека", "треков"), 11, Theme.faint(), false));
        card.setOnClickListener(v -> a.openPlaylist(playlist));
        card.setOnLongClickListener(v -> {
            a.openPlaylistMenu(playlist, a::refreshCurrent);
            return true;
        });
        return card;
    }

    public static View albumCard(MainActivity a, Models.Album album, int widthDp) {        LinearLayout card = Ui.column(a);
        card.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(a, widthDp),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        card.addView(cover(a, album.coverUrl, widthDp, 12));
        TextView t = Ui.text(a, album.title, 13, Theme.text(), true);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = Ui.dp(a, 8);
        card.addView(t, tlp);
        String sub = album.year > 0 ? String.valueOf(album.year) : "Альбом";
        card.addView(Ui.text(a, sub, 11, Theme.faint(), false));
        card.setOnClickListener(v -> a.openAlbum(album.id));
        return card;
    }

    public static View artistCard(MainActivity a, Models.Artist artist, int widthDp) {
        LinearLayout card = Ui.column(a);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(a, widthDp),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        ImageLoader.RoundedImageView art = cover(a, artist.coverUrl.isEmpty()
                ? "" : artist.coverUrl, widthDp, widthDp / 2f);
        card.addView(art);
        TextView t = Ui.text(a, artist.name, 12.5f, Theme.text(), true);
        t.setGravity(Gravity.CENTER);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        t.setPadding(0, Ui.dp(a, 8), 0, 0);
        card.addView(t);
        card.setOnClickListener(v -> a.openArtist(artist.id));
        return card;
    }

    public static LinearLayout horizontal(MainActivity a) {
        LinearLayout row = Ui.row(a);
        row.setPadding(0, 0, 0, Ui.dp(a, 2));
        return row;
    }

    public static ScrollView horizontalScroll(MainActivity a, View content) {
        ScrollView scroll = new ScrollView(a);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.addView(content);
        return scroll;
    }

    public static LinearLayout empty(MainActivity a, String title, String subtitle) {
        LinearLayout box = Ui.column(a);
        box.setGravity(Gravity.CENTER);
        box.setPadding(Ui.dp(a, 20), Ui.dp(a, 44), Ui.dp(a, 20), Ui.dp(a, 44));
        box.addView(circleIcon(a, R.drawable.ic_music, 56, Theme.surface2(), Theme.faint()));
        TextView t = Ui.text(a, title, 15, Theme.text(), true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, Ui.dp(a, 14), 0, Ui.dp(a, 4));
        box.addView(t);
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView s = Ui.text(a, subtitle, 12.5f, Theme.dim(), false);
            s.setGravity(Gravity.CENTER);
            box.addView(s);
        }
        return box;
    }

    public static LinearLayout loading(MainActivity a) {
        LinearLayout box = Ui.row(a);
        box.setGravity(Gravity.CENTER);
        box.setPadding(0, Ui.dp(a, 40), 0, Ui.dp(a, 40));
        ProgressBar p = Ui.progress(a, false);
        box.addView(p);
        TextView t = Ui.text(a, "Загружаем…", 13, Theme.dim(), false);
        t.setPadding(Ui.dp(a, 10), 0, 0, 0);
        box.addView(t);
        return box;
    }

    public static LinearLayout actionChip(MainActivity a, int icon, String text, Runnable click) {
        LinearLayout box = Ui.row(a);
        box.setBackground(Ui.strokeBg(a, Theme.surface(), 13, Theme.border(), 1));
        box.setPadding(Ui.dp(a, 13), Ui.dp(a, 11), Ui.dp(a, 13), Ui.dp(a, 11));
        ImageView iv = Ui.icon(a, icon, 17, Theme.text());
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(Ui.dp(a, 17), Ui.dp(a, 17));
        ilp.rightMargin = Ui.dp(a, 8);
        box.addView(iv, ilp);
        TextView label = Ui.text(a, text, 12.5f, Theme.text(), true);
        box.addView(label);
        box.setOnClickListener(v -> click.run());
        Ui.ripple(box, 0x1AFFFFFF);
        return box;
    }

    public static LinearLayout actionCard(MainActivity a, int icon, String title, String subtitle,
                                          Runnable click) {
        LinearLayout box = Ui.row(a);
        box.setBackground(Ui.strokeBg(a, Theme.surface(), 15, Theme.border(), 1));
        box.setPadding(Ui.dp(a, 14), Ui.dp(a, 13), Ui.dp(a, 14), Ui.dp(a, 13));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(a, 8);
        box.setLayoutParams(lp);
        box.addView(circleIcon(a, icon, 38, Theme.surface2(), Theme.text()));
        LinearLayout copy = Ui.column(a);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = Ui.dp(a, 12);
        copy.setLayoutParams(clp);
        copy.addView(Ui.text(a, title, 14, Theme.text(), true));
        if (subtitle != null && !subtitle.isEmpty()) {
            copy.addView(Ui.text(a, subtitle, 11.5f, Theme.dim(), false));
        }
        box.addView(copy);
        ImageView arrow = Ui.icon(a, R.drawable.ic_next, 16, Theme.faint());
        box.addView(arrow);
        box.setOnClickListener(v -> click.run());
        Ui.ripple(box, 0x1AFFFFFF);
        return box;
    }

    public static LinearLayout bigAction(MainActivity a, int icon, String title, String subtitle,
                                         Runnable click) {
        LinearLayout box = Ui.row(a);
        box.setBackground(Ui.strokeBg(a, Theme.surface(), 16, Theme.border(), 1));
        box.setPadding(Ui.dp(a, 15), Ui.dp(a, 14), Ui.dp(a, 15), Ui.dp(a, 14));
        box.addView(circleIcon(a, icon, 40, Theme.accentSoft(20), Theme.accent()));
        LinearLayout copy = Ui.column(a);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = Ui.dp(a, 12);
        copy.setLayoutParams(clp);
        copy.addView(Ui.text(a, title, 14, Theme.text(), true));
        copy.addView(Ui.text(a, subtitle, 11.5f, Theme.dim(), false));
        box.addView(copy);
        box.setOnClickListener(v -> click.run());
        Ui.ripple(box, 0x1AFFFFFF);
        return box;
    }

    public static TextView countLabel(MainActivity a, int count) {
        return Ui.text(a, count + " " + Ui.plural(count, "трек", "трека", "треков"),
                12, Theme.faint(), false);
    }
}
