package fr.miuby.survi.listener;

import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.role.ERole;
import fr.miuby.survi.system.perf.PerfTimer;
import fr.miuby.survi.world.EWorld;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

import static java.lang.Math.min;
import static java.lang.Math.round;

public class DamageListener implements Listener {
    Sound slimeSound = Sound.sound(Key.key("entity.slime.attack"), Sound.Source.MASTER, 1f, 1.1f);

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

                    // ⚠ PERF — getAlphaPlayers() interdit dans les hot paths (AI_DEV §11).
                    // Si le timer FEE-propagation apparaît en prod, remplacer par un
                    // Set<UUID> de joueurs FÉE maintenu via un AlphaPlayerRoleChangeEvent.
                    try (var fee = PerfTimer.start("DamageListener.FEE-propagation")) {
                        for (AlphaPlayer other : GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers()) {
                            if (other.getPlayer() != null
                                    && other.getRole().type() == ERole.FEE
                                    && !other.getUuid().equals(damagedAlphaPlayer.getUuid())) {
                                other.setTakingNoDamage(true);
                                other.getPlayer().damage(damage);
                                other.getPlayer().playSound(slimeSound);
                            }
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