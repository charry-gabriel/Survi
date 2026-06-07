package fr.miuby.survi.system.database.repository;

import fr.miuby.lib.sqlite.MLRepository;
import fr.miuby.lib.sqlite.MLSQLite;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.quest.quest.PlayerQuestData;
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

public class QuestRepository extends MLRepository {

    public QuestRepository(Connection connection, MLSQLite db) {
        super(connection, db);
    }

    // =========================================================================
    // RÉPUTATION
    // =========================================================================

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
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.REPUTATION, "Failed to get reputation", ex);
        }
        return reputations;
    }

    public void updateReputation(UUID playerUuid, String traderId, int reputation) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO player_reputation (player_uuid, trader_id, reputation) VALUES (?, ?, ?)")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, traderId);
                ps.setInt(3, reputation);
                ps.executeUpdate();
            }
        }, ELogTag.REPUTATION, "Failed to update reputation");
    }

    // =========================================================================
    // QUÊTES — lecture
    // =========================================================================

    /**
     * Retourne uniquement les quêtes acceptées à la date donnée (typiquement aujourd'hui).
     * Exploite l'index idx_pq_uuid_date — aucun scan complet de la table.
     */
    public List<PlayerQuestData> getActivePlayerQuests(UUID playerUuid, LocalDate date) {
        List<PlayerQuestData> quests = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT slot, quest_id, progress, last_accepted, is_completed, trader_id, claimed " +
                        "FROM player_quest WHERE player_uuid = ? AND last_accepted = ? ORDER BY slot ASC")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, date.toString());
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
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.QUEST, "Failed to get active player quests", ex);
        }
        return quests;
    }

    /**
     * Supprime en une seule requête toutes les quêtes antérieures à la date donnée.
     * Exploite l'index idx_pq_uuid_date — remplace les suppressions slot-par-slot.
     */
    public void deleteExpiredPlayerQuests(UUID playerUuid, LocalDate today) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM player_quest WHERE player_uuid = ? AND last_accepted < ?")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, today.toString());
                ps.executeUpdate();
            }
        }, ELogTag.QUEST, "Failed to delete expired player quests");
    }

    /**
     * Retourne toutes les quêtes du joueur (tous les slots, toutes les dates).
     * Réservé aux opérations admin/debug — préférer {@link #getActivePlayerQuests} à la connexion.
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

    /** Insère ou met à jour un slot de quête (clé : player_uuid + slot). */
    public void updatePlayerQuest(UUID playerUuid, PlayerQuestData questData) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
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
            }
        }, ELogTag.QUEST, "Failed to update player quest");
    }

    /** Supprime un slot de quête précis (reset admin ou nettoyage). */
    public void deletePlayerQuestSlot(UUID playerUuid, int slot) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM player_quest WHERE player_uuid = ? AND slot = ?")) {
                ps.setString(1, playerUuid.toString());
                ps.setInt(2, slot);
                ps.executeUpdate();
            }
        }, ELogTag.QUEST, "Failed to delete player quest slot");
    }

    /** Supprime TOUS les slots de quêtes d'un joueur (reset complet admin). */
    public void clearAllPlayerQuests(UUID playerUuid) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM player_quest WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                ps.executeUpdate();
            }
        }, ELogTag.QUEST, "Failed to clear all player quests");
    }

    // =========================================================================
    // ANTI-RÉPÉTITION — dernière quête attribuée
    // =========================================================================

    /** Retourne l'ID de la dernière quête attribuée. Null si aucune donnée. */
    public String getLastQuestId(UUID playerUuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT last_quest_id FROM player_quest_meta WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("last_quest_id");
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.QUEST, "Failed to get last quest id", ex);
        }
        return null;
    }

    /** Mémorise la dernière quête attribuée pour l'anti-répétition. */
    public void setLastQuestId(UUID playerUuid, String questId) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO player_quest_meta (player_uuid, last_quest_id) VALUES (?, ?)")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, questId);
                ps.executeUpdate();
            }
        }, ELogTag.QUEST, "Failed to set last quest id");
    }

    // =========================================================================
    // CLASSEMENTS — réputation
    // =========================================================================

    /**
     * Top {@code limit} joueurs par réputation totale (somme de tous les métiers).
     * Seules les entrées dont le {@code trader_id} correspond à un nom de {@link fr.miuby.survi.job.EJob}
     * sont comptabilisées ; les anciennes entrées trader-id sont naturellement exclues via la jointure.
     */
    public List<ReputationRankEntry> getTopByTotalReputation(int limit) {
        List<ReputationRankEntry> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT p.pseudo, SUM(pr.reputation) as total " +
                        "FROM player_reputation pr JOIN player p ON p.uuid = pr.player_uuid " +
                        "GROUP BY pr.player_uuid ORDER BY total DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new ReputationRankEntry(rs.getString("pseudo"), rs.getLong("total")));
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.REPUTATION, "Failed to get total reputation leaderboard", ex);
        }
        return list;
    }

    /**
     * Top {@code limit} joueurs pour un métier spécifique ({@code jobName} = {@link fr.miuby.survi.job.EJob#name()}).
     */
    public List<ReputationRankEntry> getTopByJob(String jobName, int limit) {
        List<ReputationRankEntry> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT p.pseudo, pr.reputation " +
                        "FROM player_reputation pr JOIN player p ON p.uuid = pr.player_uuid " +
                        "WHERE pr.trader_id = ? ORDER BY pr.reputation DESC LIMIT ?")) {
            ps.setString(1, jobName);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new ReputationRankEntry(rs.getString("pseudo"), rs.getLong("reputation")));
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.REPUTATION, "Failed to get job reputation leaderboard for " + jobName, ex);
        }
        return list;
    }

    public record ReputationRankEntry(String pseudo, long value) {}
}