package com.mrdanielhlg.linux.app.features;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * SystemMonitor - Real-time Android/Linux system statistics.
 *
 * Reads live data directly from /proc and /sys without requiring
 * any special permissions. Displays:
 *  - CPU usage per core
 *  - RAM usage
 *  - Battery level and temperature
 *  - Storage usage
 *  - Network stats (bytes sent/received)
 *  - Running processes count
 *  - Android-specific info
 *
 * Can be displayed as a compact overlay bar or full-screen panel.
 */
public class SystemMonitor {

    private static final String TAG = "SystemMonitor";

    public static class SystemStats {
        public float cpuUsagePercent;
        public int[] cpuFreqMHz;        // per core
        public long ramUsedMB;
        public long ramTotalMB;
        public float ramUsagePercent;
        public int batteryPercent;
        public float batteryTempCelsius;
        public boolean isCharging;
        public long storageTotalGB;
        public long storageUsedGB;
        public long netRxBytes;
        public long netTxBytes;
        public int processCount;
        public String androidVersion;
        public String deviceModel;
        public long uptimeSeconds;

        @Override
        public String toString() {
            return String.format(
                "CPU: %.1f%% | RAM: %dMB/%dMB | Battery: %d%% %.1f°C | Storage: %dGB/%dGB",
                cpuUsagePercent, ramUsedMB, ramTotalMB,
                batteryPercent, batteryTempCelsius,
                storageUsedGB, storageTotalGB
            );
        }

        public String toCompactBar() {
            String bat = isCharging ? "⚡" + batteryPercent + "%" : "🔋" + batteryPercent + "%";
            return String.format("CPU %.0f%% | RAM %d/%dM | %s | Net↑%s↓%s",
                cpuUsagePercent, ramUsedMB, ramTotalMB, bat,
                formatBytes(netTxBytes), formatBytes(netRxBytes));
        }

