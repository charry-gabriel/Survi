package fr.miuby.survi.listener;

import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.player.event.AlphaPlayerRoleChangeEvent;
import fr.miuby.survi.role.ERole;
import fr.miuby.survi.role.Role;
import fr.miuby.survi.system.perf.PerfTimer;
import fr.miuby.survi.world.EWorld;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static java.lang.Math.min;
import static java.lang.Math.round;

public class DamageListener implements Listener {
    private final Sound slimeSound = Sound.sound(Key.key("entity.slime.attack"), Sound.Source.MASTER, 1f, 1.1f);

    /**
     * UUID des joueurs FÉE connectés.
     * Mis à jour sur les events de connexion, déconnexion et changement de rôle,
     * ce qui évite de scanner {@code getAlphaPlayers()} dans le hot path de {@link EntityDamageEvent}.
     */
    private final Set<UUID> feePlayerUuids = new HashSet<>();

    // ─── Maintenance du set FÉE ──────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (AlphaPlayer.get(uuid).getRole().type() == ERole.FEE) feePlayerUuids.add(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        feePlayerUuids.remove(event.getPlayer().getUniqueId());
    }

    /** Met à jour le set quand un joueur change de rôle (MONITOR, après annulation éventuelle). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAlphaPlayerRoleChange(AlphaPlayerRoleChangeEvent event) {
        UUID uuid    = event.getAlphaPlayer().getUuid();
        Role newRole = event.getNewRole();
        Role oldRole = event.getOldRole();
        if (newRole != null && newRole.type() == ERole.FEE) {
            feePlayerUuids.add(uuid);
        } else if (oldRole != null && oldRole.type() == ERole.FEE) {
            feePlayerUuids.remove(uuid);
        }
    }

    // ─── Events de dégâts ────────────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager().getType() != EntityType.PLAYER) return;

        try (var t = PerfTimer.start("DamageListener.onEntityDamageByEntity")) {
            double damage = event.getDamage();
            UUID uuid = event.getDamager().getUniqueId();
            AlphaPlayer alphaPlayer = AlphaPlayer.get(uuid);

            if (WorldRegistry.get(EWorld.END).isPlayerInWorld(alphaPlayer.getPlayer())) {
                event.setDamage(damage * alphaPlayer.getDamageModifier() * alphaPlayer.getEndDamageModifier());
            } else {
                event.setDamage(damage * alphaPlayer.getDamageModifier());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {

        if (event.getEntityType() == EntityType.VILLAGER) {
            Villager villager = (Villager) event.getEntity();
            if (VillagerRegistry.get(villager.getUniqueId()) != null)
                event.setCancelled(true);
            return;
        }

        if (event.getEntityType() != EntityType.PLAYER) return;

        try (var t = PerfTimer.start("DamageListener.onEntityDamage")) {
            AlphaPlayer damagedAlphaPlayer = AlphaPlayer.get(event.getEntity().getUniqueId());
            double damage = event.getDamage();
            double modifiedDamage;

            if (WorldRegistry.get(EWorld.END).isPlayerInWorld(damagedAlphaPlayer.getPlayer())) {
                modifiedDamage = round(damage / (damagedAlphaPlayer.getResistanceModifier() * damagedAlphaPlayer.getEndResistanceModifier()));
            } else {
                modifiedDamage = round(damage / damagedAlphaPlayer.getResistanceModifier());
            }

            if (damagedAlphaPlayer.getRole().type() == ERole.FEE) {
                if (!damagedAlphaPlayer.isTakingNoDamage()) {

                    try (var fee = PerfTimer.start("DamageListener.FEE-propagation")) {
                        UUID damagedUuid = damagedAlphaPlayer.getUuid();
                        for (UUID feeUuid : feePlayerUuids) {
                            if (feeUuid.equals(damagedUuid)) continue;
                            AlphaPlayer other = AlphaPlayer.get(feeUuid);
                            if (other.getPlayer() == null) continue;
                            other.setTakingNoDamage(true);
                            other.getPlayer().damage(damage);
                            other.getPlayer().playSound(slimeSound);
                        }
                    }

                } else {
                    modifiedDamage = modifiedDamage / 4;
                    modifiedDamage = min(modifiedDamage, damagedAlphaPlayer.getPlayer().getHealth() - 4);
                    damagedAlphaPlayer.setTakingNoDamage(false);
                }
            }

            event.setDamage(modifiedDamage);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        AlphaPlayer.get(event.getPlayer().getUniqueId()).addMort(1);
    }
}
