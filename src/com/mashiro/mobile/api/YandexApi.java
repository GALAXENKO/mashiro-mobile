package com.mashiro.mobile.api;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/** Synchronous client for the unofficial Yandex Music API. Call from a background thread. */
public class YandexApi {
    public static final String BASE = "https://api.music.yandex.net";
    public static final String MUSIC_CLIENT = "YandexMusicAndroid/24023621";
    public static final String OAUTH_CLIENT_ID = "23cabbbdc6cd418abb4b39c32c41195d";
    public static final String OAUTH_CLIENT_SECRET = "53bc75238f0c4d08a118e51fe9203300";
    private static final String SIGN_SALT = "XGRlBW9FXlekgbPrRHuSiA";

    private volatile String token = "";
    public volatile long uid = 0;
    public volatile String quality = "high";

    private final Map<String, CachedUrl> streamCache = new HashMap<>();

    public static class ApiException extends Exception {
        public final int code;
        public ApiException(int code, String message) {
            super(message);
            this.code = code;
        }
    }

    private static class CachedUrl {
        String url;
        long at;
        CachedUrl(String url) {
            this.url = url;
            this.at = System.currentTimeMillis();
        }
    }

    public void setToken(String token, long uid) {
        this.token = token == null ? "" : token.trim();
        this.uid = uid;
        streamCache.clear();
    }

    public String token() {
        return token;
    }

    public boolean hasToken() {
        return token != null && !token.isEmpty();
    }

    // ---------------------------------------------------------------- auth

    public JSONObject requestDeviceCode() throws ApiException {
        Map<String, String> form = new HashMap<>();
        form.put("client_id", OAUTH_CLIENT_ID);
        form.put("device_id", "mashiro-mobile-client");
        form.put("device_name", "Mashiro Mobile");
        return requestForm("https://oauth.yandex.ru/device/code", form, false);
    }

    /** Returns an access token, or null while authorization is still pending. */
    public String pollDeviceToken(String deviceCode) throws ApiException {
        Map<String, String> form = new HashMap<>();
        form.put("grant_type", "device_code");
        form.put("code", deviceCode);
        form.put("client_id", OAUTH_CLIENT_ID);
        form.put("client_secret", OAUTH_CLIENT_SECRET);
        try {
            JSONObject o = requestForm("https://oauth.yandex.ru/token", form, false);
            String value = o.optString("access_token", "");
            return value.isEmpty() ? null : value;
        } catch (ApiException e) {
            if (e.getMessage() != null && e.getMessage().contains("authorization_pending")) return null;
            if (e.getMessage() != null && e.getMessage().contains("slow_down")) return null;
            throw e;
        }
    }

    // ------------------------------------------------------------- account

    public Models.Profile profile() throws ApiException {
        JSONObject result = getObject("/account/status");
        JSONObject acc = result.optJSONObject("account");
        Models.Profile p = new Models.Profile();
        if (acc != null) {
            p.uid = acc.optLong("uid", uid);
            if (p.uid > 0) uid = p.uid;
            p.login = acc.optString("login", "");
            p.name = acc.optString("displayName", "");
            if (p.name.isEmpty()) p.name = acc.optString("fullName", p.login);
            String avatar = acc.optString("avatarUrl", "");
            if (avatar.isEmpty()) {
                String id = acc.optString("avatarId", "");
                if (!id.isEmpty()) avatar = "https://avatars.yandex.net/get-yapic/" + id + "/islands-200";
            }
            p.avatarUrl = normalizeUrl(avatar);
        }
        JSONObject plus = result.optJSONObject("plus");
        p.hasPlus = plus != null && plus.optBoolean("hasPlus", false);
        return p;
    }

    // ----------------------------------------------------------- playlists

