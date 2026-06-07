package fr.miuby.survi.system.database.repository;

import fr.miuby.lib.sqlite.MLRepository;
import fr.miuby.lib.sqlite.MLSQLite;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.world.crops.PlantedCrop;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;

public class CropRepository extends MLRepository {

    public CropRepository(Connection connection, MLSQLite db) {
        super(connection, db);
    }

    /** Remplit la map key(position) → farmLevel depuis la DB. Retourne true en cas de succès. */
    public boolean loadAll(Map<String, Integer> plantedCrops) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM planted_crops");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String worldUid = rs.getString("world_uid");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                int farmLevel = rs.getInt("farm_level"); // migration v13 ; défaut 3 sur les anciennes lignes
                plantedCrops.put(new PlantedCrop(worldUid, x, y, z).getKey(), farmLevel);
            }

            return true;
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.WORLD, "Failed to load planted crops", ex);
            return false;
        }
    }

    public void save(PlantedCrop crop) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT OR IGNORE INTO planted_crops (world_uid, x, y, z, farm_level) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, crop.getWorldUid());
                ps.setInt(2, crop.getX());
                ps.setInt(3, crop.getY());
                ps.setInt(4, crop.getZ());
                ps.setInt(5, crop.getFarmLevel());
                ps.executeUpdate();
            }
        }, ELogTag.WORLD, "Failed to save planted crop");
    }

    public void remove(PlantedCrop crop) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM planted_crops WHERE world_uid = ? AND x = ? AND y = ? AND z = ?")) {
                ps.setString(1, crop.getWorldUid());
                ps.setInt(2, crop.getX());
                ps.setInt(3, crop.getY());
                ps.setInt(4, crop.getZ());
                ps.executeUpdate();
            }
        }, ELogTag.WORLD, "Failed to remove planted crop");
    }
}