package fr.miuby.survi.listener;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.role.RoleAttribute;
import fr.miuby.survi.world.EWorld;
import io.papermc.paper.advancement.AdvancementDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import fr.miuby.survi.quest.PlayerQuestData;
import fr.miuby.survi.quest.Quest;
import fr.miuby.survi.quest.QuestManager;

import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if ((WorldRegistry.get(EWorld.VILLAGE).isPlayerOutOfLimit(event.getPlayer()) || WorldRegistry.get(EWorld.WILDERNESS).isPlayerOutOfLimit(event.getPlayer())) && !event.getPlayer().isOp()) {
            AlphaPlayer.get(event.getPlayer().getUniqueId()).teleport(WorldRegistry.get(EWorld.VILLAGE));
            event.getPlayer().sendMessage(Component.text("Ne sort pas des limite du village, c'est dangereux !!", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getPlayer().getWorld().getName().equals(WorldRegistry.get(EWorld.VILLAGE).getName())) {
            event.getPlayer().sendMessage(Component.text("C'est pas autorisé ça !", NamedTextColor.RED));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        Advancement advancement = event.getAdvancement();
        AdvancementProgress advancementProgress = player.getAdvancementProgress(advancement);
        AdvancementDisplay advancementDisplay = advancement.getDisplay();

        String category = advancement.getKey().getKey().split("/")[0];
        if (advancementProgress.isDone() && advancementDisplay != null && !category.equals("recipes")) {
            if (advancementDisplay.frame() == AdvancementDisplay.Frame.CHALLENGE) {
                AlphaPlayer.get(player.getUniqueId()).gainOneSuccess(true);
            }
        }
    }

    @EventHandler
    public void onPlayerArmorChange(PlayerArmorChangeEvent event) {
        boolean malus = false;
        for (ItemStack item : event.getPlayer().getInventory().getArmorContents()) {
            if (item != null && GameManager.getInstance().getLockedItemsFactory().isLocked(item.getType().getKey())) {
                malus = true;
            }
        }
        AlphaPlayer.get(event.getPlayer().getUniqueId()).getAlphaLife().setArmorMalus(malus);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        final Player player = event.getEntity();

        final ItemStack[] armor = player.getInventory().getArmorContents();
        GameManager.getInstance().getScheduler().scheduleSyncDelayedTask(GameManager.getInstance().getPlugin(), () -> player.getInventory().setArmorContents(armor));

        for (ItemStack is : armor) {
            event.getDrops().remove(is);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        AlphaPlayer alphaPlayer = AlphaPlayer.get(player.getUniqueId());
        alphaPlayer.getAlphaLife().actualizeDeath();

        for (RoleAttribute roleAttribute : alphaPlayer.getRole().attributes()) {
            if (roleAttribute.getAttributeType() == Attribute.MAX_ABSORPTION) {
                player.removePotionEffect(PotionEffectType.ABSORPTION);
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 0, (int) roleAttribute.getValue()));
            }
        }

        // Réapplique les buffs de la quête active si elle est claim (récompense reçue)
        // Les buffs durent jusqu'au reset de 6h même après la mort
        PlayerQuestData questData = alphaPlayer.getActiveQuest();
        if (questData != null && questData.isClaimed()) {
            Quest quest = QuestManager.getInstance().getQuest(questData.getQuestId());
            if (quest != null) {
                // Petit délai pour éviter les conflits avec les PotionEffects par défaut au respawn
                GameManager.getInstance().getScheduler().runTaskLater(GameManager.getInstance().getPlugin(), () -> {
                    if (player.isOnline()) {
                        for (PotionEffect effect : quest.getRewards()) {
                            player.addPotionEffect(effect);
                        }
                    }
                }, 5L);
            }
        }
    }

    @EventHandler
    public void onPlayerRecipeDiscover(PlayerRecipeDiscoverEvent event) {
        for (NamespacedKey nsKey : GameManager.getInstance().getCustomRecipeFactory().getOldRecipes()) {
            if (nsKey.toString().equals(event.getRecipe().toString()))
                event.setCancelled(true);
        }

        if (GameManager.getInstance().getLockedItemsFactory().isLocked(event.getRecipe()))
            event.setCancelled(true);
    }

    /*@EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult().equals(PlayerBedEnterEvent.BedEnterResult.OK)) {
            event.getPlayer().chat("zzz");
        }
    }*/
}