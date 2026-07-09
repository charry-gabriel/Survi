package fr.miuby.survi.system.database.repository;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.lib.sqlite.MLRepository;
import fr.miuby.lib.sqlite.MLSQLite;
import fr.miuby.survi.system.log.ELogTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Persiste les montées de niveau de villageois survenues pendant qu'un joueur était hors ligne,
 * pour qu'elles survivent à un redémarrage du serveur et soient livrées à la reconnexion par
 * {@link fr.miuby.survi.player.service.OfflineNotificationService}.
 */
public class PendingVillagerLevelUpRepository extends MLRepository {

    public record PendingVillagerLevelUp(String nameId, int oldLevel, int newLevel) {}

    public PendingVillagerLevelUpRepository(Connection connection, MLSQLite db) {
        super(connection, db);
    }

    // -------------------------------------------------------------------------
    // Lecture (thread principal)
    // -------------------------------------------------------------------------

    public List<PendingVillagerLevelUp> loadForPlayer(UUID playerUuid) {
        List<PendingVillagerLevelUp> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT name_id, old_level, new_level FROM player_pending_villager_levelup WHERE player_uuid = ? ORDER BY id")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new PendingVillagerLevelUp(rs.getString("name_id"), rs.getInt("old_level"), rs.getInt("new_level")));
                }
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.VILLAGER, "Failed to load player_pending_villager_levelup for " + playerUuid, ex);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Écriture (async)
    // -------------------------------------------------------------------------

    public void save(UUID playerUuid, String nameId, int oldLevel, int newLevel) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO player_pending_villager_levelup (player_uuid, name_id, old_level, new_level) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, nameId);
                ps.setInt(3, oldLevel);
                ps.setInt(4, newLevel);
                ps.executeUpdate();
            }
        }, ELogTag.VILLAGER, "Failed to save player_pending_villager_levelup for " + playerUuid);
    }

    public void deleteForPlayer(UUID playerUuid) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM player_pending_villager_levelup WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                ps.executeUpdate();
            }
        }, ELogTag.VILLAGER, "Failed to delete player_pending_villager_levelup for " + playerUuid);
    }
}