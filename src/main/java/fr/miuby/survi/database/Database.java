package fr.miuby.survi.database;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.crops.PlantedCrop;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.role.ERole;
import fr.miuby.survi.villager.AVillager;
import fr.miuby.survi.villager.VillagerLevel;
import fr.miuby.survi.world.EWorld;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class Database {
    protected Connection connection;
    private boolean isLoaded = false;

    public abstract Connection getSQLConnection();

    public abstract void load();

    public void initialize(){
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM player WHERE pseudo = ?")) {
            
            ps.setString(1, "Miuby");
            try (ResultSet rs = ps.executeQuery()) {
                isLoaded = true;
                GameManager.getInstance().getLogger().log(Level.INFO, "Database connexion succeeded !");
            }
        } catch (SQLException ex) {
            GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.noSQLConnection, ex);
        }
    }

    //region Player
    public void createAlphaPlayers() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM player");

            rs = ps.executeQuery();
            while(rs.next()){

                UUID uuid = UUID.fromString(rs.getString("uuid"));
                AlphaPlayer alphaPlayer = new AlphaPlayer(uuid);
                GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers().put(uuid, alphaPlayer);

                alphaPlayer.setMort(rs.getInt("mort"));
                alphaPlayer.setSuccess(rs.getInt("success"));
                alphaPlayer.setRole(GameManager.getInstance().getRoleFactory().getRole(ERole.valueOf(rs.getString("role"))));

                String subRoles = rs.getString("subroles");
                if (subRoles != null && !subRoles.isEmpty()) {
                    for (String subRole : subRoles.split(","))
                        alphaPlayer.addSubRole(GameManager.getInstance().getRoleFactory().getRole(ERole.valueOf(subRole)));
                }

                alphaPlayer.setPseudo(rs.getString("pseudo"));
                GameManager.getInstance().getScheduler().runTask(GameManager.getInstance().getPlugin(), alphaPlayer::joinServer);
            }
        } catch (SQLException ex) {
            GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute, ex);
        } finally {
            closeResources(conn, ps);
        }
    }

    public void initAlphaPlayer(AlphaPlayer alphaPlayer, UUID uuid) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM player WHERE uuid = '"+uuid+"'");
            rs = ps.executeQuery();

            if (rs.next()) {
                int mort = rs.getInt("mort");
                int success = rs.getInt("success");
                String role = rs.getString("role");
                String pseudo = rs.getString("pseudo");

                alphaPlayer.setMort(mort);
                alphaPlayer.setSuccess(success);
                alphaPlayer.setRole(GameManager.getInstance().getRoleFactory().getRole(ERole.valueOf(role)));

                String subRoles = rs.getString("subroles");
                if (subRoles != null && !subRoles.isEmpty()) {
                    for (String subRole : subRoles.split(","))
                        alphaPlayer.addSubRole(GameManager.getInstance().getRoleFactory().getRole(ERole.valueOf(subRole)));
                }

                alphaPlayer.setPseudo(pseudo);
                GameManager.getInstance().getScheduler().runTask(GameManager.getInstance().getPlugin(), alphaPlayer::joinServer);
            } else {
                Player player = GameManager.getInstance().getPlugin().getServer().getPlayer(uuid);

                GameManager.getInstance().getLogger().info("player "+(player != null));
                if (player != null) {
                    GameManager.getInstance().getLogger().info("player "+player.getName());
                    alphaPlayer.setPlayer(player);
                    alphaPlayer.setMort(0);
                    alphaPlayer.setSuccess(0);
                    alphaPlayer.setRole(GameManager.getInstance().getRoleFactory().getDefaultRole());
                    alphaPlayer.setPseudo(player.getName());
                    CreateDBPlayer(player.getUniqueId(), player.getName());
                }
                GameManager.getInstance().getScheduler().runTask(GameManager.getInstance().getPlugin(), alphaPlayer::joinServer);
            }
        } catch (SQLException ex) {
            GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute, ex);
        } finally {
            closeResources(conn, ps);
        }
    }

    public void CreateDBPlayer(UUID uuid, String pseudo) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            try (Connection conn = getSQLConnection();
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO player (uuid, mort, success, pseudo, role) VALUES (?, 0, 0, ?, ?)")) {
                
                ps.setString(1, uuid.toString());
                ps.setString(2, pseudo);
                ps.setString(3, GameManager.getInstance().getRoleFactory().getDefaultRole().type().toString());
                ps.executeUpdate();
                
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, "Failed to create player in database", ex);
            }
        });
    }

    public void updatePlayer(UUID uuid, String column, String value) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            try (Connection conn = getSQLConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE player SET " + column + " = ? WHERE uuid = ?")) {
                
                ps.setString(1, value);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
                
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, "Failed to update player data for column: " + column, ex);
            }
        });
    }
    //endregion

    //region Villager
    public boolean initVillager(AVillager villager, String name) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM villager WHERE name = '"+name+"'");
            rs = ps.executeQuery();

            if (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));

                if (villager instanceof VillagerLevel villagerLevel) {
                    villagerLevel.setLevel(rs.getInt("level"));

                    String givenItems = rs.getString("givenItems");
                    if (givenItems != null)
                        villagerLevel.setGivenItems(ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(givenItems)));
                }

                villager.setLocation(new Location(WorldRegistry.get(EWorld.VILLAGE).getWorld(), rs.getFloat("locationX"), rs.getFloat("locationY"), rs.getFloat("locationZ"), rs.getFloat("locationYaw"), rs.getFloat("locationPitch")));
                villager.setRealVillager(uuid);
                return true;
            }
        } catch (SQLException ex) {
            GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute, ex);
        } finally {
            closeResources(conn, ps);
        }
        return false;
    }

    public void CreateDBVillager(String name, UUID uuid) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            try (Connection conn = getSQLConnection();
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO villager (uuid, level, name, givenItems, locationX, locationY, locationZ, locationYaw, locationPitch) VALUES (?, 0, ?, NULL, 700, 0, 0, 0, 0)")) {
                
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.executeUpdate();
                
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, "Failed to create villager in database", ex);
            }
        });
    }

    public void updateVillagerLevel(UUID uuid, int level) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            try (Connection conn = getSQLConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE villager SET level = ? WHERE uuid = ?")) {
                
                ps.setInt(1, level);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
                
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, "Failed to update villager level", ex);
            }
        });
    }

    public void updateVillagerLocation(UUID uuid, Location location) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            try (Connection conn = getSQLConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE villager SET locationX = ?, locationY = ?, locationZ = ?, locationYaw = ?, locationPitch = ? WHERE uuid = ?")) {
                
                ps.setDouble(1, location.getX());
                ps.setDouble(2, location.getY());
                ps.setDouble(3, location.getZ());
                ps.setFloat(4, location.getYaw());
                ps.setFloat(5, location.getPitch());
                ps.setString(6, uuid.toString());
                ps.executeUpdate();
                
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, "Failed to update villager location", ex);
            }
        });
    }

    public void updateVillagerUUID(UUID uuid, String name) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            try (Connection conn = getSQLConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE villager SET uuid = ? WHERE name = ?")) {

                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.executeUpdate();

            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, "Failed to update villager UUID", ex);
            }
        });
    }

    public void updateVillagerGivenItem(UUID uuid, List<ItemStack> givenItems) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            try (Connection conn = getSQLConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE villager SET givenItems = ? WHERE uuid = ?")) {
                
                String givenItemsString = Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(givenItems));
                ps.setString(1, givenItemsString);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
                
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, "Failed to update villager given items", ex);
            }
        });
    }
    //endregion

    //region Crop
    public boolean selectCrop(Set<String> plantedCrops) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM planted_crops");
            rs = ps.executeQuery();

            while (rs.next()) {
                String worldUid = rs.getString("world_uid");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");

                plantedCrops.add(new PlantedCrop(worldUid, x, y, z).getKey());
            }

            return true;
        } catch (SQLException ex) {
            GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute, ex);
        } finally {
            closeResources(conn, ps);
        }
        return false;
    }

    public void saveCrop(PlantedCrop crop) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            Connection conn = null;
            PreparedStatement ps = null;

            try {
                conn = getSQLConnection();
                ps = conn.prepareStatement("INSERT OR IGNORE INTO planted_crops (world_uid, x, y, z) VALUES (?, ?, ?, ?)");

                ps.setString(1, crop.getWorldUid());
                ps.setInt(2, crop.getX());
                ps.setInt(3, crop.getY());
                ps.setInt(4, crop.getZ());

                ps.executeUpdate();
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, "Failed to save planted crop", ex);
            } finally {
                closeResources(conn, ps);
            }
        });
    }

    public void removeCrop(PlantedCrop crop) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            Connection conn = null;
            PreparedStatement ps = null;

            try {
                conn = getSQLConnection();
                ps = conn.prepareStatement("DELETE FROM planted_crops WHERE world_uid = ? AND x = ? AND y = ? AND z = ?");

                ps.setString(1, crop.getWorldUid());
                ps.setInt(2, crop.getX());
                ps.setInt(3, crop.getY());
                ps.setInt(4, crop.getZ());

                ps.executeUpdate();
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, "Failed to remove planted crop", ex);
            } finally {
                closeResources(conn, ps);
            }
        });
    }
    //endregion

    public String Request(String sql) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement(sql);

            if (sql.split(" ")[0].equalsIgnoreCase("select")) {
                rs = ps.executeQuery();

                int column = rs.getMetaData().getColumnCount();
                StringBuilder result = new StringBuilder();
                while(rs.next()) {
                    for (int i = 1; i <= column; i++) {
                        result.append(rs.getString(i));
                        if (i != column)
                            result.append(", ");
                    }
                    result.append("\n");
                }
                return result.toString();
            } else {
                ps.executeUpdate();
                return "Query executed !";
            }

        } catch (SQLException ex) {
            GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute, ex);
        } finally {
            closeResources(conn, ps);
        }
        return "error";
    }

    private void closeResources(Connection conn, PreparedStatement ps) {
        try {
            if (ps != null) ps.close();
            if (conn != null) conn.close();
        } catch (SQLException ex) {
            GameManager.getInstance().getLogger().log(Level.SEVERE, "Failed to close database resources", ex);
        }
    }

    public boolean IsLoaded() {
        return isLoaded;
    }
}