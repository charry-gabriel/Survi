package fr.miuby.survi.listener;

import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.role.ERole;
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
    Sound slimeSound = Sound.sound(Key.key("entity.slime.attack"), Sound.Source.AMBIENT, 1f, 1.1f);

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        //si on tape
        if(event.getDamager().getType() == EntityType.PLAYER) {
            double damage = event.getDamage();
            UUID uuid = event.getDamager().getUniqueId();
            AlphaPlayer alphaPlayer =  AlphaPlayer.get(uuid);

            if(WorldRegistry.get(EWorld.END).isPlayerInWorld(alphaPlayer.getPlayer())) {
                event.setDamage(damage * alphaPlayer.getDamageModifier() * alphaPlayer.getEndDamageModifier());
            } else {
                event.setDamage(damage * alphaPlayer.getDamageModifier());
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntityType() == EntityType.VILLAGER) {
            Villager villager = (Villager) event.getEntity();

            if (VillagerRegistry.get(villager.getUniqueId()) != null)
                event.setCancelled(true);
        }
        else if(event.getEntityType() == EntityType.PLAYER) {
            AlphaPlayer damagedAlphaPlayer = AlphaPlayer.get(event.getEntity().getUniqueId());
            double damage = event.getDamage();
            double modifiedDamage;

            if(WorldRegistry.get(EWorld.END).isPlayerInWorld(damagedAlphaPlayer.getPlayer())) {
                modifiedDamage = round(damage / (damagedAlphaPlayer.getResistanceModifier() * damagedAlphaPlayer.getEndResistanceModifier()));
            } else {
                modifiedDamage = round(damage / damagedAlphaPlayer.getResistanceModifier());
            }

            if (damagedAlphaPlayer.getRole().type() == ERole.FEE) {
                if (!damagedAlphaPlayer.isTakingNoDamage()) {
                    for (AlphaPlayer otherPlayer : GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers().values()) {
                        if (otherPlayer.getPlayer() != null && otherPlayer.getRole().type() == ERole.FEE) {
                            if (!otherPlayer.getUuid().equals(damagedAlphaPlayer.getUuid())) {
                                otherPlayer.setTakingNoDamage(true);
                                otherPlayer.getPlayer().damage(damage);
                                otherPlayer.getPlayer().playSound(slimeSound);
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
