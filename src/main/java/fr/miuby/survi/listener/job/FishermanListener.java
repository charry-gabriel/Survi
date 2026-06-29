package fr.miuby.survi.listener.job;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.alchemic.AlchemicLootEntry;
import fr.miuby.survi.job.config.JobsConfig;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.lang.LangService;
import fr.miuby.survi.system.log.ELogTag;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

/**
 * Gère les effets du métier {@link EJob#FISHERMAN} liés à la pêche.
 *
 * <h3>Pipeline de loot ({@code CAUGHT_FISH})</h3>
 * <ol>
 *   <li><b>Malus global</b> — {@code dirt-chance[level]} : remplace tout item par un matériau de
 *       {@code dirt-replacement-materials}. Tombe à 0 au niv.7.</li>
 *   <li><b>Malus trésor</b> — {@code treasure-penalty[level]} : si l'item est un trésor
 *       (livre enchanté, arc, canne enchantée, selle, name tag…), probabilité de le remplacer
 *       par {@code treasure-replacement-materials}. Tombe à 0 au niv.6.</li>
 *   <li><b>Pêche alchimique</b> — uniquement sur les <em>non-trésors</em> survivants ; chance =
 *       {@code min(1, alchemic-catch-chance[level] × (1 + alchemic-luck-scale × vanillaLuck))},
 *       où {@code vanillaLuck = attribut LUCK du joueur + niveau Chance de la Mer}.
 *       Formule <b>multiplicative</b> : la luck amplifie la base du niveau, jamais l'inverse.
 *       Au niveau 0 (base = 0), les potions sont à 0 % quelle que soit la luck du pantalon.</li>
 *   <li><b>Multiplicateur de quantité</b> — {@code loot-multiplier[level]} sur l'item restant.</li>
 * </ol>
 *
 * <p>Les effets aquatiques passifs (pression, vitesse, respiration) sont gérés par
 * {@link fr.miuby.survi.job.task.FishermanEffectsTask} et
 * {@link fr.miuby.survi.job.FishermanAttributeService} ; cette classe réagit instantanément
 * à un changement de jambières ({@code onLeggingsChanged}) pour éviter la latence du cycle 3s.</p>
 *
 * <p>Tous les paramètres sont lus depuis {@link JobsConfig} ({@code jobs/fisherman.yml}).</p>
 */
public class FishermanListener implements Listener {

    private static final Random RANDOM = new Random();

    /** Matériaux considérés comme trésors sans enchantements visibles. */
    private static final Set<Material> TREASURE_MATERIALS = EnumSet.of(
            Material.NAME_TAG,
            Material.SADDLE,
            Material.NAUTILUS_SHELL,
            Material.HEART_OF_THE_SEA
    );

    // ─── Équipement — retour instantané quand le pantalon est mis/retiré ──────────

