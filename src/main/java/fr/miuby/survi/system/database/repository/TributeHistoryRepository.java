package fr.miuby.survi.system.database.repository;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.lib.sqlite.MLRepository;
import fr.miuby.lib.sqlite.MLSQLite;
import fr.miuby.survi.system.log.ELogTag;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;

public class TributeHistoryRepository extends MLRepository {

    public TributeHistoryRepository(Connection connection, MLSQLite db) {
        super(connection, db);
    }

    // =========================================================================
    // Écriture
    // =========================================================================

    public void insert(UUID playerUuid, String playerPseudo, String villagerId, String itemMaterial, int quantity) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO player_tribute_history (player_uuid, player_pseudo, villager_id, item_material, quantity, given_at) VALUES (?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, playerPseudo);
                ps.setString(3, villagerId);
                ps.setString(4, itemMaterial);
                ps.setInt(5, quantity);
                ps.setString(6, LocalDate.now().toString());
                ps.executeUpdate();
            }
        }, ELogTag.VILLAGER, "Failed to insert tribute history for " + playerUuid);
    }

    // =========================================================================
    // Lecture — classement
    // =========================================================================

    /** Top {@code limit} joueurs par quantité totale d'items donnés en tribut, tous villageois confondus. */
    public List<PlayerRankEntry> getTopByQuantity(int limit) {
        List<PlayerRankEntry> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT player_pseudo, SUM(quantity) as qty FROM player_tribute_history GROUP BY player_uuid ORDER BY qty DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new PlayerRankEntry(rs.getString("player_pseudo"), rs.getLong("qty")));
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.VILLAGER, "Failed to get top tribute leaderboard", ex);
        }
        return list;
    }

    /** UUID distincts de tous les joueurs ayant donné au moins un tribut. */
    public Set<UUID> getDonorUuids() {
        Set<UUID> set = new HashSet<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT DISTINCT player_uuid FROM player_tribute_history")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) set.add(UUID.fromString(rs.getString(1)));
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.VILLAGER, "Failed to get tribute donor uuids", ex);
        }
        return set;
    }

    public record PlayerRankEntry(String pseudo, long quantity) {}
}