        private String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + "B";
            if (bytes < 1024 * 1024) return (bytes / 1024) + "K";
            if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)) + "M";
            return (bytes / (1024 * 1024 * 1024)) + "G";
        }
    }

    // Previous CPU stats for delta calculation
    private long[] prevCpuStats = null;

    public SystemStats getStats() {
        SystemStats stats = new SystemStats();

        readCpuUsage(stats);
        readMemory(stats);
        readBattery(stats);
        readStorage(stats);
        readNetwork(stats);
        readProcessCount(stats);

        stats.androidVersion = Build.VERSION.RELEASE;
        stats.deviceModel = Build.MODEL;
        stats.uptimeSeconds = readUptime();

        return stats;
    }

    /**
     * Get a formatted status string suitable for terminal display.
     */
    public String getStatusString() {
        SystemStats s = getStats();
        return s.toCompactBar();
    }

    /**
     * Get detailed system report as multi-line string.
     */
    public String getDetailedReport() {
        SystemStats s = getStats();
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════╗\n");
        sb.append("║   MrDanielHlg Linux - System Monitor  ║\n");
        sb.append("╚══════════════════════════════════════╝\n");
        sb.append(String.format("📱 Device:   %s\n", s.deviceModel));
        sb.append(String.format("🤖 Android:  %s\n", s.androidVersion));
        sb.append(String.format("⏱  Uptime:   %s\n", formatUptime(s.uptimeSeconds)));
        sb.append("─────────────────────────────────────\n");
        sb.append(String.format("🖥  CPU:      %.1f%% usage\n", s.cpuUsagePercent));
        sb.append(String.format("💾 RAM:      %dMB / %dMB (%.0f%%)\n",
            s.ramUsedMB, s.ramTotalMB, s.ramUsagePercent));
        sb.append(String.format("💽 Storage:  %dGB / %dGB used\n",
            s.storageUsedGB, s.storageTotalGB));
        sb.append("─────────────────────────────────────\n");
        String charging = s.isCharging ? " ⚡CHARGING" : "";
        sb.append(String.format("🔋 Battery:  %d%% | %.1f°C%s\n",
            s.batteryPercent, s.batteryTempCelsius, charging));
        sb.append("─────────────────────────────────────\n");
        sb.append(String.format("📤 Net TX:   %s\n", formatBytes(s.netTxBytes)));
        sb.append(String.format("📥 Net RX:   %s\n", formatBytes(s.netRxBytes)));
        sb.append(String.format("⚙  Processes: %d running\n", s.processCount));
        return sb.toString();
    }

    private void readCpuUsage(SystemStats stats) {
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/stat"));
            String line = br.readLine();
            br.close();
            if (line != null && line.startsWith("cpu ")) {
                String[] parts = line.trim().split("\\s+");
                long[] curr = new long[parts.length - 1];
                for (int i = 1; i < parts.length; i++) {
                    curr[i-1] = Long.parseLong(parts[i]);
                }
                if (prevCpuStats != null && prevCpuStats.length == curr.length) {
                    long totalDelta = 0, idleDelta = 0;
                    for (int i = 0; i < curr.length; i++) {
                        totalDelta += curr[i] - prevCpuStats[i];
                    }
                    // index 3 is idle, index 4 is iowait
                    idleDelta = (curr[3] - prevCpuStats[3]) + (curr[4] - prevCpuStats[4]);
                    stats.cpuUsagePercent = totalDelta > 0
                        ? (1f - (float) idleDelta / totalDelta) * 100f : 0f;
                }
                prevCpuStats = curr;
            }
        } catch (Exception e) {
            Log.d(TAG, "CPU read failed: " + e.getMessage());
        }
    }

    private void readMemory(SystemStats stats) {
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/meminfo"));
            String line;
            long total = 0, available = 0;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("MemTotal:")) {
                    total = parseKb(line);
                } else if (line.startsWith("MemAvailable:")) {
                    available = parseKb(line);
                }
            }
            br.close();
            stats.ramTotalMB = total / 1024;
            stats.ramUsedMB = (total - available) / 1024;
            stats.ramUsagePercent = total > 0 ? (float)(total - available) / total * 100f : 0;
        } catch (Exception e) {
            Log.d(TAG, "Memory read failed: " + e.getMessage());
        }
    }

    private void readBattery(SystemStats stats) {
        try {
            // Battery capacity
            File capFile = new File("/sys/class/power_supply/battery/capacity");
            if (capFile.exists()) {
                stats.batteryPercent = Integer.parseInt(readFirstLine(capFile).trim());
            }
            // Battery temperature
            File tempFile = new File("/sys/class/power_supply/battery/temp");
            if (tempFile.exists()) {
                stats.batteryTempCelsius = Integer.parseInt(readFirstLine(tempFile).trim()) / 10f;
            }
            // Charging status
            File statusFile = new File("/sys/class/power_supply/battery/status");
            if (statusFile.exists()) {
                String status = readFirstLine(statusFile).trim();
                stats.isCharging = "Charging".equalsIgnoreCase(status) || "Full".equalsIgnoreCase(status);
            }
        } catch (Exception e) {
            Log.d(TAG, "Battery read failed: " + e.getMessage());
        }
    }

    private void readStorage(SystemStats stats) {
        try {
            File dataDir = new File("/data");
            stats.storageTotalGB = dataDir.getTotalSpace() / (1024 * 1024 * 1024);
            stats.storageUsedGB = (dataDir.getTotalSpace() - dataDir.getFreeSpace()) / (1024 * 1024 * 1024);
        } catch (Exception e) {
            Log.d(TAG, "Storage read failed: " + e.getMessage());
        }
    }

    private void readNetwork(SystemStats stats) {
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/net/dev"));
            String line;
            long rx = 0, tx = 0;
            boolean header = true;
            while ((line = br.readLine()) != null) {
                if (header) { header = false; continue; }
                if (line.contains(":")) {
                    String[] parts = line.trim().split("[:\\s]+");
                    if (parts.length > 10) {
                        String iface = parts[0];
                        if (!iface.equals("lo")) { // skip loopback
                            rx += Long.parseLong(parts[1]);
                            tx += Long.parseLong(parts[9]);
                        }
                    }
                }
            }
            br.close();
            stats.netRxBytes = rx;
            stats.netTxBytes = tx;
        } catch (Exception e) {
            Log.d(TAG, "Network read failed: " + e.getMessage());
        }
    }

    private void readProcessCount(SystemStats stats) {
        try {
            File proc = new File("/proc");
            File[] dirs = proc.listFiles(f -> f.isDirectory() && f.getName().matches("\\d+"));
            stats.processCount = dirs != null ? dirs.length : 0;
        } catch (Exception e) {
            Log.d(TAG, "Process count failed: " + e.getMessage());
        }
    }

    private long readUptime() {
        try {
            String line = readFirstLine(new File("/proc/uptime"));
            return (long) Double.parseDouble(line.trim().split("\\s+")[0]);
        } catch (Exception e) {
            return 0;
        }
    }

    private long parseKb(String line) {
        return Long.parseLong(line.replaceAll("[^0-9]", ""));
    }

    private String readFirstLine(File f) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line = br.readLine();
        br.close();
        return line != null ? line : "";
    }

    private String formatUptime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long mins = (seconds % 3600) / 60;
        if (days > 0) return days + "d " + hours + "h " + mins + "m";
        if (hours > 0) return hours + "h " + mins + "m";
        return mins + "m";
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024f);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024f * 1024));
        return String.format("%.2f GB", bytes / (1024f * 1024 * 1024));
    }
}
