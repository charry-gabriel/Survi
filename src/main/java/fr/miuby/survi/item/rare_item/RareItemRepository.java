package fr.miuby.survi.item.rare_item;

import fr.miuby.lib.sqlite.MLRepository;
import fr.miuby.lib.sqlite.MLSQLite;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.system.log.ELogTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class RareItemRepository extends MLRepository {

    public RareItemRepository(Connection connection, MLSQLite db) {
        super(connection, db);
    }

    /**
     * Charge toutes les entrées rare-item d'un joueur depuis la DB.
     * Appelé en thread async par {@link RareItemService}.
     * Retourne un EnumMap vide si le joueur n'a aucune entrée.
     *
     * @return map job → long[]{actionCount, hasItem (0 ou 1)}
     */
    public Map<EJob, long[]> loadPlayer(UUID playerUuid) {
        Map<EJob, long[]> result = new EnumMap<>(EJob.class);
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT job, action_count, has_item FROM player_rare_job_item WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        EJob job = EJob.valueOf(rs.getString("job"));
                        result.put(job, new long[]{rs.getLong("action_count"), rs.getBoolean("has_item") ? 1L : 0L});
                    } catch (IllegalArgumentException ignored) {
                        // Métier inconnu en base (colonne corrompue) — ignoré
                    }
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("[RareJobItem] loadPlayer SQL failed for " + playerUuid, ex);
        }
        return result;
    }

    /**
     * Sauvegarde ou met à jour l'état rare-item d'un joueur pour un métier.
     * {@code has_item} ne peut qu'augmenter (MAX) pour éviter d'écraser un état « obtenu ».
     * Async.
     */
    public void save(UUID playerUuid, EJob job, long actionCount, boolean hasItem) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO player_rare_job_item (player_uuid, job, action_count, has_item) VALUES (?, ?, ?, ?)" +
                            " ON CONFLICT(player_uuid, job) DO UPDATE SET" +
                            " action_count = excluded.action_count," +
                            " has_item = MAX(has_item, excluded.has_item)")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, job.name());
                ps.setLong(3, actionCount);
                ps.setInt(4, hasItem ? 1 : 0);
                ps.executeUpdate();
            }
        }, ELogTag.ITEM, "[RareJobItem] Failed to save for " + playerUuid + " / " + job);
    }

    /**
     * Sauvegarde synchrone (exécutée sur le thread appelant, sans passer par {@code runAsync}).
     * Réservée à l'arrêt du serveur : les tâches planifiées via {@code runAsync} peuvent être
     * annulées par le scheduler avant exécution lorsqu'elles sont planifiées pendant le
     * {@code PlayerQuitEvent} déclenché par le kick de tous les joueurs au shutdown.
     */
    public void saveSync(UUID playerUuid, EJob job, long actionCount, boolean hasItem) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO player_rare_job_item (player_uuid, job, action_count, has_item) VALUES (?, ?, ?, ?)" +
                        " ON CONFLICT(player_uuid, job) DO UPDATE SET" +
                        " action_count = excluded.action_count," +
                        " has_item = MAX(has_item, excluded.has_item)")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, job.name());
            ps.setLong(3, actionCount);
            ps.setInt(4, hasItem ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("[RareJobItem] saveSync SQL failed for " + playerUuid + " / " + job, ex);
        }
    }

    /**
     * Remet à zéro action_count et has_item pour un joueur/métier, en écrasant sans MAX.
     * Réservé à la commande admin de reset.
     */
    public void forceReset(UUID playerUuid, EJob job) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO player_rare_job_item (player_uuid, job, action_count, has_item) VALUES (?, ?, 0, 0)" +
                            " ON CONFLICT(player_uuid, job) DO UPDATE SET action_count = 0, has_item = 0")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, job.name());
                ps.executeUpdate();
            }
        }, ELogTag.ITEM, "[RareJobItem] Failed to force-reset for " + playerUuid + " / " + job);
    }
}