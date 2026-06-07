package fr.miuby.survi.listener;

import fr.miuby.lib.MiubyLib;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gère la réduction (ou amplification) des dégâts de chute selon le niveau AVENTURIER.
 *
 * <h3>Seuil de sécurité par niveau :</h3>
 * <pre>
 *  niv.0 = 0 bloc  → tout bloc de chute fait des dégâts
 *  niv.1 = 1 bloc  → dégâts à partir de 2 blocs
 *  niv.2 = 2 blocs → dégâts à partir de 3 blocs
 *  niv.3 = 3 blocs → dégâts à partir de 4 blocs (vanilla)
 *  niv.4 = 4 blocs → dégâts à partir de 5 blocs
 *  …
 *  niv.10= 10 blocs→ dégâts à partir de 11 blocs
 * </pre>
 *
 * <p>La formule est : {@code dégâts = max(0, distanceChute - niveau)}</p>
 *
 * <p>Deux handlers couvrent tous les cas :</p>
 * <ul>
 *   <li>{@link #onFallDamage} — chutes > 3 blocs (EntityDamageEvent vanilla).</li>
 *   <li>{@link #trackSmallFall} — chutes ≤ 3 blocs depuis une corniche (niv. 0-2 seulement).</li>
 * </ul>
 */
public class AventurierListener implements Listener {

    // ─── État interne ─────────────────────────────────────────────────────────────

    /** Joueurs dont la chute a déjà été traitée ce tick (via EntityDamageEvent). */
    private final Set<UUID> fallHandledThisTick = Collections.synchronizedSet(new HashSet<>());

    /** Y où le joueur a quitté le sol pour la dernière fois (pour les petites chutes). */
    private final Map<UUID, Double> leftGroundY = new HashMap<>();

    // ════════════════════════════════════════════════════════════════════════════
    //  Chutes grandes (> 3 blocs) — EntityDamageEvent
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;

        AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        if (alpha == null) return;
        int level = alpha.getJobLevel(EJob.AVENTURIER);

        // Marquer : la chute sera traitée ici (évite double-dégâts avec trackSmallFall)
        fallHandledThisTick.add(player.getUniqueId());
        MiubyLib.runLater(() -> fallHandledThisTick.remove(player.getUniqueId()), 1L);

        float fallDist = player.getFallDistance();
        double newDmg = Math.max(0.0, fallDist - level);

        // Feather Falling : réduction de 12 % par niveau d'enchantement
        int featherLevel = getFeatherFallingLevel(player);
        if (featherLevel > 0) newDmg *= Math.max(0, 1.0 - featherLevel * 0.12);

        if (newDmg <= 0) {
            event.setCancelled(true);
        } else {
            event.setDamage(newDmg);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Petites chutes (≤ 3 blocs depuis une corniche) — PlayerMoveEvent
    //  Uniquement pour niv. 0-2 (en dessous du seuil vanilla)
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR)
    public void trackSmallFall(PlayerMoveEvent event) {
        if (!event.hasChangedPosition()) return;
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        boolean onGround = player.isOnGround();

        if (!onGround) {
            // Mémoriser le Y de décollage (première fois seulement)
            leftGroundY.putIfAbsent(id, event.getFrom().getY());
        } else {
            Double startY = leftGroundY.remove(id);
            if (startY == null) return;

            // Si EntityDamageEvent a déjà géré la chute ce tick, ne pas doubler
            if (fallHandledThisTick.contains(id)) return;

            double landY = event.getTo().getY();
            double fallDist = startY - landY;

            // Seulement les petites chutes (≤ 3,5 blocs depuis la corniche)
            // Les grandes chutes sont gérées par EntityDamageEvent
            if (fallDist <= 0 || fallDist > 3.5) return;

            AlphaPlayer alpha = AlphaPlayer.get(id);
            if (alpha == null) return;
            int level = alpha.getJobLevel(EJob.AVENTURIER);

            // Niv. 3+ = seuil vanilla ou mieux → pas de dégâts pour les petites chutes
            if (level >= 3) return;

            double smallFallDmg = Math.max(0, fallDist - level);
            if (smallFallDmg > 0) {
                int featherLevel = getFeatherFallingLevel(player);
                if (featherLevel > 0) smallFallDmg *= Math.max(0, 1.0 - featherLevel * 0.12);
                if (smallFallDmg > 0) player.damage(smallFallDmg);
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Nettoyage
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        leftGroundY.remove(id);
        fallHandledThisTick.remove(id);
    }

    // ─── Utilitaire ───────────────────────────────────────────────────────────────

    private static int getFeatherFallingLevel(Player player) {
        ItemStack boots = player.getInventory().getBoots();
        return (boots != null) ? boots.getEnchantmentLevel(Enchantment.FEATHER_FALLING) : 0;
    }
}