package fr.miuby.survi.system.database;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.log.ELogTag;

import java.sql.*;
import java.util.logging.Level;

public class SQLite extends Database {
    private static final int CURRENT_DB_VERSION = 9;

    public SQLite() {
        super(GameManager.getInstance().getPlugin().getConfig().getString("SQLite.Filename", "minecraft"));
    }

    // =========================================================================
    // Cycle de vie
    // =========================================================================

    @Override
    protected int getTargetVersion() {
        return CURRENT_DB_VERSION;
    }

    @Override
    protected void onLoaded() {
        super.onLoaded(); // init repositories
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM player WHERE pseudo = ?")) {
            ps.setString(1, "Miuby");
            try (ResultSet rs = ps.executeQuery()) {
                MLLogManager.getInstance().log(Level.INFO, ELogTag.SYSTEM, "Database connexion succeeded !");
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.SYSTEM, "No SQL connection", ex);
        }
    }

    // =========================================================================
    // Création des tables
    // =========================================================================

    @Override
    protected void createTables() throws SQLException {
        try (Statement s = getConnection().createStatement()) {
            s.executeUpdate(createPlayerTable());
            s.executeUpdate(createVillagerTable());
            s.executeUpdate(createCropTable());
            s.executeUpdate(createReputationTable());
            s.executeUpdate(createPlayerQuestTable());
            s.executeUpdate(createPlayerQuestMetaTable());
            s.executeUpdate(createServerDataTable());
            s.executeUpdate(createDelayedEffectsTable());
        }
    }

    private String createPlayerTable() {
        return "CREATE TABLE IF NOT EXISTS player (" +
                "`uuid` varchar(255) NOT NULL," +
                "`mort` int(11) NOT NULL," +
                "`success` int(11) NOT NULL," +
                "`pseudo` varchar(255) NOT NULL," +
                "`role` varchar(255) NOT NULL," +
                "`subroles` varchar(255)," +
                "PRIMARY KEY (`uuid`)" +
                ");";
    }

    private String createVillagerTable() {
        return "CREATE TABLE IF NOT EXISTS villager (" +
                "`uuid` varchar(255) NOT NULL," +
                "`level` int(11) NOT NULL," +
                "`name` varchar(255) NOT NULL," +
                "`givenItems` varchar(255)," +
                "`locationX` FLOAT NOT NULL," +
                "`locationY` FLOAT NOT NULL," +
                "`locationZ` FLOAT NOT NULL," +
                "`locationYaw` FLOAT NOT NULL," +
                "`locationPitch` FLOAT NOT NULL," +
                "`unlockedDate` BIGINT NOT NULL," +
                "PRIMARY KEY (`uuid`)" +
                ");";
    }

    private String createCropTable() {
        return "CREATE TABLE IF NOT EXISTS planted_crops (" +
                "`world_uid` VARCHAR(36) NOT NULL," +
                "`x` INT NOT NULL," +
                "`y` INT NOT NULL," +
                "`z` INT NOT NULL," +
                "PRIMARY KEY (`world_uid`, `x`, `y`, `z`)" +
                ");";
    }

    private String createReputationTable() {
        return "CREATE TABLE IF NOT EXISTS player_reputation (" +
                "`player_uuid` varchar(255) NOT NULL," +
                "`trader_id` varchar(255) NOT NULL," +
                "`reputation` int(11) NOT NULL," +
                "PRIMARY KEY (`player_uuid`, `trader_id`)" +
                ");";
    }

    private String createPlayerQuestTable() {
        return "CREATE TABLE IF NOT EXISTS player_quest (" +
                "`player_uuid` varchar(255) NOT NULL," +
                "`slot` int NOT NULL DEFAULT 0," +
                "`quest_id` varchar(255) NOT NULL," +
                "`progress` int(11) NOT NULL," +
                "`last_accepted` varchar(255) NOT NULL," +
                "`is_completed` boolean NOT NULL," +
                "`trader_id` varchar(255) NOT NULL DEFAULT ''," +
                "`claimed` boolean NOT NULL DEFAULT 0," +
                "PRIMARY KEY (`player_uuid`, `slot`)" +
                ");";
    }

    private String createPlayerQuestMetaTable() {
        return "CREATE TABLE IF NOT EXISTS player_quest_meta (" +
                "`player_uuid` varchar(255) NOT NULL," +
                "`last_quest_id` varchar(255)," +
                "PRIMARY KEY (`player_uuid`)" +
                ");";
    }

    private String createServerDataTable() {
        return "CREATE TABLE IF NOT EXISTS server_data (" +
                "key TEXT PRIMARY KEY," +
                "value TEXT NOT NULL" +
                ");";
    }

    private String createDelayedEffectsTable() {
        return "CREATE TABLE IF NOT EXISTS delayed_effects (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "effect_type varchar(255) NOT NULL," +
                "target_uuid varchar(255)," +
                "trigger_time BIGINT NOT NULL," +
                "data TEXT" +
                ");";
    }

    // =========================================================================
    // Migrations
    // =========================================================================

    @Override
    protected void runMigrations(int currentVersion) throws SQLException {
        try (Statement s = getConnection().createStatement()) {
            if (currentVersion < 2) {
                if (!hasColumn("player", "subroles")) {
                    s.executeUpdate("ALTER TABLE player ADD COLUMN subroles varchar(255)");
                }
            }
            if (currentVersion < 3) {
                s.executeUpdate(createReputationTable());
                s.executeUpdate(createPlayerQuestTable());
            }
            if (currentVersion < 4) {
                if (!hasColumn("player_quest", "trader_id")) {
                    s.executeUpdate("ALTER TABLE player_quest ADD COLUMN trader_id varchar(255) NOT NULL DEFAULT ''");
                }
            }
            if (currentVersion < 5) {
                if (!hasColumn("player_quest", "claimed")) {
                    s.executeUpdate("ALTER TABLE player_quest ADD COLUMN claimed boolean NOT NULL DEFAULT 0");
                }
            }
            if (currentVersion < 6) {
                s.executeUpdate(createServerDataTable());
            }
            if (currentVersion < 7) {
                s.executeUpdate(createDelayedEffectsTable());
            }
            if (currentVersion < 8) {
                if (!hasColumn("villager", "unlockedDate")) {
                    s.executeUpdate("ALTER TABLE villager ADD COLUMN unlockedDate BIGINT DEFAULT 0");
                }
            }
            if (currentVersion < 9) {
                s.executeUpdate("ALTER TABLE player_quest RENAME TO player_quest_old");
                s.executeUpdate(createPlayerQuestTable());
                s.executeUpdate("INSERT INTO player_quest (player_uuid, slot, quest_id, progress, last_accepted, is_completed, trader_id, claimed) " +
                        "SELECT player_uuid, 0, quest_id, progress, last_accepted, is_completed, trader_id, claimed FROM player_quest_old");
                s.executeUpdate("DROP TABLE player_quest_old");
                s.executeUpdate(createPlayerQuestMetaTable());
                s.executeUpdate("INSERT OR IGNORE INTO player_quest_meta (player_uuid, last_quest_id) " +
                        "SELECT player_uuid, quest_id FROM player_quest WHERE slot = 0");
            }
        }
    }
}