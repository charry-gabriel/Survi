package fr.miuby.survi.system.database;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.lib.sqlite.MLSQLite;
import fr.miuby.survi.system.database.repository.*;
import fr.miuby.survi.system.log.ELogTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Classe abstraite représentant la base de données de Survi.
 * Étend {@link MLSQLite} pour la gestion de la connexion et des migrations.
 * Délègue les opérations métier à des repositories spécialisés.
 *
 * <p>L'implémentation concrète ({@link SQLite}) fournit la version cible du schéma,
 * le SQL de création des tables et la logique de migration.</p>
 */
public abstract class Database extends MLSQLite {

    // Repositories
    protected PlayerRepository playerRepository;
    protected VillagerRepository villagerRepository;
    protected CropRepository cropRepository;
    protected QuestRepository questRepository;
    protected SystemRepository systemRepository;

    protected Database(String dbName) {
        super(dbName);
    }

    /**
     * Initialise les repositories après que la connexion est ouverte et les migrations appliquées.
     * Appelé automatiquement par {@link MLSQLite#load()}.
     * Les sous-classes qui surchargent cette méthode doivent appeler {@code super.onLoaded()} en premier.
     */
    @Override
    protected void onLoaded() {
        Connection conn = getConnection();
        playerRepository   = new PlayerRepository(conn);
        villagerRepository = new VillagerRepository(conn);
        cropRepository     = new CropRepository(conn);
        questRepository    = new QuestRepository(conn);
        systemRepository   = new SystemRepository(conn);
    }

    // =========================================================================
    // Délégués aux repositories
    // =========================================================================

    public PlayerRepository players() {
        return playerRepository;
    }

    public VillagerRepository villagers() {
        return villagerRepository;
    }

    public CropRepository crops() {
        return cropRepository;
    }

    public QuestRepository quests() {
        return questRepository;
    }

    public SystemRepository system() {
        return systemRepository;
    }

    // =========================================================================
    // Utilitaire SQL brut (usage debug — /sql query)
    // =========================================================================

    public String Request(String sql) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);

            if (sql.trim().split("\\s+")[0].equalsIgnoreCase("select")) {
                ResultSet rs = ps.executeQuery();
                int column = rs.getMetaData().getColumnCount();
                StringBuilder result = new StringBuilder();

                while (rs.next()) {
                    for (int i = 1; i <= column; i++) {
                        result.append(rs.getString(i));
                        if (i != column) result.append(", ");
                    }
                    result.append("\n");
                }

                return result.toString();
            } else {
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
            if (ps != null) ps.close();
            if (conn != null) conn.close();
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.SYSTEM, "Failed to close database resources", ex);
        }
    }
}