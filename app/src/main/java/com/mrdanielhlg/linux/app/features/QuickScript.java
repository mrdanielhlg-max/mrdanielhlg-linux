package com.mrdanielhlg.linux.app.features;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * QuickScript - Personal script library with one-tap execution.
 *
 * Features:
 *  - Save frequently used commands or multi-line scripts
 *  - Categorized (System, Network, Git, Python, Custom...)
 *  - Variables: scripts can have {{VARIABLE}} placeholders
 *    that prompt the user before running
 *  - Favorite/pin scripts for instant access
 *  - Import/export scripts as JSON
 *  - Built-in useful scripts for Android/Termux
 */
public class QuickScript {

    private static final String TAG = "QuickScript";
    private static final String PREFS_NAME = "mrdanielhlg_scripts";
    private static final String KEY_SCRIPTS = "scripts_json";

    public static class Script {
        public String id;
        public String name;
        public String category;
        public String content;       // the command or script body
        public String description;
        public boolean isFavorite;
        public boolean requiresRoot;
        public long createdAt;
        public long lastRun;
        public int runCount;
        public String iconEmoji;     // emoji icon for visual identification

        public Script() {
            createdAt = System.currentTimeMillis();
            iconEmoji = "📜";
        }

        public JSONObject toJson() throws Exception {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("name", name);
            obj.put("category", category);
            obj.put("content", content);
            obj.put("description", description != null ? description : "");
            obj.put("isFavorite", isFavorite);
            obj.put("requiresRoot", requiresRoot);
            obj.put("createdAt", createdAt);
            obj.put("lastRun", lastRun);
            obj.put("runCount", runCount);
            obj.put("iconEmoji", iconEmoji != null ? iconEmoji : "📜");
            return obj;
        }

        public static Script fromJson(JSONObject obj) throws Exception {
            Script s = new Script();
            s.id = obj.getString("id");
            s.name = obj.getString("name");
            s.category = obj.optString("category", "Custom");
            s.content = obj.getString("content");
            s.description = obj.optString("description", "");
            s.isFavorite = obj.optBoolean("isFavorite", false);
            s.requiresRoot = obj.optBoolean("requiresRoot", false);
            s.createdAt = obj.optLong("createdAt", System.currentTimeMillis());
            s.lastRun = obj.optLong("lastRun", 0);
            s.runCount = obj.optInt("runCount", 0);
            s.iconEmoji = obj.optString("iconEmoji", "📜");
            return s;
        }
    }

    public static final String CAT_SYSTEM = "System";
    public static final String CAT_NETWORK = "Network";
    public static final String CAT_GIT = "Git";
    public static final String CAT_PYTHON = "Python";
    public static final String CAT_FILES = "Files";
    public static final String CAT_CUSTOM = "Custom";

    private final SharedPreferences prefs;
    private final List<Script> scripts = new ArrayList<>();

    public QuickScript(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        load();
        if (scripts.isEmpty()) {
            seedBuiltinScripts();
        }
    }

    public Script addScript(String name, String category, String content, String description, String emoji) {
        Script s = new Script();
        s.id = "script_" + System.currentTimeMillis();
        s.name = name;
        s.category = category != null ? category : CAT_CUSTOM;
        s.content = content;
        s.description = description;
        s.iconEmoji = emoji != null ? emoji : "📜";
        scripts.add(s);
        save();
        return s;
    }

    public void updateScript(String id, String name, String category, String content, String description) {
        Script s = findById(id);
        if (s == null) return;
        if (name != null) s.name = name;
        if (category != null) s.category = category;
        if (content != null) s.content = content;
        if (description != null) s.description = description;
        save();
    }

    public void toggleFavorite(String id) {
        Script s = findById(id);
        if (s != null) {
            s.isFavorite = !s.isFavorite;
            save();
        }
    }

    public void recordRun(String id) {
        Script s = findById(id);
        if (s != null) {
            s.runCount++;
            s.lastRun = System.currentTimeMillis();
            save();
        }
    }

    public void deleteScript(String id) {
        scripts.removeIf(s -> s.id.equals(id));
        save();
    }

    public List<Script> getScripts() {
        return new ArrayList<>(scripts);
    }

    public List<Script> getFavorites() {
        List<Script> favs = new ArrayList<>();
        for (Script s : scripts) if (s.isFavorite) favs.add(s);
        return favs;
    }

    public List<Script> getByCategory(String category) {
        List<Script> result = new ArrayList<>();
        for (Script s : scripts) if (category.equals(s.category)) result.add(s);
        return result;
    }

    public List<String> getCategories() {
        List<String> cats = new ArrayList<>();
        for (Script s : scripts) {
            if (!cats.contains(s.category)) cats.add(s.category);
        }
        return cats;
    }

    public Script findById(String id) {
        for (Script s : scripts) if (s.id.equals(id)) return s;
        return null;
    }

