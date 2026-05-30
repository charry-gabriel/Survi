package fr.miuby.survi.system.database.repository;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.role.ERole;
import fr.miuby.survi.role.Role;
import fr.miuby.survi.system.database.EPlayerColumn;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.log.ELogTag;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerRepository {
    private final Connection connection;

    public PlayerRepository(Connection connection) {
        this.connection = connection;
    }

    public void createAlphaPlayers() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM player");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String pseudo = rs.getString("pseudo");
                Role role = GameManager.getInstance().getRoleLoader()
                        .getRole(ERole.valueOf(rs.getString("role")));

                AlphaPlayer alphaPlayer = GameManager.getInstance()
                        .getAlphaPlayerFactory()
                        .registerAlphaPlayer(uuid, pseudo, role);

                alphaPlayer.setMort(rs.getInt("mort"));
                alphaPlayer.setSuccess(rs.getInt("success"));

                String subRoles = rs.getString("subroles");
                if (subRoles != null && !subRoles.isEmpty()) {
                    for (String subRole : subRoles.split(",")) {
                        alphaPlayer.addSubRole(GameManager.getInstance()
                                .getRoleLoader()
                                .getRole(ERole.valueOf(subRole)));
                    }
                }
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.PLAYER, "Failed to load players", ex);
        }
    }

    public void create(Player player) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(
                GameManager.getInstance().getPlugin(), () -> {
                    try (Connection conn = GameManager.getInstance().getDatabase().getSQLConnection();
                         PreparedStatement ps = conn.prepareStatement("INSERT INTO player (uuid, mort, success, pseudo, role) VALUES (?, 0, 0, ?, ?)")) {

                        ps.setString(1, player.getUniqueId().toString());
                        ps.setString(2, player.getName());
                        ps.setString(3, GameManager.getInstance().getRoleLoader().getDefaultRole().type().toString());
                        ps.executeUpdate();

                    } catch (SQLException ex) {
                        MLLogManager.getInstance().log(Level.SEVERE, ELogTag.PLAYER, "Failed to create player", ex);
                    }
                }
        );
    }

    public void update(UUID uuid, EPlayerColumn column, String value) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(
                GameManager.getInstance().getPlugin(), () -> {
                    try (Connection conn = GameManager.getInstance().getDatabase().getSQLConnection();
                         PreparedStatement ps = conn.prepareStatement("UPDATE player SET " + column.getColumnName() + " = ? WHERE uuid = ?")) {

                        ps.setString(1, value);
                        ps.setString(2, uuid.toString());
                        ps.executeUpdate();

                    } catch (SQLException ex) {
                        MLLogManager.getInstance().log(Level.SEVERE, ELogTag.PLAYER, "Failed to update player", ex);
                    }
                }
        );
    }
}