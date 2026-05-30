package fr.miuby.survi.system.database;

import fr.miuby.survi.system.database.repository.*;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.log.ELogTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Classe abstraite représentant une base de données.
 * Délègue les opérations à des repositories spécialisés.
 */
public abstract class Database {
    protected Connection connection;

    // Repositories
    protected PlayerRepository playerRepository;
    protected VillagerRepository villagerRepository;
    protected CropRepository cropRepository;
    protected QuestRepository questRepository;
    protected SystemRepository systemRepository;

    public abstract Connection getSQLConnection();
    public abstract void load();
    public abstract void initialize();

    // === Delegates aux repositories ===

    // Player
    public PlayerRepository players() {
        return playerRepository;
    }

    // Villager
    public VillagerRepository villagers() {
        return villagerRepository;
    }

    // Crop
    public CropRepository crops() {
        return cropRepository;
    }

    // Quest & Reputation
    public QuestRepository quests() {
        return questRepository;
    }

    // System (logs, server data, etc.)
    public SystemRepository system() {
        return systemRepository;
    }

    public String Request(String sql) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs;

        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement(sql);

            // Détermine si c'est un SELECT ou autre chose
            if (sql.trim().split("\\s+")[0].equalsIgnoreCase("select")) {
                rs = ps.executeQuery();

                int column = rs.getMetaData().getColumnCount();
                StringBuilder result = new StringBuilder();

                while (rs.next()) {
                    for (int i = 1; i <= column; i++) {
                        result.append(rs.getString(i));
                        if (i != column) {
                            result.append(", ");
                        }
                    }
                    result.append("\n");
                }

                return result.toString();
            } else {
                // UPDATE, INSERT, DELETE, etc.
                ps.executeUpdate();
                return "Query executed !";
            }

        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.SYSTEM, "Failed to execute request: " + sql, ex);
            return "Error: " + ex.getMessage();
        } finally {
            closeResources(conn, ps);
        }
    }

    protected void closeResources(Connection conn, PreparedStatement ps) {
        try {
            if (ps != null)
                ps.close();
            if (conn != null)
                conn.close();
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.SYSTEM, "Failed to close database resources", ex);
        }
    }
}