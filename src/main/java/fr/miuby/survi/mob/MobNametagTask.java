package fr.miuby.survi.mob;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

/**
 * Tâche répétée qui gère la visibilité des noms de mobs modifiés.
 *
 * <p>La visibilité est gérée globalement (pas par joueur individuel) : si un joueur
 * voit un mob, son nom devient visible pour tous les joueurs proches. Pour une
 * gestion individuelle il faudrait manipuler des paquets (ProtocolLib).</p>
 */
public class MobNametagTask extends BukkitRunnable {

    /** Fréquence de rafraîchissement (5 ticks ≈ 250 ms). */
    public static final long PERIOD_TICKS = 5L;

    /** Mobs dont le nom est visible au tick courant ; purgé à chaque tick. */
    private final Set<LivingEntity> currentlyVisible = new HashSet<>();

    @Override
    public void run() {
        for (LivingEntity mob : currentlyVisible) {
            if (mob.isValid()) mob.setCustomNameVisible(false);
        }
        currentlyVisible.clear();

        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 40, 40, 40)) {

                if (!(entity instanceof LivingEntity mob)
                        || mob.customName() == null
                        || currentlyVisible.contains(mob)
                        || !player.hasLineOfSight(mob))
                    continue;

                mob.setCustomNameVisible(true);
                currentlyVisible.add(mob);
            }
        }
    }
}