    public List<Models.Playlist> playlists() throws ApiException {
        JSONArray arr = getArray("/users/" + uid + "/playlists/list");
        List<Models.Playlist> out = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            Models.Playlist p = playlistBrief(o);
            if (p != null) out.add(p);
        }
        return out;
    }

    public Models.Playlist playlist(long kind) throws ApiException {
        return playlistOf(uid, kind);
    }

    public Models.Playlist playlistOf(long ownerUid, long kind) throws ApiException {
        long targetUid = ownerUid > 0 ? ownerUid : uid;
        JSONObject o = getObject("/users/" + targetUid + "/playlists/" + kind);
        Models.Playlist p = new Models.Playlist();
        p.kind = o.optLong("kind", kind);
        p.title = o.optString("title", "Плейлист");
        p.trackCount = o.optInt("trackCount", 0);
        p.description = o.optString("description", "");
        p.isPublic = !"private".equalsIgnoreCase(o.optString("visibility", "public"));
        JSONObject cover = o.optJSONObject("cover");
        p.coverUrl = coverUrl(cover != null ? cover.optString("uri", "") : o.optString("ogImage", ""), "600x600");
        JSONObject owner = o.optJSONObject("owner");
        p.ownerUid = owner != null ? owner.optLong("uid", targetUid) : targetUid;
        JSONArray tracks = o.optJSONArray("tracks");
        if (tracks != null) {
            for (int i = 0; i < tracks.length(); i++) {
                JSONObject wrap = tracks.optJSONObject(i);
                if (wrap == null) continue;
                JSONObject t = wrap.optJSONObject("track");
                if (t == null) continue;
                Models.Track track = mapTrack(t);
                if (track != null) p.tracks.add(track);
            }
        }
        return p;
    }

    public Models.Playlist createPlaylist(String title, boolean isPublic) throws ApiException {
        Map<String, String> form = new HashMap<>();
        form.put("title", title.trim());
        form.put("visibility", isPublic ? "public" : "private");
        JSONObject o = postForm("/users/" + uid + "/playlists/create", form);
        Models.Playlist p = new Models.Playlist();
        p.kind = o.optLong("kind", 0);
        p.title = o.optString("title", title);
        p.trackCount = 0;
        p.isPublic = isPublic;
        return p;
    }

    public void renamePlaylist(long kind, String title) throws ApiException {
        Map<String, String> form = new HashMap<>();
        form.put("value", title.trim());
        postForm("/users/" + uid + "/playlists/" + kind + "/name", form);
    }

    public void setPlaylistVisibility(long kind, boolean isPublic) throws ApiException {
        Map<String, String> form = new HashMap<>();
        form.put("value", isPublic ? "public" : "private");
        postForm("/users/" + uid + "/playlists/" + kind + "/visibility", form);
    }

    public void deletePlaylist(long kind) throws ApiException {
        postForm("/users/" + uid + "/playlists/" + kind + "/delete", new HashMap<>());
    }

    public void addTrackToPlaylist(long kind, Models.Track track) throws ApiException {
        for (int attempt = 0; attempt < 3; attempt++) {
            long revision = playlistRevision(kind);
            JSONArray diff = new JSONArray();
            JSONObject op = new JSONObject();
            JSONArray items = new JSONArray();
            try {
                op.put("op", "insert");
                op.put("at", 0);
                JSONObject item = new JSONObject();
                item.put("id", track.id);
                item.put("albumId", track.albumId);
                items.put(item);
                op.put("tracks", items);
                diff.put(op);
            } catch (Exception e) {
                throw new ApiException(0, "Не удалось собрать запрос");
            }
            Map<String, String> form = new HashMap<>();
            form.put("diff", diff.toString());
            form.put("revision", String.valueOf(revision));
            try {
                postFormRaw("/users/" + uid + "/playlists/" + kind + "/change-relative", form);
                return;
            } catch (ApiException e) {
                if (e.code != 412 && e.code != 409 && e.code < 500) throw e;
            }
        }
        throw new ApiException(0, "Плейлист изменился на сервере, попробуйте снова");
    }

    public void removeTrackFromPlaylist(long kind, int index, Models.Track track) throws ApiException {
        for (int attempt = 0; attempt < 3; attempt++) {
            long revision = playlistRevision(kind);
            JSONArray diff = new JSONArray();
            JSONObject op = new JSONObject();
            try {
                op.put("op", "delete");
                op.put("from", index);
                op.put("to", index + 1);
                JSONObject item = new JSONObject();
                item.put("id", track.id);
                item.put("albumId", track.albumId);
                JSONArray items = new JSONArray();
                items.put(item);
                op.put("tracks", items);
                diff.put(op);
            } catch (Exception e) {
                throw new ApiException(0, "Не удалось собрать запрос");
            }
            Map<String, String> form = new HashMap<>();
            form.put("diff", diff.toString());
            form.put("revision", String.valueOf(revision));
            try {
                postFormRaw("/users/" + uid + "/playlists/" + kind + "/change-relative", form);
                return;
            } catch (ApiException e) {
                if (e.code != 412 && e.code != 409 && e.code < 500) throw e;
            }
        }
        throw new ApiException(0, "Плейлист изменился на сервере, попробуйте снова");
    }

    private long playlistRevision(long kind) throws ApiException {
        JSONObject o = getObject("/users/" + uid + "/playlists/" + kind);
        return o.optLong("revision", 0);
    }

    // --------------------------------------------------------------- likes

    public Set<String> likedIds() throws ApiException {
        JSONObject result = getObject("/users/" + uid + "/likes/tracks");
        Set<String> ids = new LinkedHashSet<>();
        JSONObject library = result.optJSONObject("library");
        JSONArray arr = library != null ? library.optJSONArray("tracks") : null;
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) ids.add(o.optString("id", ""));
            }
        }
        return ids;
    }

    public Set<String> dislikedIds() throws ApiException {
        JSONObject result = getObject("/users/" + uid + "/dislikes/tracks");
        Set<String> ids = new LinkedHashSet<>();
        JSONObject library = result.optJSONObject("library");
        JSONArray arr = library != null ? library.optJSONArray("tracks") : null;
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) ids.add(o.optString("id", ""));
            }
        }
        return ids;
    }

    public List<Models.Track> likedTracks() throws ApiException {
        Set<String> ids = likedIds();
        List<Models.Track> out = new ArrayList<>();
        if (ids.isEmpty()) return out;
        List<String> all = new ArrayList<>(ids);
        for (int from = 0; from < all.size(); from += 100) {
            int to = Math.min(all.size(), from + 100);
            String joined = String.join(",", all.subList(from, to));
            JSONArray arr = getArray("/tracks?track-ids=" + enc(joined));
            Map<String, Models.Track> found = new HashMap<>();
            for (int i = 0; i < arr.length(); i++) {
                Models.Track t = mapTrack(arr.optJSONObject(i));
                if (t != null) found.put(t.id, t);
            }
            for (int i = from; i < to; i++) {
                Models.Track t = found.get(all.get(i));
                if (t != null) out.add(t);
            }
        }
        return out;
    }

    public void setLike(String trackId, boolean dislike, boolean remove) throws ApiException {
        String bucket = dislike ? "dislikes" : "likes";
        String action = remove ? "remove" : "add-multiple";
        Map<String, String> form = new HashMap<>();
        form.put("track-ids", trackId);
        try {
            postForm("/users/" + uid + "/" + bucket + "/tracks/" + action, form);
        } catch (ApiException e) {
            if (remove) throw e;
            // Older deployments only know /add.
            postForm("/users/" + uid + "/" + bucket + "/tracks/add", form);
        }
    }

    // ------------------------------------------------------------- catalog

    public Models.Album album(String id) throws ApiException {
        JSONObject o = getObject("/albums/" + id + "/with-tracks");
        Models.Album a = new Models.Album();
        a.id = id;
        a.title = o.optString("title", "Альбом");
        a.year = o.optInt("year", 0);
        a.genre = o.optString("genre", "");
        JSONObject cover = o.optJSONObject("cover");
        a.coverUrl = coverUrl(o.optString("coverUri", cover != null ? cover.optString("uri", "") : ""), "600x600");
        JSONArray artists = o.optJSONArray("artists");
        if (artists != null) {
            for (int i = 0; i < artists.length(); i++) {
                Models.Artist ar = mapArtist(artists.optJSONObject(i));
                if (ar != null) a.artists.add(ar);
            }
        }
        JSONArray volumes = o.optJSONArray("volumes");
        if (volumes != null) {
            for (int v = 0; v < volumes.length(); v++) {
                JSONArray vol = volumes.optJSONArray(v);
                if (vol == null) continue;
                for (int i = 0; i < vol.length(); i++) {
                    Models.Track t = mapTrack(vol.optJSONObject(i));
                    if (t != null) a.tracks.add(t);
                }
            }
        }
        return a;
    }

    public Models.ArtistPage artist(String id) throws ApiException {
        JSONObject result = getObject("/artists/" + id + "/brief-info");
        Models.ArtistPage page = new Models.ArtistPage();
        JSONObject artist = result.optJSONObject("artist");
        if (artist == null) artist = new JSONObject();
        page.artist.id = id;
        page.artist.name = artist.optString("name", "Артист");
        JSONObject cover = artist.optJSONObject("cover");
        page.artist.coverUrl = coverUrl(cover != null ? cover.optString("uri", "") : "", "600x600");
        JSONObject desc = artist.optJSONObject("description");
        if (desc != null) page.description = desc.optString("text", "");
        if (page.description.isEmpty()) page.description = artist.optString("description", "");
        JSONObject stats = result.optJSONObject("stats");
        if (stats != null) {
            page.listeners = stats.optLong("lastMonthListeners", 0);
            page.likes = stats.optLong("likesCount", 0);
        }
        JSONObject counts = artist.optJSONObject("counts");
        if (counts != null) page.tracksCount = counts.optLong("tracks", 0);
        JSONArray pop = result.optJSONArray("popularTracks");
        if (pop != null) {
            for (int i = 0; i < pop.length() && i < 40; i++) {
                Models.Track t = mapTrack(pop.optJSONObject(i));
                if (t != null) page.tracks.add(t);
            }
        }
        JSONArray albums = result.optJSONArray("albums");
        if (albums == null) albums = result.optJSONArray("alsoAlbums");
        if (albums != null) {
            for (int i = 0; i < albums.length(); i++) {
                Models.Album a = mapAlbumBrief(albums.optJSONObject(i));
                if (a != null) page.albums.add(a);
            }
        }
        return page;
    }

    public List<Models.Track> artistTracks(String id) throws ApiException {
        List<Models.Track> out = new ArrayList<>();
        Map<String, Boolean> seen = new HashMap<>();
        for (int page = 0; page < 10; page++) {
            JSONObject result = getObject("/artists/" + id + "/tracks?page=" + page + "&page-size=100");
            JSONArray arr = result.optJSONArray("tracks");
            if (arr == null) break;
            for (int i = 0; i < arr.length(); i++) {
                Models.Track t = mapTrack(arr.optJSONObject(i));
                if (t != null && !seen.containsKey(t.id)) {
                    seen.put(t.id, true);
                    out.add(t);
                }
            }
            if (arr.length() < 100) break;
        }
        return out;
    }

    public Models.SearchResult search(String text) throws ApiException {
        JSONObject r = getObject("/search?text=" + enc(text) + "&type=all&page=0&nocorrect=false");
        Models.SearchResult s = new Models.SearchResult();
        s.best = r.optString("text", "");
        JSONObject tracks = r.optJSONObject("tracks");
        JSONArray tr = tracks != null ? tracks.optJSONArray("results") : null;
        if (tr != null) {
            for (int i = 0; i < tr.length(); i++) {
                Models.Track t = mapTrack(tr.optJSONObject(i));
                if (t != null) s.tracks.add(t);
            }
        }
        JSONObject artists = r.optJSONObject("artists");
        JSONArray ar = artists != null ? artists.optJSONArray("results") : null;
        if (ar != null) {
            for (int i = 0; i < ar.length() && i < 15; i++) {
                Models.Artist a = mapArtist(ar.optJSONObject(i));
                if (a != null) s.artists.add(a);
            }
        }
        JSONObject albums = r.optJSONObject("albums");
        JSONArray al = albums != null ? albums.optJSONArray("results") : null;
        if (al != null) {
            for (int i = 0; i < al.length() && i < 15; i++) {
                Models.Album a = mapAlbumBrief(al.optJSONObject(i));
                if (a != null) s.albums.add(a);
            }
        }
        JSONObject playlists = r.optJSONObject("playlists");
        JSONArray pl = playlists != null ? playlists.optJSONArray("results") : null;
        if (pl != null) {
            for (int i = 0; i < pl.length() && i < 10; i++) {
                Models.Playlist p = playlistBrief(pl.optJSONObject(i));
                if (p != null) s.playlists.add(p);
            }
        }
        return s;
    }

    public List<String> suggest(String part) throws ApiException {
        JSONObject r = getObject("/search/suggest?part=" + enc(part));
        List<String> out = new ArrayList<>();
        String best = r.optString("best", "");
        if (!best.isEmpty()) out.add(best);
        JSONArray arr = r.optJSONArray("suggestions");
        if (arr != null) {
            for (int i = 0; i < arr.length() && out.size() < 8; i++) {
                Object item = arr.opt(i);
                String value = item instanceof String ? (String) item
                        : item instanceof JSONObject ? ((JSONObject) item).optString("text", "") : "";
                if (!value.trim().isEmpty() && !out.contains(value)) out.add(value);
            }
        }
        return out;
    }

    public Models.Section chart() throws ApiException {
        JSONObject r = getObject("/landing3/chart");
        Models.Section s = new Models.Section();
        s.kind = "chart";
        s.title = r.optString("title", "Чарт");
        s.subtitle = r.optString("chartDescription", "Сейчас в чарте");
        JSONObject chart = r.optJSONObject("chart");
        JSONArray tracks = chart != null ? chart.optJSONArray("tracks") : null;
        if (tracks != null) {
            for (int i = 0; i < tracks.length() && i < 50; i++) {
                JSONObject wrap = tracks.optJSONObject(i);
                if (wrap == null) continue;
                Models.Track t = mapTrack(wrap.optJSONObject("track"));
                if (t != null) s.tracks.add(t);
            }
        }
        return s;
    }

    public List<Models.Track> newReleases() throws ApiException {
        List<Models.Track> out = new ArrayList<>();
        JSONObject r = getObject("/landing3/new-releases");
        JSONArray arr = r.optJSONArray("newReleases");
        if (arr == null) arr = r.optJSONArray("albums");
        if (arr == null) arr = r.optJSONArray("items");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                JSONObject album = o.optJSONObject("album");
                if (album != null) o = album;
                Models.Album a = mapAlbumBrief(o);
                if (a == null) continue;
                Models.Track placeholder = new Models.Track();
                placeholder.id = "";
                placeholder.title = a.title;
                placeholder.albumId = a.id;
                placeholder.albumTitle = a.title;
                placeholder.coverUrl = a.coverUrl;
                placeholder.coverLarge = a.coverUrl;
                placeholder.available = false;
                out.add(placeholder);
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- radio

    public Models.Wave wave(String station, String sessionId, List<String> queue) throws ApiException {
        JSONObject body = new JSONObject();
        try {
            if (sessionId == null || sessionId.isEmpty()) {
                JSONArray seeds = new JSONArray();
                seeds.put(station);
                body.put("seeds", seeds);
                body.put("includeTracksInResponse", true);
            } else {
                JSONArray q = new JSONArray();
                if (queue != null) {
                    for (String id : queue) q.put(id);
                }
                body.put("queue", q);
            }
        } catch (Exception e) {
            throw new ApiException(0, "Не удалось собрать запрос");
        }
        String path = sessionId == null || sessionId.isEmpty()
                ? "/rotor/session/new"
                : "/rotor/session/" + sessionId + "/tracks";
        JSONObject r = postJson(path, body);
        Models.Wave wave = new Models.Wave();
        wave.stationId = station;
        wave.sessionId = r.optString("radioSessionId", sessionId == null ? "" : sessionId);
        wave.batchId = r.optString("batchId", "");
        JSONArray seq = r.optJSONArray("sequence");
        if (seq != null) {
            for (int i = 0; i < seq.length(); i++) {
                JSONObject wrap = seq.optJSONObject(i);
                if (wrap == null) continue;
                Models.Track t = mapTrack(wrap.optJSONObject("track"));
                if (t != null) wave.tracks.add(t);
            }
        }
        return wave;
    }

    public void waveFeedback(String station, String type, String trackId, String batchId, double seconds) {
        try {
            JSONObject body = new JSONObject();
            body.put("type", type);
            body.put("from", "mashiro-mobile");
            if (trackId != null && !trackId.isEmpty()) body.put("trackId", trackId);
            if (seconds > 0) body.put("totalPlayedSeconds", seconds);
            String path = "/rotor/station/" + station + "/feedback";
            if (batchId != null && !batchId.isEmpty()) path += "?batch-id=" + enc(batchId);
            postJson(path, body);
        } catch (Exception ignored) {}
    }

    public List<Models.Artist> stations() throws ApiException {
        JSONObject r = getObject("/rotor/stations/dashboard");
        List<Models.Artist> out = new ArrayList<>();
        JSONArray arr = r.optJSONArray("stations");
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject item = arr.optJSONObject(i);
            if (item == null) continue;
            JSONObject st = item.optJSONObject("station");
            if (st == null) st = item;
            JSONObject id = st.optJSONObject("id");
            if (id == null) continue;
            String type = id.optString("type", "");
            String tag = id.optString("tag", "");
            if (type.isEmpty() || tag.isEmpty()) continue;
            Models.Artist a = new Models.Artist();
            a.id = type + ":" + tag;
            a.name = st.optString("name", a.id);
            JSONObject icon = st.optJSONObject("icon");
            if (icon != null) a.coverUrl = coverUrl(icon.optString("imageUrl", ""), "200x200");
            out.add(a);
        }
        return out;
    }

    // -------------------------------------------------------------- streams

    public String streamUrl(String trackId, String quality) throws ApiException {
        String key = trackId + ":" + quality;
        synchronized (streamCache) {
            CachedUrl cached = streamCache.get(key);
            if (cached != null && System.currentTimeMillis() - cached.at < 220_000L) return cached.url;
        }
        String url = resolveStream(trackId, quality);
        synchronized (streamCache) {
            streamCache.put(key, new CachedUrl(url));
            if (streamCache.size() > 24) {
                String oldest = null;
                long at = Long.MAX_VALUE;
                for (Map.Entry<String, CachedUrl> e : streamCache.entrySet()) {
                    if (e.getValue().at < at) {
                        at = e.getValue().at;
                        oldest = e.getKey();
                    }
                }
                if (oldest != null) streamCache.remove(oldest);
            }
        }
        return url;
    }

    private String resolveStream(String trackId, String quality) throws ApiException {
        JSONArray infos = getArray("/tracks/" + trackId + "/download-info");
        JSONObject best = null;
        long bestScore = Long.MIN_VALUE;
        long bestBitrate = -1;
        for (int i = 0; i < infos.length(); i++) {
            JSONObject o = infos.optJSONObject(i);
            if (o == null) continue;
            String codec = o.optString("codec", "").toLowerCase(Locale.ROOT);
            long br = o.optLong("bitrateInKbps", 0);
            boolean lossless = codec.contains("flac") || codec.contains("alac");
            long score = br + (lossless ? 3000 : codec.contains("aac") ? 1000 : 0);
            boolean acceptable;
            if ("low".equals(quality)) acceptable = br <= 128 && !lossless;
            else if ("normal".equals(quality)) acceptable = br <= 256 && !lossless;
            else if ("high".equals(quality)) acceptable = !lossless;
            else acceptable = true;
            if (acceptable && score > bestScore) {
                bestScore = score;
                best = o;
            }
            if (br > bestBitrate) bestBitrate = br;
        }
        if (best == null && infos.length() > 0) best = infos.optJSONObject(0);
        if (best == null) throw new ApiException(0, "Нет доступных потоков для этого трека");
        String infoUrl = best.optString("downloadInfoUrl", "");
        if (infoUrl.startsWith("//")) infoUrl = "https:" + infoUrl;
        if (infoUrl.isEmpty()) throw new ApiException(0, "Нет ссылки на поток");
        String xml = httpGetText(infoUrl, true);
        String host = between(xml, "<host>", "</host>");
        String path = between(xml, "<path>", "</path>");
        String ts = between(xml, "<ts>", "</ts>");
        String s = between(xml, "<s>", "</s>");
        if (host == null || path == null || ts == null || s == null) {
            throw new ApiException(0, "CDN вернул некорректный ответ");
        }
        String raw = SIGN_SALT + path.substring(1) + s;
        String sign = md5(raw);
        return "https://" + host + "/get-mp3/" + sign + "/" + ts + path;
    }

    public byte[] downloadBytes(String url) throws ApiException {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestProperty("X-Yandex-Music-Client", MUSIC_CLIENT);
            c.setConnectTimeout(15000);
            c.setReadTimeout(30000);
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) throw new ApiException(code, "CDN ответил " + code);
            return readAll(c.getInputStream());
        } catch (IOException e) {
            throw new ApiException(0, "Сеть: " + e.getMessage());
        } finally {
            if (c != null) c.disconnect();
        }
    }

    // --------------------------------------------------------------- lyrics

    /** Looks for lyrics in several open catalogues and returns the best match. */
    public Models.Lyrics lyrics(Models.Track track) {
        Models.Lyrics empty = new Models.Lyrics();
        if (track == null) return empty;
        empty.trackId = track.id;

        String artist = track.artists.isEmpty() ? "" : track.artists.get(0).name;
        String title = cleanTitle(track.title);
        long duration = track.durationMs / 1000L;

        List<Models.Lyrics> found = new ArrayList<>();

        // LRCLIB — community catalogue, usually timed.
        Models.Lyrics lrc = fromLrclib(artist, title, duration);
        if (lrc != null) found.add(lrc);

        // Netease Cloud Music — plenty of Russian tracks with timed LRC.
        Models.Lyrics netease = fromNetease(artist, title, duration);
        if (netease != null) found.add(netease);

        // Yandex' own supplement — official, but usually plain text.
        Models.Lyrics yandex = fromYandex(track);
        if (yandex != null) found.add(yandex);

        // lyrics.ovh — plain text fallback.
        Models.Lyrics ovh = fromLyricsOvh(artist, title);
        if (ovh != null) found.add(ovh);

        Models.Lyrics best = null;
        for (Models.Lyrics candidate : found) {
            if (best == null) {
                best = candidate;
            } else if (!best.synced && candidate.synced) {
                best = candidate;
            }
        }
        return best == null ? empty : best;
    }

    private Models.Lyrics fromLrclib(String artist, String title, long duration) {
        if (title.isEmpty()) return null;
        try {
            String url = "https://lrclib.net/api/get?artist_name=" + enc(artist)
                    + "&track_name=" + enc(title)
                    + "&duration=" + duration;
            JSONObject o = fetchObject(url, null);
            if (o == null && !artist.isEmpty()) {
                o = pickLrclib(fetchArray("https://lrclib.net/api/search?artist_name=" + enc(artist)
                        + "&track_name=" + enc(title), null), duration);
                if (o == null) {
                    o = pickLrclib(fetchArray("https://lrclib.net/api/search?q="
                            + enc(artist + " " + title), null), duration);
                }
            }
            if (o == null) return null;
            String text = o.optString("syncedLyrics", "");
            if (text.trim().isEmpty()) text = o.optString("plainLyrics", "");
            if (text.trim().isEmpty()) return null;
            Models.Lyrics lyrics = new Models.Lyrics();
            parseLrc(lyrics, text);
            if (lyrics.lines.isEmpty()) return null;
            lyrics.source = "LRCLIB";
            return lyrics;
        } catch (Exception e) {
            return null;
        }
    }

    private JSONObject pickLrclib(JSONArray list, long duration) {
        if (list == null || list.length() == 0) return null;
        JSONObject fallback = null;
        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.optJSONObject(i);
            if (item == null) continue;
            boolean synced = !item.optString("syncedLyrics", "").trim().isEmpty();
            long d = Math.round(item.optDouble("duration", 0));
            boolean durationOk = duration <= 0 || d <= 0 || Math.abs(d - duration) <= 6;
            if (synced && durationOk) return item;
            if (fallback == null) fallback = item;
        }
        return fallback;
    }

    private Models.Lyrics fromNetease(String artist, String title, long duration) {
        if (title.isEmpty()) return null;
        try {
            String query = (artist + " " + title).trim();
            JSONObject search = fetchObject("https://music.163.com/api/search/get/web?s=" + enc(query)
                    + "&type=1&offset=0&total=true&limit=8", "https://music.163.com/");
            if (search == null) return null;
            JSONObject result = search.optJSONObject("result");
            JSONArray songs = result == null ? null : result.optJSONArray("songs");
            if (songs == null || songs.length() == 0) return null;
            long bestId = 0;
            for (int i = 0; i < songs.length(); i++) {
                JSONObject song = songs.optJSONObject(i);
                if (song == null) continue;
                long id = song.optLong("id", 0);
                if (id <= 0) continue;
                long d = song.optLong("duration", 0) / 1000L;
                if (duration > 0 && d > 0 && Math.abs(d - duration) > 10) continue;
                bestId = id;
                break;
            }
            if (bestId == 0) {
                JSONObject first = songs.optJSONObject(0);
                bestId = first == null ? 0 : first.optLong("id", 0);
            }
            if (bestId == 0) return null;
            JSONObject lyric = fetchObject("https://music.163.com/api/song/lyric?id=" + bestId
                    + "&lv=-1&kv=-1&tv=-1", "https://music.163.com/");
            if (lyric == null) return null;
            JSONObject lrc = lyric.optJSONObject("lrc");
            String text = lrc == null ? "" : lrc.optString("lyric", "");
            if (text.trim().isEmpty()) {
                JSONObject tlyric = lyric.optJSONObject("tlyric");
                text = tlyric == null ? "" : tlyric.optString("lyric", "");
            }
            if (text.trim().isEmpty()) return null;
            Models.Lyrics lyrics = new Models.Lyrics();
            parseLrc(lyrics, text);
            if (lyrics.lines.isEmpty()) return null;
            lyrics.source = "Netease";
            return lyrics;
        } catch (Exception e) {
            return null;
        }
    }

    private Models.Lyrics fromYandex(Models.Track track) {
        if (!hasToken()) return null;
        try {
            JSONObject r = getObject("/tracks/" + track.id + "/supplement");
            JSONObject node = r.optJSONObject("lyrics");
            if (node == null) return null;
            String text = node.optString("fullLyrics", "");
            if (text.isEmpty()) text = node.optString("lyrics", "");
            if (text.isEmpty()) text = node.optString("text", "");
            if (text.trim().isEmpty()) return null;
            Models.Lyrics lyrics = new Models.Lyrics();
            parseLrc(lyrics, text);
            if (lyrics.lines.isEmpty()) return null;
            lyrics.source = "Яндекс";
            String authors = node.optString("textAuthor", "");
            for (String w : authors.split(",")) {
                if (!w.trim().isEmpty()) lyrics.writers.add(w.trim());
            }
            return lyrics;
        } catch (Exception e) {
            return null;
        }
    }

    private Models.Lyrics fromLyricsOvh(String artist, String title) {
        if (artist.isEmpty() || title.isEmpty()) return null;
        try {
            JSONObject o = fetchObject("https://api.lyrics.ovh/v1/" + enc(artist) + "/" + enc(title), null);
            if (o == null) return null;
            String text = o.optString("lyrics", "");
            if (text.trim().isEmpty()) return null;
            Models.Lyrics lyrics = new Models.Lyrics();
            parseLrc(lyrics, text);
            if (lyrics.lines.isEmpty()) return null;
            lyrics.source = "lyrics.ovh";
            return lyrics;
        } catch (Exception e) {
            return null;
        }
    }

    private JSONObject fetchObject(String url, String referer) {
        try {
            return new JSONObject(httpGetText(url, false, referer));
        } catch (Exception e) {
            return null;
        }
    }

    private JSONArray fetchArray(String url, String referer) {
        try {
            return new JSONArray(httpGetText(url, false, referer));
        } catch (Exception e) {
            return null;
        }
    }

    /** Strips featuring credits, remaster notes and brackets so catalogues match better. */
    private static String cleanTitle(String raw) {
        if (raw == null) return "";
        String t = raw.replaceAll("\\([^)]*\\)", " ").replaceAll("\\[[^\\]]*\\]", " ");
        t = t.replaceAll("(?i)\\s*-\\s*(remaster(ed)?|live|acoustic|radio edit|single version|version).*$", " ");
        t = t.replaceAll("(?i)\\s+(feat\\.?|ft\\.?|при уч\\.?)\\s+.*$", " ");
        return t.replaceAll("\\s+", " ").trim();
    }

    private void parseLrc(Models.Lyrics lyrics, String text) {
        Pattern tag = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?\\]");
        String[] rawLines = text.split("\\r?\\n");
        boolean anyTime = false;
        List<Models.LyricLine> parsed = new ArrayList<>();
        for (String raw : rawLines) {
            String line = raw;
            Matcher m = tag.matcher(line);
            List<Long> times = new ArrayList<>();
            int cut = 0;
            while (m.find()) {
                int mins = Integer.parseInt(m.group(1));
                int secs = Integer.parseInt(m.group(2));
                String frac = m.group(3);
                long ms = mins * 60_000L + secs * 1000L;
                if (frac != null && !frac.isEmpty()) {
                    double value = Double.parseDouble("0." + frac);
                    ms += (long) (value * 1000);
                }
                times.add(ms);
                cut = m.end();
            }
            String content = line.substring(Math.min(cut, line.length())).trim();
            if (times.isEmpty()) {
                if (!content.isEmpty() && !content.startsWith("[")) {
                    parsed.add(new Models.LyricLine(parsed.isEmpty() ? 0 : parsed.get(parsed.size() - 1).timeMs, content));
                }
            } else {
                anyTime = true;
                for (long ts : times) parsed.add(new Models.LyricLine(ts, content));
            }
        }
        if (!parsed.isEmpty()) {
            parsed.sort((a, b) -> Long.compare(a.timeMs, b.timeMs));
            lyrics.lines = parsed;
            lyrics.synced = anyTime;
        }
    }

    // ------------------------------------------------------------ transport

    private JSONObject getObject(String path) throws ApiException {
        Object result = request("GET", BASE + path, null, null, true);
        if (result instanceof JSONObject) return (JSONObject) result;
        throw new ApiException(0, "Неожиданный ответ API");
    }

    private JSONArray getArray(String path) throws ApiException {
        Object result = request("GET", BASE + path, null, null, true);
        if (result instanceof JSONArray) return (JSONArray) result;
        if (result instanceof JSONObject) {
            JSONObject o = (JSONObject) result;
            JSONArray arr = o.optJSONArray("items");
            if (arr != null) return arr;
        }
        throw new ApiException(0, "Неожиданный ответ API");
    }

    private JSONObject postForm(String path, Map<String, String> form) throws ApiException {
        JSONObject result = postFormRaw(path, form);
        return result;
    }

    private JSONObject postFormRaw(String path, Map<String, String> form) throws ApiException {
        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> e : form.entrySet()) {
            if (body.length() > 0) body.append('&');
            body.append(enc(e.getKey())).append('=').append(enc(e.getValue()));
        }
        Object result = request("POST", BASE + path, body.toString(), "application/x-www-form-urlencoded", true);
        if (result instanceof JSONObject) return (JSONObject) result;
        return new JSONObject();
    }

    private JSONObject postJson(String path, JSONObject body) throws ApiException {
        Object result = request("POST", BASE + path, body.toString(), "application/json", true);
        if (result instanceof JSONObject) return (JSONObject) result;
        return new JSONObject();
    }

    private JSONObject requestForm(String url, Map<String, String> form, boolean auth) throws ApiException {
        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> e : form.entrySet()) {
            if (body.length() > 0) body.append('&');
            body.append(enc(e.getKey())).append('=').append(enc(e.getValue()));
        }
        Object result = request("POST", url, body.toString(), "application/x-www-form-urlencoded", auth);
        if (result instanceof JSONObject) return (JSONObject) result;
        return new JSONObject();
    }

    private Object request(String method, String url, String body, String contentType, boolean useAuth) throws ApiException {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod(method);
            c.setConnectTimeout(20000);
            c.setReadTimeout(30000);
            c.setRequestProperty("Accept", "application/json");
            c.setRequestProperty("Accept-Language", "ru");
            c.setRequestProperty("Accept-Encoding", "gzip");
            c.setRequestProperty("User-Agent", "Mashiro/1.2 (Android)");
            c.setRequestProperty("X-Yandex-Music-Client", MUSIC_CLIENT);
            if (useAuth && hasToken()) c.setRequestProperty("Authorization", "OAuth " + token);
            if (body != null) {
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", contentType == null ? "application/json" : contentType);
                OutputStream os = c.getOutputStream();
                os.write(body.getBytes(StandardCharsets.UTF_8));
                os.close();
            }
            int code = c.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
            String text = stream == null ? "" : new String(readAll(stream), StandardCharsets.UTF_8);
            if (code < 200 || code >= 300) {
                String message = errorMessage(text, code);
                throw new ApiException(code, message);
            }
            if (text.trim().isEmpty()) return new JSONObject();
            Object parsed;
            try {
                parsed = text.trim().startsWith("[") ? new JSONArray(text) : new JSONObject(text);
            } catch (Exception e) {
                throw new ApiException(0, "Некорректный ответ сервера");
            }
            if (parsed instanceof JSONObject) {
                JSONObject o = (JSONObject) parsed;
                Object result = o.opt("result");
                if (result != null && result != JSONObject.NULL) return result;
            }
            return parsed;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(0, "Сеть: " + safeMessage(e));
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private String httpGetText(String url, boolean auth) throws ApiException {
        return httpGetText(url, auth, null);
    }

    private String httpGetText(String url, boolean auth, String referer) throws ApiException {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(25000);
            c.setRequestProperty("Accept", "application/json, text/plain, */*");
            c.setRequestProperty("Accept-Encoding", "gzip");
            c.setRequestProperty("Accept-Language", "ru");
            c.setRequestProperty("User-Agent", "Mashiro/1.2 (Android)");
            c.setRequestProperty("X-Yandex-Music-Client", MUSIC_CLIENT);
            if (referer != null && !referer.isEmpty()) c.setRequestProperty("Referer", referer);
            if (auth && hasToken()) c.setRequestProperty("Authorization", "OAuth " + token);
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) {
                InputStream err = c.getErrorStream();
                String text = err == null ? "" : new String(readAll(err), StandardCharsets.UTF_8);
                throw new ApiException(code, errorMessage(text, code));
            }
            return new String(readAll(c.getInputStream()), StandardCharsets.UTF_8);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(0, "Сеть: " + safeMessage(e));
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static String errorMessage(String text, int code) {
        String message = "";
        try {
            JSONObject o = new JSONObject(text);
            message = o.optString("message", "");
            if (message.isEmpty()) message = o.optString("error_description", "");
            if (message.isEmpty()) message = o.optString("error", "");
        } catch (Exception ignored) {}
        if (code == 401) return message.isEmpty() ? "Токен недействителен — войдите снова" : "Недействительный токен";
        if (code == 403) return "Нужна активная подписка Яндекс Плюс";
        if (code == 404) return "Не найдено";
        if (message.isEmpty()) return "Ошибка API: " + code;
        return message;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
        in.close();
        byte[] raw = out.toByteArray();
        if (raw.length > 2 && (raw[0] & 0xff) == 0x1f && (raw[1] & 0xff) == 0x8b) {
            GZIPInputStream gzip = new GZIPInputStream(new java.io.ByteArrayInputStream(raw));
            ByteArrayOutputStream decoded = new ByteArrayOutputStream();
            while ((read = gzip.read(buffer)) != -1) decoded.write(buffer, 0, read);
            gzip.close();
            return decoded.toByteArray();
        }
        return raw;
    }

    // --------------------------------------------------------------- mapping

    public static Models.Track mapTrack(JSONObject o) {
        if (o == null) return null;
        Models.Track t = new Models.Track();
        t.id = o.optString("id", "");
        if (t.id.isEmpty()) return null;
        t.title = o.optString("title", "");
        t.durationMs = o.optLong("durationMs", 0);
        t.available = o.optBoolean("available", true);
        t.coverUrl = coverUrl(o.optString("coverUri", ""), "300x300");
        t.coverLarge = coverUrl(o.optString("coverUri", ""), "1000x1000");
        JSONArray artists = o.optJSONArray("artists");
        if (artists != null) {
            for (int i = 0; i < artists.length(); i++) {
                Models.Artist a = mapArtist(artists.optJSONObject(i));
                if (a != null) t.artists.add(a);
            }
        }
        JSONArray albums = o.optJSONArray("albums");
        if (albums != null && albums.length() > 0) {
            JSONObject album = albums.optJSONObject(0);
            if (album != null) {
                t.albumId = album.optString("id", "");
                t.albumTitle = album.optString("title", "");
                if (t.coverUrl.isEmpty()) t.coverUrl = coverUrl(album.optString("coverUri", ""), "300x300");
                if (t.coverLarge.isEmpty()) t.coverLarge = coverUrl(album.optString("coverUri", ""), "1000x1000");
            }
        }
        return t;
    }

    public static Models.Artist mapArtist(JSONObject o) {
        if (o == null) return null;
        Models.Artist a = new Models.Artist();
        a.id = o.optString("id", "");
        a.name = o.optString("name", "");
        JSONObject cover = o.optJSONObject("cover");
        a.coverUrl = coverUrl(cover != null ? cover.optString("uri", "") : o.optString("ogImage", ""), "300x300");
        return a;
    }

    public static Models.Album mapAlbumBrief(JSONObject o) {
        if (o == null) return null;
        Models.Album a = new Models.Album();
        a.id = o.optString("id", "");
        if (a.id.isEmpty()) return null;
        a.title = o.optString("title", "Альбом");
        a.year = o.optInt("year", 0);
        a.coverUrl = coverUrl(o.optString("coverUri", ""), "400x400");
        JSONArray artists = o.optJSONArray("artists");
        if (artists != null) {
            for (int i = 0; i < artists.length(); i++) {
                Models.Artist ar = mapArtist(artists.optJSONObject(i));
                if (ar != null) a.artists.add(ar);
            }
        }
        return a;
    }

    private static Models.Playlist playlistBrief(JSONObject o) {
        if (o == null) return null;
        Models.Playlist p = new Models.Playlist();
        p.kind = o.optLong("kind", 0);
        p.title = o.optString("title", "Без названия");
        p.trackCount = o.optInt("trackCount", 0);
        p.isPublic = !"private".equalsIgnoreCase(o.optString("visibility", "public"));
        p.description = o.optString("description", "");
        JSONObject cover = o.optJSONObject("cover");
        p.coverUrl = coverUrl(cover != null ? cover.optString("uri", "") : o.optString("ogImage", ""), "400x400");
        JSONObject owner = o.optJSONObject("owner");
        if (owner != null) p.ownerUid = owner.optLong("uid", 0);
        return p;
    }

    public static String coverUrl(String uri, String size) {
        if (uri == null || uri.trim().isEmpty()) return "";
        String value = uri.replace("%%", size).replace("%2F", "/");
        return normalizeUrl(value);
    }

    public static String normalizeUrl(String url) {
        if (url == null) return "";
        String value = url.trim();
        if (value.isEmpty()) return "";
        if (value.startsWith("//")) return "https:" + value;
        if (value.startsWith("http://") || value.startsWith("https://")) return value;
        return "https://" + value;
    }

    public static String enc(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return "";
        }
    }

    private static String between(String text, String start, String end) {
        int a = text.indexOf(start);
        if (a < 0) return null;
        a += start.length();
        int b = text.indexOf(end, a);
        if (b < 0) return null;
        return text.substring(a, b);
    }

    private static String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format(Locale.ROOT, "%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String safeMessage(Exception e) {
        String m = e.getMessage();
        return m == null ? e.getClass().getSimpleName() : m;
    }
}
