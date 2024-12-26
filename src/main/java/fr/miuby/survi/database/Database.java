package fr.miuby.survi.database;

import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.villager.AVillager;
import org.bukkit.entity.Player;

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
            close(ps,rs);
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
        GameManager.getInstance().getLogger().info("createAlphaPlayers");
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
                alphaPlayer.setRole(rs.getString("role"));
                GameManager.getInstance().getScheduler().runTask(GameManager.getInstance().getPlugin(), alphaPlayer::joinServer);
            }
        } catch (SQLException ex) {
            GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute, ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionClose, ex);
            }
        }
    }

    public void getAlphaPlayer(AlphaPlayer alphaPlayer, UUID uuid) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs;
        GameManager.getInstance().getLogger().info("getAlphaPlayer");
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM player WHERE uuid = '"+uuid+"'");
            rs = ps.executeQuery();

            if (rs.next()) {
                int mort = rs.getInt("mort");
                int success = rs.getInt("success");
                String role = rs.getString("role");

                alphaPlayer.setMort(mort);
                alphaPlayer.setSuccess(success);
                alphaPlayer.setRole(role);
                GameManager.getInstance().getScheduler().runTask(GameManager.getInstance().getPlugin(), alphaPlayer::joinServer);
            } else {
                Player player = GameManager.getInstance().getPlugin().getServer().getPlayer(uuid);

                GameManager.getInstance().getLogger().info("player "+(player != null));
                if (player != null) {
                    GameManager.getInstance().getLogger().info("player "+player.getName());
                    alphaPlayer.setPlayer(player);
                    CreateDBPlayer(player.getUniqueId(), player.getName());
                }
                GameManager.getInstance().getScheduler().runTask(GameManager.getInstance().getPlugin(), alphaPlayer::joinServer);
            }
        } catch (SQLException ex) {
            GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute, ex);
        } finally {
            try {
                GameManager.getInstance().getLogger().info("close");
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionClose, ex);
            }
        }
    }

    public void CreateDBPlayer(UUID uuid, String pseudo) {
        Connection conn = null;
        PreparedStatement ps = null;
        GameManager.getInstance().getLogger().info("CreateDBPlayer");
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("INSERT INTO player VALUES ('"+uuid+"', 0, 0, '"+pseudo+"', 'Simplet')");
            ps.executeUpdate();
        } catch (SQLException ex) {
            GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute, ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionClose, ex);
            }
        }
    }

    public void updatePlayer(UUID uuid, String column, int value) {
        Connection conn = null;
        PreparedStatement ps = null;
        GameManager.getInstance().getLogger().info("updatePlayer");
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("UPDATE player SET "+column+" = '"+value+"' WHERE uuid = '"+uuid+"'");
            ps.executeUpdate();
        } catch (SQLException ex) {
            GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute, ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionClose, ex);
            }
        }
    }
    //endregion

    //region Villager
    public boolean getVillager(AVillager villager, String name) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs;
        GameManager.getInstance().getLogger().info("getVillager");
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM villager WHERE name = '"+name+"'");
            rs = ps.executeQuery();

            if (rs.next()) {
                villager.setLevel(rs.getInt("level"));
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                villager.setRealVillager(uuid);
                return true;
            }
        } catch (SQLException ex) {
            GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute, ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionClose, ex);
            }
        }
        return false;
    }

    public void CreateDBVillager(String name, UUID uuid) {
        Connection conn = null;
        PreparedStatement ps = null;
        GameManager.getInstance().getLogger().info("CreateDBVillager");
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("INSERT INTO villager VALUES ('"+uuid+"', '0', '"+name+"')");
            ps.executeUpdate();
        } catch (SQLException ex) {
            GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute, ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionClose, ex);
            }
        }
    }

    public void updateVillager(UUID uuid, int level) {
        Connection conn = null;
        PreparedStatement ps = null;
        GameManager.getInstance().getLogger().info("updateVillager");
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("UPDATE villager SET level = '"+level+"' WHERE uuid = '"+uuid+"'");
            ps.executeUpdate();
        } catch (SQLException ex) {
            GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute, ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionClose, ex);
            }
        }
    }

    public void updateVillagerUUID(UUID uuid, String name) {
        Connection conn = null;
        PreparedStatement ps = null;
        GameManager.getInstance().getLogger().info("updateVillager " + uuid + " " + name);
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("UPDATE villager SET uuid = '"+uuid+"' WHERE name = '"+name+"'");
            ps.executeUpdate();
        } catch (SQLException ex) {
            GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute, ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionClose, ex);
            }
        }
    }

    public boolean isVillagerUUIDExist(UUID uuid) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs;
        GameManager.getInstance().getLogger().info("isVillagerUUIDExist");
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
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionClose, ex);
            }
        }
        return false;
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
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.sqlConnectionClose, ex);
            }
        }
        return "error";
    }

    public void close(PreparedStatement ps,ResultSet rs){
        try {
            if (ps != null)
                ps.close();
            if (rs != null)
                rs.close();
        } catch (SQLException ex) {
            Error.close(ex);
        }
    }

    public boolean IsLoaded() {
        return isLoaded;
    }
}