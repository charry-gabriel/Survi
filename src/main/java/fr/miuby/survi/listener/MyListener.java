package fr.miuby.survi.listener;

import fr.miuby.survi.*;
import fr.miuby.survi.role.ERole;
import fr.miuby.survi.villager.AVillager;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.Monde;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.UUID;

import static java.lang.Math.round;

public class MyListener implements Listener {
    static Survi plugin;

    public MyListener(Survi instance) {
        plugin = instance;
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event){
        if(event.getInventory().getResult() != null) {
            if (GameManager.getInstance().getLockedItemsManager().isLocked(event.getInventory().getResult())) {
                ItemStack air = new ItemStack(Material.AIR);
                event.getInventory().setResult(air);
            }
        }

        ItemStack[] matrix = event.getInventory().getMatrix();
        if((matrix[3] != null && matrix[3].getType() == Material.LAVA_BUCKET
                && matrix[5] != null && matrix[5].getType() == Material.LAVA_BUCKET
                && matrix[6] != null && matrix[6].getType() == Material.LAVA_BUCKET
                && matrix[8] != null && matrix[8].getType() == Material.LAVA_BUCKET
                && (matrix[0] == null || matrix[0].getType() == Material.AIR)
                && (matrix[1] == null || matrix[1].getType() == Material.AIR)
                && (matrix[2] == null || matrix[2].getType() == Material.AIR)
                && (matrix[4] == null || matrix[4].getType() == Material.AIR)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR))
                || (matrix[0] != null && matrix[0].getType() == Material.LAVA_BUCKET
                && matrix[2] != null && matrix[2].getType() == Material.LAVA_BUCKET
                && matrix[3] != null && matrix[3].getType() == Material.LAVA_BUCKET
                && matrix[5] != null && matrix[5].getType() == Material.LAVA_BUCKET)
                && (matrix[1] == null || matrix[1].getType() == Material.AIR)
                && (matrix[4] == null || matrix[4].getType() == Material.AIR)
                && (matrix[6] == null || matrix[6].getType() == Material.AIR)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR)
                && (matrix[8] == null || matrix[8].getType() == Material.AIR))
            event.getInventory().setResult(new ItemStack(Material.CHAINMAIL_BOOTS));

        if((matrix[3] != null && matrix[3].getType() == Material.LAVA_BUCKET
                && matrix[4] != null && matrix[4].getType() == Material.LAVA_BUCKET
                && matrix[5] != null && matrix[5].getType() == Material.LAVA_BUCKET
                && matrix[6] != null && matrix[6].getType() == Material.LAVA_BUCKET
                && matrix[8] != null && matrix[8].getType() == Material.LAVA_BUCKET
                && (matrix[0] == null || matrix[0].getType() == Material.AIR)
                && (matrix[1] == null || matrix[1].getType() == Material.AIR)
                && (matrix[2] == null || matrix[2].getType() == Material.AIR)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR))
                || (matrix[0] != null && matrix[0].getType() == Material.LAVA_BUCKET
                && matrix[1] != null && matrix[1].getType() == Material.LAVA_BUCKET
                && matrix[2] != null && matrix[2].getType() == Material.LAVA_BUCKET
                && matrix[3] != null && matrix[3].getType() == Material.LAVA_BUCKET
                && matrix[5] != null && matrix[5].getType() == Material.LAVA_BUCKET)
                && (matrix[4] == null || matrix[4].getType() == Material.AIR)
                && (matrix[6] == null || matrix[6].getType() == Material.AIR)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR)
                && (matrix[8] == null || matrix[8].getType() == Material.AIR))
            event.getInventory().setResult(new ItemStack(Material.CHAINMAIL_HELMET));

        if(matrix[0] != null && matrix[0].getType() == Material.LAVA_BUCKET
                && matrix[2] != null && matrix[2].getType() == Material.LAVA_BUCKET
                && matrix[3] != null && matrix[3].getType() == Material.LAVA_BUCKET
                && matrix[4] != null && matrix[4].getType() == Material.LAVA_BUCKET
                && matrix[5] != null && matrix[5].getType() == Material.LAVA_BUCKET
                && matrix[6] != null && matrix[6].getType() == Material.LAVA_BUCKET
                && matrix[7] != null && matrix[7].getType() == Material.LAVA_BUCKET
                && matrix[8] != null && matrix[8].getType() == Material.LAVA_BUCKET
                && (matrix[1] == null || matrix[1].getType() == Material.AIR))
            event.getInventory().setResult(new ItemStack(Material.CHAINMAIL_CHESTPLATE));

        if(matrix[0] != null && matrix[0].getType() == Material.LAVA_BUCKET
                && matrix[1] != null && matrix[1].getType() == Material.LAVA_BUCKET
                && matrix[2] != null && matrix[2].getType() == Material.LAVA_BUCKET
                && matrix[3] != null && matrix[3].getType() == Material.LAVA_BUCKET
                && matrix[5] != null && matrix[5].getType() == Material.LAVA_BUCKET
                && matrix[6] != null && matrix[6].getType() == Material.LAVA_BUCKET
                && matrix[8] != null && matrix[8].getType() == Material.LAVA_BUCKET
                && (matrix[4] == null || matrix[4].getType() == Material.AIR)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR))
            event.getInventory().setResult(new ItemStack(Material.CHAINMAIL_LEGGINGS));
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        //si on tape
        if(event.getDamager().getType() == EntityType.PLAYER) {
            double damage = event.getDamage();
            UUID uuid = event.getDamager().getUniqueId();
            AlphaPlayer alphaPlayer =  AlphaPlayer.get(uuid);

            if(Monde.isPlayerOnWorld(alphaPlayer.getPlayer(), EWorld.END) || Monde.isPlayerOnWorld(alphaPlayer.getPlayer(), EWorld.END2)) {
                event.setDamage(damage * alphaPlayer.getDamage() * alphaPlayer.getEndDamage());
            } else {
                event.setDamage(damage * alphaPlayer.getDamage());
            }
        }
    }
    boolean first = true;

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntityType() == EntityType.VILLAGER) {
            Villager villager = (Villager) event.getEntity();
            if (AVillager.contains(villager.getUniqueId())) {
                event.setCancelled(true);
            }
        }

        if(event.getEntityType() == EntityType.PLAYER) {
            AlphaPlayer damagedAlphaPlayer = AlphaPlayer.get(event.getEntity().getUniqueId());
            double damage = event.getDamage();
            double modifiedDamage;

            if(Monde.isPlayerOnWorld(damagedAlphaPlayer.getPlayer(), EWorld.END) || Monde.isPlayerOnWorld(damagedAlphaPlayer.getPlayer(), EWorld.END2)) {
                modifiedDamage = damage / (damagedAlphaPlayer.getResistance() * damagedAlphaPlayer.getEndResistance());
            } else {
                modifiedDamage = damage / damagedAlphaPlayer.getResistance();
            }

            if (damagedAlphaPlayer.getRole().getType() == ERole.COUPLE) {
                if (first) {
                    first = false;
                    for (AlphaPlayer otherPlayer : GameManager.getInstance().getAlphaPlayers().values()) {
                        if (otherPlayer.getPlayer() != null && otherPlayer.getRole().getType() == ERole.COUPLE) {
                            if (!otherPlayer.getUUID().equals(damagedAlphaPlayer.getUUID())) {
                                otherPlayer.getPlayer().damage(damage, damagedAlphaPlayer.getPlayer());
                                otherPlayer.getPlayer().sendMessage(Component.text(damagedAlphaPlayer.getPseudo() + " a pris des dégats"));
                            }
                        }
                    }
                }
                first = true;
            }
            event.setDamage(round(modifiedDamage));
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event){
        if (event.getEntity() instanceof EnderDragon) {
            EnderDragon dragon = (EnderDragon) event.getEntity();
            try {
                Objects.requireNonNull(dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(2000);
                dragon.setHealth(2000);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player)event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof Player && event.getInventory().getHolder() instanceof Villager) {
            if (item != null && item.getType() != Material.AIR) {
                Villager v = (Villager) event.getInventory().getHolder();
                AVillager villager = AVillager.get(v.getUniqueId());

                villager.giveItems(event.getInventory(), item, player);
            }

            event.setCancelled(true);
        } else if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof Villager) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        if (event.getReason().equals(PortalCreateEvent.CreateReason.FIRE)) {
            if (!GameManager.getInstance().hasNetherAccess())
                event.setCancelled(true);
        } else {
            if (!GameManager.getInstance().hasEndAccess())
                event.setCancelled(true);
        }
    }
}
