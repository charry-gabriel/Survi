package fr.miuby.survi.system.database.repository;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.lib.sqlite.MLRepository;
import fr.miuby.lib.sqlite.MLSQLite;
import fr.miuby.survi.system.log.ELogTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Persiste, pour un joueur hors ligne, le niveau du monde juste avant la première montée de
 * niveau manquée, afin qu'elle survive à un redémarrage du serveur et soit livrée à la
 * reconnexion par {@link fr.miuby.survi.player.service.OfflineNotificationService}.
 */
public class PendingWorldLevelUpRepository extends MLRepository {

    public PendingWorldLevelUpRepository(Connection connection, MLSQLite db) {
        super(connection, db);
    }

    // -------------------------------------------------------------------------
    // Lecture (thread principal)
    // -------------------------------------------------------------------------

    /** @return le niveau du monde avant la première montée manquée, ou {@code null} si aucune. */
    public Integer loadForPlayer(UUID playerUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT old_level FROM player_pending_world_levelup WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("old_level") : null;
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.WORLD, "Failed to load player_pending_world_levelup for " + playerUuid, ex);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Écriture (async)
    // -------------------------------------------------------------------------

    /** N'écrase pas une valeur déjà enregistrée : on garde le niveau de la 1ère montée manquée. */
    public void save(UUID playerUuid, int oldLevel) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT OR IGNORE INTO player_pending_world_levelup (player_uuid, old_level) VALUES (?, ?)")) {
                ps.setString(1, playerUuid.toString());
                ps.setInt(2, oldLevel);
                ps.executeUpdate();
            }
        }, ELogTag.WORLD, "Failed to save player_pending_world_levelup for " + playerUuid);
    }

    public void deleteForPlayer(UUID playerUuid) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM player_pending_world_levelup WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                ps.executeUpdate();
            }
        }, ELogTag.WORLD, "Failed to delete player_pending_world_levelup for " + playerUuid);
    }
}