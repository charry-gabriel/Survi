package fr.miuby.survi.database;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import fr.miuby.survi.GameManager;

public class SQLite extends Database {
    private final String dbname;

    public SQLite(){
        dbname = GameManager.getInstance().getPlugin().getConfig().getString("SQLite.Filename", "minecraft");
    }

    public final String SQLiteCreatePlayerTable = "CREATE TABLE IF NOT EXISTS player (" +
            "`uuid` varchar(255) NOT NULL," +
            "`mort` int(11) NOT NULL," +
            "`success` int(11) NOT NULL," +
            "`pseudo` varchar(255) NOT NULL," +
            "`role` varchar(255) NOT NULL," +
            "PRIMARY KEY (`uuid`)" +
            ");";

    public final String SQLiteCreateVillagerTable = "CREATE TABLE IF NOT EXISTS villager (" +
            "`name` varchar(255) NOT NULL," +
            "`level` int(11) NOT NULL," +
            "PRIMARY KEY (`name`)" +
            ");";

    public Connection getSQLConnection() {
        File dataFolder = new File(GameManager.getInstance().getPlugin().getDataFolder(), dbname+".db");
        if (!dataFolder.exists()){
            try {
                dataFolder.createNewFile();
            } catch (IOException e) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, "File write error: "+dbname+".db");
            }
        }
        try {
            if(connection!=null&&!connection.isClosed()){
                return connection;
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder);
            GameManager.getInstance().getLogger().log(Level.SEVERE,"jdbc:sqlite:" + dataFolder);
            return connection;
        } catch (SQLException ex) {
            GameManager.getInstance().getLogger().log(Level.SEVERE,"SQLite exception on initialize", ex);
        } catch (ClassNotFoundException ex) {
            GameManager.getInstance().getLogger().log(Level.SEVERE, "You need the SQLite JBDC library. Google it. Put it in /lib folder.");
        }
        return null;
    }

    public void load() {
        connection = getSQLConnection();
        try {
            Statement s = connection.createStatement();
            s.executeUpdate(SQLiteCreatePlayerTable);
            s.executeUpdate(SQLiteCreateVillagerTable);
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        initialize();
    }
}