package fr.miuby.survi.listener.job;

import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.config.JobsConfig;
import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.Material;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

/**
 * Gère les effets du métier {@link EJob#FISHERMAN} liés à la pêche :
 *
 * <ul>
 *   <li>Temps d'attente modulé par le niveau (2× plus long au niv.0, 4× plus rapide au niv.10).</li>
 *   <li>Chance de remplacer l'item pêché par une dirt (forte aux bas niveaux, 0 à partir du niv.7).</li>
 *   <li>Malus supplémentaire sur les trésors (livres enchantés, arcs, cannes…) jusqu'au niv.6.</li>
 *   <li>Multiplicateur de quantité sur les items non remplacés.</li>
 * </ul>
 *
 * <p>Les effets aquatiques passifs (pression, vitesse, respiration) sont gérés par
 * {@link fr.miuby.survi.job.task.FishermanEffectsTask}.</p>
 *
 * <p>Tous les paramètres numériques sont lus depuis {@link JobsConfig} ({@code jobs.yml}).</p>
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

        // Étape 1 : chance globale de remplacer tout item pêché par une dirt
        if (RANDOM.nextDouble() < cfg.getDirtChance()[level]) {
            caughtItem.setItemStack(new ItemStack(Material.DIRT));
            return;
        }

        // Étape 2 : malus supplémentaire si l'item est un trésor (livre enchanté, arc, canne, selle…)
        if (isTreasure(stack) && RANDOM.nextDouble() < cfg.getTreasurePenalty()[level]) {
            caughtItem.setItemStack(new ItemStack(Material.DIRT));
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
}