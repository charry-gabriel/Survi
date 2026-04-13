package fr.miuby.survi.system.database.repository;

import fr.miuby.survi.system.log.LogManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Repository pour les données système : server_data, logs, config, etc.
 */
public class SystemRepository {
    private final Connection connection;

    public SystemRepository(Connection connection) {
        this.connection = connection;
    }

    public String getServerData(String key) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT value FROM server_data WHERE key = ?")) {

            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("value");
            }
        } catch (SQLException e) {
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.SYSTEM, "Erreur getServerData: " + key + " (" + e.getMessage() + ")");
        }
        return null;
    }

    public void saveServerData(String key, String value) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT OR REPLACE INTO server_data (key, value) VALUES (?, ?)")) {

            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();

        } catch (SQLException e) {
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.SYSTEM, "Erreur saveServerData: " + key + " (" + e.getMessage() + ")");
        }
    }

    public int getWorldLevel() {
        String raw = getServerData("world_level");
        try {
            return raw != null ? Integer.parseInt(raw) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void saveWorldLevel(int level) {
        saveServerData("world_level", String.valueOf(level));
    }

    //region log
    /**
     * Récupère l'état d'un tag de log (VILLAGER, QUEST, etc.).
     * @return true/false ou null si pas trouvé
     */
    public Boolean getLogTagState(String tagName) {
        String key = "log_tag_" + tagName.toLowerCase();
        String value = getServerData(key);
        return value != null ? Boolean.parseBoolean(value) : null;
    }

    /**
     * Sauvegarde l'état d'un tag de log.
     */
    public void saveLogTagState(String tagName, boolean enabled) {
        String key = "log_tag_" + tagName.toLowerCase();
        saveServerData(key, String.valueOf(enabled));
    }

    // === LOG LEVEL STATE ===

    /**
     * Récupère l'état d'un level de log (INFO, WARNING, SEVERE).
     * @return true/false ou null si pas trouvé
     */
    public Boolean getLogLevelState(String levelName) {
        String key = "log_level_" + levelName.toLowerCase();
        String value = getServerData(key);
        return value != null ? Boolean.parseBoolean(value) : null;
    }

    /**
     * Sauvegarde l'état d'un level de log.
     */
    public void saveLogLevelState(String levelName, boolean enabled) {
        String key = "log_level_" + levelName.toLowerCase();
        saveServerData(key, String.valueOf(enabled));
    }
    //endregion
}