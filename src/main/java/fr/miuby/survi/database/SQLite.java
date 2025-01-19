package fr.miuby.survi.database;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import fr.miuby.survi.GameManager;

import javax.annotation.Nullable;

/**
 * The SQLite class provides an implementation for managing a SQLite database.
 * This database is used to store and handle data related to players and villagers.
 * It extends the Database class and includes functionality for establishing
 * database connections, creating tables, and initializing the database.
 */
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
            "`uuid` varchar(255) NOT NULL," +
	        "`level` int(11) NOT NULL," +
            "`name` varchar(255) NOT NULL," +
	        "`givenItems` varchar(255)," +
	        "`locationX` FLOAT NOT NULL," +
            "`locationY` FLOAT NOT NULL," +
            "`locationZ` FLOAT NOT NULL," +
            "`locationYaw` FLOAT NOT NULL," +
            "`locationPitch` FLOAT NOT NULL," +
            "PRIMARY KEY (`uuid`)" +
            ");";

    /**
     * Establishes and returns a connection to the SQLite database for the application.
     * If the database file does not exist, it attempts to create the file.
     * If the connection is already established and not closed, it returns the existing connection.
     * Otherwise, it initializes a new SQLite connection.
     *
     * @return a {@link Connection} object representing the SQLite database connection,
     * or null if unable to establish a connection due to an exception or missing dependencies.
     */
    @Nullable
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
            return connection;
        } catch (SQLException ex) {
            GameManager.getInstance().getLogger().log(Level.SEVERE,"SQLite exception on initialize", ex);
        } catch (ClassNotFoundException ex) {
            GameManager.getInstance().getLogger().log(Level.SEVERE, "You need the SQLite JBDC library. Google it. Put it in /lib folder.");
        }
        return null;
    }

    /**
     * Loads the SQLite database by initializing the connection and executing the necessary SQL statements
     * to ensure the required tables exist. If an error occurs during table creation, it logs the error using
     * the application's logger. After database setup, the method calls the {@code initialize()} function
     * to finalize the setup process.
     *
     * This method performs the following operations:
     * - Establishes an SQLite connection using {@link #getSQLConnection()}.
     * - Executes SQL commands to create the tables defined by {@code SQLiteCreatePlayerTable} and
     *   {@code SQLiteCreateVillagerTable}.
     * - Logs any SQL exceptions encountered during the execution.
     */
    public void load() {
        connection = getSQLConnection();
        try {
            Statement s = connection.createStatement();
            s.executeUpdate(SQLiteCreatePlayerTable);
            s.executeUpdate(SQLiteCreateVillagerTable);
            s.close();
        } catch (SQLException e) {
            GameManager.getInstance().getLogger().severe(e.getMessage());
        }
        initialize();
    }
}