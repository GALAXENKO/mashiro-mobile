package com.mashiro.mobile.api;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** DTOs shared by the API client, player and UI. */
public final class Models {
    private Models() {}

    public static class Artist {
        public String id = "";
        public String name = "";
        public String coverUrl = "";

        public JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id);
                o.put("name", name);
                o.put("coverUrl", coverUrl);
            } catch (Exception ignored) {}
            return o;
        }

        public static Artist fromJson(JSONObject o) {
            Artist a = new Artist();
            if (o == null) return a;
            a.id = o.optString("id", "");
            a.name = o.optString("name", "");
            a.coverUrl = o.optString("coverUrl", "");
            return a;
        }
    }

    public static class Track {
        public String id = "";
        public String title = "";
        public List<Artist> artists = new ArrayList<>();
        public String albumId = "";
        public String albumTitle = "";
        public String coverUrl = "";
        public String coverLarge = "";
        public long durationMs = 0;
        public boolean available = true;

        public String artistLine() {
            StringBuilder sb = new StringBuilder();
            for (Artist a : artists) {
                if (a.name == null || a.name.isEmpty()) continue;
                if (sb.length() > 0) sb.append(", ");
                sb.append(a.name);
            }
            return sb.length() == 0 ? "Неизвестный исполнитель" : sb.toString();
        }

        public String shareUrl() {
            return "https://music.yandex.ru/track/" + id;
        }

        public JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id);
                o.put("title", title);
                JSONArray arr = new JSONArray();
                for (Artist a : artists) arr.put(a.toJson());
                o.put("artists", arr);
                o.put("albumId", albumId);
                o.put("albumTitle", albumTitle);
                o.put("coverUrl", coverUrl);
                o.put("coverLarge", coverLarge);
                o.put("durationMs", durationMs);
                o.put("available", available);
            } catch (Exception ignored) {}
            return o;
        }

        public static Track fromJson(JSONObject o) {
            Track t = new Track();
            if (o == null) return t;
            t.id = o.optString("id", "");
            t.title = o.optString("title", "");
            t.albumId = o.optString("albumId", "");
            t.albumTitle = o.optString("albumTitle", "");
            t.coverUrl = o.optString("coverUrl", "");
            t.coverLarge = o.optString("coverLarge", "");
            t.durationMs = o.optLong("durationMs", 0);
            t.available = o.optBoolean("available", true);
            JSONArray arr = o.optJSONArray("artists");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    t.artists.add(Artist.fromJson(arr.optJSONObject(i)));
                }
            }
            return t;
        }
    }

    public static class Playlist {
        public long kind = 0;
        public long ownerUid = 0;
        public String title = "";
        public int trackCount = 0;
        public String coverUrl = "";
        public String description = "";
        public boolean isPublic = true;
        public List<Track> tracks = new ArrayList<>();
    }

    public static class Album {
        public String id = "";
        public String title = "";
        public String coverUrl = "";
        public String genre = "";
        public int year = 0;
        public List<Artist> artists = new ArrayList<>();
        public List<Track> tracks = new ArrayList<>();
    }

    public static class ArtistPage {
        public Artist artist = new Artist();
        public String description = "";
        public long listeners = 0;
        public long likes = 0;
        public long tracksCount = 0;
        public List<Track> tracks = new ArrayList<>();
        public List<Album> albums = new ArrayList<>();
    }

    public static class SearchResult {
        public List<Track> tracks = new ArrayList<>();
        public List<Artist> artists = new ArrayList<>();
        public List<Album> albums = new ArrayList<>();
        public List<Playlist> playlists = new ArrayList<>();
        public String best = "";
    }

    public static class LyricLine {
        public long timeMs;
        public String text = "";
        public LyricLine(long timeMs, String text) {
            this.timeMs = timeMs;
            this.text = text;
        }
    }

    public static class Lyrics {
        public String trackId = "";
        public boolean synced = false;
        public String source = "";
        public List<LyricLine> lines = new ArrayList<>();
        public List<String> writers = new ArrayList<>();
    }

    public static class Profile {
        public long uid = 0;
        public String login = "";
        public String name = "";
        public String avatarUrl = "";
        public boolean hasPlus = false;
    }

    public static class Wave {
        public String stationId = "";
        public String sessionId = "";
        public String batchId = "";
        public List<Track> tracks = new ArrayList<>();
    }

    public static class Section {
        public String title = "";
        public String subtitle = "";
        public String kind = "";
        public List<Track> tracks = new ArrayList<>();
        public List<Album> albums = new ArrayList<>();
        public List<Playlist> playlists = new ArrayList<>();
    }
}
