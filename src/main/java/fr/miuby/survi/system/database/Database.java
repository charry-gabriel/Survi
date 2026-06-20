package fr.miuby.survi.system.database;

import fr.miuby.lib.sqlite.MLSQLite;
import fr.miuby.survi.system.database.repository.*;

import java.sql.Connection;

/**
 * Classe abstraite représentant la base de données de Survi.
 * Étend {@link MLSQLite} pour la gestion de la connexion et des migrations.
 * Délègue les opérations métier à des repositories spécialisés.
 *
 * <p>L'implémentation concrète ({@link SQLite}) fournit la version cible du schéma,
 * le SQL de création des tables et la logique de migration.</p>
 */
public abstract class Database extends MLSQLite {

    // Repositories
    protected PlayerRepository playerRepository;
    protected VillagerRepository villagerRepository;
    protected CropRepository cropRepository;
    protected QuestRepository questRepository;
    protected SystemRepository systemRepository;
    protected GraveRepository graveRepository;
    protected QuestHistoryRepository questHistoryRepository;
    protected TradeHistoryRepository tradeHistoryRepository;

    protected Database(String dbName) {
        super(dbName);
    }

    /**
     * Initialise les repositories après que la connexion est ouverte et les migrations appliquées.
     * Appelé automatiquement par {@link MLSQLite#load()}.
     * Les sous-classes qui surchargent cette méthode doivent appeler {@code super.onLoaded()} en premier.
     */
    @Override
    protected void onLoaded() {
        Connection conn = getConnection();
        playerRepository        = new PlayerRepository(conn, this);
        villagerRepository      = new VillagerRepository(conn, this);
        cropRepository          = new CropRepository(conn, this);
        questRepository         = new QuestRepository(conn, this);
        systemRepository        = new SystemRepository(conn, this);
        graveRepository         = new GraveRepository(conn, this);
        questHistoryRepository  = new QuestHistoryRepository(conn, this);
        tradeHistoryRepository  = new TradeHistoryRepository(conn, this);
    }

    // =========================================================================
    // Délégués aux repositories
    // =========================================================================

    public PlayerRepository players()                  { return playerRepository; }
    public VillagerRepository villagers()              { return villagerRepository; }
    public CropRepository crops()                      { return cropRepository; }
    public QuestRepository quests()                    { return questRepository; }
    public SystemRepository system()                   { return systemRepository; }
    public GraveRepository graves()                    { return graveRepository; }
    public QuestHistoryRepository questHistory()       { return questHistoryRepository; }
    public TradeHistoryRepository tradeHistory()       { return tradeHistoryRepository; }
}