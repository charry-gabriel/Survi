package fr.miuby.survi.system.database;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.log.ELogTag;

import java.sql.*;
import java.util.logging.Level;

public class SQLite extends Database {
    private static final int CURRENT_DB_VERSION = 19;

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
            s.executeUpdate(createPlayerQuestRerollTable());
            s.executeUpdate(createServerDataTable());
            s.executeUpdate(createDelayedEffectsTable());
            s.executeUpdate(createGraveTable());
            s.executeUpdate(createGraveLostNotificationTable());
            s.executeUpdate(createQuestHistoryTable());
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_qh_player ON quest_history (player_uuid)");
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_qh_type   ON quest_history (quest_type)");
            s.executeUpdate(createTradeHistoryTable());
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_th_player ON player_trade_history (player_uuid)");
            s.executeUpdate(createTributeHistoryTable());
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_tth_player ON player_tribute_history (player_uuid)");
            s.executeUpdate(createRareJobItemTable());
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
                "`spawn_world` VARCHAR(255) DEFAULT NULL," +
                "`spawn_x` REAL DEFAULT NULL," +
                "`spawn_y` REAL DEFAULT NULL," +
                "`spawn_z` REAL DEFAULT NULL," +
                "`spawn_yaw` REAL DEFAULT NULL," +
                "`spawn_pitch` REAL DEFAULT NULL," +
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
                "`farm_level` INTEGER NOT NULL DEFAULT 3," +
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

    private String createGraveTable() {
        return "CREATE TABLE IF NOT EXISTS grave (" +
                "`id` VARCHAR(36) NOT NULL," +
                "`owner_uuid` VARCHAR(36) NOT NULL," +
                "`world_uid` VARCHAR(36) NOT NULL," +
                "`x` INT NOT NULL," +
                "`y` INT NOT NULL," +
                "`z` INT NOT NULL," +
                "`items_yaml` TEXT NOT NULL," +
                "PRIMARY KEY (`id`)" +
                ");";
    }

    private String createGraveLostNotificationTable() {
        return "CREATE TABLE IF NOT EXISTS grave_lost_notification (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT," +
                "`player_uuid` VARCHAR(36) NOT NULL," +
                "`world_name` VARCHAR(255) NOT NULL," +
                "`x` INT NOT NULL," +
                "`y` INT NOT NULL," +
                "`z` INT NOT NULL" +
                ");";
    }

    private String createQuestHistoryTable() {
        return "CREATE TABLE IF NOT EXISTS quest_history (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT," +
                "`player_uuid` VARCHAR(36) NOT NULL," +
                "`player_pseudo` VARCHAR(255) NOT NULL," +
                "`quest_id` VARCHAR(255) NOT NULL," +
                "`completed_at` TEXT NOT NULL," +
                "`difficulty` INT NOT NULL DEFAULT -1," +
                "`job` VARCHAR(50)," +
                "`quest_type` VARCHAR(20) NOT NULL DEFAULT 'daily'," +
                "`contribution` INT NOT NULL DEFAULT 0" +
                ");";
    }

    private String createTradeHistoryTable() {
        return "CREATE TABLE IF NOT EXISTS player_trade_history (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT," +
                "`player_uuid` VARCHAR(36) NOT NULL," +
                "`player_pseudo` VARCHAR(255) NOT NULL," +
                "`trader_id` VARCHAR(255) NOT NULL," +
                "`item_material` VARCHAR(255) NOT NULL," +
                "`quantity` INT NOT NULL DEFAULT 1," +
                "`traded_at` TEXT NOT NULL" +
                ");";
    }

    private String createTributeHistoryTable() {
        return "CREATE TABLE IF NOT EXISTS player_tribute_history (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT," +
                "`player_uuid` VARCHAR(36) NOT NULL," +
                "`player_pseudo` VARCHAR(255) NOT NULL," +
                "`villager_id` VARCHAR(255) NOT NULL," +
                "`item_material` VARCHAR(255) NOT NULL," +
                "`quantity` INT NOT NULL DEFAULT 1," +
                "`given_at` TEXT NOT NULL" +
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
            if (currentVersion < 10) {
                s.executeUpdate(createGraveTable());
            }
            if (currentVersion < 11) {
                s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_pq_uuid_date ON player_quest (player_uuid, last_accepted)");
            }
            if (currentVersion < 12) {
                s.executeUpdate(createQuestHistoryTable());
                s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_qh_player ON quest_history (player_uuid)");
                s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_qh_type   ON quest_history (quest_type)");
            }
            if (currentVersion < 13) {
                if (!hasColumn("planted_crops", "farm_level")) {
                    s.executeUpdate("ALTER TABLE planted_crops ADD COLUMN farm_level INTEGER NOT NULL DEFAULT 3");
                }
            }
            if (currentVersion < 14) {
                s.executeUpdate(createTradeHistoryTable());
                s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_th_player ON player_trade_history (player_uuid)");
            }
            if (currentVersion < 15) {
                s.executeUpdate(createTributeHistoryTable());
                s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_tth_player ON player_tribute_history (player_uuid)");
            }
            if (currentVersion < 16) {
                s.executeUpdate(createGraveLostNotificationTable());
            }
            if (currentVersion < 17) {
                if (!hasColumn("player", "spawn_world")) {
                    s.executeUpdate("ALTER TABLE player ADD COLUMN spawn_world VARCHAR(255) DEFAULT NULL");
                }
                if (!hasColumn("player", "spawn_x")) {
                    s.executeUpdate("ALTER TABLE player ADD COLUMN spawn_x REAL DEFAULT NULL");
                }
                if (!hasColumn("player", "spawn_y")) {
                    s.executeUpdate("ALTER TABLE player ADD COLUMN spawn_y REAL DEFAULT NULL");
                }
                if (!hasColumn("player", "spawn_z")) {
                    s.executeUpdate("ALTER TABLE player ADD COLUMN spawn_z REAL DEFAULT NULL");
                }
                if (!hasColumn("player", "spawn_yaw")) {
                    s.executeUpdate("ALTER TABLE player ADD COLUMN spawn_yaw REAL DEFAULT NULL");
                }
                if (!hasColumn("player", "spawn_pitch")) {
                    s.executeUpdate("ALTER TABLE player ADD COLUMN spawn_pitch REAL DEFAULT NULL");
                }
            }
            if (currentVersion < 18) {
                s.executeUpdate(createRareJobItemTable());
            }
            if (currentVersion < 19) {
                s.executeUpdate(createPlayerQuestRerollTable());
            }
        }
    }

    private String createRareJobItemTable() {
        return "CREATE TABLE IF NOT EXISTS player_rare_job_item (" +
                "`player_uuid` VARCHAR(36) NOT NULL," +
                "`job`         VARCHAR(50) NOT NULL," +
                "`action_count` INTEGER    NOT NULL DEFAULT 0," +
                "`has_item`     INTEGER    NOT NULL DEFAULT 0," +
                "PRIMARY KEY (`player_uuid`, `job`)" +
                ");";
    }

    /** Limite quotidienne du consommable {@code QUEST_REROLL} — voir {@code QuestManager#rerollQuest}. */
    private String createPlayerQuestRerollTable() {
        return "CREATE TABLE IF NOT EXISTS player_quest_reroll (" +
                "`player_uuid` VARCHAR(36) NOT NULL," +
                "`last_reroll_day` INTEGER NOT NULL DEFAULT -1," +
                "PRIMARY KEY (`player_uuid`)" +
                ");";
    }
}