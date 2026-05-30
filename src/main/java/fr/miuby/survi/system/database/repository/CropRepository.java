package fr.miuby.survi.system.database.repository;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.world.crops.PlantedCrop;
import fr.miuby.lib.log.MLLogManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.logging.Level;

public class CropRepository {
    private final Connection connection;

    public CropRepository(Connection connection) {
        this.connection = connection;
    }

    public boolean loadAll(Set<String> plantedCrops) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM planted_crops");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String worldUid = rs.getString("world_uid");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");

                plantedCrops.add(new PlantedCrop(worldUid, x, y, z).getKey());
            }

            return true;
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.WORLD, "Failed to load planted crops", ex);
            return false;
        }
    }

    public void save(PlantedCrop crop) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(
                GameManager.getInstance().getPlugin(), () -> {
                    try (Connection conn = GameManager.getInstance().getDatabase().getConnection();
                         PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO planted_crops (world_uid, x, y, z) VALUES (?, ?, ?, ?)")) {

                        ps.setString(1, crop.getWorldUid());
                        ps.setInt(2, crop.getX());
                        ps.setInt(3, crop.getY());
                        ps.setInt(4, crop.getZ());
                        ps.executeUpdate();

                    } catch (SQLException ex) {
                        MLLogManager.getInstance().log(Level.SEVERE, ELogTag.WORLD, "Failed to save planted crop", ex);
                    }
                }
        );
    }

    public void remove(PlantedCrop crop) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(
                GameManager.getInstance().getPlugin(), () -> {
                    try (Connection conn = GameManager.getInstance().getDatabase().getConnection();
                         PreparedStatement ps = conn.prepareStatement("DELETE FROM planted_crops WHERE world_uid = ? AND x = ? AND y = ? AND z = ?")) {

                        ps.setString(1, crop.getWorldUid());
                        ps.setInt(2, crop.getX());
                        ps.setInt(3, crop.getY());
                        ps.setInt(4, crop.getZ());
                        ps.executeUpdate();

                    } catch (SQLException ex) {
                        MLLogManager.getInstance().log(Level.SEVERE, ELogTag.WORLD, "Failed to remove planted crop", ex);
                    }
                }
        );
    }
}