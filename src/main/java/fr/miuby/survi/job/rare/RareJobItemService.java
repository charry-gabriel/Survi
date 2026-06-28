package fr.miuby.survi.job.rare;

import fr.miuby.lib.MiubyLib;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.ECustomItem;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.lang.LangService;
import fr.miuby.survi.system.log.ELogTag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gère les objets rares de collection liés aux métiers.
 *
 * <p>Chaque métier possède un objet unique qu'un joueur ne peut obtenir qu'une seule fois.
 * La chance de l'obtenir est débloquée à partir d'un certain nombre d'actions métier
 * (minerais cassés, bûches, récoltes, enchantements, poissons, monstres tués loin).
 * Elle augmente linéairement jusqu'à un plafond de 0,01 % (1 chance sur 10 000).</p>
 *
 * <h3>Formule</h3>
 * <pre>
 *   effectiveActions = max(0, actionCount − threshold[job])
 *   chance = min(0,0001 ; effectiveActions × 0,0001 / growthRange[job])
 * </pre>
 *
 * Pour l'Explorateur le seuil est positionnel (≥ 600 blocs du 0,0), vérifié par
 * {@link fr.miuby.survi.listener.job.RareJobItemListener}, donc threshold=0 ici.
 */
public class RareJobItemService {

    // ─── Constantes ──────────────────────────────────────────────────────────────

    private static final double MAX_CHANCE  = 0.0001; // 0,01 %
    private static final int    SAVE_EVERY  = 25;     // flush DB toutes les 25 actions

    /** Niveau métier minimum requis avant que la chance ne commence à s'activer. */
    private static final int MIN_JOB_LEVEL = 5;

    /**
     * Fenêtre de détection des activités suspectes (10 minutes).
     * Si un joueur dépasse {@link #SUSPICIOUS_THRESHOLDS} dans cette fenêtre, un WARNING est loggé.
     */
    private static final long SUSPICIOUS_WINDOW_MS = 10 * 60 * 1_000L;

    /** Seuils au-delà desquels l'activité est considérée suspecte (par fenêtre de 10 min). */
    private static final Map<EJob, Long> SUSPICIOUS_THRESHOLDS = new EnumMap<>(EJob.class);

    /** Nombre d'actions minimum avant que la chance ne soit débloquée. */
    private static final Map<EJob, Long> THRESHOLDS = new EnumMap<>(EJob.class);
    /** Nombre d'actions au-delà du seuil pour atteindre MAX_CHANCE. */
    private static final Map<EJob, Long> GROWTH_RANGES = new EnumMap<>(EJob.class);
    /** Objet rare associé à chaque métier. */
    private static final Map<EJob, ECustomItem> RARE_ITEMS = new EnumMap<>(EJob.class);

    static {
        THRESHOLDS.put(EJob.MINER,       5_000L);
        THRESHOLDS.put(EJob.LUMBERJACK,  3_000L);
        THRESHOLDS.put(EJob.FARMER,     10_000L);
        THRESHOLDS.put(EJob.ENCHANTER,     500L);
        THRESHOLDS.put(EJob.FISHERMAN,     500L);
        THRESHOLDS.put(EJob.EXPLORER,        0L);

        GROWTH_RANGES.put(EJob.MINER,       50_000L);  // ×10 — minage régulier
        GROWTH_RANGES.put(EJob.LUMBERJACK,  30_000L);  // ×10
        GROWTH_RANGES.put(EJob.FARMER,     200_000L);  // ×100 — actions très rapides
        GROWTH_RANGES.put(EJob.ENCHANTER,    2_000L);  // ×10
        GROWTH_RANGES.put(EJob.FISHERMAN,    5_000L);  // ×10
        GROWTH_RANGES.put(EJob.EXPLORER,     5_000L);  // ×10

        // Seuils suspects (10 min) — clairement anormaux, pas de faux positifs sur AFK farms raisonnables
        SUSPICIOUS_THRESHOLDS.put(EJob.MINER,        100L); // >800 vrais minerais/10min = hack évident
        SUSPICIOUS_THRESHOLDS.put(EJob.LUMBERJACK,   500L); // >1000 bûches/10min
        SUSPICIOUS_THRESHOLDS.put(EJob.FARMER,     1_000L); // >5000 récoltes/10min
        SUSPICIOUS_THRESHOLDS.put(EJob.ENCHANTER,     50L); // >100 enchantements/10min = exploit XP
        SUSPICIOUS_THRESHOLDS.put(EJob.FISHERMAN,     50L); // >500 poissons/10min = plusieurs farms simultanés
        SUSPICIOUS_THRESHOLDS.put(EJob.EXPLORER,      50L); // >500 kills/10min au-delà de 600 blocs

        RARE_ITEMS.put(EJob.MINER,      ECustomItem.RARE_MINER);
        RARE_ITEMS.put(EJob.LUMBERJACK, ECustomItem.RARE_LUMBERJACK);
        RARE_ITEMS.put(EJob.FARMER,     ECustomItem.RARE_FARMER);
        RARE_ITEMS.put(EJob.ENCHANTER,  ECustomItem.RARE_ENCHANTER);
        RARE_ITEMS.put(EJob.FISHERMAN,  ECustomItem.RARE_FISHERMAN);
        RARE_ITEMS.put(EJob.EXPLORER,   ECustomItem.RARE_EXPLORER);
    }

