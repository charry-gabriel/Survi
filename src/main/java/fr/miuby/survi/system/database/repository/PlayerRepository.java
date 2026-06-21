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
}