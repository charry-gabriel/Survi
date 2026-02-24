package fr.miuby.survi.listener;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.role.RoleAttribute;
import fr.miuby.survi.villager.AVillager;
import fr.miuby.survi.villager.Trader;
import fr.miuby.survi.villager.VillagerLevel;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.quest.QuestManager;
import io.papermc.paper.advancement.AdvancementDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
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
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        if (event.getRightClicked().getType() == EntityType.VILLAGER)
        {
            Villager villager = (Villager) event.getRightClicked();
            AVillager aVillager = (AVillager) VillagerRegistry.get(villager.getUniqueId());

            switch (aVillager) {
                case VillagerLevel level when level.getTribute() == null || !level.isUnlocked() -> {
                    player.sendMessage(Component.text("<", NamedTextColor.AQUA).append(level.getDisplayName()).append(Component.text("> ", NamedTextColor.AQUA)).append(level.getMessage()));
                    event.setCancelled(true);
                }
                case VillagerLevel level -> {
                    player.openInventory(level.getInventory());
                    event.setCancelled(true);
                }
                case Trader trader -> {
                    AlphaPlayer alphaPlayer = AlphaPlayer.get(player.getUniqueId());

                    if (alphaPlayer.getActiveQuest() != null && alphaPlayer.getActiveQuest().isCompleted() && !alphaPlayer.getActiveQuest().isClaimed()) {
                        QuestManager.getInstance().completeQuest(alphaPlayer, trader);
                        event.setCancelled(true);
                        return;
                    }

                    // On ne propose la quête que si le joueur n'en a pas déjà une aujourd'hui
                    boolean hasQuestToday = false;
                    if (alphaPlayer.getActiveQuest() != null) {
                        java.time.LocalDate lastAccepted = alphaPlayer.getActiveQuest().getLastAccepted();
                        if (lastAccepted != null && lastAccepted.isEqual(java.time.LocalDate.now())) {
                            hasQuestToday = true;
                        }
                    }

                    if (!hasQuestToday) {
                        Component questMessage = Component.text("\n[Quête] ", NamedTextColor.GOLD)
                                .append(Component.text("Cliquez ici pour accepter la quête du jour !", NamedTextColor.YELLOW)
                                        .clickEvent(ClickEvent.runCommand("/quest accept \"" + trader.getNameId() + "\""))
                                        .hoverEvent(HoverEvent.showText(Component.text("Accepter la quête", NamedTextColor.GREEN))))
                                .append(Component.text("\n"));
                        player.sendMessage(questMessage);
                    }

                    // Update recipes based on reputation
                    int reputation = alphaPlayer.getReputation(trader.getNameId());
                    trader.getVillager().setRecipes(trader.getRecipesForPlayer(reputation));

                    player.openMerchant(trader.getVillager(), true);
                    player.sendMessage(Component.text("<", NamedTextColor.AQUA).append(aVillager.getDisplayName()).append(Component.text("> ", NamedTextColor.AQUA)).append(((Trader)aVillager).getOpenMessage()));
                    event.setCancelled(true);
                }
                case null, default -> {
                }
            }
        }
        else if (event.getRightClicked().getType() == EntityType.WANDERING_TRADER)
        {
            event.setCancelled(true);
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