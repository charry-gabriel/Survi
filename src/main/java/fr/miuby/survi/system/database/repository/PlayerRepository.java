package fr.miuby.survi.system.database.repository;

import fr.miuby.lib.sqlite.MLRepository;
import fr.miuby.lib.sqlite.MLSQLite;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.role.ERole;
import fr.miuby.survi.role.Role;
import fr.miuby.survi.system.database.EPlayerColumn;
import fr.miuby.survi.system.database.EPlayerLoadResult;
import fr.miuby.survi.system.log.ELogTag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerRepository extends MLRepository {

    public PlayerRepository(Connection connection, MLSQLite db) {
        super(connection, db);
    }

    public void createAlphaPlayers() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM player");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String rawUuid = rs.getString("uuid");
                try {
                    UUID uuid = UUID.fromString(rawUuid);
                    String pseudo = rs.getString("pseudo");
                    Role role = GameManager.getInstance().getRoleLoader().getRole(ERole.valueOf(rs.getString("role")));

                    AlphaPlayer alphaPlayer = GameManager.getInstance().getAlphaPlayerFactory().registerAlphaPlayer(uuid, pseudo, role);
                    alphaPlayer.setDeath(rs.getInt("mort"));
                    alphaPlayer.setSuccess(rs.getInt("success"));

                    String subRoles = rs.getString("subroles");
                    if (subRoles != null && !subRoles.isEmpty()) {
                        for (String subRole : subRoles.split(",")) {
                            alphaPlayer.addSubRole(GameManager.getInstance().getRoleLoader().getRole(ERole.valueOf(subRole)));
                        }
                    }

                    alphaPlayer.loadReputation();
                    alphaPlayer.setCustomSpawnLocation(loadSpawnLocation(rs));
                } catch (IllegalArgumentException ex) {
                    // Ligne corrompue (rôle/UUID invalide) — on l'ignore et on continue plutôt que
                    // d'interrompre le chargement de tous les joueurs restants dans le ResultSet.
                    MLLogManager.getInstance().log(Level.SEVERE, ELogTag.PLAYER,
                            "Ligne joueur corrompue ignorée au chargement (uuid=" + rawUuid + ")", ex);
                }
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.PLAYER, "Failed to load players", ex);
        }
    }

    /**
     * Filet de sécurité utilisé quand un joueur rejoint sans être présent dans le registre mémoire
     * (ex: ligne ignorée au démarrage car corrompue, ou tout autre désync mémoire/BDD). Cherche le
     * joueur directement par UUID et le réenregistre avec ses vraies données s'il existe, au lieu de
     * laisser l'appelant supposer qu'il s'agit d'un nouveau joueur.
     *
     * @return {@link EPlayerLoadResult#FOUND} si réenregistré avec succès,
     *         {@link EPlayerLoadResult#NOT_FOUND} si aucune ligne ne correspond (nouveau joueur),
     *         {@link EPlayerLoadResult#ERROR} si la lecture a échoué — ne jamais traiter comme NOT_FOUND.
     */
    public EPlayerLoadResult tryReloadPlayer(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM player WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return EPlayerLoadResult.NOT_FOUND;

                try {
                    String pseudo = rs.getString("pseudo");
                    Role role = GameManager.getInstance().getRoleLoader().getRole(ERole.valueOf(rs.getString("role")));

                    AlphaPlayer alphaPlayer = GameManager.getInstance().getAlphaPlayerFactory().registerAlphaPlayer(uuid, pseudo, role);
                    alphaPlayer.setDeath(rs.getInt("mort"));
                    alphaPlayer.setSuccess(rs.getInt("success"));

                    String subRoles = rs.getString("subroles");
                    if (subRoles != null && !subRoles.isEmpty()) {
                        for (String subRole : subRoles.split(",")) {
                            alphaPlayer.addSubRole(GameManager.getInstance().getRoleLoader().getRole(ERole.valueOf(subRole)));
                        }
                    }
                    alphaPlayer.loadReputation();
                    alphaPlayer.setCustomSpawnLocation(loadSpawnLocation(rs));
                    return EPlayerLoadResult.FOUND;
                } catch (IllegalArgumentException ex) {
                    MLLogManager.getInstance().log(Level.SEVERE, ELogTag.PLAYER,
                            "Ligne joueur corrompue lors du rechargement de secours (uuid=" + uuid + ")", ex);
                    return EPlayerLoadResult.ERROR;
                }
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.PLAYER, "Failed to reload player " + uuid, ex);
            return EPlayerLoadResult.ERROR;
        }
    }

    public void create(Player player) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO player (uuid, mort, success, pseudo, role) VALUES (?, 0, 0, ?, ?)")) {
                ps.setString(1, player.getUniqueId().toString());
                ps.setString(2, player.getName());
                ps.setString(3, GameManager.getInstance().getRoleLoader().getDefaultRole().type().toString());
                ps.executeUpdate();
            }
        }, ELogTag.PLAYER, "Failed to create player");
    }

    public void update(UUID uuid, EPlayerColumn column, String value) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE player SET " + column.getColumnName() + " = ? WHERE uuid = ?")) {
                ps.setString(1, value);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            }
        }, ELogTag.PLAYER, "Failed to update player");
    }

    /** Sauvegarde le spawn personnalisé du joueur en base de données (async). */
    public void saveSpawnLocation(UUID uuid, Location location) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE player SET spawn_world = ?, spawn_x = ?, spawn_y = ?, spawn_z = ?, spawn_yaw = ?, spawn_pitch = ? WHERE uuid = ?")) {
                ps.setString(1, location.getWorld().getName());
                ps.setDouble(2, location.getX());
                ps.setDouble(3, location.getY());
                ps.setDouble(4, location.getZ());
                ps.setDouble(5, location.getYaw());
                ps.setDouble(6, location.getPitch());
                ps.setString(7, uuid.toString());
                ps.executeUpdate();
            }
        }, ELogTag.PLAYER, "Failed to save spawn location for " + uuid);
    }

    /** Efface le spawn personnalisé du joueur en base de données (async). */
    public void clearSpawnLocation(UUID uuid) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE player SET spawn_world = NULL, spawn_x = NULL, spawn_y = NULL, spawn_z = NULL, spawn_yaw = NULL, spawn_pitch = NULL WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            }
        }, ELogTag.PLAYER, "Failed to clear spawn location for " + uuid);
    }

    /**
     * Efface en DB les spawns personnalisés dont le monde n'est plus chargé.
     * À appeler au démarrage, après le chargement des mondes, pour nettoyer les entrées
     * laissées par un arrêt serveur survenu avant que les writes async du reset aient commité.
     */
    public void purgeOrphanSpawns() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT uuid, spawn_world FROM player WHERE spawn_world IS NOT NULL");
             ResultSet rs = ps.executeQuery()) {

            int count = 0;
            while (rs.next()) {
                String spawnWorld = rs.getString("spawn_world");
                if (Bukkit.getWorld(spawnWorld) != null) continue;

                String rawUuid = rs.getString("uuid");
                runAsync(conn -> {
                    try (PreparedStatement del = conn.prepareStatement(
                            "UPDATE player SET spawn_world = NULL, spawn_x = NULL, spawn_y = NULL, spawn_z = NULL, spawn_yaw = NULL, spawn_pitch = NULL WHERE uuid = ?")) {
                        del.setString(1, rawUuid);
                        del.executeUpdate();
                    }
                }, ELogTag.PLAYER, "Failed to purge orphan spawn for " + rawUuid);
                count++;
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.PLAYER,
                        "[purgeOrphanSpawns] Spawn orphelin supprimé : uuid=" + rawUuid + " monde=" + spawnWorld);
            }

            if (count > 0) {
                MLLogManager.getInstance().log(Level.INFO, ELogTag.PLAYER,
                        "[purgeOrphanSpawns] " + count + " spawn(s) orphelin(s) supprimé(s)");
            } else {
                MLLogManager.getInstance().log(Level.FINE, ELogTag.PLAYER, "[purgeOrphanSpawns] Aucun spawn orphelin");
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.PLAYER, "Failed to purge orphan spawns", ex);
        }
    }

    /**
     * Lit les colonnes spawn_* du ResultSet courant et retourne la Location correspondante.
     * Retourne {@code null} si aucun spawn n'est défini ou si le monde n'est pas chargé.
     */
    private Location loadSpawnLocation(java.sql.ResultSet rs) throws java.sql.SQLException {
        String spawnWorld = rs.getString("spawn_world");
        if (spawnWorld == null) return null;
        World world = Bukkit.getWorld(spawnWorld);
        if (world == null) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.PLAYER,
                    "[loadSpawnLocation] Monde introuvable pour spawn : " + spawnWorld + " — spawn ignoré");
            return null;
        }
        return new Location(world,
                rs.getDouble("spawn_x"),
                rs.getDouble("spawn_y"),
                rs.getDouble("spawn_z"),
                (float) rs.getDouble("spawn_yaw"),
                (float) rs.getDouble("spawn_pitch"));
    }
}