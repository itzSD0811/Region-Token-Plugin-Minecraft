package com.regiontokens.database;

import org.bukkit.plugin.java.JavaPlugin;
import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private Connection connection;
    private final JavaPlugin plugin;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        initDatabase();
    }

    private void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            String dbPath = plugin.getDataFolder().getAbsolutePath() + "/playerdata.db";
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            
            createTables();
            plugin.getLogger().info("Database initialized!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS player_tokens (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "player_uuid TEXT NOT NULL," +
                "zone_id TEXT NOT NULL," +
                "token_id TEXT NOT NULL," +
                "time_remaining LONG NOT NULL," +
                "UNIQUE(player_uuid, zone_id, token_id)" +
                ")");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS zone_selections (" +
                "player_uuid TEXT PRIMARY KEY," +
                "world TEXT NOT NULL," +
                "x1 INTEGER," +
                "y1 INTEGER," +
                "z1 INTEGER," +
                "x2 INTEGER," +
                "y2 INTEGER," +
                "z2 INTEGER" +
                ")");
        }
    }

    public void setPlayerTokenTime(String playerUUID, String zoneId, String tokenId, long timeRemaining) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "INSERT OR REPLACE INTO player_tokens (player_uuid, zone_id, token_id, time_remaining) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, playerUUID);
            stmt.setString(2, zoneId);
            stmt.setString(3, tokenId);
            stmt.setLong(4, timeRemaining);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public long getPlayerTokenTime(String playerUUID, String zoneId, String tokenId) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT time_remaining FROM player_tokens WHERE player_uuid = ? AND zone_id = ? AND token_id = ?")) {
            stmt.setString(1, playerUUID);
            stmt.setString(2, zoneId);
            stmt.setString(3, tokenId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("time_remaining");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public Map<String, Long> getPlayerTokensForZone(String playerUUID, String zoneId) {
        Map<String, Long> tokens = new HashMap<>();
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT token_id, time_remaining FROM player_tokens WHERE player_uuid = ? AND zone_id = ?")) {
            stmt.setString(1, playerUUID);
            stmt.setString(2, zoneId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tokens.put(rs.getString("token_id"), rs.getLong("time_remaining"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tokens;
    }

    public long getPlayerTokenTime(String playerUUID, String zoneId) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT time_remaining FROM player_tokens WHERE player_uuid = ? AND zone_id = ? LIMIT 1")) {
            stmt.setString(1, playerUUID);
            stmt.setString(2, zoneId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("time_remaining");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void removePlayerToken(String playerUUID, String zoneId) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "DELETE FROM player_tokens WHERE player_uuid = ? AND zone_id = ?")) {
            stmt.setString(1, playerUUID);
            stmt.setString(2, zoneId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removePlayerTokenSpecific(String playerUUID, String zoneId, String tokenId) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "DELETE FROM player_tokens WHERE player_uuid = ? AND zone_id = ? AND token_id = ?")) {
            stmt.setString(1, playerUUID);
            stmt.setString(2, zoneId);
            stmt.setString(3, tokenId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setZoneSelection(String playerUUID, String world, int x1, int y1, int z1, int x2, int y2, int z2) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "INSERT OR REPLACE INTO zone_selections (player_uuid, world, x1, y1, z1, x2, y2, z2) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, playerUUID);
            stmt.setString(2, world);
            stmt.setInt(3, x1);
            stmt.setInt(4, y1);
            stmt.setInt(5, z1);
            stmt.setInt(6, x2);
            stmt.setInt(7, y2);
            stmt.setInt(8, z2);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int[] getZoneSelection(String playerUUID) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT x1, y1, z1, x2, y2, z2, world FROM zone_selections WHERE player_uuid = ?")) {
            stmt.setString(1, playerUUID);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new int[]{rs.getInt("x1"), rs.getInt("y1"), rs.getInt("z1"), 
                                    rs.getInt("x2"), rs.getInt("y2"), rs.getInt("z2")};
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getZoneSelectionWorld(String playerUUID) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT world FROM zone_selections WHERE player_uuid = ?")) {
            stmt.setString(1, playerUUID);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("world");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void clearZoneSelection(String playerUUID) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "DELETE FROM zone_selections WHERE player_uuid = ?")) {
            stmt.setString(1, playerUUID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