    private static final Random RANDOM = new Random();

    // ─── État en mémoire ─────────────────────────────────────────────────────────

    private static final class PlayerRareData {
        /** job → long[]{actionCount, hasItem (0/1)} */
        final EnumMap<EJob, long[]> jobData = new EnumMap<>(EJob.class);
        /**
         * Fenêtre glissante pour la détection d'activité suspecte.
         * job → long[]{actionsInWindow, windowStartMs, warnedThisWindow (0/1)}
         */
        final EnumMap<EJob, long[]> suspectWindow = new EnumMap<>(EJob.class);
        /**
         * Vrai une fois que les données ont été chargées depuis la DB.
         * Toutes les écritures dans jobData se font avant que ready passe à true (volatile
         * → garantie happens-before), donc la lecture sur le thread principal est sûre.
         */
        volatile boolean ready = false;
    }

    private final ConcurrentHashMap<UUID, PlayerRareData> playerData = new ConcurrentHashMap<>();
    private final RareJobItemRepository repo;

    public RareJobItemService(RareJobItemRepository repo) {
        this.repo = repo;
    }

    // ─── Cycle de vie joueur ─────────────────────────────────────────────────────

    /**
     * Charge les données d'un joueur depuis la DB en async et les met en mémoire.
     * Appelé à la connexion du joueur.
     */
    public void loadPlayer(UUID uuid) {
        PlayerRareData data = new PlayerRareData();
        playerData.put(uuid, data);

        MiubyLib.runAsync(() -> {
            try {
                Map<EJob, long[]> loaded = repo.loadPlayer(uuid);
                data.jobData.putAll(loaded);
                for (EJob job : EJob.values()) {
                    data.jobData.computeIfAbsent(job, k -> new long[]{0L, 0L});
                }
                data.ready = true;
                MLLogManager.getInstance().log(Level.FINE, ELogTag.ITEM,
                        "[RareJobItem] Données chargées pour " + uuid
                                + " (" + loaded.size() + " entrées existantes en DB)");
            } catch (Exception ex) {
                MLLogManager.getInstance().log(Level.SEVERE, ELogTag.ITEM,
                        "[RareJobItem] Échec du chargement pour " + uuid, ex);
            }
        });
    }

    /**
     * Sauvegarde l'état final d'un joueur en DB et le retire de la mémoire.
     * Appelé à la déconnexion du joueur.
     */
    public void unloadPlayer(UUID uuid) {
        PlayerRareData data = playerData.remove(uuid);
        if (data == null || !data.ready) return;

        boolean shuttingDown = Bukkit.isStopping();

        for (Map.Entry<EJob, long[]> entry : data.jobData.entrySet()) {
            long[] jd = entry.getValue();
            if (shuttingDown) {
                try {
                    repo.saveSync(uuid, entry.getKey(), jd[0], jd[1] == 1L);
                } catch (Exception ex) {
                    MLLogManager.getInstance().log(Level.SEVERE, ELogTag.ITEM,
                            "[RareJobItem] Échec saveSync (arrêt serveur) pour " + uuid + " / " + entry.getKey()
                                    + " (action_count=" + jd[0] + ")", ex);
                }
            } else {
                repo.save(uuid, entry.getKey(), jd[0], jd[1] == 1L);
            }
        }
        MLLogManager.getInstance().log(Level.FINE, ELogTag.ITEM,
                "[RareJobItem] Données déchargées et sauvegardées pour " + uuid
                        + (shuttingDown ? " (sync — arrêt serveur)" : " (async)"));
    }

    // ─── Logique métier ──────────────────────────────────────────────────────────

    /**
     * À appeler à chaque action métier qualifiante.
     * Incrémente le compteur en mémoire, teste la chance et attribue l'objet si gagné.
     *
     * @param player joueur ayant effectué l'action
     * @param job    métier concerné
     */
    public void onJobAction(Player player, EJob job) {
        UUID uuid = player.getUniqueId();
        PlayerRareData data = playerData.get(uuid);
        if (data == null || !data.ready) {
            MLLogManager.getInstance().log(Level.FINE, ELogTag.ITEM,
                    "[RareJobItem] Action ignorée — données non prêtes pour " + player.getName() + " / " + job);
            return;
        }

        // ── Garde : niveau métier minimum ────────────────────────────────────
        AlphaPlayer alpha = AlphaPlayer.get(uuid);
        if (alpha.getJobLevel(job) < MIN_JOB_LEVEL) return;

        // ── Détection d'activité suspecte (loggue un WARNING, ne bloque pas) ─
        checkSuspiciousActivity(player, job, data);

        long[] jd = data.jobData.computeIfAbsent(job, k -> new long[]{0L, 0L});

        if (jd[1] == 1L) return; // objet déjà obtenu

        jd[0]++;
        long actionCount = jd[0];

        // Flush périodique en DB
        if (actionCount % SAVE_EVERY == 0) {
            repo.save(uuid, job, actionCount, false);
            MLLogManager.getInstance().log(Level.FINE, ELogTag.ITEM,
                    "[RareJobItem] Flush action_count=" + actionCount + " pour " + player.getName() + " / " + job);
        }

        double chance = computeChance(job, actionCount);
        if (chance <= 0) return;

        if (RANDOM.nextDouble() < chance) {
            grantItem(player, job, jd, actionCount, chance);
        }
    }

