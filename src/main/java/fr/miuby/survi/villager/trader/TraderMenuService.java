package fr.miuby.survi.villager.trader;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.quest.quest.PlayerQuestData;
import fr.miuby.survi.quest.quest.QuestManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

        inv.setItem(SLOT_INFO, buildReputationItem(alphaPlayer, trader));
        inv.setItem(SLOT_ACCEPT_QUEST, buildQuestItem(alphaPlayer, trader));
        inv.setItem(SLOT_OPEN_TRADE, buildTradeItem());
        inv.setItem(SLOT_CANCEL, buildCloseItem());

        player.openInventory(inv);
    }

    private static ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildReputationItem(AlphaPlayer alphaPlayer, Trader trader) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Réputation & Profil", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        EJob job = trader.getJob();
        if (job != null) {
            String jobLevelText = alphaPlayer.isJobMaxLevel(job) ? "MAX" : String.valueOf(alphaPlayer.getJobLevel(job));
            lore.add(Component.text("Métier : ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(job.toComponent().decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Niveau métier : ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(jobLevelText, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.empty());
        }

        lore.add(Component.text("Rang global : ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                .append(alphaPlayer.getGlobalRank().displayComponent().decoration(TextDecoration.ITALIC, false)));
        lore.add(Component.empty());
        lore.add(Component.text("Quêtes complétées : ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(
                        (alphaPlayer.getTotalDailyQuestsClaimed() + alphaPlayer.countActiveUnclaimedQuests()) + "/" + GameManager.getInstance().getQuestManager().getTotalCapacity(),
                        NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
        lore.add(Component.empty());

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildQuestItem(AlphaPlayer alphaPlayer, Trader trader) {
        PlayerQuestData current = alphaPlayer.getCurrentActiveQuest();

        // Quête terminée à réclamer ici
        if (current != null && current.isCompleted() && !current.isClaimed()
                && (current.getTraderId() == null || current.getTraderId().equals(trader.getNameId()))) {
            ItemStack item = new ItemStack(Material.GOLD_INGOT);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text("Réclamer la récompense", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Votre quête est terminée !", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Cliquez pour récupérer votre récompense.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            meta.lore(lore);
            item.setItemMeta(meta);
            return item;
        }

        // Quête terminée à réclamer chez un autre commerçant
        if (current != null && current.isCompleted() && !current.isClaimed()) {
            return buildGreyItem("Récompense en attente", List.of(
                    "Votre quête est terminée, mais elle doit",
                    "être validée auprès d'un autre commerçant."
            ));
        }

        // Quête en cours
        if (current != null && !current.isCompleted()) {
            return buildGreyItem("Quête en cours", List.of(
                    "Vous avez déjà une quête en cours.",
                    "Terminez-la avant d'en accepter une nouvelle."
            ));
        }

        // Capacité cumulative atteinte
        int capacity  = GameManager.getInstance().getQuestManager().getTotalCapacity();
        int usedSlots = alphaPlayer.getTotalDailyQuestsClaimed() + alphaPlayer.countActiveUnclaimedQuests();
        if (capacity == 0 || usedSlots >= capacity) {
            String msg = capacity == 0
                    ? "La partie n'a pas encore démarré."
                    : "Vous avez complété " + usedSlots + "/" + capacity + " quêtes disponibles.";
            return buildGreyItem("Aucun créneau disponible", List.of(
                    msg,
                    "Revenez demain pour de nouveaux créneaux !"
            ));
        }

        // Aucune quête disponible à la bonne difficulté pour ce commerçant
        QuestManager qm = GameManager.getInstance().getQuestManager();
        int difficulty = qm.computeDifficulty(alphaPlayer, trader);
        if (!qm.hasAvailableQuestFor(trader.getJob(), difficulty)) {
            return buildGreyItem("Aucune quête disponible", List.of(
                    "Aucune quête disponible pour votre profil."
            ));
        }

        // Peut accepter une quête
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Accepter une quête", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Cliquez pour accepter une nouvelle", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("quête auprès de ce commerçant.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildGreyItem(String title, List<String> lines) {
        ItemStack item = new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(title, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        for (String line : lines) lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildTradeItem() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Ouvrir le commerce", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Ouvre l'inventaire de trade.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Fermer", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Ferme ce menu.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}