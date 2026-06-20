package fr.miuby.survi.villager.trader;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.quest.quest.PlayerQuestData;
import fr.miuby.survi.quest.quest.QuestManager;
import fr.miuby.survi.system.lang.LangService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TraderMenuService {
    public static final int SLOT_INFO         = 32;
    public static final int SLOT_ACCEPT_QUEST = 12;
    public static final int SLOT_OPEN_TRADE   = 14;
    public static final int SLOT_CANCEL       = 31;
    public static final int TOTAL_SLOT        = 36;

    private TraderMenuService() {}

    public static void openMenu(Player player, Trader trader, AlphaPlayer alphaPlayer) {
        TraderMenuHolder holder = new TraderMenuHolder(trader);
        Inventory inv = Bukkit.createInventory(holder, TOTAL_SLOT, trader.getDisplayName());
        holder.setInventory(inv);

        ItemStack filler = buildFiller();
        for (int i = 0; i < TOTAL_SLOT; i++) inv.setItem(i, filler);

        inv.setItem(SLOT_INFO, buildInfoItem(alphaPlayer, trader));
        inv.setItem(SLOT_ACCEPT_QUEST, buildQuestItem(alphaPlayer, trader));
        inv.setItem(SLOT_OPEN_TRADE, buildTradeItem(player));
        inv.setItem(SLOT_CANCEL, buildCloseItem(player));

        player.openInventory(inv);
    }

    private static ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildInfoItem(AlphaPlayer alphaPlayer, Trader trader) {
        LangService ls = GameManager.getInstance().getLangService();
        Player player = alphaPlayer.getPlayer();
        EJob job = trader.getJob();

        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        if (job != null) {
            meta.displayName(job.toComponent().decoration(TextDecoration.ITALIC, false));
            for (int i = 1; i <= 3; i++) lore.add(ls.text(player, "trader.info.desc." + job.name() + "." + i).decoration(TextDecoration.ITALIC, false));
        } else {
            meta.displayName(ls.text(player, "trader.info.title.generic").decoration(TextDecoration.ITALIC, false));
            lore.add(ls.text(player, "trader.info.desc.generic").decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildQuestItem(AlphaPlayer alphaPlayer, Trader trader) {
        LangService ls = GameManager.getInstance().getLangService();
        Player player = alphaPlayer.getPlayer();
        PlayerQuestData current = alphaPlayer.getCurrentActiveQuest();

        if (current != null && current.isCompleted() && !current.isClaimed()
                && (current.getTraderId() == null || current.getTraderId().equals(trader.getNameId()))) {
            return buildItem(Material.GOLD_INGOT,
                    ls.text(player, "trader.menu.claim.title"),
                    List.of(ls.text(player, "trader.menu.claim.done"), ls.text(player, "trader.menu.claim.click")));
        }

        if (current != null && current.isCompleted() && !current.isClaimed()) {
            return buildItem(Material.GRAY_DYE,
                    ls.text(player, "trader.menu.pending.title"),
                    List.of(ls.text(player, "trader.menu.pending.1"), ls.text(player, "trader.menu.pending.2")));
        }

        if (current != null && !current.isCompleted()) {
            return buildItem(Material.GRAY_DYE,
                    ls.text(player, "trader.menu.ongoing.title"),
                    List.of(ls.text(player, "trader.menu.ongoing.1"), ls.text(player, "trader.menu.ongoing.2")));
        }

        int capacity  = GameManager.getInstance().getQuestManager().getTotalCapacity();
        int usedSlots = alphaPlayer.getTotalDailyQuestsClaimed() + alphaPlayer.countActiveUnclaimedQuests();
        if (capacity == 0 || usedSlots >= capacity) {
            Component capacityLine = capacity == 0
                    ? ls.text(player, "trader.menu.no_slot.not_started")
                    : ls.text(player, "trader.menu.no_slot.full", usedSlots, capacity);
            return buildItem(Material.GRAY_DYE,
                    ls.text(player, "trader.menu.no_slot.title"),
                    List.of(capacityLine, ls.text(player, "trader.menu.no_slot.tomorrow")));
        }

        QuestManager qm = GameManager.getInstance().getQuestManager();
        if (qm.isLockedByWorldLevel(alphaPlayer, trader)) {
            return buildItem(Material.GRAY_DYE,
                    ls.text(player, "trader.menu.locked_world.title"),
                    List.of(ls.text(player, "trader.menu.locked_world.1"), ls.text(player, "trader.menu.locked_world.2")));
        }

        if (!qm.hasAvailableQuestFor(trader.getJob(), qm.computeDifficulty(alphaPlayer, trader))) {
            return buildItem(Material.GRAY_DYE,
                    ls.text(player, "trader.menu.no_quest.title"),
                    List.of(ls.text(player, "trader.menu.no_quest.1")));
        }

        return buildItem(Material.WRITABLE_BOOK,
                ls.text(player, "trader.menu.accept.title"),
                List.of(ls.text(player, "trader.menu.accept.1"), ls.text(player, "trader.menu.accept.2")));
    }

    private static ItemStack buildTradeItem(Player player) {
        LangService ls = GameManager.getInstance().getLangService();
        return buildItem(Material.EMERALD,
                ls.text(player, "trader.menu.trade.title"),
                List.of(ls.text(player, "trader.menu.trade.1")));
    }

    private static ItemStack buildCloseItem(Player player) {
        LangService ls = GameManager.getInstance().getLangService();
        return buildItem(Material.BARRIER,
                ls.text(player, "trader.menu.close.title"),
                List.of(ls.text(player, "trader.menu.close.1")));
    }

    private static ItemStack buildItem(Material material, Component title, List<Component> lines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(title.decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        for (Component line : lines) lore.add(line.decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}