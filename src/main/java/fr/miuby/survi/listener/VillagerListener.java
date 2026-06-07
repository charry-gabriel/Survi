package fr.miuby.survi.listener;

import fr.miuby.lib.villager.MLVillager;
import fr.miuby.lib.villager.VillagerLoadedEvent;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.quest.quest.PlayerQuestData;
import fr.miuby.survi.quest.quest.QuestGlowService;
import fr.miuby.survi.quest.quest.QuestManager;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.sound.ESound;
import fr.miuby.survi.sound.SoundService;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.villager.AVillager;
import fr.miuby.survi.villager.trader.Trader;
import fr.miuby.survi.villager.trader.TraderMenuHolder;
import fr.miuby.survi.villager.trader.TraderMenuService;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import fr.miuby.survi.villager.VillagerPostLoadActions;
import fr.miuby.survi.blessing.BlessingEffect;
import fr.miuby.survi.villager.villagerlevel.event.VillagerLevelUpEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.MerchantRecipe;

import java.util.List;
import java.util.logging.Level;

public class VillagerListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        if (event.getRightClicked().getType() == EntityType.VILLAGER) {
            Villager villager = (Villager) event.getRightClicked();
            AVillager aVillager = (AVillager) VillagerRegistry.get(villager.getUniqueId());

            switch (aVillager) {
                case VillagerLevel level when level.getTribute() == null || !level.isUnlocked() -> {
                    player.sendMessage(Component.text("<", NamedTextColor.AQUA).append(level.getDisplayName()).append(Component.text("> ", NamedTextColor.AQUA)).append(level.getMessage()));
                    if (!level.isUnlocked())
                        player.sendMessage(Component.text(villager.getName() + " est indisponible pendant encore " + level.getRemainingLock(), NamedTextColor.YELLOW));
                    event.setCancelled(true);
                }
                case VillagerLevel level -> {
                    player.openInventory(level.getInventory());
                    event.setCancelled(true);
                }
                case Trader trader -> {
                    AlphaPlayer alphaPlayer = AlphaPlayer.get(player.getUniqueId());
                    MLLogManager.getInstance().log(Level.FINE, ELogTag.VILLAGER, "[VillagerInteract] " + player.getName() + " → Trader " + trader.getNameId());
                    TraderMenuService.openMenu(player, trader, alphaPlayer);
                    event.setCancelled(true);
                }
                case null, default -> {
                    event.setCancelled(true);
                    MLLogManager.getInstance().log(Level.SEVERE, ELogTag.VILLAGER, "Villager interacted with an unknown entity: " + event.getRightClicked().getType());
                }
            }
        } else if (event.getRightClicked().getType() == EntityType.WANDERING_TRADER) {
            event.setCancelled(true);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler(ignoreCancelled = true)
    public void onTraderMenuClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TraderMenuHolder holder)) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        AlphaPlayer alphaPlayer = AlphaPlayer.get(player.getUniqueId());
        Trader trader = holder.getTrader();
        QuestManager qm = GameManager.getInstance().getQuestManager();

        if (slot == TraderMenuService.SLOT_ACCEPT_QUEST) {
            PlayerQuestData current = alphaPlayer.getCurrentActiveQuest();
            boolean isClaimableHere = current != null && current.isCompleted() && !current.isClaimed()
                    && (current.getTraderId() == null || current.getTraderId().equals(trader.getNameId()));
            int difficulty = qm.computeDifficulty(alphaPlayer, trader);
            boolean canAccept = current == null
                    && alphaPlayer.countTodayQuests() < QuestManager.DAILY_QUEST_LIMIT
                    && qm.hasAvailableQuestFor(trader.getJob(), difficulty);

            if (isClaimableHere) {
                player.closeInventory();
                qm.completeQuest(alphaPlayer, trader, false);
            } else if (canAccept) {
                player.closeInventory();
                qm.assignQuest(alphaPlayer, trader);
            }

        } else if (slot == TraderMenuService.SLOT_OPEN_TRADE) {
            int reputation = trader.getJob() != null ? alphaPlayer.getJobReputation(trader.getJob()) : 0;
            List<MerchantRecipe> recipes = trader.getRecipesForPlayer(reputation);
            trader.getVillager().setRecipes(recipes);

            if (!player.isOnline()) return;
            player.openInventory(MenuType.MERCHANT.builder().merchant(trader.getVillager()).title(trader.getDisplayName()).build(player));
            player.sendMessage(Component.text("<", NamedTextColor.AQUA).append(trader.getDisplayName()).append(Component.text("> ", NamedTextColor.AQUA)).append(trader.getOpenMessage()));
        } else if (slot == TraderMenuService.SLOT_CANCEL) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onVillagerLoaded(VillagerLoadedEvent event) {
        MLVillager villager = event.getVillager();
        MLLogManager.getInstance().log(Level.FINE, ELogTag.VILLAGER, "[VillagerLoaded] " + villager.getNameId());
        VillagerRegistry.register(villager);
        VillagerPostLoadActions.executeAndClear(villager);

        // Si le villageois qui vient de charger est un Trader, vérifier si des joueurs
        // connectés ont une quête terminée à récupérer chez lui (ex. : respawn du villageois).
        if (villager instanceof Trader trader) {
            QuestGlowService glowService = GameManager.getInstance().getQuestGlowService();
            if (glowService == null) return;
            for (AlphaPlayer alphaPlayer : GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers()) {
                if (alphaPlayer.getPlayer() == null) continue;
                PlayerQuestData q = alphaPlayer.getCurrentActiveQuest();
                if (q != null && q.isCompleted() && !q.isClaimed() && trader.getNameId().equals(q.getTraderId())) {
                    glowService.enableGlow(alphaPlayer, trader.getNameId());
                }
            }
        }
    }

    @EventHandler
    public void onVillagerLevelUp(VillagerLevelUpEvent event) {
        VillagerLevel villager = event.getVillagerLevel();
        MLLogManager.getInstance().log(Level.INFO, ELogTag.VILLAGER, "[VillagerLevelUp] " + villager.getNameId() + " → niveau " + villager.getLevel());
        Component message = villager.getMessage();

        if (message != null && !PlainTextComponentSerializer.plainText().serialize(message).isBlank()) {
            Bukkit.broadcast(Component.text("<", NamedTextColor.AQUA)
                    .append(villager.getDisplayName())
                    .append(Component.text("> ", NamedTextColor.AQUA))
                    .append(message)
            );
        }

        SoundService.broadcast(ESound.VILLAGER_LEVEL_UP);

        for (Player p : Bukkit.getOnlinePlayers()) {
            for (BlessingEffect effect : villager.getBlessing().blessingEffects()) {
                effect.applyEffect(AlphaPlayer.get(p.getUniqueId()));
            }
        }
    }
}