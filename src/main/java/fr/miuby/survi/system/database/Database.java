package fr.miuby.survi.system.database;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;

import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.crops.PlantedCrop;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.quest.PlayerQuestData;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.role.ERole;
import fr.miuby.survi.role.Role;
import fr.miuby.survi.system.log.LogManager;
import fr.miuby.survi.villager.AlphaVillagerData;
import fr.miuby.survi.world.EWorld;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class Database {
    protected Connection connection;

    public abstract Connection getSQLConnection();

    public abstract void load();

    public void initialize(){
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM player WHERE pseudo = ?")) {
            
            ps.setString(1, "Miuby");
            try (ResultSet rs = ps.executeQuery()) {
                LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.SYSTEM, "Database connexion succeeded !");
            }
        } catch (SQLException ex) {
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.SYSTEM, Errors.noSQLConnection + " (" + ex.getMessage() + ")");
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
                String pseudo = rs.getString("pseudo");
                Role role = GameManager.getInstance().getRoleRegistry().getRole(ERole.valueOf(rs.getString("role")));

                AlphaPlayer alphaPlayer = GameManager.getInstance().getAlphaPlayerFactory().registerAlphaPlayer(uuid, pseudo, role);

                alphaPlayer.setMort(rs.getInt("mort"));
                alphaPlayer.setSuccess(rs.getInt("success"));

                String subRoles = rs.getString("subroles");
                if (subRoles != null && !subRoles.isEmpty()) {
                    for (String subRole : subRoles.split(","))
                        alphaPlayer.addSubRole(GameManager.getInstance().getRoleRegistry().getRole(ERole.valueOf(subRole)));
                }
            }
        } catch (SQLException ex) {
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.PLAYER, Errors.sqlConnectionExecute + " (" + ex.getMessage() + ")");
        } finally {
            closeResources(conn, ps);
        }
    }

    public void CreateDBPlayer(Player player) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            try (Connection conn = getSQLConnection();
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO player (uuid, mort, success, pseudo, role) VALUES (?, 0, 0, ?, ?)")) {
                
                ps.setString(1, player.getUniqueId().toString());
                ps.setString(2, player.getName());
                ps.setString(3, GameManager.getInstance().getRoleRegistry().getDefaultRole().type().toString());
                ps.executeUpdate();
                
            } catch (SQLException ex) {
                LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.PLAYER, "Failed to create player in database (" + ex.getMessage() + ")");
            }
        });
    }

    /**
     * Updates a single player column. Only columns in {@link PlayerColumn} are allowed (no raw strings).
     */
    public void updatePlayer(UUID uuid, PlayerColumn column, String value) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            try (Connection conn = getSQLConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE player SET " + column.getColumnName() + " = ? WHERE uuid = ?")) {

                ps.setString(1, value);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();

            } catch (SQLException ex) {
                LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.PLAYER, "Failed to update player data for column: " + column.getColumnName() + " (" + ex.getMessage() + ")");
            }
        });
    }
    //endregion

    //region Villager
    public AlphaVillagerData initVillager(String nameId) {
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM villager WHERE name = ? ORDER BY rowid DESC LIMIT 1")) {
            
            ps.setString(1, nameId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    return null;

                UUID uuid = UUID.fromString(rs.getString("uuid"));


                int level = rs.getInt("level");

                List<ItemStack> givenItems;
                String givenItemsString = rs.getString("givenItems");
                if (givenItemsString == null)
                    givenItems = new ArrayList<>();
                else
                    givenItems = List.of(ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(givenItemsString)));


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

    public void CreateDBVillager(String name, UUID uuid) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            try (Connection conn = getSQLConnection();
                 PreparedStatement deletePs = conn.prepareStatement("DELETE FROM villager WHERE name = ?");
                 PreparedStatement insertPs = conn.prepareStatement("INSERT INTO villager (uuid, level, name, givenItems, locationX, locationY, locationZ, locationYaw, locationPitch, unlockedDate) VALUES (?, 0, ?, NULL, -23.5, 184.5, -19.5, 0, 0, 0)")) {

                deletePs.setString(1, name);
                deletePs.executeUpdate();

                insertPs.setString(1, uuid.toString());
                insertPs.setString(2, name);
                insertPs.executeUpdate();
                
            } catch (SQLException ex) {
                LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.VILLAGER, "Failed to create villager in database (" + ex.getMessage() + ")");
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
                LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.VILLAGER, "Failed to update villager level (" + ex.getMessage() + ")");
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
                LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.VILLAGER, "Failed to update villager location (" + ex.getMessage() + ")");
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
                LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.VILLAGER, "Failed to update villager given items (" + ex.getMessage() + ")");
            }
        });
    }

    public void lockVillager(UUID uuid, Long unlockedDate) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            try (Connection conn = getSQLConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE villager SET unlockedDate = ? WHERE uuid = ?")) {

                if (unlockedDate != null) {
                    ps.setLong(1, unlockedDate);
                } else {
                    ps.setNull(1, java.sql.Types.BIGINT);
                }
                ps.setString(2, uuid.toString());
                ps.executeUpdate();

            } catch (SQLException ex) {
                LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.VILLAGER, "Failed to update villager unlockedDate (" + ex.getMessage() + ")");
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
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.WORLD, Errors.sqlConnectionExecute + " (" + ex.getMessage() + ")");
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
                LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.WORLD, "Failed to save planted crop (" + ex.getMessage() + ")");
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
                LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.WORLD, "Failed to remove planted crop (" + ex.getMessage() + ")");
            } finally {
                closeResources(conn, ps);
            }
        });
    }
    //endregion

    //region Quest & Reputation
    public Map<String, Integer> getReputation(UUID playerUuid) {
        Map<String, Integer> reputations = new HashMap<>();
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT trader_id, reputation FROM player_reputation WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reputations.put(rs.getString("trader_id"), rs.getInt("reputation"));
                }
            }
        } catch (SQLException ex) {
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.REPUTATION, "Failed to get reputation (" + ex.getMessage() + ")");
        }
        return reputations;
    }

    public void updateReputation(UUID playerUuid, String traderId, int reputation) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            try (Connection conn = getSQLConnection();
                 PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO player_reputation (player_uuid, trader_id, reputation) VALUES (?, ?, ?)")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, traderId);
                ps.setInt(3, reputation);
                ps.executeUpdate();
            } catch (SQLException ex) {
                LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.REPUTATION, "Failed to update reputation (" + ex.getMessage() + ")");
            }
        });
    }

    public PlayerQuestData getPlayerQuest(UUID playerUuid) {
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT quest_id, progress, last_accepted, is_completed, trader_id, claimed FROM player_quest WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PlayerQuestData(
                            rs.getString("quest_id"),
                            rs.getInt("progress"),
                            LocalDate.parse(rs.getString("last_accepted")),
                            rs.getBoolean("is_completed"),
                            rs.getString("trader_id"),
                            rs.getBoolean("claimed")
                    );
                }
            }
        } catch (SQLException ex) {
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.QUEST, "Failed to get player quest (" + ex.getMessage() + ")");
        }
        return null;
    }

    public void updatePlayerQuest(UUID playerUuid, PlayerQuestData questData) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            try (Connection conn = getSQLConnection();
                 PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO player_quest (player_uuid, quest_id, progress, last_accepted, is_completed, trader_id, claimed) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, questData.getQuestId());
                ps.setInt(3, questData.getProgress());
                ps.setString(4, questData.getLastAccepted().toString());
                ps.setBoolean(5, questData.isCompleted());
                ps.setString(6, questData.getTraderId());
                ps.setBoolean(7, questData.isClaimed());
                ps.executeUpdate();
            } catch (SQLException ex) {
                LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.QUEST, "Failed to update player quest (" + ex.getMessage() + ")");
            }
        });
    }

    public void clearPlayerQuest(UUID playerUuid) {
        GameManager.getInstance().getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            try (Connection conn = getSQLConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM player_quest WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                ps.executeUpdate();
            } catch (SQLException ex) {
                LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.QUEST, "Failed to clear player quest (" + ex.getMessage() + ")");
            }
        });
    }
    //endregion

    //region time
    public String getServerData(String key) {
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT value FROM server_data WHERE key = ?")) {

            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("value");
            }
        } catch (SQLException e) {
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.SYSTEM, "Erreur getServerData: " + key + " (" + e.getMessage() + ")");
        }
        return null;
    }

    public void saveServerData(String key, String value) {
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT OR REPLACE INTO server_data (key, value) VALUES (?, ?)")) {

            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();

        } catch (SQLException e) {
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.SYSTEM, "Erreur saveServerData: " + key + " (" + e.getMessage() + ")");
        }
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
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.SYSTEM, Errors.sqlConnectionExecute + " (" + ex.getMessage() + ")");
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
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.SYSTEM, "Failed to close database resources (" + ex.getMessage() + ")");
        }
    }
}