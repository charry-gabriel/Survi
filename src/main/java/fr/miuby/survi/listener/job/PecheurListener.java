package fr.miuby.survi.listener.job;

import fr.miuby.survi.job.EJob;
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
 * Gère les effets du métier PÊCHEUR liés à la pêche :
 *
 * <ul>
 *   <li>Temps d'attente modulé par le niveau (2× plus long au niv.0, 4× plus rapide au niv.10).</li>
 *   <li>Multiplicateur de loot sur les items pêchés (identique aux autres métiers).</li>
 * </ul>
 *
 * <p>Les effets aquatiques passifs (pression, vitesse, respiration) sont gérés par
 * {@link fr.miuby.survi.job.task.PecheurEffectsTask}.</p>
 */
public class PecheurListener implements Listener {

    private static final Random RANDOM = new Random();

    // ─── Temps d'attente (en multiplicateur du vanilla : 100-600 ticks) ──────────

    private static final double[] FISHING_WAIT_MULT = {
            2.00,  // niv. 0  — 200-1200 t (2× plus long)
            1.75,  // niv. 1
            1.50,  // niv. 2
            1.25,  // niv. 3
            1.10,  // niv. 4
            1.00,  // niv. 5  — vanilla (100-600 t)
            0.85,  // niv. 6
            0.70,  // niv. 7
            0.55,  // niv. 8
            0.40,  // niv. 9
            0.25   // niv. 10 — 25-150 t (4× plus rapide)
    };

    // ─── Multiplicateur de loot (même courbe que les autres métiers) ──────────────

    private static final double[] LOOT_MULTIPLIER = {
            0.20,  // niv. 0  — 20 % de chance d'obtenir quelque chose
            0.50,  // niv. 1
            0.80,  // niv. 2
            1.00,  // niv. 3  — vanilla
            1.10,  // niv. 4
            1.20,  // niv. 5
            1.30,  // niv. 6
            1.40,  // niv. 7
            1.50,  // niv. 8
            1.75,  // niv. 9
            2.00   // niv. 10 — double loot
    };

    /** Temps d'attente vanilla de la canne à pêche (ticks). */
    private static final int VANILLA_MIN_WAIT = 100;
    private static final int VANILLA_MAX_WAIT = 600;

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        if (alpha == null) return;
        int level = alpha.getJobLevel(EJob.PECHEUR);

        switch (event.getState()) {

            case FISHING -> applyWaitTime(event.getHook(), level);

            case CAUGHT_FISH -> applyLootMultiplier(event, level);

            default -> { /* autres états non modifiés */ }
        }
    }

    // ─── Temps d'attente ─────────────────────────────────────────────────────────

    private static void applyWaitTime(FishHook hook, int level) {
        if (hook == null) return;
        double mult = FISHING_WAIT_MULT[level];
        int min = Math.max(1, (int) Math.round(VANILLA_MIN_WAIT * mult));
        int max = Math.max(min + 1, (int) Math.round(VANILLA_MAX_WAIT * mult));
        hook.setMinWaitTime(min);
        hook.setMaxWaitTime(max);
    }

    // ─── Multiplicateur de loot ───────────────────────────────────────────────────

    private static void applyLootMultiplier(PlayerFishEvent event, int level) {
        if (!(event.getCaught() instanceof Item caughtItem)) return;
        ItemStack stack = caughtItem.getItemStack();
        double multiplier = LOOT_MULTIPLIER[level];
        double totalAmount = stack.getAmount() * multiplier;
        int amount = (int) totalAmount;
        if (RANDOM.nextDouble() < totalAmount - amount)
            amount++;

        stack.setAmount(Math.clamp(amount, 0, stack.getMaxStackSize()));
    }
}