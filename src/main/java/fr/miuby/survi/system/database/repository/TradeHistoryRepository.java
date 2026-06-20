package fr.miuby.survi.system.database.repository;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.lib.sqlite.MLRepository;
import fr.miuby.lib.sqlite.MLSQLite;
import fr.miuby.survi.system.log.ELogTag;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;

public class TradeHistoryRepository extends MLRepository {

    public TradeHistoryRepository(Connection connection, MLSQLite db) {
        super(connection, db);
    }

    // =========================================================================
    // Écriture
    // =========================================================================

    public void insert(UUID playerUuid, String playerPseudo, String traderId, String itemMaterial, int quantity) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO player_trade_history (player_uuid, player_pseudo, trader_id, item_material, quantity, traded_at) VALUES (?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, playerPseudo);
                ps.setString(3, traderId);
                ps.setString(4, itemMaterial);
                ps.setInt(5, quantity);
                ps.setString(6, LocalDate.now().toString());
                ps.executeUpdate();
            }
        }, ELogTag.PLAYER, "Failed to insert trade history for " + playerUuid);
    }

    // =========================================================================
    // Lecture — par joueur
    // =========================================================================

    /** Nombre total d'opérations d'achat d'un joueur (tous traders confondus). */
    public int countTotal(UUID playerUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM player_trade_history WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.PLAYER, "Failed to count trades for " + playerUuid, ex);
        }
        return 0;
    }

    /** Nombre d'achats par traderId pour un joueur, trié par volume décroissant. */
    public Map<String, Integer> countByTrader(UUID playerUuid) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT trader_id, COUNT(*) as cnt FROM player_trade_history WHERE player_uuid = ? GROUP BY trader_id ORDER BY cnt DESC")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) counts.put(rs.getString("trader_id"), rs.getInt("cnt"));
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.PLAYER, "Failed to count trades by trader for " + playerUuid, ex);
        }
        return counts;
    }

    // =========================================================================
    // Lecture — classement
    // =========================================================================

    /** Top {@code limit} joueurs par nombre total d'achats. */
    public List<PlayerRankEntry> getTopByPurchases(int limit) {
        List<PlayerRankEntry> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT player_pseudo, COUNT(*) as cnt FROM player_trade_history GROUP BY player_uuid ORDER BY cnt DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new PlayerRankEntry(rs.getString("player_pseudo"), rs.getLong("cnt")));
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.PLAYER, "Failed to get top trades leaderboard", ex);
        }
        return list;
    }

    public record PlayerRankEntry(String pseudo, long count) {}
}