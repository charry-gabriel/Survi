package fr.miuby.survi.system.database.repository;

import fr.miuby.lib.sqlite.MLRepository;
import fr.miuby.lib.sqlite.MLSQLite;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.log.ELogTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Repository pour les données système : server_data, logs, config, etc.
 */
public class SystemRepository extends MLRepository {

    public SystemRepository(Connection connection, MLSQLite db) {
        super(connection, db);
    }

    public String getServerData(String key) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT value FROM server_data WHERE key = ?")) {
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("value");
        } catch (SQLException e) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.SYSTEM, "Erreur getServerData: " + key, e);
        }
        return null;
    }

    public void saveServerData(String key, String value) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT OR REPLACE INTO server_data (key, value) VALUES (?, ?)")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.SYSTEM, "Erreur saveServerData: " + key, e);
        }
    }

    public void deleteServerData(String key) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM server_data WHERE key = ?")) {
            ps.setString(1, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.SYSTEM, "Erreur deleteServerData: " + key, e);
        }
    }

    public int getWorldLevel() {
        String raw = getServerData("world_level");
        try {
            return raw != null ? Integer.parseInt(raw) : 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public void saveWorldLevel(int level) {
        saveServerData("world_level", String.valueOf(level));
    }

    public void saveExtraGlobalSlots(int value) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO server_data (key, value) VALUES (?, ?)")) {
                ps.setString(1, "quest_extra_global_slots");
                ps.setString(2, String.valueOf(value));
                ps.executeUpdate();
            }
        }, ELogTag.SYSTEM, "Erreur saveExtraGlobalSlots");
    }

    //region log

    /** Récupère l'état d'un tag de log (VILLAGER, QUEST, etc.). @return true/false ou null si pas trouvé */
    public Boolean getLogTagState(String tagName) {
        String value = getServerData("log_tag_" + tagName.toLowerCase());
        return value != null ? Boolean.parseBoolean(value) : null;
    }

    /** Sauvegarde l'état d'un tag de log. */
    public void saveLogTagState(String tagName, boolean enabled) {
        saveServerData("log_tag_" + tagName.toLowerCase(), String.valueOf(enabled));
    }

    /** Récupère l'état d'un level de log (INFO, WARNING, SEVERE). @return true/false ou null si pas trouvé */
    public Boolean getLogLevelState(String levelName) {
        String value = getServerData("log_level_" + levelName.toLowerCase());
        return value != null ? Boolean.parseBoolean(value) : null;
    }

    /** Sauvegarde l'état d'un level de log. */
    public void saveLogLevelState(String levelName, boolean enabled) {
        saveServerData("log_level_" + levelName.toLowerCase(), String.valueOf(enabled));
    }

    //endregion
}