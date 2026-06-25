package com.mrdanielhlg.linux.app.features;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AICommandSuggestion - Intelligent command auto-suggestion engine.
 *
 * Learns from the user's command history and suggests relevant completions
 * using a frequency + context-based scoring model. Works completely offline.
 *
 * Features:
 *  - Frequency-based ranking (most used commands rank higher)
 *  - Context-aware: suggests based on what was typed before
 *  - Persistent learning: history saved across sessions
 *  - Built-in cheat sheet for 100+ common Linux commands
 */
public class AICommandSuggestion {

    private static final String TAG = "AICommandSuggestion";
    private static final String PREFS_NAME = "mrdanielhlg_cmd_history";
    private static final String KEY_HISTORY = "history_json";
    private static final int MAX_HISTORY = 500;
    private static final int MAX_SUGGESTIONS = 8;

    private final SharedPreferences prefs;
    // command -> use count
    private final Map<String, Integer> frequencyMap = new LinkedHashMap<>();
    // bigram: previous_command -> next_command -> count
    private final Map<String, Map<String, Integer>> bigramMap = new HashMap<>();

    private String lastCommand = "";

    // Built-in common Linux commands with short description
    private static final String[][] BUILTIN_COMMANDS = {
        {"ls", "list directory contents"},
        {"ls -la", "list all files with details"},
        {"cd", "change directory"},
        {"pwd", "print working directory"},
        {"mkdir", "make directory"},
        {"rm", "remove files"},
        {"rm -rf", "force remove directory recursively"},
        {"cp", "copy files"},
        {"mv", "move or rename files"},
        {"cat", "concatenate and display files"},
        {"nano", "simple text editor"},
        {"vim", "advanced text editor"},
        {"grep", "search text patterns"},
        {"grep -r", "recursive grep search"},
        {"find", "search for files"},
        {"find . -name", "find file by name"},
        {"chmod", "change file permissions"},
        {"chmod +x", "make file executable"},
        {"chown", "change file owner"},
        {"ps aux", "list all running processes"},
        {"kill", "terminate a process"},
        {"top", "display system processes"},
        {"htop", "interactive process viewer"},
        {"df -h", "disk usage human-readable"},
        {"du -sh", "directory size summary"},
        {"free -h", "memory usage"},
        {"uname -a", "system information"},
        {"ifconfig", "network interface info"},
        {"ip addr", "show IP addresses"},
        {"ping", "test network connectivity"},
        {"curl", "transfer data from URLs"},
        {"wget", "download files from web"},
        {"tar -xzf", "extract .tar.gz archive"},
        {"tar -czf", "create .tar.gz archive"},
        {"zip", "create zip archive"},
        {"unzip", "extract zip archive"},
        {"ssh", "connect to remote server"},
        {"scp", "secure copy over SSH"},
        {"git init", "initialize git repo"},
        {"git clone", "clone a repository"},
        {"git status", "show git status"},
        {"git add .", "stage all changes"},
        {"git commit -m", "commit with message"},
        {"git push", "push to remote"},
        {"git pull", "pull from remote"},
        {"git log --oneline", "compact git log"},
        {"python3", "run Python 3"},
        {"python3 -m pip install", "install Python package"},
        {"pip install", "install pip package"},
        {"node", "run Node.js"},
        {"npm install", "install npm packages"},
        {"apt update", "update package lists"},
        {"apt upgrade", "upgrade packages"},
        {"apt install", "install a package"},
        {"apt remove", "remove a package"},
        {"apt search", "search for a package"},
        {"pkg update", "update termux packages"},
        {"pkg install", "install termux package"},
        {"pkg list-installed", "list installed packages"},
        {"history", "show command history"},
        {"clear", "clear terminal screen"},
        {"exit", "exit terminal"},
        {"echo", "print text"},
        {"export", "set environment variable"},
        {"env", "show environment variables"},
        {"which", "find command location"},
        {"man", "show manual page"},
        {"help", "show help"},
        {"alias", "create command shortcut"},
        {"source", "run script in current shell"},
        {"bash", "start bash shell"},
        {"sh", "start sh shell"},
        {"whoami", "current user"},
        {"id", "user and group IDs"},
        {"date", "show current date/time"},
        {"cal", "show calendar"},
        {"bc", "calculator"},
        {"wc -l", "count lines"},
        {"wc -w", "count words"},
        {"sort", "sort lines"},
        {"sort -u", "sort and remove duplicates"},
        {"uniq", "remove duplicate lines"},
        {"head", "first lines of file"},
        {"tail", "last lines of file"},
        {"tail -f", "follow file in real-time"},
        {"diff", "compare two files"},
        {"sed", "stream editor"},
        {"awk", "text processing tool"},
        {"cut", "cut sections from lines"},
        {"tr", "translate characters"},
        {"tee", "read and write simultaneously"},
        {"xargs", "build command from stdin"},
        {"screen", "terminal multiplexer"},
        {"tmux", "advanced terminal multiplexer"},
        {"tmux new", "new tmux session"},
        {"tmux attach", "attach to tmux session"},
        {"jobs", "list background jobs"},
        {"bg", "resume job in background"},
        {"fg", "bring job to foreground"},
        {"nohup", "run command immune to hangups"},
        {"crontab -e", "edit cron jobs"},
        {"crontab -l", "list cron jobs"},
        {"netstat -tuln", "show open ports"},
        {"ss -tuln", "show socket statistics"},
        {"lsof", "list open files"},
        {"strace", "trace system calls"},
        {"base64", "encode/decode base64"},
        {"md5sum", "compute MD5 checksum"},
        {"sha256sum", "compute SHA256 checksum"},
        {"openssl", "SSL/TLS toolkit"},
        {"gpg", "GNU Privacy Guard"},
        {"passwd", "change password"},
        {"su", "switch user"},
        {"sudo", "run as superuser"},
        {"tput cls", "clear screen portable"},
        {"watch", "execute command periodically"},
        {"time", "measure command execution time"},
        {"xclip", "clipboard tool"},
        {"termux-clipboard-get", "get clipboard"},
        {"termux-clipboard-set", "set clipboard"},
        {"termux-share", "share file from termux"},
        {"termux-open", "open file with Android"},
        {"termux-notification", "send Android notification"},
        {"termux-battery-status", "show battery status"},
        {"termux-camera-photo", "take photo"},
        {"termux-location", "get GPS location"},
        {"termux-sms-list", "list SMS messages"},
        {"termux-wifi-scaninfo", "scan WiFi networks"},
    };

