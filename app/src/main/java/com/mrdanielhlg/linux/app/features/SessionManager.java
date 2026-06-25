package com.mrdanielhlg.linux.app.features;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * SessionManager - Advanced named terminal session management.
 *
 * Features:
 *  - Named sessions (e.g., "Python Dev", "Git Work", "SSH Server")
 *  - Session color coding for quick visual identification
 *  - Auto-restore: remembers last working directory per session
 *  - Session notes: attach a description to each session
 *  - Pin sessions to keep them at the top
 *  - Session groups/tags
 */
public class SessionManager {

    private static final String TAG = "SessionManager";
    private static final String PREFS_NAME = "mrdanielhlg_sessions";
    private static final String KEY_SESSIONS = "sessions_json";
    private static final int MAX_SESSIONS = 20;

    public static class SessionInfo {
        public String id;
        public String name;
        public String colorHex;    // e.g. "#FF5722"
        public String lastDir;     // last known working directory
        public String notes;       // user notes about this session
        public boolean pinned;
        public long createdAt;
        public long lastUsed;
        public List<String> tags;

        public SessionInfo() {
            tags = new ArrayList<>();
            colorHex = "#4CAF50";
            createdAt = System.currentTimeMillis();
            lastUsed = createdAt;
        }

        public JSONObject toJson() throws Exception {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("name", name);
            obj.put("colorHex", colorHex);
            obj.put("lastDir", lastDir != null ? lastDir : "");
            obj.put("notes", notes != null ? notes : "");
            obj.put("pinned", pinned);
            obj.put("createdAt", createdAt);
            obj.put("lastUsed", lastUsed);
            JSONArray tagsArr = new JSONArray();
            for (String t : tags) tagsArr.put(t);
            obj.put("tags", tagsArr);
            return obj;
        }

        public static SessionInfo fromJson(JSONObject obj) throws Exception {
            SessionInfo s = new SessionInfo();
            s.id = obj.getString("id");
            s.name = obj.getString("name");
            s.colorHex = obj.optString("colorHex", "#4CAF50");
            s.lastDir = obj.optString("lastDir", "");
            s.notes = obj.optString("notes", "");
            s.pinned = obj.optBoolean("pinned", false);
            s.createdAt = obj.optLong("createdAt", System.currentTimeMillis());
            s.lastUsed = obj.optLong("lastUsed", s.createdAt);
            s.tags = new ArrayList<>();
            JSONArray tagsArr = obj.optJSONArray("tags");
            if (tagsArr != null) {
                for (int i = 0; i < tagsArr.length(); i++) {
                    s.tags.add(tagsArr.getString(i));
                }
            }
            return s;
        }
    }

    // Predefined session colors
    public static final String[] SESSION_COLORS = {
        "#4CAF50", // Green
        "#2196F3", // Blue
        "#FF5722", // Deep Orange
        "#9C27B0", // Purple
        "#FF9800", // Orange
        "#00BCD4", // Cyan
        "#E91E63", // Pink
        "#607D8B", // Blue Grey
        "#795548", // Brown
        "#CDDC39", // Lime
    };

    private final SharedPreferences prefs;
    private final List<SessionInfo> sessions = new ArrayList<>();

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        load();
    }

    public SessionInfo createSession(String name) {
        if (sessions.size() >= MAX_SESSIONS) {
            // Remove oldest non-pinned session
            for (int i = sessions.size() - 1; i >= 0; i--) {
                if (!sessions.get(i).pinned) {
                    sessions.remove(i);
                    break;
                }
            }
        }

        SessionInfo s = new SessionInfo();
        s.id = "session_" + System.currentTimeMillis();
        s.name = (name != null && !name.isEmpty()) ? name : "Session " + (sessions.size() + 1);
        s.colorHex = SESSION_COLORS[sessions.size() % SESSION_COLORS.length];
        sessions.add(s);
        save();
        return s;
    }

    public void updateSession(String id, String name, String colorHex, String notes, boolean pinned, List<String> tags) {
        SessionInfo s = findById(id);
        if (s == null) return;
        if (name != null) s.name = name;
        if (colorHex != null) s.colorHex = colorHex;
        if (notes != null) s.notes = notes;
        s.pinned = pinned;
        if (tags != null) s.tags = tags;
        s.lastUsed = System.currentTimeMillis();
        save();
    }

    public void updateLastDir(String id, String dir) {
        SessionInfo s = findById(id);
        if (s != null) {
            s.lastDir = dir;
            s.lastUsed = System.currentTimeMillis();
            save();
        }
    }

    public void deleteSession(String id) {
        sessions.removeIf(s -> s.id.equals(id));
        save();
    }

    public List<SessionInfo> getSessions() {
        // Pinned first, then sorted by last used descending
        List<SessionInfo> pinned = new ArrayList<>();
        List<SessionInfo> others = new ArrayList<>();
        for (SessionInfo s : sessions) {
            if (s.pinned) pinned.add(s);
            else others.add(s);
        }
        others.sort((a, b) -> Long.compare(b.lastUsed, a.lastUsed));
        List<SessionInfo> result = new ArrayList<>(pinned);
        result.addAll(others);
        return result;
    }

    public List<SessionInfo> getSessionsByTag(String tag) {
        List<SessionInfo> result = new ArrayList<>();
        for (SessionInfo s : sessions) {
            if (s.tags.contains(tag)) result.add(s);
        }
        return result;
    }

    public SessionInfo findById(String id) {
        for (SessionInfo s : sessions) {
            if (s.id.equals(id)) return s;
        }
        return null;
    }

    public int getSessionCount() {
        return sessions.size();
    }

    private void save() {
        try {
            JSONArray arr = new JSONArray();
            for (SessionInfo s : sessions) arr.put(s.toJson());
            prefs.edit().putString(KEY_SESSIONS, arr.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save sessions", e);
        }
    }

    private void load() {
        try {
            String json = prefs.getString(KEY_SESSIONS, "[]");
            JSONArray arr = new JSONArray(json);
            sessions.clear();
            for (int i = 0; i < arr.length(); i++) {
                sessions.add(SessionInfo.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load sessions", e);
        }
    }
}
