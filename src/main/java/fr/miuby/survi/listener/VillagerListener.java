package fr.miuby.survi.listener;

import fr.miuby.lib.villager.MLVillager;
import fr.miuby.lib.villager.VillagerLoadedEvent;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.display.TabSkins;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.quest.quest.PlayerQuestData;
import fr.miuby.survi.quest.quest.QuestGlowService;
import fr.miuby.survi.quest.quest.QuestManager;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.sound.ESound;
import fr.miuby.survi.system.sound.SoundService;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.villager.AVillager;
import fr.miuby.survi.villager.VillagerPostLoadActions;
import fr.miuby.survi.villager.trader.Trader;
import fr.miuby.survi.villager.trader.TraderMenuHolder;
import fr.miuby.survi.villager.trader.TraderMenuService;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import fr.miuby.survi.blessing.BlessingEffect;
import fr.miuby.survi.villager.villagerlevel.event.VillagerLevelUpEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.MenuType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class VillagerListener implements Listener {

    /**
     * Trace quel Trader est ouvert dans l'écran marchand de chaque joueur.
     * Peuplé à l'ouverture du menu marchand, vidé à la fermeture.
     */
    private final Map<UUID, Trader> activeMerchantSessions = new HashMap<>();

    // =========================================================================
    // Interaction entité
    // =========================================================================

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        Entity clicked = event.getRightClicked();

        if (clicked.getType() == EntityType.VILLAGER || clicked.getType() == EntityType.WANDERING_TRADER) {
            event.setCancelled(true);
            return;
        }

        if (clicked.getType() != EntityType.MANNEQUIN) return;

        AVillager aVillager = (AVillager) VillagerRegistry.get(clicked.getUniqueId());

        switch (aVillager) {
            case VillagerLevel level when level.getTribute() == null || !level.isUnlocked() -> {
                if (!level.isUnlocked()) {
                    player.sendMessage(GameManager.getInstance().getLangService().text(player, "villager.locked",
                            Placeholder.component("villager", level.getDisplayName()),
                            Placeholder.unparsed("remaining", level.getRemainingLock())));
                } else {
                    player.sendMessage(Component.text("<", NamedTextColor.AQUA)
                            .append(level.getDisplayName())
                            .append(Component.text("> ", NamedTextColor.AQUA))
                            .append(level.getMessage()));
                }
                event.setCancelled(true);
            }
            case VillagerLevel level -> {
                player.openInventory(level.getInventory());
                event.setCancelled(true);
            }
            case Trader trader -> {
                AlphaPlayer alphaPlayer = AlphaPlayer.get(player.getUniqueId());
                MLLogManager.getInstance().log(Level.FINE, ELogTag.VILLAGER,
                        "[VillagerInteract] " + player.getName() + " → Trader " + trader.getNameId());
                TraderMenuService.openMenu(player, trader, alphaPlayer);
                event.setCancelled(true);
            }
            case null, default -> {
                event.setCancelled(true);
                MLLogManager.getInstance().log(Level.SEVERE, ELogTag.VILLAGER,
                        "Mannequin cliqué sans correspondance dans le registry : " + clicked.getUniqueId());
            }
        }
    }

    // =========================================================================
    // Menu Trader (TraderMenuHolder) — quête + bouton "Ouvrir le commerce"
    // =========================================================================
    @EventHandler
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
            int capacity   = qm.getTotalCapacity();
            int usedSlots  = alphaPlayer.getTotalDailyQuestsClaimed() + alphaPlayer.countActiveUnclaimedQuests();
            boolean canAccept = current == null
                    && capacity > 0
                    && usedSlots < capacity
                    && qm.hasAvailableQuestFor(trader.getJob(), difficulty);

            if (isClaimableHere) {
                player.closeInventory();
                qm.claimQuest(alphaPlayer, trader, false);
            } else if (canAccept) {
                player.closeInventory();
                qm.assignQuest(alphaPlayer, trader);
            }

        } else if (slot == TraderMenuService.SLOT_OPEN_TRADE) {
            int reputation = trader.getJob() != null ? alphaPlayer.getJobReputation(trader.getJob()) : 0;
            List<MerchantRecipe> recipes = trader.getRecipesForPlayer(reputation);

            // Merchant virtuel — pas besoin d'entité Villager
            Merchant merchant = Bukkit.createMerchant(trader.getDisplayName());
            merchant.setRecipes(recipes);
            activeMerchantSessions.put(player.getUniqueId(), trader);

            if (!player.isOnline()) return;
            player.openInventory(MenuType.MERCHANT.builder()
                    .merchant(merchant)
                    .title(trader.getDisplayName())
                    .build(player));
            player.sendMessage(Component.text("<", NamedTextColor.AQUA)
                    .append(trader.getDisplayName())
                    .append(Component.text("> ", NamedTextColor.AQUA))
                    .append(trader.getOpenMessage()));

        } else if (slot == TraderMenuService.SLOT_CANCEL) {
            player.closeInventory();
        }
    }

    // =========================================================================
    // Écran marchand — message à l'achat (remplace PlayerTradeEvent)
    // =========================================================================

    @EventHandler
    public void onMerchantResultTaken(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.MERCHANT) return;
        if (event.getRawSlot() != 2) return; // slot résultat
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Trader trader = activeMerchantSessions.get(player.getUniqueId());
        if (trader == null) return;

        var result = event.getCurrentItem();
        if (result == null || result.getType() == Material.AIR) return;

        try {
            AlphaPlayer alphaPlayer = AlphaPlayer.get(player.getUniqueId());
            GameManager.getInstance().getDatabase().tradeHistory().insert(
                    alphaPlayer.getUuid(), alphaPlayer.getPseudo(), trader.getNameId(), result.getType().name(), result.getAmount());
        } catch (fr.miuby.survi.system.exception.AlphaPlayerNotFoundException ignored) {}

        player.sendMessage(Component.text("<", NamedTextColor.AQUA)
                .append(trader.getDisplayName())
                .append(Component.text("> ", NamedTextColor.AQUA))
                .append(trader.getMessage(result).color(NamedTextColor.AQUA)));
    }

    @EventHandler
    public void onMerchantClose(InventoryCloseEvent event) {
        if (event.getView().getTopInventory().getType() == InventoryType.MERCHANT) {
            activeMerchantSessions.remove(event.getPlayer().getUniqueId());
        }
    }

    // =========================================================================
    // Événements villageois
    // =========================================================================

    @EventHandler
    public void onVillagerLoaded(VillagerLoadedEvent event) {
        MLVillager villager = event.getVillager();
        MLLogManager.getInstance().log(Level.FINE, ELogTag.VILLAGER, "[VillagerLoaded] " + villager.getNameId());
        VillagerRegistry.register(villager);
        VillagerPostLoadActions.executeAndClear(villager);
        TabSkins.loadEntitySkin(GameManager.getInstance().getPlugin(), villager);

        if (villager instanceof Trader trader) {
            QuestGlowService glowService = GameManager.getInstance().getQuestGlowService();
            if (glowService == null) return;
            for (AlphaPlayer alphaPlayer : GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers()) {
                if (alphaPlayer.getPlayer() == null) continue;
                PlayerQuestData q = alphaPlayer.getCurrentActiveQuest();
                if (q != null && q.isCompleted() && !q.isClaimed() && trader.getNameId().equals(q.getTraderId()))
                    glowService.enableGlow(alphaPlayer, trader.getNameId());
            }
        }
    }

    @EventHandler
    public void onVillagerLevelUp(VillagerLevelUpEvent event) {
        VillagerLevel villager = event.getVillagerLevel();
        MLLogManager.getInstance().log(Level.INFO, ELogTag.VILLAGER,
                "[VillagerLevelUp] " + villager.getNameId() + " → niveau " + villager.getLevel());

        Component message = villager.getMessage();
        if (message != null && !PlainTextComponentSerializer.plainText().serialize(message).isBlank()) {
            Bukkit.broadcast(Component.text("<", NamedTextColor.AQUA)
                    .append(villager.getDisplayName())
                    .append(Component.text("> ", NamedTextColor.AQUA))
                    .append(message));
        }

        SoundService.broadcast(ESound.VILLAGER_LEVEL_UP);

        for (BlessingEffect effect : villager.getBlessing().blessingEffects()) {
            if (effect.isOneShot()) {
                Bukkit.getOnlinePlayers().stream()
                        .findFirst()
                        .map(p -> AlphaPlayer.get(p.getUniqueId()))
                        .ifPresent(effect::applyEffect);
                MLLogManager.getInstance().log(Level.FINE, ELogTag.VILLAGER,
                        "[VillagerLevelUp] one-shot appliqué une fois : " + effect.getClass().getSimpleName());
            } else {
                for (var p : Bukkit.getOnlinePlayers()) {
                    effect.applyEffect(AlphaPlayer.get(p.getUniqueId()));
                }
            }
        }
    }
}