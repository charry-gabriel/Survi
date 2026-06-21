package fr.miuby.survi.system.database.repository;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.lib.sqlite.MLRepository;
import fr.miuby.lib.sqlite.MLSQLite;
import fr.miuby.survi.quest.quest.QuestHistoryEntry;
import fr.miuby.survi.quest.quest.Quest;
import fr.miuby.survi.system.log.ELogTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class QuestHistoryRepository extends MLRepository {

    public QuestHistoryRepository(Connection connection, MLSQLite db) {
        super(connection, db);
    }

    // =========================================================================
    // Écriture
    // =========================================================================

    /**
     * Insère une entrée d'historique. Async.
     *
     * @param playerUuid   UUID du joueur
     * @param playerPseudo pseudo au moment de la complétion (dénormalisé pour l'affichage)
     * @param questId      identifiant de la quête
     * @param completedAt  date de complétion
     * @param difficulty   difficulté ({@link Quest#getDifficulty()} ou niveau du monde)
     * @param job          nom EJob ({@code null} si aucun métier lié)
     * @param questType    {@code "daily"} ou {@code "global"}
     * @param contribution contribution individuelle au progrès (0 pour daily)
     */
    public void insert(UUID playerUuid, String playerPseudo, String questId, LocalDate completedAt, int difficulty, String job, String questType, int contribution) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO quest_history (player_uuid, player_pseudo, quest_id, completed_at, difficulty, job, quest_type, contribution) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, playerPseudo);
                ps.setString(3, questId);
                ps.setString(4, completedAt.toString());
                ps.setInt(5, difficulty);
                ps.setString(6, job);
                ps.setString(7, questType);
                ps.setInt(8, contribution);
                ps.executeUpdate();
            }
        }, ELogTag.QUEST, "Failed to insert quest history for " + playerUuid);
    }

    // =========================================================================
    // Lecture — par joueur
    // =========================================================================

    /**
     * Retourne les {@code limit} dernières quêtes complétées par un joueur, plus récentes en premier.
     */
    public List<QuestHistoryEntry> getHistory(UUID playerUuid, int limit) {
        List<QuestHistoryEntry> entries = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, player_uuid, player_pseudo, quest_id, completed_at, difficulty, job, quest_type, contribution " +
                        "FROM quest_history WHERE player_uuid = ? ORDER BY id DESC LIMIT ?")) {
            ps.setString(1, playerUuid.toString());
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new QuestHistoryEntry(
                            rs.getInt("id"),
                            rs.getString("player_uuid"),
                            rs.getString("player_pseudo"),
                            rs.getString("quest_id"),
                            LocalDate.parse(rs.getString("completed_at")),
                            rs.getInt("difficulty"),
                            rs.getString("job"),
                            rs.getString("quest_type"),
                            rs.getInt("contribution")
                    ));
                }
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.QUEST, "Failed to get quest history for " + playerUuid, ex);
        }
        return entries;
    }

    /** Nombre total de quêtes complétées par un joueur (tous types confondus). */
    public int countCompleted(UUID playerUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM quest_history WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.QUEST, "Failed to count completed quests for " + playerUuid, ex);
        }
        return 0;
    }

    /** Nombre de quêtes journalières ({@code quest_type = 'daily'}) complétées par un joueur. */
    public int countDailyCompleted(UUID playerUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM quest_history WHERE player_uuid = ? AND quest_type = 'daily'")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.QUEST, "Failed to count daily completed quests for " + playerUuid, ex);
        }
        return 0;
    }

    /**
     * Nombre de quêtes complétées par difficulté pour un joueur.
     * Clé {@code -1} = quêtes globales (difficulté = niveau du monde).
     */
    public Map<Integer, Integer> countByDifficulty(UUID playerUuid) {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT difficulty, COUNT(*) as cnt FROM quest_history WHERE player_uuid = ? AND quest_type = 'daily' GROUP BY difficulty ORDER BY difficulty")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) counts.put(rs.getInt("difficulty"), rs.getInt("cnt"));
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.QUEST, "Failed to count quests by difficulty for " + playerUuid, ex);
        }
        return counts;
    }

    /** Nombre de quêtes complétées par type ({@code "daily"}/{@code "global"}) pour un joueur. */
    public Map<String, Integer> countByType(UUID playerUuid) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT quest_type, COUNT(*) as cnt FROM quest_history WHERE player_uuid = ? GROUP BY quest_type")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) counts.put(rs.getString("quest_type"), rs.getInt("cnt"));
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.QUEST, "Failed to count quests by type for " + playerUuid, ex);
        }
        return counts;
    }

    /**
     * Nombre de quêtes journalières complétées par métier, pour tous les joueurs en une seule requête.
     * Clé externe : UUID joueur ; clé interne : nom {@link fr.miuby.survi.job.EJob#name()}.
     * Utilisé pour reconstruire {@code player_reputation} depuis l'historique (réparation après corruption).
     */
    public Map<UUID, Map<String, Integer>> countDailyByPlayerAndJob() {
        Map<UUID, Map<String, Integer>> result = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT player_uuid, job, COUNT(*) as cnt FROM quest_history " +
                        "WHERE quest_type = 'daily' AND job IS NOT NULL GROUP BY player_uuid, job")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                    result.computeIfAbsent(uuid, k -> new HashMap<>()).put(rs.getString("job"), rs.getInt("cnt"));
                }
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.QUEST, "Failed to count daily quests by player and job", ex);
        }
        return result;
    }

    /**
     * Nombre de quêtes journalières complétées par identifiant de quête, pour tous les joueurs.
     * Clé externe : UUID joueur ; clé interne : {@code quest_id}.
     * Utilisé pour reconstruire les récompenses de réputation propres à chaque quête
     * (champ {@code rewards}) depuis l'historique (réparation après corruption).
     */
    public Map<UUID, Map<String, Integer>> countDailyByPlayerAndQuestId() {
        Map<UUID, Map<String, Integer>> result = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT player_uuid, quest_id, COUNT(*) as cnt FROM quest_history " +
                        "WHERE quest_type = 'daily' GROUP BY player_uuid, quest_id")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                    result.computeIfAbsent(uuid, k -> new HashMap<>()).put(rs.getString("quest_id"), rs.getInt("cnt"));
                }
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.QUEST, "Failed to count daily quests by player and quest id", ex);
        }
        return result;
    }

    /**
     * Nombre de quêtes globales complétées par identifiant de quête, pour tous les joueurs.
     * Clé externe : UUID joueur ; clé interne : {@code quest_id}.
     * Utilisé pour reconstruire les récompenses de réputation des quêtes globales
     * (champ {@code rewards}) depuis l'historique (réparation après corruption).
     */
    public Map<UUID, Map<String, Integer>> countGlobalByPlayerAndQuestId() {
        Map<UUID, Map<String, Integer>> result = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT player_uuid, quest_id, COUNT(*) as cnt FROM quest_history " +
                        "WHERE quest_type = 'global' GROUP BY player_uuid, quest_id")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                    result.computeIfAbsent(uuid, k -> new HashMap<>()).put(rs.getString("quest_id"), rs.getInt("cnt"));
                }
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.QUEST, "Failed to count global quests by player and quest id", ex);
        }
        return result;
    }

    // =========================================================================
    // Lecture — classements
    // =========================================================================

    /** Top {@code limit} joueurs par nombre total de quêtes complétées. */
    public List<PlayerRankEntry> getTopByCompletions(int limit) {
        List<PlayerRankEntry> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT player_pseudo, COUNT(*) as cnt FROM quest_history GROUP BY player_uuid ORDER BY cnt DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new PlayerRankEntry(rs.getString("player_pseudo"), rs.getLong("cnt")));
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.QUEST, "Failed to get top completions leaderboard", ex);
        }
        return list;
    }

    /** Top {@code limit} joueurs par nombre de quêtes journalières complétées. */
    public List<PlayerRankEntry> getTopByDailyCompletions(int limit) {
        List<PlayerRankEntry> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT player_pseudo, COUNT(*) as cnt FROM quest_history WHERE quest_type = 'daily' GROUP BY player_uuid ORDER BY cnt DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new PlayerRankEntry(rs.getString("player_pseudo"), rs.getLong("cnt")));
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.QUEST, "Failed to get top daily leaderboard", ex);
        }
        return list;
    }

    /** Top {@code limit} joueurs par nombre de quêtes globales complétées. */
    public List<PlayerRankEntry> getTopByGlobalCompletions(int limit) {
        List<PlayerRankEntry> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT player_pseudo, COUNT(*) as cnt FROM quest_history WHERE quest_type = 'global' GROUP BY player_uuid ORDER BY cnt DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new PlayerRankEntry(rs.getString("player_pseudo"), rs.getLong("cnt")));
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.QUEST, "Failed to get top global leaderboard", ex);
        }
        return list;
    }

    public record PlayerRankEntry(String pseudo, long count) {}
}