    public AICommandSuggestion(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadHistory();
    }

    /**
     * Record a command that was executed.
     * Updates frequency and bigram models.
     */
    public void recordCommand(String command) {
        if (command == null || command.trim().isEmpty()) return;
        command = command.trim();

        // Update frequency
        frequencyMap.put(command, frequencyMap.getOrDefault(command, 0) + 1);

        // Update bigram (previous -> current)
        if (!lastCommand.isEmpty()) {
            bigramMap.computeIfAbsent(lastCommand, k -> new HashMap<>());
            Map<String, Integer> nextMap = bigramMap.get(lastCommand);
            nextMap.put(command, nextMap.getOrDefault(command, 0) + 1);
        }

        lastCommand = command;
        trimHistory();
        saveHistory();
    }

    /**
     * Get suggestions for the current partial input.
     * Returns list of [command, description] pairs.
     */
    public List<String[]> getSuggestions(String partial) {
        if (partial == null) partial = "";
        partial = partial.trim().toLowerCase();

        Map<String, Double> scores = new LinkedHashMap<>();

        // Score from user history (highest weight)
        for (Map.Entry<String, Integer> entry : frequencyMap.entrySet()) {
            String cmd = entry.getKey();
            if (cmd.toLowerCase().startsWith(partial) || cmd.toLowerCase().contains(partial)) {
                double score = entry.getValue() * 10.0;
                // Prefer prefix match over contains match
                if (cmd.toLowerCase().startsWith(partial)) score *= 2.0;
                scores.put(cmd, scores.getOrDefault(cmd, 0.0) + score);
            }
        }

        // Boost commands that follow the last command (context-aware)
        if (!lastCommand.isEmpty() && bigramMap.containsKey(lastCommand)) {
            Map<String, Integer> nextCmds = bigramMap.get(lastCommand);
            for (Map.Entry<String, Integer> entry : nextCmds.entrySet()) {
                String cmd = entry.getKey();
                if (cmd.toLowerCase().startsWith(partial) || cmd.toLowerCase().contains(partial)) {
                    scores.put(cmd, scores.getOrDefault(cmd, 0.0) + entry.getValue() * 5.0);
                }
            }
        }

        // Add built-in commands (lower weight, fills gaps)
        for (String[] builtin : BUILTIN_COMMANDS) {
            String cmd = builtin[0];
            if (cmd.toLowerCase().startsWith(partial) || cmd.toLowerCase().contains(partial)) {
                if (!scores.containsKey(cmd)) {
                    double baseScore = 1.0;
                    if (cmd.toLowerCase().startsWith(partial)) baseScore = 2.0;
                    scores.put(cmd, baseScore);
                }
            }
        }

        // Sort by score descending
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(scores.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Build result list with descriptions
        List<String[]> results = new ArrayList<>();
        for (Map.Entry<String, Double> entry : sorted) {
            if (results.size() >= MAX_SUGGESTIONS) break;
            String cmd = entry.getKey();
            String desc = getDescription(cmd);
            results.add(new String[]{cmd, desc});
        }

        return results;
    }

    /**
     * Get all user history sorted by frequency.
     */
    public List<String> getHistory() {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(frequencyMap.entrySet());
        entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Integer> e : entries) {
            result.add(e.getKey());
        }
        return result;
    }

    public void clearHistory() {
        frequencyMap.clear();
        bigramMap.clear();
        lastCommand = "";
        prefs.edit().remove(KEY_HISTORY).apply();
    }

    private String getDescription(String cmd) {
        for (String[] builtin : BUILTIN_COMMANDS) {
            if (builtin[0].equals(cmd)) return builtin[1];
        }
        return "custom command";
    }

    private void trimHistory() {
        while (frequencyMap.size() > MAX_HISTORY) {
            String oldest = frequencyMap.entrySet().iterator().next().getKey();
            frequencyMap.remove(oldest);
        }
    }

    private void saveHistory() {
        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> e : frequencyMap.entrySet()) {
                sb.append(e.getKey().replace("|", "\\|"))
                  .append("|")
                  .append(e.getValue())
                  .append("\n");
            }
            prefs.edit().putString(KEY_HISTORY, sb.toString()).apply();
        } catch (Exception ex) {
            Log.e(TAG, "Failed to save history", ex);
        }
    }

    private void loadHistory() {
        try {
            String raw = prefs.getString(KEY_HISTORY, "");
            if (raw == null || raw.isEmpty()) return;
            for (String line : raw.split("\n")) {
                String[] parts = line.split("(?<!\\\\)\\|", 2);
                if (parts.length == 2) {
                    String cmd = parts[0].replace("\\|", "|");
                    int count = Integer.parseInt(parts[1].trim());
                    frequencyMap.put(cmd, count);
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to load history", ex);
        }
    }
}
