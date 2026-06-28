package fr.miuby.survi.system.database;

import fr.miuby.lib.sqlite.MLSQLite;
import fr.miuby.survi.item.rare_item.RareItemRepository;
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
    protected GraveLostNotificationRepository graveLostNotificationRepository;
    protected QuestHistoryRepository questHistoryRepository;
    protected TradeHistoryRepository tradeHistoryRepository;
    protected TributeHistoryRepository tributeHistoryRepository;
    protected RareItemRepository rareItemRepository;

    protected Database(String dbName) {
        super(dbName);
    }

    /**
     * Initialise les repositories après que la connexion est ouverte et les migrations appliquées.
     * Appelé automatiquement par {@link MLSQLite#load()}.
     * Les sous-classes qui surchargent cette méthode doivent appeler {@code super.onLoaded()} en premier.
     *
     * <p>La connexion distribuée aux repositories est {@link MLSQLite#getResilientConnection()} :
     * un proxy auto-réparant, pas une référence figée. Si la connexion physique sous-jacente venait
     * à se rompre (verrou bloqué, disque plein…), elle se rouvre automatiquement au prochain appel —
     * un repository ne reste plus jamais bloqué sur une connexion morte jusqu'au redémarrage.</p>
     */
    @Override
    protected void onLoaded() {
        Connection conn = getResilientConnection();
        playerRepository        = new PlayerRepository(conn, this);
        villagerRepository      = new VillagerRepository(conn, this);
        cropRepository          = new CropRepository(conn, this);
        questRepository         = new QuestRepository(conn, this);
        systemRepository        = new SystemRepository(conn, this);
        graveRepository                  = new GraveRepository(conn, this);
        graveLostNotificationRepository  = new GraveLostNotificationRepository(conn, this);
        questHistoryRepository           = new QuestHistoryRepository(conn, this);
        tradeHistoryRepository  = new TradeHistoryRepository(conn, this);
        tributeHistoryRepository = new TributeHistoryRepository(conn, this);
        rareItemRepository = new RareItemRepository(conn, this);
    }

    // =========================================================================
    // Délégués aux repositories
    // =========================================================================

    public PlayerRepository players()                  { return playerRepository; }
    public VillagerRepository villagers()              { return villagerRepository; }
    public CropRepository crops()                      { return cropRepository; }
    public QuestRepository quests()                    { return questRepository; }
    public SystemRepository system()                   { return systemRepository; }
    public GraveRepository graves()                                        { return graveRepository; }
    public GraveLostNotificationRepository graveLostNotifications()        { return graveLostNotificationRepository; }
    public QuestHistoryRepository questHistory()                           { return questHistoryRepository; }
    public TradeHistoryRepository tradeHistory()       { return tradeHistoryRepository; }
    public TributeHistoryRepository tributeHistory()   { return tributeHistoryRepository; }
    public RareItemRepository rareJobItems()        { return rareItemRepository; }
}