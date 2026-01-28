package com.regiontokens.util;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigUtil {
    private static FileConfiguration config;

    public static void setConfig(FileConfiguration cfg) {
        config = cfg;
    }

    public static String getMessage(String path) {
        if (config == null) return path;
        String msg = config.getString("messages." + path, "");
        msg = ColorUtil.colorize(msg);
        
        // Always replace {prefix} in all messages
        if (msg.contains("{prefix}")) {
            String prefix = config.getString("messages.prefix", "");
            msg = msg.replace("{prefix}", ColorUtil.colorize(prefix));
        }
        
        return msg;
    }

    public static String getMessage(String path, Object... replacements) {
        if (config == null) return path;
        String msg = config.getString("messages." + path, "");
        msg = ColorUtil.colorize(msg);
        
        // Always replace {prefix} first
        if (msg.contains("{prefix}")) {
            String prefix = config.getString("messages.prefix", "");
            msg = msg.replace("{prefix}", ColorUtil.colorize(prefix));
        }
        
        // Then replace any custom placeholders
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                msg = msg.replace("{" + replacements[i] + "}", String.valueOf(replacements[i + 1]));
            }
        }
        return msg;
    }

    public static int getItemsPerPage() {
        if (config == null) return 5;
        return config.getInt("pagination.items-per-page", 5);
    }

    public static int getTimeWarningThreshold() {
        if (config == null) return 10;
        return config.getInt("settings.time-warning-threshold", 10);
    }
}