    /**
     * Extract {{VARIABLE}} placeholders from a script.
     */
    public List<String> extractVariables(String content) {
        List<String> vars = new ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{\\{([^}]+)\\}\\}").matcher(content);
        while (m.find()) {
            String var = m.group(1);
            if (!vars.contains(var)) vars.add(var);
        }
        return vars;
    }

    /**
     * Replace {{VARIABLE}} in content with given values.
     */
    public String resolveVariables(String content, java.util.Map<String, String> values) {
        for (java.util.Map.Entry<String, String> e : values.entrySet()) {
            content = content.replace("{{" + e.getKey() + "}}", e.getValue());
        }
        return content;
    }

    /**
     * Export all scripts to JSON string.
     */
    public String exportToJson() {
        try {
            JSONArray arr = new JSONArray();
            for (Script s : scripts) arr.put(s.toJson());
            return arr.toString(2);
        } catch (Exception e) {
            Log.e(TAG, "Export failed", e);
            return "[]";
        }
    }

    /**
     * Import scripts from JSON string (merges with existing).
     */
    public int importFromJson(String json) {
        int count = 0;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                Script s = Script.fromJson(arr.getJSONObject(i));
                s.id = "imported_" + System.currentTimeMillis() + "_" + i;
                scripts.add(s);
                count++;
            }
            save();
        } catch (Exception e) {
            Log.e(TAG, "Import failed", e);
        }
        return count;
    }

    private void seedBuiltinScripts() {
        // System info
        addBuiltin("📊", "System Info", CAT_SYSTEM,
            "echo '=== CPU ===' && cat /proc/cpuinfo | grep 'model name' | head -1\n" +
            "echo '=== Memory ===' && free -h\n" +
            "echo '=== Disk ===' && df -h /data\n" +
            "echo '=== Android Version ===' && getprop ro.build.version.release",
            "Show CPU, RAM, disk and Android version");

        // Network check
        addBuiltin("🌐", "Network Check", CAT_NETWORK,
            "echo '=== IP Address ===' && ip addr show wlan0 | grep 'inet '\n" +
            "echo '=== Ping Google ===' && ping -c 3 8.8.8.8\n" +
            "echo '=== DNS ===' && cat /etc/resolv.conf",
            "Check IP, ping and DNS");

        // Scan LAN devices
        addBuiltin("🔍", "Scan Local Network", CAT_NETWORK,
            "# Requires: pkg install nmap\n" +
            "ip route | grep -oP 'src \\K[\\d.]+' | head -1 | xargs -I{} nmap -sn {}/24 --open",
            "Scan all devices on local WiFi network");

        // Git status all
        addBuiltin("🔀", "Git Full Status", CAT_GIT,
            "git log --oneline -10\n" +
            "echo '---'\n" +
            "git status\n" +
            "echo '---'\n" +
            "git diff --stat",
            "Show git log, status and diff stats");

        // Quick commit
        addBuiltin("✅", "Quick Git Commit & Push", CAT_GIT,
            "git add .\n" +
            "git commit -m \"{{COMMIT_MESSAGE}}\"\n" +
            "git push",
            "Stage, commit and push. Asks for commit message.");

        // Python venv
        addBuiltin("🐍", "Setup Python Venv", CAT_PYTHON,
            "python3 -m venv .venv\n" +
            "source .venv/bin/activate\n" +
            "echo 'Virtual environment activated!'",
            "Create and activate Python virtual environment");

        // Clean temp files
        addBuiltin("🧹", "Clean Temp Files", CAT_FILES,
            "echo 'Cleaning tmp...'\n" +
            "rm -rf $TMPDIR/*\n" +
            "echo 'Done. Space freed:'\n" +
            "df -h $HOME",
            "Remove temporary files and show freed space");

        // Battery status
        addBuiltin("🔋", "Battery Status", CAT_SYSTEM,
            "termux-battery-status 2>/dev/null || cat /sys/class/power_supply/battery/capacity",
            "Show battery percentage and status");

        // Download file
        addBuiltin("⬇️", "Download File", CAT_NETWORK,
            "wget -c --show-progress '{{URL}}' -O '{{FILENAME}}'",
            "Download a file with resume support. Asks for URL and filename.");

        // SSH connect
        addBuiltin("🔐", "SSH Connect", CAT_NETWORK,
            "ssh {{USER}}@{{HOST}} -p {{PORT}}",
            "Connect to remote server via SSH");

        save();
    }

    private void addBuiltin(String emoji, String name, String category, String content, String desc) {
        Script s = new Script();
        s.id = "builtin_" + name.toLowerCase().replace(" ", "_");
        s.name = name;
        s.category = category;
        s.content = content;
        s.description = desc;
        s.iconEmoji = emoji;
        s.isFavorite = false;
        scripts.add(s);
    }

    private void save() {
        try {
            JSONArray arr = new JSONArray();
            for (Script s : scripts) arr.put(s.toJson());
            prefs.edit().putString(KEY_SCRIPTS, arr.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save scripts", e);
        }
    }

    private void load() {
        try {
            String json = prefs.getString(KEY_SCRIPTS, "[]");
            JSONArray arr = new JSONArray(json);
            scripts.clear();
            for (int i = 0; i < arr.length(); i++) {
                scripts.add(Script.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load scripts", e);
        }
    }
}
