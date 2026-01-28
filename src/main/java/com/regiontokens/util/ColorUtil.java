package com.regiontokens.util;

import org.bukkit.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {
    private static final Pattern colorPattern = Pattern.compile("&[0-9a-fA-Fk-oK-OrR]");

    public static String colorize(String text) {
        if (text == null) return null;
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String stripColor(String text) {
        if (text == null) return null;
        return ChatColor.stripColor(colorize(text));
    }

    public static String getColorCode(String text) {
        Matcher matcher = colorPattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return "&f";
    }
}
