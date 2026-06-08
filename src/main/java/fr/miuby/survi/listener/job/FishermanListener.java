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

import java.util.Random;

/**
 * Gère les effets du métier {@link EJob#FISHERMAN} liés à la pêche :
 *
 * <ul>
 *   <li>Temps d'attente modulé par le niveau (2× plus long au niv.0, 4× plus rapide au niv.10).</li>
 *   <li>Multiplicateur de loot sur les items pêchés.</li>
 * </ul>
 *
 * <p>Les effets aquatiques passifs (pression, vitesse, respiration) sont gérés par
 * {@link fr.miuby.survi.job.task.FishermanEffectsTask}.</p>
 *
 * <p>Tous les paramètres numériques sont lus depuis {@link JobsConfig} ({@code jobs.yml}).</p>
 */
public class FishermanListener implements Listener {

    private static final Random RANDOM = new Random();

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        if (alpha == null) return;
        int level = alpha.getJobLevel(EJob.FISHERMAN);

        switch (event.getState()) {
            case FISHING     -> applyWaitTime(event.getHook(), level);
            case CAUGHT_FISH -> applyLootMultiplier(event, level);
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

    // ─── Multiplicateur de loot ───────────────────────────────────────────────────

    private static void applyLootMultiplier(PlayerFishEvent event, int level) {
        if (!(event.getCaught() instanceof Item caughtItem)) return;
        ItemStack stack = caughtItem.getItemStack();
        double multiplier = JobsConfig.getInstance().getFisherman().getLootMultiplier()[level];
        double totalAmount = stack.getAmount() * multiplier;
        int amount = (int) totalAmount;
        if (RANDOM.nextDouble() < totalAmount - amount)
            amount++;

        stack.setAmount(Math.clamp(amount, 0, stack.getMaxStackSize()));
    }
}