    /**
     * Réagit immédiatement à un changement de jambières pour appliquer/retirer le kit aquatique
     * (vitesse, respiration, minage sous l'eau) sans attendre le prochain cycle de
     * {@link fr.miuby.survi.job.task.FishermanEffectsTask} (jusqu'à 3s de latence sinon).
     */
    @EventHandler(ignoreCancelled = true)
    public void onLeggingsChanged(EntityEquipmentChangedEvent event) {
        if (!event.getEquipmentChanges().containsKey(EquipmentSlot.LEGS)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        if (alpha == null) return;
        GameManager.getInstance().getAlphaPlayerFactory().getFishermanAttributeService().applyAttributes(alpha);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        if (alpha == null) return;
        int level = alpha.getJobLevel(EJob.FISHERMAN);

        switch (event.getState()) {
            case FISHING     -> applyWaitTime(event.getHook(), level);
            case CAUGHT_FISH -> applyLoot(event, level);
            default          -> { /* autres états non modifiés */ }
        }
    }

    // ─── Temps d'attente ─────────────────────────────────────────────────────────

    private static void applyWaitTime(FishHook hook, int level) {
        if (hook == null) return;
        JobsConfig.FishermanCfg cfg = JobsConfig.getInstance().getFisherman();
        double mult = cfg.getFishingWaitMultiplier()[level];
        int min = Math.max(1, (int) Math.round(cfg.getVanillaMinWaitTicks() * mult));
        int max = Math.max(min + 1, (int) Math.round(cfg.getVanillaMaxWaitTicks() * mult));
        hook.setMinWaitTime(min);
        hook.setMaxWaitTime(max);
    }

    // ─── Loot ────────────────────────────────────────────────────────────────────

    private static void applyLoot(PlayerFishEvent event, int level) {
        if (!(event.getCaught() instanceof Item caughtItem)) return;
        ItemStack stack = caughtItem.getItemStack();
        JobsConfig.FishermanCfg cfg = JobsConfig.getInstance().getFisherman();
        Player player = event.getPlayer();

        // Étape 1 : malus global — remplace tout item par un matériau de dirt-replacement-materials
        if (RANDOM.nextDouble() < cfg.getDirtChance()[level]) {
            caughtItem.setItemStack(new ItemStack(pickRandom(cfg.getDirtReplacementMaterials())));
            return;
        }

        // Étape 2 : malus trésor — pénalise les trésors survivants (livres enchantés, arcs, cannes, selles…)
        if (isTreasure(stack) && RANDOM.nextDouble() < cfg.getTreasurePenalty()[level]) {
            caughtItem.setItemStack(new ItemStack(pickRandom(cfg.getTreasureReplacementMaterials())));
            return;
        }

        // Étape 3 : pêche alchimique — uniquement sur les non-trésors.
        //   Les trésors sont volontairement exclus : la luck vanilla (Luck of the Sea + effet Luck)
        //   les booste déjà via la loot table vanilla ; leur ajouter une chance de conversion en
        //   potion les pénaliserait inutilement.
        //   Chance effective = alchemic-catch-chance[level] + alchemic-luck-scale × vanillaLuck.
        if (!isTreasure(stack)) {
            double luck = getVanillaLuck(player);
            // Formule purement multiplicative : base[level] × scale × luck
            // → 0 % si luck = 0 (pas de pantalon ou pantalon tier 0)
            // → 0 % si level = 0 (base = 0), quelle que soit la luck
            // La luck amplifie l'ouverture du niveau ; sans l'un ou l'autre, aucune potion.
            double alchemicChance = Math.min(1.0,
                    cfg.getAlchemicCatchChance()[level] * cfg.getAlchemicLuckScale() * luck);
            if (RANDOM.nextDouble() < alchemicChance) {
                ItemStack alchemicItem = pickAlchemic(level, luck, cfg.getAlchemicLuckWeightBonus(), cfg.getAlchemicLoot());
                if (alchemicItem != null) {
                    caughtItem.setItemStack(alchemicItem);
                    LangService ls = GameManager.getInstance().getLangService();
                    player.sendActionBar(ls.text(player, "job.fisherman.alchemic.catch"));
                    MLLogManager.getInstance().log(Level.FINE, ELogTag.JOB,
                            "[FishermanListener] " + player.getName()
                                    + " niv." + level + " luck=" + String.format("%.1f", luck)
                                    + " chance=" + String.format("%.3f", alchemicChance)
                                    + " → alchimique (" + alchemicItem.getType() + ")");
                    return;
                }
            }
        }

        // Étape 4 : multiplicateur de quantité sur l'item restant (poisson ou trésor)
        double multiplier = cfg.getLootMultiplier()[level];
        double totalAmount = stack.getAmount() * multiplier;
        int amount = (int) totalAmount;
        if (RANDOM.nextDouble() < totalAmount - amount)
            amount++;
        stack.setAmount(Math.clamp(amount, 0, stack.getMaxStackSize()));
    }

    // ─── Luck vanilla ─────────────────────────────────────────────────────────────

    /**
     * Retourne la luck du joueur issue du pantalon de pêcheur (attribut {@code LUCK}).
     * <ul>
     *   <li>Les potions de Chance modifient l'attribut en interne et sont incluses automatiquement.</li>
     *   <li>L'enchantement <b>Chance de la Mer</b> est volontairement <b>exclu</b> : il booste déjà
     *       les trésors vanilla via la loot table interne de Minecraft. L'inclure ici lui ferait
     *       bénéficier d'un double avantage sur les potions alchimiques.</li>
     * </ul>
     */
    private static double getVanillaLuck(Player player) {
        AttributeInstance luckAttr = player.getAttribute(Attribute.LUCK);
        return luckAttr != null ? luckAttr.getValue() : 0.0;
    }

    /**
     * Détecte si l'item est un trésor de pêche :
     * item enchanté (arc, canne), livre enchanté, ou matériau spécifique (selle, name tag…).
     */
    private static boolean isTreasure(ItemStack stack) {
        if (TREASURE_MATERIALS.contains(stack.getType())) return true;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        if (!meta.getEnchants().isEmpty()) return true;
        if (meta instanceof EnchantmentStorageMeta esm) return !esm.getStoredEnchants().isEmpty();
        return false;
    }

    /** Tire un matériau aléatoire parmi le tableau fourni. */
    private static Material pickRandom(Material[] materials) {
        return materials[RANDOM.nextInt(materials.length)];
    }

    /**
     * Sélectionne un item alchimique dans la table de loot par tirage au sort pondéré.
     *
     * <p>Seules les entrées dont {@code levelMin <= level} sont éligibles.</p>
     *
     * <p><b>Effet luck :</b> un bonus de {@code luck × luckWeightBonus} est ajouté au poids de
     * <em>chaque</em> entrée éligible. Cela aplatit la distribution : les items communs
     * (gros poids de base) dominent sans luck, tandis que les items rares (petit poids de base)
     * deviennent proportionnellement bien plus probables avec une luck élevée — ils se comportent
     * ainsi comme des "trésors de potions". Exemple : Nether Wart (400) vs DEFLAGRATION (10) ;
     * avec luck=3 et bonus=80, le ratio passe de 40:1 à 2,6:1.</p>
     *
     * @param level           niveau du pêcheur (filtre les entrées par {@code level-min})
     * @param luck            luck vanilla du joueur (Luck of the Sea + amplificateur effet Luck)
     * @param luckWeightBonus bonus ajouté à chaque entrée éligible par point de luck
     * @param loot            table de loot alchimique
     * @return item sélectionné, ou {@code null} si aucune entrée éligible
     */
    private static ItemStack pickAlchemic(int level, double luck, int luckWeightBonus,
                                          List<AlchemicLootEntry> loot) {
        List<AlchemicLootEntry> eligible = loot.stream()
                .filter(e -> e.levelMin() <= level)
                .toList();
        if (eligible.isEmpty()) return null;

        // Formule quadratique : bonus = round(luck² × luckWeightBonus).
        // Convergence lente au début (Pan.II : ratio ~33:1) et progressive vers 2:1 au Maître.
        int bonus = (int) Math.round(luck * luck * luckWeightBonus);
        int totalWeight = eligible.stream().mapToInt(e -> e.weight() + bonus).sum();
        int roll = RANDOM.nextInt(totalWeight);
        int cumul = 0;
        for (AlchemicLootEntry entry : eligible) {
            cumul += entry.weight() + bonus;
            if (roll < cumul) return entry.createItem();
        }
        return eligible.getLast().createItem();
    }
}