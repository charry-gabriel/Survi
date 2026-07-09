package fr.miuby.survi.system.database.repository;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.lib.sqlite.MLRepository;
import fr.miuby.lib.sqlite.MLSQLite;
import fr.miuby.survi.job.EJob;
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
 * Persiste les montées de niveau de métier survenues pendant qu'un joueur était hors ligne,
 * pour qu'elles survivent à un redémarrage du serveur et soient livrées à la reconnexion par
 * {@link fr.miuby.survi.player.service.OfflineNotificationService}.
 */
public class PendingJobLevelUpRepository extends MLRepository {

    public record PendingJobLevelUp(EJob job, int oldLevel, int newLevel) {}

    public PendingJobLevelUpRepository(Connection connection, MLSQLite db) {
        super(connection, db);
    }

    // -------------------------------------------------------------------------
    // Lecture (thread principal)
    // -------------------------------------------------------------------------

    public List<PendingJobLevelUp> loadForPlayer(UUID playerUuid) {
        List<PendingJobLevelUp> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT job, old_level, new_level FROM player_pending_job_levelup WHERE player_uuid = ? ORDER BY id")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        result.add(new PendingJobLevelUp(EJob.valueOf(rs.getString("job")), rs.getInt("old_level"), rs.getInt("new_level")));
                    } catch (IllegalArgumentException ignored) {
                        // Métier inconnu en base (colonne corrompue) — ignoré
                    }
                }
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.JOB, "Failed to load player_pending_job_levelup for " + playerUuid, ex);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Écriture (async)
    // -------------------------------------------------------------------------

    public void save(UUID playerUuid, EJob job, int oldLevel, int newLevel) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO player_pending_job_levelup (player_uuid, job, old_level, new_level) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, job.name());
                ps.setInt(3, oldLevel);
                ps.setInt(4, newLevel);
                ps.executeUpdate();
            }
        }, ELogTag.JOB, "Failed to save player_pending_job_levelup for " + playerUuid);
    }

    public void deleteForPlayer(UUID playerUuid) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM player_pending_job_levelup WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                ps.executeUpdate();
            }
        }, ELogTag.JOB, "Failed to delete player_pending_job_levelup for " + playerUuid);
    }
}