    // ─── API publique (commandes admin) ──────────────────────────────────────────

    /** Retourne un snapshot immutable des données en mémoire. Null si le joueur n'est pas chargé. */
    public Map<EJob, long[]> getMemorySnapshot(UUID uuid) {
        PlayerRareData data = playerData.get(uuid);
        if (data == null || !data.ready) return null;
        Map<EJob, long[]> snap = new EnumMap<>(EJob.class);
        for (Map.Entry<EJob, long[]> e : data.jobData.entrySet()) {
            long[] jd = e.getValue();
            snap.put(e.getKey(), new long[]{jd[0], jd[1]});
        }
        return snap;
    }

    /** Seuil d'actions minimal avant que la chance ne s'active pour ce métier. */
    public static long getThreshold(EJob job) {
        return THRESHOLDS.get(job);
    }

    /**
     * Remet à zéro le compteur et l'état has_item d'un joueur pour un métier,
     * en mémoire (si connecté) et en DB (async).
     */
    public void resetJobData(UUID uuid, EJob job) {
        PlayerRareData data = playerData.get(uuid);
        if (data != null && data.ready) {
            long[] jd = data.jobData.computeIfAbsent(job, k -> new long[]{0L, 0L});
            jd[0] = 0L;
            jd[1] = 0L;
        }
        repo.forceReset(uuid, job);
        MLLogManager.getInstance().log(Level.INFO, ELogTag.ITEM,
                "[RareJobItem] resetJobData : " + uuid + " / " + job.name());
    }

    // ─── Privé ───────────────────────────────────────────────────────────────────

    /**
     * Calcule la chance (probabilité entre 0 et MAX_CHANCE) d'obtenir l'objet rare.
     * Retourne 0 si le seuil n'est pas encore atteint.
     */
    public static double computeChance(EJob job, long actionCount) {
        long threshold   = THRESHOLDS.get(job);
        long growthRange = GROWTH_RANGES.get(job);
        long effective   = actionCount - threshold;
        if (effective <= 0) return 0;
        return Math.min(MAX_CHANCE, effective * MAX_CHANCE / growthRange);
    }

    /**
     * Détecte une activité anormalement rapide et loggue un WARNING.
     * N'empêche pas les actions de compter — sert uniquement d'alerte admin.
     * Le WARNING n'est émis qu'une seule fois par fenêtre de 10 minutes.
     */
    private void checkSuspiciousActivity(Player player, EJob job, PlayerRareData data) {
        long now = System.currentTimeMillis();
        long[] w = data.suspectWindow.computeIfAbsent(job, k -> new long[]{0L, now, 0L});

        // Réinitialiser la fenêtre si expirée
        if (now - w[1] > SUSPICIOUS_WINDOW_MS) {
            w[0] = 0L;
            w[1] = now;
            w[2] = 0L;
        }

        w[0]++;

        // Logguer une seule fois par fenêtre quand le seuil est dépassé
        if (w[2] == 0L && w[0] >= SUSPICIOUS_THRESHOLDS.get(job)) {
            w[2] = 1L;
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.ITEM,
                    "[RareJobItem] ⚠ Activité SUSPECTE — " + player.getName()
                            + " a accumulé " + w[0] + " actions [" + job.name()
                            + "] en moins de 10 minutes (seuil=" + SUSPICIOUS_THRESHOLDS.get(job) + ").");
        }
    }

    /** Attribue l'objet rare au joueur, marque l'état en mémoire et en DB, et broadcast. */
    private void grantItem(Player player, EJob job, long[] jd, long actionCount, double chance) {
        jd[1] = 1L;
        repo.save(player.getUniqueId(), job, actionCount, true);

        ItemStack item = RARE_ITEMS.get(job).getItemStack().clone();
        java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover.get(0));
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.ITEM,
                    "[RareJobItem] Inventaire plein pour " + player.getName() + " — item rare " + job.name() + " droppé au sol");
        }

        LangService ls = GameManager.getInstance().getLangService();
        ls.broadcast("job.rare_item.broadcast",
                Placeholder.unparsed("player", player.getName()),
                Placeholder.component("job", job.toComponent()));

        MLLogManager.getInstance().log(Level.INFO, ELogTag.ITEM,
                "[RareJobItem] Objet rare attribué : " + player.getName()
                        + " / " + job.name()
                        + " (actions=" + actionCount
                        + ", chance=" + String.format("%.6f%%", chance * 100) + ")");
    }
}