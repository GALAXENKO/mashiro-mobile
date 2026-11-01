package com.mashiro.mobile;

import android.content.Context;
import android.content.SharedPreferences;

import com.mashiro.mobile.api.Models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Local storage for the session, settings, likes, queue and listening statistics. */
public final class Store {
    private static final String FILE = "mashiro";
    private static SharedPreferences prefs;

    public static void init(Context context) {
        if (prefs == null) {
            prefs = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
        }
    }

    private static SharedPreferences p() {
        return prefs;
    }

    // ------------------------------------------------------------- session

    public static void saveSession(String token, Models.Profile profile) {
        try {
            JSONObject o = new JSONObject();
            o.put("token", token);
            o.put("uid", profile.uid);
            o.put("login", profile.login);
            o.put("name", profile.name);
            o.put("avatar", profile.avatarUrl);
            o.put("plus", profile.hasPlus);
            p().edit().putString("session", o.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static String token() {
        return session().optString("token", "");
    }

    public static long uid() {
        return session().optLong("uid", 0);
    }

    public static String name() {
        return session().optString("name", session().optString("login", "Слушатель"));
    }

    public static String login() {
        return session().optString("login", "");
    }

    public static String avatar() {
        return session().optString("avatar", "");
    }

    public static boolean hasPlus() {
        return session().optBoolean("plus", false);
    }

    public static JSONObject session() {
        try {
            return new JSONObject(p().getString("session", "{}"));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public static void setPlus(boolean plus) {
        try {
            JSONObject o = session();
            o.put("plus", plus);
            p().edit().putString("session", o.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static void logout() {
        p().edit()
                .remove("session")
                .remove("liked")
                .remove("disliked")
                .remove("queue")
                .remove("recent")
                .remove("stats")
                .apply();
        ImageLoader.clear();
    }

    // ------------------------------------------------------------ settings

    public static boolean isLight() {
        return "light".equals(p().getString("theme", "dark"));
    }

    public static void setLight(boolean light) {
        p().edit().putString("theme", light ? "light" : "dark").apply();
    }

    public static String accent() {
        return p().getString("accent", "#FFFFFF");
    }

    public static void setAccent(String accent) {
        p().edit().putString("accent", accent).apply();
    }

    public static String quality() {
        return p().getString("quality", "high");
    }

    public static void setQuality(String quality) {
        p().edit().putString("quality", quality).apply();
    }

    public static boolean reduceMotion() {
        return p().getBoolean("reduceMotion", false);
    }

    public static void setReduceMotion(boolean value) {
        p().edit().putBoolean("reduceMotion", value).apply();
    }

    // -------------------------------------------------------------- likes

    public static Set<String> liked() {
        return stringSet("liked");
    }

    public static Set<String> disliked() {
        return stringSet("disliked");
    }

    private static Set<String> stringSet(String key) {
        Set<String> out = new LinkedHashSet<>();
        try {
            JSONArray arr = new JSONArray(p().getString(key, "[]"));
            for (int i = 0; i < arr.length(); i++) out.add(arr.optString(i, ""));
        } catch (Exception ignored) {}
        return out;
    }

    private static void saveSet(String key, Set<String> values) {
        JSONArray arr = new JSONArray();
        for (String value : values) arr.put(value);
        p().edit().putString(key, arr.toString()).apply();
    }

    public static void saveLikedSet(Set<String> values) {
        saveSet("liked", values);
    }

    public static void saveDislikedSet(Set<String> values) {
        saveSet("disliked", values);
    }

    public static boolean isLiked(String id) {
        return liked().contains(id);
    }

    public static void setLiked(String id, boolean liked) {
        Set<String> values = liked();
        if (liked) {
            values.add(id);
            Set<String> disliked = disliked();
            if (disliked.remove(id)) saveSet("disliked", disliked);
        } else {
            values.remove(id);
        }
        saveSet("liked", values);
    }

    public static boolean isDisliked(String id) {
        return disliked().contains(id);
    }

    public static void setDisliked(String id, boolean value) {
        Set<String> values = disliked();
        if (value) values.add(id); else values.remove(id);
        saveSet("disliked", values);
        if (value) {
            Set<String> liked = liked();
            if (liked.remove(id)) saveSet("liked", liked);
        }
    }

    // ---------------------------------------------------------- recent list

    public static void addRecent(Models.Track track) {
        try {
            JSONArray arr = new JSONArray(p().getString("recent", "[]"));
            JSONArray next = new JSONArray();
            next.put(track.toJson());
            for (int i = 0; i < arr.length() && next.length() < 40; i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                if (track.id.equals(o.optString("id", ""))) continue;
                next.put(o);
            }
            p().edit().putString("recent", next.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static List<Models.Track> recent() {
        List<Models.Track> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(p().getString("recent", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                Models.Track t = Models.Track.fromJson(arr.optJSONObject(i));
                if (!t.id.isEmpty()) out.add(t);
            }
        } catch (Exception ignored) {}
        return out;
    }

    public static void addSearch(String text) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) return;
        try {
            JSONArray arr = new JSONArray(p().getString("searches", "[]"));
            JSONArray next = new JSONArray();
            next.put(value);
            for (int i = 0; i < arr.length() && next.length() < 10; i++) {
                String item = arr.optString(i, "");
                if (!item.isEmpty() && !item.equalsIgnoreCase(value)) next.put(item);
            }
            p().edit().putString("searches", next.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static List<String> searches() {
        List<String> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(p().getString("searches", "[]"));
            for (int i = 0; i < arr.length(); i++) out.add(arr.optString(i, ""));
        } catch (Exception ignored) {}
        return out;
    }

    public static void clearSearches() {
        p().edit().remove("searches").apply();
    }

    // ------------------------------------------------------------- playback

    public static void saveQueue(List<Models.Track> queue, int index, long position,
                                 int repeat, boolean shuffle, boolean wave,
                                 String station, String waveSession, String waveBatch) {
        try {
            JSONObject o = new JSONObject();
            JSONArray arr = new JSONArray();
            for (Models.Track t : queue) arr.put(t.toJson());
            o.put("queue", arr);
            o.put("index", index);
            o.put("pos", position);
            o.put("repeat", repeat);
            o.put("shuffle", shuffle);
            o.put("wave", wave);
            o.put("station", station == null ? "" : station);
            o.put("waveSession", waveSession == null ? "" : waveSession);
            o.put("waveBatch", waveBatch == null ? "" : waveBatch);
            p().edit().putString("queue", o.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static JSONObject queue() {
        try {
            return new JSONObject(p().getString("queue", "{}"));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public static void clearQueue() {
        p().edit().remove("queue").apply();
    }

    // ---------------------------------------------------------------- stats

    public static void countPlay(Models.Track track) {
        if (track == null || track.id.isEmpty()) return;
        try {
            JSONObject o = new JSONObject(p().getString("stats", "{}"));
            JSONObject item = o.optJSONObject(track.id);
            if (item == null) item = new JSONObject();
            item.put("title", track.title);
            item.put("artist", track.artistLine());
            item.put("cover", track.coverUrl);
            item.put("count", item.optInt("count", 0) + 1);
            item.put("last", System.currentTimeMillis());
            o.put(track.id, item);
            p().edit().putString("stats", o.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static List<Models.Track> topTracks() {
        List<Models.Track> out = new ArrayList<>();
        try {
            JSONObject o = new JSONObject(p().getString("stats", "{}"));
            JSONArray keys = o.names();
            if (keys == null) return out;
            List<JSONObject> items = new ArrayList<>();
            for (int i = 0; i < keys.length(); i++) {
                JSONObject item = o.optJSONObject(keys.optString(i));
                if (item != null) items.add(item);
            }
            items.sort((a, b) -> Integer.compare(b.optInt("count", 0), a.optInt("count", 0)));
            for (JSONObject item : items) {
                Models.Track t = new Models.Track();
                t.id = "";
                t.title = item.optString("title", "");
                t.coverUrl = item.optString("cover", "");
                Models.Artist a = new Models.Artist();
                a.name = item.optString("artist", "");
                t.artists.add(a);
                out.add(t);
            }
        } catch (Exception ignored) {}
        return out;
    }

    public static int totalPlays() {
        int total = 0;
        try {
            JSONObject o = new JSONObject(p().getString("stats", "{}"));
            JSONArray keys = o.names();
            if (keys == null) return 0;
            for (int i = 0; i < keys.length(); i++) {
                JSONObject item = o.optJSONObject(keys.optString(i));
                if (item != null) total += item.optInt("count", 0);
            }
        } catch (Exception ignored) {}
        return total;
    }

    public static void clearStats() {
        p().edit().remove("stats").apply();
    }

    // ------------------------------------------------------------ downloads

    public static String downloadPath(String trackId) {
        try {
            return new JSONObject(p().getString("downloads", "{}")).optString(trackId, "");
        } catch (Exception e) {
            return "";
        }
    }

    public static void setDownloadPath(String trackId, String path) {
        try {
            JSONObject o = new JSONObject(p().getString("downloads", "{}"));
            if (path == null || path.isEmpty()) o.remove(trackId);
            else o.put(trackId, path);
            p().edit().putString("downloads", o.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static void saveDownloadedTrack(Models.Track track, String path) {
        setDownloadPath(track.id, path);
        try {
            JSONObject o = new JSONObject(p().getString("downloadTracks", "{}"));
            o.put(track.id, track.toJson());
            p().edit().putString("downloadTracks", o.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static Set<String> downloadedIds() {
        Set<String> out = new LinkedHashSet<>();
        try {
            JSONObject o = new JSONObject(p().getString("downloads", "{}"));
            JSONArray keys = o.names();
            if (keys != null) {
                for (int i = 0; i < keys.length(); i++) {
                    String key = keys.optString(i);
                    String path = o.optString(key, "");
                    if (!path.isEmpty() && new java.io.File(path).exists()) out.add(key);
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    public static void removeDownloadedTrack(String trackId) {
        String path = downloadPath(trackId);
        if (!path.isEmpty()) {
            try {
                new java.io.File(path).delete();
            } catch (Exception ignored) {}
        }
        setDownloadPath(trackId, "");
        try {
            JSONObject o = new JSONObject(p().getString("downloadTracks", "{}"));
            o.remove(trackId);
            p().edit().putString("downloadTracks", o.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static List<Models.Track> downloadedTracks() {
        List<Models.Track> out = new ArrayList<>();
        Set<String> existing = downloadedIds();
        try {
            JSONObject o = new JSONObject(p().getString("downloadTracks", "{}"));
            JSONArray keys = o.names();
            if (keys == null) return out;
            for (int i = 0; i < keys.length(); i++) {
                String id = keys.optString(i);
                if (!existing.contains(id)) continue;
                Models.Track t = Models.Track.fromJson(o.optJSONObject(id));
                if (!t.id.isEmpty()) out.add(t);
            }
        } catch (Exception ignored) {}
        return out;
    }

    public static class StatItem {
        public String title = "";
        public String artist = "";
        public String cover = "";
        public int count = 0;
    }

    public static List<StatItem> topStats() {
        List<StatItem> out = new ArrayList<>();
        try {
            JSONObject o = new JSONObject(p().getString("stats", "{}"));
            JSONArray keys = o.names();
            if (keys == null) return out;
            for (int i = 0; i < keys.length(); i++) {
                JSONObject item = o.optJSONObject(keys.optString(i));
                if (item == null) continue;
                StatItem s = new StatItem();
                s.title = item.optString("title", "");
                s.artist = item.optString("artist", "");
                s.cover = item.optString("cover", "");
                s.count = item.optInt("count", 0);
                out.add(s);
            }
            out.sort((a, b) -> Integer.compare(b.count, a.count));
        } catch (Exception ignored) {}
        return out;
    }

    public static List<StatItem> topArtists() {
        List<StatItem> tracks = topStats();
        java.util.LinkedHashMap<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (StatItem t : tracks) {
            String key = t.artist == null || t.artist.isEmpty() ? "Неизвестный исполнитель" : t.artist;
            counts.put(key, (counts.containsKey(key) ? counts.get(key) : 0) + t.count);
        }
        List<StatItem> out = new ArrayList<>();
        for (java.util.Map.Entry<String, Integer> e : counts.entrySet()) {
            StatItem item = new StatItem();
            item.title = e.getKey();
            item.count = e.getValue();
            out.add(item);
        }
        out.sort((a, b) -> Integer.compare(b.count, a.count));
        return out;
    }
}
