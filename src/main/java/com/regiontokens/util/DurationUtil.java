package com.regiontokens.util;

public class DurationUtil {
    public static long parseDuration(String input) {
        if (input == null || input.isEmpty()) {
            return 60;
        }

        try {
            if (input.endsWith("h")) {
                return Long.parseLong(input.substring(0, input.length() - 1)) * 3600;
            } else if (input.endsWith("m")) {
                return Long.parseLong(input.substring(0, input.length() - 1)) * 60;
            } else if (input.endsWith("s")) {
                return Long.parseLong(input.substring(0, input.length() - 1));
            } else {
                return Long.parseLong(input);
            }
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String formatDuration(long seconds) {
        if (seconds <= 0) return "0s";
        
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (secs > 0) sb.append(secs).append("s");

        return sb.toString().trim();
    }

    public static String formatCountdown(long seconds) {
        if (seconds <= 0) return "0s";
        
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }
}
