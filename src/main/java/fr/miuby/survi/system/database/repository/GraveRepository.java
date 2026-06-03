package fr.miuby.survi.system.database.repository;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.lib.sqlite.MLRepository;
import fr.miuby.lib.sqlite.MLSQLite;
import fr.miuby.survi.system.log.ELogTag;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class GraveRepository extends MLRepository {

    /**
     * Données brutes d'une tombe telles que stockées en base — sans résolution de monde Bukkit.
     * La résolution (world_uid → World) est faite par GraveManager au chargement.
     */
    public record RawGrave(UUID id, UUID ownerId, UUID worldUid, int x, int y, int z) {}

    public GraveRepository(Connection connection, MLSQLite db) {
        super(connection, db);
    }

    // -------------------------------------------------------------------------
    // Lecture (thread principal)
    // -------------------------------------------------------------------------

    public List<RawGrave> loadAll() {
        List<RawGrave> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT id, owner_uuid, world_uid, x, y, z FROM grave");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new RawGrave(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("owner_uuid")),
                        UUID.fromString(rs.getString("world_uid")),
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z")
                ));
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.GRAVE, "Failed to load graves", ex);
        }
        return result;
    }

    /** Charge la liste d'items d'une tombe. Sync — appelé sur le thread principal lors de la collecte. */
    public List<ItemStack> loadItems(UUID graveId) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT items_yaml FROM grave WHERE id = ?")) {
            ps.setString(1, graveId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new ArrayList<>();
                return deserializeItems(rs.getString("items_yaml"));
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.GRAVE, "Failed to load items for grave " + graveId, ex);
            return new ArrayList<>();
        }
    }

    // -------------------------------------------------------------------------
    // Écriture (async)
    // -------------------------------------------------------------------------

    /**
     * Insère une nouvelle tombe en base.
     * La sérialisation YAML des items est effectuée sur le thread appelant (thread principal)
     * avant de passer la main à l'executor async.
     */
    public void save(UUID graveId, UUID ownerId, UUID worldUid, int x, int y, int z, List<ItemStack> items) {
        String yaml = serializeItems(items);
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO grave (id, owner_uuid, world_uid, x, y, z, items_yaml) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, graveId.toString());
                ps.setString(2, ownerId.toString());
                ps.setString(3, worldUid.toString());
                ps.setInt(4, x);
                ps.setInt(5, y);
                ps.setInt(6, z);
                ps.setString(7, yaml);
                ps.executeUpdate();
            }
        }, ELogTag.GRAVE, "Failed to save grave " + graveId);
    }

    public void remove(UUID graveId) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM grave WHERE id = ?")) {
                ps.setString(1, graveId.toString());
                ps.executeUpdate();
            }
        }, ELogTag.GRAVE, "Failed to remove grave " + graveId);
    }

    /** Supprime en base toutes les tombes appartenant à un monde donné. Utilisé lors du reset. */
    public void removeByWorldUid(UUID worldUid) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM grave WHERE world_uid = ?")) {
                ps.setString(1, worldUid.toString());
                ps.executeUpdate();
            }
        }, ELogTag.GRAVE, "Failed to remove graves for world " + worldUid);
    }

    // -------------------------------------------------------------------------
    // Sérialisation items ↔ YAML string
    // -------------------------------------------------------------------------

    private String serializeItems(List<ItemStack> items) {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("items", items);
        return cfg.saveToString();
    }

    private List<ItemStack> deserializeItems(String yaml) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (InvalidConfigurationException ex) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.GRAVE, "Failed to deserialize items YAML", ex);
            return new ArrayList<>();
        }
        List<?> raw = cfg.getList("items", new ArrayList<>());
        List<ItemStack> result = new ArrayList<>();
        for (Object obj : raw) {
            if (obj instanceof ItemStack itemStack) result.add(itemStack);
        }
        return result;
    }
}