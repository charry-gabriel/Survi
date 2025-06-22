package fr.miuby.survi.database;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

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
        connection = getSQLConnection();
        try{
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM player WHERE pseudo = 'Miuby'");
            ResultSet rs = ps.executeQuery();
            closeResources(connection, ps, null);
            isLoaded = true;
            GameManager.getInstance().getLogger().log(Level.INFO, "Database connexion succeeded !");
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
            closeResources(conn, ps, null);
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
            closeResources(conn, ps, null);
        }
    }

    public void CreateDBPlayer(UUID uuid, String pseudo) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            Connection conn = null;
            PreparedStatement ps = null;
            try {
                conn = getSQLConnection();
                ps = conn.prepareStatement("INSERT INTO player VALUES ('" + uuid + "', 0, 0, '" + pseudo + "', '" + GameManager.getInstance().getRoleFactory().getDefaultRole().type().toString() + "', NULL)");
                ps.executeUpdate();
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute, ex);
            } finally {
                closeResources(conn, ps, null);
            }
        });
    }

    public void updatePlayer(UUID uuid, String column, String value) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            Connection conn = null;
            PreparedStatement ps = null;
            try {
                conn = getSQLConnection();
                ps = conn.prepareStatement("UPDATE player SET " + column + " = '" + value + "' WHERE uuid = '" + uuid + "'");
                ps.executeUpdate();
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute, ex);
            } finally {
                closeResources(conn, ps, null);
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

                villager.setLocation(new Location(GameManager.getInstance().getWorldFactory().getWorld(EWorld.VILLAGE).getWorld(), rs.getFloat("locationX"), rs.getFloat("locationY"), rs.getFloat("locationZ"), rs.getFloat("locationYaw"), rs.getFloat("locationPitch")));
                villager.setRealVillager(uuid);
                return true;
            }
        } catch (SQLException ex) {
            GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute, ex);
        } finally {
            closeResources(conn, ps, null);
        }
        return false;
    }

    public void CreateDBVillager(String name, UUID uuid) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            Connection conn = null;
            PreparedStatement ps = null;
            try {
                conn = getSQLConnection();
                ps = conn.prepareStatement("INSERT INTO villager VALUES ('" + uuid + "', '0', '" + name + "', NULL, '0', '700', '0', '0', '0')");
                ps.executeUpdate();
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute, ex);
            } finally {
                closeResources(conn, ps, null);
            }
        });
    }

    public void updateVillagerLevel(UUID uuid, int level) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            Connection conn = null;
            PreparedStatement ps = null;
            try {
                conn = getSQLConnection();
                ps = conn.prepareStatement("UPDATE villager SET level = '" + level + "' WHERE uuid = '" + uuid + "'");
                ps.executeUpdate();
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute, ex);
            } finally {
                closeResources(conn, ps, null);
            }
        });
    }

    public void updateVillagerLocation(UUID uuid, Location location) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            Connection conn = null;
            PreparedStatement ps = null;
            try {
                conn = getSQLConnection();
                ps = conn.prepareStatement("UPDATE villager SET locationX = '" + location.getX() + "', locationY = '" + location.getY() + "', locationZ = '" + location.getZ() + "', locationYaw = '" + location.getYaw() + "', locationPitch = '" + location.getPitch() + "' WHERE uuid = '" + uuid + "'");
                ps.executeUpdate();
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute, ex);
            } finally {
                closeResources(conn, ps, null);
            }
        });
    }

    public void updateVillagerUUID(UUID uuid, String name) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            Connection conn = null;
            PreparedStatement ps = null;
            try {
                conn = getSQLConnection();
                ps = conn.prepareStatement("UPDATE villager SET uuid = '" + uuid + "' WHERE name = '" + name + "'");
                ps.executeUpdate();
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute, ex);
            } finally {
                closeResources(conn, ps, null);
            }
        });
    }

    public void updateVillagerGivenItem(UUID uuid, List<ItemStack> givenItems) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            Connection conn = null;
            PreparedStatement ps = null;
            try {
                conn = getSQLConnection();
                String givenItemsString = Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(givenItems));
                ps = conn.prepareStatement("UPDATE villager SET givenItems = '" + givenItemsString + "' WHERE uuid = '" + uuid + "'");
                ps.executeUpdate();
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute, ex);
            } finally {
                closeResources(conn, ps, null);
            }
        });
    }

    public boolean isVillagerUUIDExist(UUID uuid) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM villager WHERE uuid = '"+uuid+"'");
            rs = ps.executeQuery();

            if (rs.next()) {
                return true;
            }
        } catch (SQLException ex) {
            GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute, ex);
        } finally {
            closeResources(conn, ps, null);
        }
        return false;
    }
    //endregion

    //region Crop
    public boolean selectCrop(Set<String> plantedCrops) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("CREATE TABLE IF NOT EXISTS planted_crops (" +
                    "world_uid VARCHAR(36) NOT NULL," +
                    "x INT NOT NULL," +
                    "y INT NOT NULL," +
                    "z INT NOT NULL," +
                    "PRIMARY KEY (world_uid, x, y, z)" +
                    ")");
            ps.executeUpdate();

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
            closeResources(conn, ps, null);
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
                closeResources(conn, ps, null);
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
                closeResources(conn, ps, null);
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
            closeResources(conn, ps, null);
        }
        return "error";
    }

    private void closeResources(Connection conn, PreparedStatement ps, ResultSet rs) {
        try {
            if (rs != null) rs.close();
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