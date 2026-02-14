package fr.miuby.survi.database;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
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
 * Schema versioning is handled via PRAGMA user_version; migrations run on load when needed.
 */
public class SQLite extends Database {
    private final String dbname;

    /** Current schema version. Bump this when you add a migration. */
    private static final int CURRENT_DB_VERSION = 2;

    public SQLite(){
        dbname = GameManager.getInstance().getPlugin().getConfig().getString("SQLite.Filename", "minecraft");
    }

    public final String SQLiteCreatePlayerTable = "CREATE TABLE IF NOT EXISTS player (" +
            "`uuid` varchar(255) NOT NULL," +
            "`mort` int(11) NOT NULL," +
            "`success` int(11) NOT NULL," +
            "`pseudo` varchar(255) NOT NULL," +
            "`role` varchar(255) NOT NULL," +
            "`subroles` varchar(255)," +
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
            
    public final String SQLiteCreateCropTable = "CREATE TABLE IF NOT EXISTS planted_crops (" +
            "`world_uid` VARCHAR(36) NOT NULL," +
            "`x` INT NOT NULL," +
            "`y` INT NOT NULL," +
            "`z` INT NOT NULL," +
            "PRIMARY KEY (`world_uid`, `x`, `y`, `z`)" +
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
            // For async operations, always create a new connection to avoid "stmt pointer is closed"
            if(connection != null && !connection.isClosed() && Thread.currentThread().getName().equals("Server thread")){
                return connection;
            }
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection("jdbc:sqlite:" + dataFolder);
        } catch (SQLException ex) {
            GameManager.getInstance().getLogger().log(Level.SEVERE,"SQLite exception on initialize", ex);
        } catch (ClassNotFoundException ex) {
            GameManager.getInstance().getLogger().log(Level.SEVERE, "You need the SQLite JBDC library. Google it. Put it in /lib folder.");
        }
        return null;
    }

    /**
     * Returns the current schema version from the database (PRAGMA user_version).
     * Returns 0 if the database is new or the pragma fails.
     */
    private int getCurrentVersion(Connection conn) {
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("PRAGMA user_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            GameManager.getInstance().getLogger().log(Level.WARNING, "Could not read DB version, assuming 0", e);
            return 0;
        }
    }

    /**
     * Sets the schema version in the database (PRAGMA user_version).
     */
    private void setVersion(Connection conn, int version) throws SQLException {
        try (Statement s = conn.createStatement()) {
            s.execute("PRAGMA user_version = " + version);
        }
    }

    /**
     * Returns true if the given table has the given column (SQLite PRAGMA table_info).
     */
    private boolean hasColumn(Connection conn, String table, String column) throws SQLException {
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Applies schema changes to go from previous version to CURRENT_DB_VERSION.
     * Called only when currentVersion < CURRENT_DB_VERSION. Update this method and bump
     * CURRENT_DB_VERSION each time you change the schema.
     */
    private void runMigration(Connection conn, int currentVersion) throws SQLException {
        GameManager.getInstance().getLogger().info("Database schema " + currentVersion + " -> " + CURRENT_DB_VERSION + ", running migration.");
        try (Statement s = conn.createStatement()) {
            if (!hasColumn(conn, "player", "subroles")) {
                s.executeUpdate("ALTER TABLE player ADD COLUMN subroles varchar(255)");
            }
            setVersion(conn, CURRENT_DB_VERSION);
        }
    }

    /**
     * Loads the SQLite database by initializing the connection and executing the necessary SQL statements
     * to ensure the required tables exist. If the database already exists with an older schema version,
     * migrations are run (e.g. ALTER TABLE) to bring it up to date. If an error occurs during table
     * creation or migration, it is logged. After setup, {@link #initialize()} is called.
     */
    public void load() {
        connection = getSQLConnection();
        if (connection == null) {
            return;
        }
        try {
            Statement s = connection.createStatement();
            s.executeUpdate(SQLiteCreatePlayerTable);
            s.executeUpdate(SQLiteCreateVillagerTable);
            s.executeUpdate(SQLiteCreateCropTable);
            s.close();

            int current = getCurrentVersion(connection);
            if (current < CURRENT_DB_VERSION) {
                runMigration(connection, current);
            }
        } catch (SQLException e) {
            GameManager.getInstance().getLogger().severe(e.getMessage());
        }
        initialize();
    }
}