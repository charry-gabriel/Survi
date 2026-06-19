package fr.miuby.survi.system.database.repository;

import fr.miuby.lib.sqlite.MLRepository;
import fr.miuby.lib.sqlite.MLSQLite;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.villager.villagerlevel.AlphaVillagerData;
import fr.miuby.survi.world.EWorld;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class VillagerRepository extends MLRepository {

    public VillagerRepository(Connection connection, MLSQLite db) {
        super(connection, db);
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

                long rawUnlockedDate = rs.getLong("unlockedDate");
                Long unlockToEpochMilli = rawUnlockedDate == 0L ? null : rawUnlockedDate;

                return new AlphaVillagerData(uuid, nameId, location, givenItems, level, unlockToEpochMilli);
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.VILLAGER, "Failed to load villager: " + nameId, ex);
            return null;
        }
    }

    public void create(String name, UUID uuid) {
        runAsync(conn -> {
            try (PreparedStatement deletePs = conn.prepareStatement("DELETE FROM villager WHERE name = ?");
                 PreparedStatement insertPs = conn.prepareStatement(
                         "INSERT INTO villager (uuid, level, name, givenItems, locationX, locationY, locationZ, locationYaw, locationPitch, unlockedDate) " +
                                 "VALUES (?, 0, ?, NULL, -23.5, 184.5, -19.5, 0, 0, 0)")) {

                deletePs.setString(1, name);
                deletePs.executeUpdate();

                insertPs.setString(1, uuid.toString());
                insertPs.setString(2, name);
                insertPs.executeUpdate();
            }
        }, ELogTag.VILLAGER, "Failed to create villager");
    }

    public void updateLevel(UUID uuid, int level) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE villager SET level = ? WHERE uuid = ?")) {
                ps.setInt(1, level);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            }
        }, ELogTag.VILLAGER, "Failed to update villager level");
    }

    public void updateLocation(UUID uuid, Location location) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE villager SET locationX = ?, locationY = ?, locationZ = ?, locationYaw = ?, locationPitch = ? WHERE uuid = ?")) {
                ps.setDouble(1, location.getX());
                ps.setDouble(2, location.getY());
                ps.setDouble(3, location.getZ());
                ps.setFloat(4, location.getYaw());
                ps.setFloat(5, location.getPitch());
                ps.setString(6, uuid.toString());
                ps.executeUpdate();
            }
        }, ELogTag.VILLAGER, "Failed to update villager location");
    }

    public void updateGivenItems(UUID uuid, List<ItemStack> givenItems) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE villager SET givenItems = ? WHERE uuid = ?")) {
                String encoded = Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(givenItems));
                ps.setString(1, encoded);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            }
        }, ELogTag.VILLAGER, "Failed to update villager given items");
    }

    public void updateLock(UUID uuid, Long unlockedDate) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE villager SET unlockedDate = ? WHERE uuid = ?")) {
                if (unlockedDate != null) {
                    ps.setLong(1, unlockedDate);
                } else {
                    ps.setLong(1, 0L);
                }
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            }
        }, ELogTag.VILLAGER, "Failed to update villager lock");
    }
}