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
 * Persiste les récompenses de quête globale (complétion ou timeout) destinées à un joueur hors ligne
 * au moment de la fin de la quête, pour qu'elles survivent à un redémarrage du serveur et soient
 * livrées à la reconnexion par {@link fr.miuby.survi.player.service.OfflineNotificationService#deliverQuestRewards}.
 */
public class GlobalQuestPendingRewardRepository extends MLRepository {

    public record PendingReward(String questId, boolean applyRewards, String message) {}

    public GlobalQuestPendingRewardRepository(Connection connection, MLSQLite db) {
        super(connection, db);
    }

    // -------------------------------------------------------------------------
    // Lecture (thread principal)
    // -------------------------------------------------------------------------

    public List<PendingReward> loadForPlayer(UUID playerUuid) {
        List<PendingReward> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT quest_id, apply_rewards, message FROM global_quest_pending_reward WHERE player_uuid = ? ORDER BY id")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new PendingReward(rs.getString("quest_id"), rs.getInt("apply_rewards") != 0, rs.getString("message")));
                }
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.QUEST, "Failed to load global_quest_pending_reward for " + playerUuid, ex);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Écriture (async)
    // -------------------------------------------------------------------------

    public void save(UUID playerUuid, String questId, boolean applyRewards, String message) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO global_quest_pending_reward (player_uuid, quest_id, apply_rewards, message) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, questId);
                ps.setInt(3, applyRewards ? 1 : 0);
                ps.setString(4, message);
                ps.executeUpdate();
            }
        }, ELogTag.QUEST, "Failed to save global_quest_pending_reward for " + playerUuid);
    }

    public void deleteForPlayer(UUID playerUuid) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM global_quest_pending_reward WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                ps.executeUpdate();
            }
        }, ELogTag.QUEST, "Failed to delete global_quest_pending_reward for " + playerUuid);
    }
}