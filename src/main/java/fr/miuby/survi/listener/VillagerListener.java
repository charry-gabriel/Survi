package fr.miuby.survi.listener;

import fr.miuby.lib.villager.MLVillager;
import fr.miuby.lib.villager.VillagerLoadedEvent;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.quest.PlayerQuestData;
import fr.miuby.survi.quest.QuestManager;
import fr.miuby.survi.system.log.LogManager;
import fr.miuby.survi.villager.AVillager;
import fr.miuby.survi.villager.trader.Trader;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import fr.miuby.survi.villager.VillagerPostLoadActions;
import fr.miuby.survi.villager.villagerlevel.blessing.BlessingEffect;
import fr.miuby.survi.villager.villagerlevel.event.VillagerLevelUpEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.MenuType;

import org.bukkit.inventory.MerchantRecipe;
import java.util.List;
import java.util.logging.Level;

public class VillagerListener implements Listener {
    @SuppressWarnings("UnstableApiUsage")
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
                    if (!level.isUnlocked())
                        player.sendMessage("§e" + villager.getName() + " §eest indisponible pendant encore " + level.getRemainingLock());
                    event.setCancelled(true);
                }
                case VillagerLevel level -> {
                    player.openInventory(level.getInventory());
                    event.setCancelled(true);
                }
                case Trader trader -> {
                    AlphaPlayer alphaPlayer = AlphaPlayer.get(player.getUniqueId());

                    // Priorité 1 : une quête complétée mais non réclamée est en attente → on la réclame
                    PlayerQuestData completedUnclaimed = alphaPlayer.getActiveQuests().stream()
                            .filter(q -> q.isCompleted() && !q.isClaimed())
                            .findFirst()
                            .orElse(null);

                    if (completedUnclaimed != null) {
                        GameManager.getInstance().getQuestManager().completeQuest(alphaPlayer, trader, false);
                        event.setCancelled(true);
                        return;
                    }

                    // Priorité 2 : proposer une nouvelle quête si le joueur a encore des slots disponibles
                    // (getCurrentActiveQuest() == null signifie qu'il n'a pas de quête en cours non réclamée)
                    boolean canAcceptNewQuest = alphaPlayer.getCurrentActiveQuest() == null
                            && alphaPlayer.countTodayQuests() < QuestManager.DAILY_QUEST_LIMIT;

                    if (canAcceptNewQuest) {
                        Component questMessage = Component.text("\n[Quête] ", NamedTextColor.GOLD)
                                .append(Component.text("Cliquez ici pour accepter une quête !", NamedTextColor.YELLOW)
                                        .clickEvent(ClickEvent.callback(audience -> GameManager.getInstance().getQuestManager().assignQuest(alphaPlayer, trader)))
                                        .hoverEvent(HoverEvent.showText(Component.text("Accepter la quête", NamedTextColor.GREEN))))
                                .append(Component.text("\n"));
                        player.sendMessage(questMessage);
                    }

                    // Update recipes based on reputation
                    int reputation = alphaPlayer.getReputation(trader.getNameId());
                    List<MerchantRecipe> recipes = trader.getRecipesForPlayer(reputation);
                    trader.getVillager().setRecipes(recipes);

                    player.openInventory(MenuType.MERCHANT.builder().merchant(trader.getVillager()).title(trader.getDisplayName()).build(player));

                    player.sendMessage(Component.text("<", NamedTextColor.AQUA).append(aVillager.getDisplayName()).append(Component.text("> ", NamedTextColor.AQUA)).append(((Trader)aVillager).getOpenMessage()));
                    event.setCancelled(true);
                }
                case null, default -> {
                    event.setCancelled(true);
                    LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.VILLAGER, "Villager interacted with an unknown entity: " + event.getRightClicked().getType());
                }
            }
        }
        else if (event.getRightClicked().getType() == EntityType.WANDERING_TRADER)
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onVillagerLoaded(VillagerLoadedEvent event) {
        MLVillager villager = event.getVillager();
        VillagerRegistry.register(villager);
        VillagerPostLoadActions.executeAndClear(villager);
    }

    @EventHandler
    public void onVillagerLevelUp(VillagerLevelUpEvent event) {
        VillagerLevel villager = event.getVillagerLevel();
        Component message = villager.getMessage();

        if (message != null && !PlainTextComponentSerializer.plainText().serialize(message).isBlank()) {
            Bukkit.broadcast(Component.text("<", NamedTextColor.AQUA)
                    .append(villager.getDisplayName())
                    .append(Component.text("> ", NamedTextColor.AQUA))
                    .append(message)
            );
        }

        Sound myCustomSound = Sound.sound(Key.key("ui.toast.challenge_complete"), Sound.Source.MASTER, 1f, 1.1f);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(myCustomSound);

            for (BlessingEffect effect : villager.getBlessing().blessingEffects()) {
                effect.applyEffect(villager, AlphaPlayer.get(p.getUniqueId()));
            }
        }
    }
}