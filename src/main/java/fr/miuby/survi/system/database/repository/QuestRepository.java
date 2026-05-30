package fr.miuby.survi.system.database.repository;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.quest.PlayerQuestData;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.log.ELogTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class QuestRepository {
    private final Connection connection;

    public QuestRepository(Connection connection) {
        this.connection = connection;
    }

    // =========================================================================
    // RÉPUTATION
    // =========================================================================

    public Map<String, Integer> getReputation(UUID playerUuid) {
        Map<String, Integer> reputations = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT trader_id, reputation FROM player_reputation WHERE player_uuid = ?")) {

            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reputations.put(rs.getString("trader_id"), rs.getInt("reputation"));
                }
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.REPUTATION, "Failed to get reputation", ex);
        }
        return reputations;
    }

    public void updateReputation(UUID playerUuid, String traderId, int reputation) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(
                GameManager.getInstance().getPlugin(), () -> {
                    try (Connection conn = GameManager.getInstance().getDatabase().getConnection();
                         PreparedStatement ps = conn.prepareStatement(
                                 "INSERT OR REPLACE INTO player_reputation (player_uuid, trader_id, reputation) VALUES (?, ?, ?)")) {

                        ps.setString(1, playerUuid.toString());
                        ps.setString(2, traderId);
                        ps.setInt(3, reputation);
                        ps.executeUpdate();

                    } catch (SQLException ex) {
                        MLLogManager.getInstance().log(Level.SEVERE, ELogTag.REPUTATION, "Failed to update reputation", ex);
                    }
                }
        );
    }

    // =========================================================================
    // QUÊTES — lecture
    // =========================================================================

    /**
     * Retourne toutes les quêtes du joueur (tous les slots, toutes les dates).
     * La sélection des quêtes valides vs expirées se fait dans AlphaPlayer.
     */
    public List<PlayerQuestData> getPlayerQuests(UUID playerUuid) {
        List<PlayerQuestData> quests = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT slot, quest_id, progress, last_accepted, is_completed, trader_id, claimed " +
                        "FROM player_quest WHERE player_uuid = ? ORDER BY slot ASC")) {

            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    quests.add(new PlayerQuestData(
                            rs.getInt("slot"),
                            rs.getString("quest_id"),
                            rs.getInt("progress"),
                            LocalDate.parse(rs.getString("last_accepted")),
                            rs.getBoolean("is_completed"),
                            rs.getString("trader_id"),
                            rs.getBoolean("claimed")
                    ));
                }
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.QUEST, "Failed to get player quests", ex);
        }
        return quests;
    }

    // =========================================================================
    // QUÊTES — écriture
    // =========================================================================

    /**
     * Insère ou met à jour un slot de quête (clé : player_uuid + slot).
     */
    public void updatePlayerQuest(UUID playerUuid, PlayerQuestData questData) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(
                GameManager.getInstance().getPlugin(), () -> {
                    try (Connection conn = GameManager.getInstance().getDatabase().getConnection();
                         PreparedStatement ps = conn.prepareStatement(
                                 "INSERT OR REPLACE INTO player_quest " +
                                         "(player_uuid, slot, quest_id, progress, last_accepted, is_completed, trader_id, claimed) " +
                                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {

                        ps.setString(1, playerUuid.toString());
                        ps.setInt(2, questData.getSlot());
                        ps.setString(3, questData.getQuestId());
                        ps.setInt(4, questData.getProgress());
                        ps.setString(5, questData.getLastAccepted().toString());
                        ps.setBoolean(6, questData.isCompleted());
                        ps.setString(7, questData.getTraderId());
                        ps.setBoolean(8, questData.isClaimed());
                        ps.executeUpdate();

                    } catch (SQLException ex) {
                        MLLogManager.getInstance().log(Level.SEVERE, ELogTag.QUEST, "Failed to update player quest", ex);
                    }
                }
        );
    }

    /**
     * Supprime un slot de quête précis (utilisé lors du reset admin ou du nettoyage).
     */
    public void deletePlayerQuestSlot(UUID playerUuid, int slot) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(
                GameManager.getInstance().getPlugin(), () -> {
                    try (Connection conn = GameManager.getInstance().getDatabase().getConnection();
                         PreparedStatement ps = conn.prepareStatement(
                                 "DELETE FROM player_quest WHERE player_uuid = ? AND slot = ?")) {

                        ps.setString(1, playerUuid.toString());
                        ps.setInt(2, slot);
                        ps.executeUpdate();

                    } catch (SQLException ex) {
                        MLLogManager.getInstance().log(Level.SEVERE, ELogTag.QUEST, "Failed to delete player quest slot", ex);
                    }
                }
        );
    }

    /**
     * Supprime TOUS les slots de quêtes d'un joueur (reset complet admin).
     */
    public void clearAllPlayerQuests(UUID playerUuid) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(
                GameManager.getInstance().getPlugin(), () -> {
                    try (Connection conn = GameManager.getInstance().getDatabase().getConnection();
                         PreparedStatement ps = conn.prepareStatement(
                                 "DELETE FROM player_quest WHERE player_uuid = ?")) {

                        ps.setString(1, playerUuid.toString());
                        ps.executeUpdate();

                    } catch (SQLException ex) {
                        MLLogManager.getInstance().log(Level.SEVERE, ELogTag.QUEST, "Failed to clear all player quests", ex);
                    }
                }
        );
    }

    // =========================================================================
    // ANTI-RÉPÉTITION — dernière quête attribuée (persisté entre les restarts)
    // =========================================================================

    /**
     * Retourne l'ID de la dernière quête attribuée au joueur.
     * Null si aucune donnée (premier jour de jeu ou table vide).
     */
    public String getLastQuestId(UUID playerUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT last_quest_id FROM player_quest_meta WHERE player_uuid = ?")) {

            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("last_quest_id");
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.QUEST, "Failed to get last quest id", ex);
        }
        return null;
    }

    /**
     * Mémorise la dernière quête attribuée pour l'anti-répétition.
     */
    public void setLastQuestId(UUID playerUuid, String questId) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(
                GameManager.getInstance().getPlugin(), () -> {
                    try (Connection conn = GameManager.getInstance().getDatabase().getConnection();
                         PreparedStatement ps = conn.prepareStatement(
                                 "INSERT OR REPLACE INTO player_quest_meta (player_uuid, last_quest_id) VALUES (?, ?)")) {

                        ps.setString(1, playerUuid.toString());
                        ps.setString(2, questId);
                        ps.executeUpdate();

                    } catch (SQLException ex) {
                        MLLogManager.getInstance().log(Level.SEVERE, ELogTag.QUEST, "Failed to set last quest id", ex);
                    }
                }
        );
    }
}