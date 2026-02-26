package fr.miuby.survi.system.database.repository;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.quest.PlayerQuestData;
import fr.miuby.survi.system.log.LogManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class QuestRepository {
    private final Connection connection;

    public QuestRepository(Connection connection) {
        this.connection = connection;
    }

    // === REPUTATION ===

    public Map<String, Integer> getReputation(UUID playerUuid) {
        Map<String, Integer> reputations = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT trader_id, reputation FROM player_reputation WHERE player_uuid = ?")) {

            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reputations.put(rs.getString("trader_id"), rs.getInt("reputation"));
                }
            }
        } catch (SQLException ex) {
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.REPUTATION, "Failed to get reputation (" + ex.getMessage() + ")");
        }
        return reputations;
    }

    public void updateReputation(UUID playerUuid, String traderId, int reputation) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(
                GameManager.getInstance().getPlugin(), () -> {
                    try (Connection conn = GameManager.getInstance().getDatabase().getSQLConnection();
                         PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO player_reputation (player_uuid, trader_id, reputation) VALUES (?, ?, ?)")) {

                        ps.setString(1, playerUuid.toString());
                        ps.setString(2, traderId);
                        ps.setInt(3, reputation);
                        ps.executeUpdate();

                    } catch (SQLException ex) {
                        LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.REPUTATION, "Failed to update reputation (" + ex.getMessage() + ")");
                    }
                }
        );
    }

    // === QUEST ===

    public PlayerQuestData getPlayerQuest(UUID playerUuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT quest_id, progress, last_accepted, is_completed, trader_id, claimed FROM player_quest WHERE player_uuid = ?")) {

            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PlayerQuestData(
                            rs.getString("quest_id"),
                            rs.getInt("progress"),
                            LocalDate.parse(rs.getString("last_accepted")),
                            rs.getBoolean("is_completed"),
                            rs.getString("trader_id"),
                            rs.getBoolean("claimed")
                    );
                }
            }
        } catch (SQLException ex) {
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.QUEST, "Failed to get player quest (" + ex.getMessage() + ")");
        }
        return null;
    }

    public void updatePlayerQuest(UUID playerUuid, PlayerQuestData questData) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(
                GameManager.getInstance().getPlugin(), () -> {
                    try (Connection conn = GameManager.getInstance().getDatabase().getSQLConnection();
                         PreparedStatement ps = conn.prepareStatement(
                                 "INSERT OR REPLACE INTO player_quest (player_uuid, quest_id, progress, last_accepted, is_completed, trader_id, claimed) " +
                                         "VALUES (?, ?, ?, ?, ?, ?, ?)")) {

                        ps.setString(1, playerUuid.toString());
                        ps.setString(2, questData.getQuestId());
                        ps.setInt(3, questData.getProgress());
                        ps.setString(4, questData.getLastAccepted().toString());
                        ps.setBoolean(5, questData.isCompleted());
                        ps.setString(6, questData.getTraderId());
                        ps.setBoolean(7, questData.isClaimed());
                        ps.executeUpdate();

                    } catch (SQLException ex) {
                        LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.QUEST, "Failed to update player quest (" + ex.getMessage() + ")");
                    }
                }
        );
    }

    public void clearPlayerQuest(UUID playerUuid) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(
                GameManager.getInstance().getPlugin(), () -> {
                    try (Connection conn = GameManager.getInstance().getDatabase().getSQLConnection();
                         PreparedStatement ps = conn.prepareStatement("DELETE FROM player_quest WHERE player_uuid = ?")) {

                        ps.setString(1, playerUuid.toString());
                        ps.executeUpdate();

                    } catch (SQLException ex) {
                        LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.QUEST, "Failed to clear player quest (" + ex.getMessage() + ")");
                    }
                }
        );
    }
}