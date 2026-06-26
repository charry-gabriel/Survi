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

public class GraveLostNotificationRepository extends MLRepository {

    public record LostGraveEntry(UUID playerUuid, String worldName, int x, int y, int z) {}

    public GraveLostNotificationRepository(Connection connection, MLSQLite db) {
        super(connection, db);
    }

    // -------------------------------------------------------------------------
    // Lecture (thread principal)
    // -------------------------------------------------------------------------

    public List<LostGraveEntry> loadForPlayer(UUID playerUuid) {
        List<LostGraveEntry> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT world_name, x, y, z FROM grave_lost_notification WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new LostGraveEntry(playerUuid, rs.getString("world_name"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z")));
                }
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.GRAVE, "Failed to load grave_lost_notification for " + playerUuid, ex);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Écriture (async)
    // -------------------------------------------------------------------------

    public void save(UUID playerUuid, String worldName, int x, int y, int z) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO grave_lost_notification (player_uuid, world_name, x, y, z) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, worldName);
                ps.setInt(3, x);
                ps.setInt(4, y);
                ps.setInt(5, z);
                ps.executeUpdate();
            }
        }, ELogTag.GRAVE, "Failed to save grave_lost_notification for " + playerUuid);
    }

    public void deleteForPlayer(UUID playerUuid) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM grave_lost_notification WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                ps.executeUpdate();
            }
        }, ELogTag.GRAVE, "Failed to delete grave_lost_notification for " + playerUuid);
    }
}