package fr.miuby.survi.system.database.repository;

import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.log.LogManager;
import fr.miuby.survi.villager.villagerlevel.AlphaVillagerData;
import fr.miuby.survi.world.EWorld;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public class VillagerRepository {
    private final Connection connection;

    public VillagerRepository(Connection connection) {
        this.connection = connection;
    }

    public AlphaVillagerData load(String nameId) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM villager WHERE name = ? ORDER BY rowid DESC LIMIT 1")) {

            ps.setString(1, nameId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                UUID uuid = UUID.fromString(rs.getString("uuid"));
                int level = rs.getInt("level");

                List<ItemStack> givenItems;
                String givenItemsString = rs.getString("givenItems");
                if (givenItemsString == null) {
                    givenItems = new ArrayList<>();
                } else {
                    givenItems = List.of(ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(givenItemsString)));
                }

                Location location = new Location(
                        WorldRegistry.get(EWorld.VILLAGE).getWorld(),
                        rs.getFloat("locationX"),
                        rs.getFloat("locationY"),
                        rs.getFloat("locationZ"),
                        rs.getFloat("locationYaw"),
                        rs.getFloat("locationPitch")
                );

                Long unlockToEpochMilli = rs.getLong("unlockedDate");

                return new AlphaVillagerData(uuid, nameId, location, givenItems, level, unlockToEpochMilli);
            }
        } catch (SQLException ex) {
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.VILLAGER, "Failed to load villager: " + nameId + " (" + ex.getMessage() + ")");
            return null;
        }
    }

    public void create(String name, UUID uuid) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(
                GameManager.getInstance().getPlugin(), () -> {
                    try (Connection conn = GameManager.getInstance().getDatabase().getSQLConnection();
                         PreparedStatement deletePs = conn.prepareStatement("DELETE FROM villager WHERE name = ?");
                         PreparedStatement insertPs = conn.prepareStatement(
                                 "INSERT INTO villager (uuid, level, name, givenItems, locationX, locationY, locationZ, locationYaw, locationPitch, unlockedDate) " +
                                         "VALUES (?, 0, ?, NULL, -23.5, 184.5, -19.5, 0, 0, 0)")) {

                        deletePs.setString(1, name);
                        deletePs.executeUpdate();

                        insertPs.setString(1, uuid.toString());
                        insertPs.setString(2, name);
                        insertPs.executeUpdate();

                    } catch (SQLException ex) {
                        LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.VILLAGER, "Failed to create villager (" + ex.getMessage() + ")");
                    }
                }
        );
    }

    public void updateLevel(UUID uuid, int level) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(
                GameManager.getInstance().getPlugin(), () -> {
                    try (Connection conn = GameManager.getInstance().getDatabase().getSQLConnection();
                         PreparedStatement ps = conn.prepareStatement("UPDATE villager SET level = ? WHERE uuid = ?")) {

                        ps.setInt(1, level);
                        ps.setString(2, uuid.toString());
                        ps.executeUpdate();

                    } catch (SQLException ex) {
                        LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.VILLAGER, "Failed to update villager level (" + ex.getMessage() + ")");
                    }
                }
        );
    }

    public void updateLocation(UUID uuid, Location location) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(
                GameManager.getInstance().getPlugin(), () -> {
                    try (Connection conn = GameManager.getInstance().getDatabase().getSQLConnection();
                         PreparedStatement ps = conn.prepareStatement("UPDATE villager SET locationX = ?, locationY = ?, locationZ = ?, locationYaw = ?, locationPitch = ? WHERE uuid = ?")) {

                        ps.setDouble(1, location.getX());
                        ps.setDouble(2, location.getY());
                        ps.setDouble(3, location.getZ());
                        ps.setFloat(4, location.getYaw());
                        ps.setFloat(5, location.getPitch());
                        ps.setString(6, uuid.toString());
                        ps.executeUpdate();

                    } catch (SQLException ex) {
                        LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.VILLAGER, "Failed to update villager location (" + ex.getMessage() + ")");
                    }
                }
        );
    }

    public void updateGivenItems(UUID uuid, List<ItemStack> givenItems) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(
                GameManager.getInstance().getPlugin(), () -> {
                    try (Connection conn = GameManager.getInstance().getDatabase().getSQLConnection();
                         PreparedStatement ps = conn.prepareStatement("UPDATE villager SET givenItems = ? WHERE uuid = ?")) {

                        String givenItemsString = Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(givenItems));
                        ps.setString(1, givenItemsString);
                        ps.setString(2, uuid.toString());
                        ps.executeUpdate();

                    } catch (SQLException ex) {
                        LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.VILLAGER, "Failed to update villager given items (" + ex.getMessage() + ")");
                    }
                }
        );
    }

    public void updateLock(UUID uuid, Long unlockedDate) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(
                GameManager.getInstance().getPlugin(), () -> {
                    try (Connection conn = GameManager.getInstance().getDatabase().getSQLConnection();
                         PreparedStatement ps = conn.prepareStatement("UPDATE villager SET unlockedDate = ? WHERE uuid = ?")) {

                        if (unlockedDate != null) {
                            ps.setLong(1, unlockedDate);
                        } else {
                            ps.setNull(1, java.sql.Types.BIGINT);
                        }
                        ps.setString(2, uuid.toString());
                        ps.executeUpdate();

                    } catch (SQLException ex) {
                        LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.VILLAGER, "Failed to update villager lock (" + ex.getMessage() + ")");
                    }
                }
        );
    }
}