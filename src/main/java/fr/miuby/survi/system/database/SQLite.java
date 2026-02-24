package fr.miuby.survi.system.database;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.logging.Level;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.log.LogManager;

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
    private static final int CURRENT_DB_VERSION = 8;

    public SQLite(){
        dbname = GameManager.getInstance().getPlugin().getConfig().getString("SQLite.Filename", "minecraft");
    }

    public final String SQLiteCreateDelayedEffectsTable = "CREATE TABLE IF NOT EXISTS delayed_effects (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "effect_type varchar(255) NOT NULL," +
            "target_uuid varchar(255)," +
            "trigger_time BIGINT NOT NULL," +
            "data TEXT" +
            ");";

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
            "`unlockedDate` BIGINT DEFAULT NULL," +
            "PRIMARY KEY (`uuid`)" +
            ");";
            
    public final String SQLiteCreateCropTable = "CREATE TABLE IF NOT EXISTS planted_crops (" +
            "`world_uid` VARCHAR(36) NOT NULL," +
            "`x` INT NOT NULL," +
            "`y` INT NOT NULL," +
            "`z` INT NOT NULL," +
            "PRIMARY KEY (`world_uid`, `x`, `y`, `z`)" +
            ");";

    public final String SQLiteCreateReputationTable = "CREATE TABLE IF NOT EXISTS player_reputation (" +
            "`player_uuid` varchar(255) NOT NULL," +
            "`trader_id` varchar(255) NOT NULL," +
            "`reputation` int(11) NOT NULL," +
            "PRIMARY KEY (`player_uuid`, `trader_id`)" +
            ");";

    public final String SQLiteCreatePlayerQuestTable = "CREATE TABLE IF NOT EXISTS player_quest (" +
            "`player_uuid` varchar(255) NOT NULL," +
            "`quest_id` varchar(255) NOT NULL," +
            "`progress` int(11) NOT NULL," +
            "`last_accepted` varchar(255) NOT NULL," +
            "`is_completed` boolean NOT NULL," +
            "`trader_id` varchar(255) NOT NULL DEFAULT ''," +
            "`claimed` boolean NOT NULL DEFAULT 0," +
            "PRIMARY KEY (`player_uuid`)" +
            ");";

    public final String SQLiteCreateServerData = "CREATE TABLE IF NOT EXISTS server_data (" +
            "    key TEXT PRIMARY KEY," +
            "    value TEXT NOT NULL" +
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
                LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.SYSTEM, "File write error: " + dbname + ".db");
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
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.SYSTEM, "SQLite exception on initialize (" + ex.getMessage() + ")");
        } catch (ClassNotFoundException ex) {
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.SYSTEM, "You need the SQLite JBDC library. Google it. Put it in /lib folder.");
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
            LogManager.getInstance().log(Level.WARNING, LogManager.ETagLog.SYSTEM, "Could not read DB version, assuming 0 (" + e.getMessage() + ")");
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
        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.SYSTEM, "Database schema " + currentVersion + " -> " + CURRENT_DB_VERSION + ", running migration.");
        try (Statement s = conn.createStatement()) {
            if (currentVersion < 2) {
                if (!hasColumn(conn, "player", "subroles")) {
                    s.executeUpdate("ALTER TABLE player ADD COLUMN subroles varchar(255)");
                }
            }
            if (currentVersion < 3) {
                s.executeUpdate(SQLiteCreateReputationTable);
                s.executeUpdate(SQLiteCreatePlayerQuestTable);
            }
            if (currentVersion < 4) {
                if (!hasColumn(conn, "player_quest", "trader_id")) {
                    s.executeUpdate("ALTER TABLE player_quest ADD COLUMN trader_id varchar(255) NOT NULL DEFAULT ''");
                }
            }
            if (currentVersion < 5) {
                if (!hasColumn(conn, "player_quest", "claimed")) {
                    s.executeUpdate("ALTER TABLE player_quest ADD COLUMN claimed boolean NOT NULL DEFAULT 0");
                }
            }
            if (currentVersion < 6) {
                s.executeUpdate(SQLiteCreateServerData);
            }
            if (currentVersion < 7) {
                s.executeUpdate(SQLiteCreateDelayedEffectsTable);
            }
            if (currentVersion < 8) {
                if (!hasColumn(conn, "villager", "unlockedDate")) {
                    s.executeUpdate("ALTER TABLE villager ADD COLUMN unlockedDate BIGINT DEFAULT NULL");
                }
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
            s.executeUpdate(SQLiteCreateReputationTable);
            s.executeUpdate(SQLiteCreatePlayerQuestTable);
            s.executeUpdate(SQLiteCreateDelayedEffectsTable);
            s.close();

            int current = getCurrentVersion(connection);
            if (current < CURRENT_DB_VERSION) {
                runMigration(connection, current);
            }
        } catch (SQLException e) {
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.SYSTEM, e.getMessage());
        }
        initialize();
    }
}