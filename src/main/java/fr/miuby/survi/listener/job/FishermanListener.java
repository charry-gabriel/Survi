package fr.miuby.survi.listener.job;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.alchemic.AlchemicLootEntry;
import fr.miuby.survi.job.config.JobsConfig;
import fr.miuby.survi.player.AlphaPlayer;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
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

/**
 * Gère les effets du métier {@link EJob#FISHERMAN} liés à la pêche :
 *
 * <ul>
 *   <li>Temps d'attente modulé par le niveau (2× plus long au niv.0, 4× plus rapide au niv.10).</li>
 *   <li>Chance de remplacer l'item pêché par un matériau de {@code dirt-replacement-materials}
 *       (forte aux bas niveaux, 0 à partir du niv.7).</li>
 *   <li>Malus supplémentaire sur les trésors (livres enchantés, arcs, cannes…) jusqu'au niv.6 ;
 *       remplacement par {@code treasure-replacement-materials}.</li>
 *   <li>Multiplicateur de quantité sur les items non remplacés.</li>
 * </ul>
 *
 * <p>Les effets aquatiques passifs (pression, vitesse, respiration) sont gérés par
 * {@link fr.miuby.survi.job.task.FishermanEffectsTask} et {@link fr.miuby.survi.job.FishermanAttributeService} ;
 * cette classe se contente de réagir instantanément à un changement de jambières
 * ({@code onLeggingsChanged}) pour ne pas attendre le cycle de 3s de la tâche périodique.</p>
 *
 * <p>Tous les paramètres numériques et les listes de matériaux sont lus depuis
 * {@link JobsConfig} ({@code jobs/fisherman.yml}).</p>
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

        // Étape 0 : chance de pêche alchimique — remplace l'item normal
        if (RANDOM.nextDouble() < cfg.getAlchemicCatchChance()[level]) {
            ItemStack alchemicItem = pickAlchemic(level, cfg.getAlchemicLoot());
            if (alchemicItem != null) {
                caughtItem.setItemStack(alchemicItem);
                event.getPlayer().sendActionBar(Component.text(
                        "✦ Votre ligne a ramené quelque chose d'alchimique…", NamedTextColor.AQUA));
                return;
            }
        }

        // Étape 1 : chance globale de remplacer tout item pêché par un matériau de la liste
        if (RANDOM.nextDouble() < cfg.getDirtChance()[level]) {
            caughtItem.setItemStack(new ItemStack(pickRandom(cfg.getDirtReplacementMaterials())));
            return;
        }

        // Étape 2 : malus supplémentaire si l'item est un trésor (livre enchanté, arc, canne, selle…)
        if (isTreasure(stack) && RANDOM.nextDouble() < cfg.getTreasurePenalty()[level]) {
            caughtItem.setItemStack(new ItemStack(pickRandom(cfg.getTreasureReplacementMaterials())));
            return;
        }

        // Étape 3 : multiplicateur de quantité sur l'item normal
        double multiplier = cfg.getLootMultiplier()[level];
        double totalAmount = stack.getAmount() * multiplier;
        int amount = (int) totalAmount;
        if (RANDOM.nextDouble() < totalAmount - amount)
            amount++;
        stack.setAmount(Math.clamp(amount, 0, stack.getMaxStackSize()));
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
     * Sélectionne un item alchimique dans la table de loot par tirage au sort pondéré,
     * en ne considérant que les entrées dont {@code levelMin <= level}.
     * Retourne {@code null} si la table est vide ou aucune entrée éligible.
     */
    private static ItemStack pickAlchemic(int level, List<AlchemicLootEntry> loot) {
        List<AlchemicLootEntry> eligible = loot.stream()
                .filter(e -> e.levelMin() <= level)
                .toList();
        if (eligible.isEmpty()) return null;

        int totalWeight = eligible.stream().mapToInt(AlchemicLootEntry::weight).sum();
        int roll = RANDOM.nextInt(totalWeight);
        int cumul = 0;
        for (AlchemicLootEntry entry : eligible) {
            cumul += entry.weight();
            if (roll < cumul) return entry.createItem();
        }
        return eligible.getLast().createItem();
    }
}