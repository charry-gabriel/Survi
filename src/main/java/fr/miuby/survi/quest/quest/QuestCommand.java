package fr.miuby.survi.quest.quest;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.quest.globalquest.GlobalQuest;
import fr.miuby.survi.system.command.argument.AlphaPlayerArgument;
import fr.miuby.survi.system.command.argument.QuestArgument;
import fr.miuby.survi.system.command.argument.TraderArgument;
import fr.miuby.survi.villager.trader.Trader;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.lang.LangService;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"java:S3516", "SameReturnValue"})
public class QuestCommand {
    private static final String playerArgument = "player";
    private static final String villagerArgument = "villager";
    private static final String questArgument = "quest";

    private QuestCommand() {
        /* This utility class should not be instantiated */
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("quest")
                .then(Commands.literal("give")
                        .requires(source -> source.getSender().isOp())
                        .then(Commands.argument(playerArgument, AlphaPlayerArgument.alphaPlayer())
                                .then(Commands.argument(villagerArgument, TraderArgument.trader())
                                        .executes(QuestCommand::giveQuest)
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .requires(source -> source.getSender().isOp())
                        .then(Commands.argument(playerArgument, AlphaPlayerArgument.alphaPlayer())
                                .executes(QuestCommand::removeQuest)
                        )
                )
                .then(Commands.literal("reset")
                        .requires(source -> source.getSender().isOp())
                        .then(Commands.argument(playerArgument, AlphaPlayerArgument.alphaPlayer())
                                .executes(QuestCommand::resetAllQuests)
                        )
                )
                .then(Commands.literal("claim")
                        .requires(source -> source.getSender().isOp())
                        .then(Commands.argument(playerArgument, AlphaPlayerArgument.alphaPlayer())
                                .then(Commands.argument(villagerArgument, TraderArgument.trader())
                                        .executes(QuestCommand::claimQuest)
                                )
                        )
                )
                .then(Commands.literal("test")
                        .requires(source -> source.getSender().isOp())
                        .then(Commands.argument(playerArgument, AlphaPlayerArgument.alphaPlayer())
                                .then(Commands.argument(questArgument, QuestArgument.quest())
                                        .executes(QuestCommand::testQuest)
                                )
                        )
                )
                .then(Commands.literal("history")
                        .executes(QuestCommand::historyself)
                        .then(Commands.argument(playerArgument, AlphaPlayerArgument.alphaPlayer())
                                .requires(source -> source.getSender().isOp())
                                .executes(QuestCommand::historyOf)
                        )
                )
                .then(Commands.literal("extraslot")
                        .requires(source -> source.getSender().isOp())
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                        .executes(QuestCommand::extraSlotAdd)
                                )
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                        .executes(QuestCommand::extraSlotRemove)
                                )
                        )
                        .then(Commands.literal("status")
                                .executes(QuestCommand::extraSlotStatus)
                        )
                );
    }

    /**
     * Donne une nouvelle quête.
     */
    private static int giveQuest(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer   alphaPlayer = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);
        Trader        trader      = TraderArgument.getTrader(ctx, villagerArgument);
        CommandSender sender      = ctx.getSource().getSender();
        LangService   ls          = GameManager.getInstance().getLangService();

        if (alphaPlayer.getCurrentActiveQuest() == null) {
            GameManager.getInstance().getQuestManager().assignQuest(alphaPlayer, trader, true);
        } else {
            sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.quest.exists", alphaPlayer.getPseudo()));
        }
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Supprime la quête en cours (non réclamée) du joueur.
     */
    private static int removeQuest(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer   alphaPlayer = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);
        CommandSender sender      = ctx.getSource().getSender();
        LangService   ls          = GameManager.getInstance().getLangService();

        String key = GameManager.getInstance().getQuestManager().resetQuest(alphaPlayer)
                ? "cmd.quest.removed" : "cmd.quest.no_active";
        sender.sendMessage(ls.text(ls.resolveOrDefault(sender), key, alphaPlayer.getPseudo()));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Supprime les quêtes actives (non réclamées) du joueur (reset admin).
     */
    private static int resetAllQuests(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer   alphaPlayer = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);
        CommandSender sender      = ctx.getSource().getSender();
        LangService   ls          = GameManager.getInstance().getLangService();

        if (alphaPlayer.getActiveQuests().isEmpty()) {
            sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.quest.reset_empty", alphaPlayer.getPseudo()));
            return Command.SINGLE_SUCCESS;
        }

        for (PlayerQuestData data : new ArrayList<>(alphaPlayer.getActiveQuests())) {
            GameManager.getInstance().getDatabase().quests().deletePlayerQuestSlot(alphaPlayer.getUuid(), data.getSlot());
        }
        alphaPlayer.getActiveQuests().clear();
        GameManager.getInstance().getQuestActionBarService().stopRefresh(alphaPlayer.getUuid());

        QuestGlowService glowService = GameManager.getInstance().getQuestGlowService();
        if (glowService != null) glowService.disableGlow(alphaPlayer);

        if (alphaPlayer.getPlayer() != null) {
            alphaPlayer.getPlayer().sendMessage(ls.text(alphaPlayer.getPlayer(), "cmd.quest.reset_notify"));
        }
        sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.quest.reset_done", alphaPlayer.getPseudo()));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Assigne une quête spécifique à un joueur pour la tester (admin).
     */
    private static int testQuest(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer   alphaPlayer = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);
        Quest         quest       = QuestArgument.getQuest(ctx, questArgument);
        CommandSender sender      = ctx.getSource().getSender();
        LangService   ls          = GameManager.getInstance().getLangService();

        GameManager.getInstance().getQuestManager().assignSpecificQuest(alphaPlayer, quest);
        sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.quest.test_done",
                quest.getName(), alphaPlayer.getPseudo()));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Force la réception des récompenses de la quête en cours (admin).
     */
    private static int claimQuest(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer   alphaPlayer = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);
        Trader        trader      = TraderArgument.getTrader(ctx, villagerArgument);
        CommandSender sender      = ctx.getSource().getSender();
        LangService   ls          = GameManager.getInstance().getLangService();

        String key = GameManager.getInstance().getQuestManager().claimQuest(alphaPlayer, trader, true)
                ? "cmd.quest.claim_done" : "cmd.quest.claim_fail";
        sender.sendMessage(ls.text(ls.resolveOrDefault(sender), key, alphaPlayer.getPseudo()));
        return Command.SINGLE_SUCCESS;
    }

    // =========================================================================
    // /quest history
    // =========================================================================

    /** Affiche l'historique de l'expéditeur (doit être un joueur en jeu). */
    private static int historyself(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        LangService   ls     = GameManager.getInstance().getLangService();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ls.text(ls.getServerDefault(), "cmd.quest.history_console_only"));
            return Command.SINGLE_SUCCESS;
        }
        showHistory(sender, AlphaPlayer.get(player.getUniqueId()));
        return Command.SINGLE_SUCCESS;
    }

    /** Affiche l'historique d'un joueur spécifique (op seulement). */
    private static int historyOf(CommandContext<CommandSourceStack> ctx) {
        showHistory(ctx.getSource().getSender(), AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument));
        return Command.SINGLE_SUCCESS;
    }

    private static void showHistory(CommandSender sender, AlphaPlayer ap) {
        LangService           ls      = GameManager.getInstance().getLangService();
        var                   lang    = ls.resolveOrDefault(sender);
        List<QuestHistoryEntry> entries = GameManager.getInstance().getDatabase().questHistory().getHistory(ap.getUuid(), 10);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        sender.sendMessage(ls.text(lang, "cmd.quest.history_separator"));
        sender.sendMessage(ls.text(lang, "cmd.quest.history_title", ap.getPseudo()));
        sender.sendMessage(ls.text(lang, "cmd.quest.history_separator"));

        if (entries.isEmpty()) {
            sender.sendMessage(ls.text(lang, "cmd.quest.history_empty"));
        } else {
            for (int i = 0; i < entries.size(); i++) {
                QuestHistoryEntry e = entries.get(i);

                String questName = null;
                if ("daily".equals(e.questType())) {
                    Quest q = GameManager.getInstance().getQuestManager().getQuest(e.questId());
                    if (q != null) questName = q.getName();
                } else {
                    GlobalQuest gq = GameManager.getInstance().getGlobalQuestManager().getQuest(e.questId());
                    if (gq != null) questName = gq.getName();
                }
                String displayName = questName != null ? questName : e.questId();

                Component diffComp = "daily".equals(e.questType())
                        ? ls.text(lang, "cmd.quest.history_diff_daily", e.difficulty())
                        : ls.text(lang, "cmd.quest.history_diff_global");
                String jobLabel = e.job() != null ? e.job() : "—";

                Component line = Component.text("  #" + (i + 1) + " ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(e.completedAt().format(fmt) + " ", NamedTextColor.WHITE))
                        .append(Component.text("[", NamedTextColor.YELLOW))
                        .append(diffComp)
                        .append(Component.text("] ", NamedTextColor.YELLOW))
                        .append(Component.text("[" + jobLabel + "] ", NamedTextColor.AQUA))
                        .append(Component.text(displayName, NamedTextColor.GRAY));

                if ("global".equals(e.questType()) && e.contribution() > 0) {
                    line = line.append(ls.text(lang, "cmd.quest.history_contrib", e.contribution()));
                }

                sender.sendMessage(line);
            }
        }

        sender.sendMessage(ls.text(lang, "cmd.quest.history_separator"));
    }

    // =========================================================================
    // /quest extraslot
    // =========================================================================

    private static int extraSlotAdd(CommandContext<CommandSourceStack> ctx) {
        int amount = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount");
        CommandSender sender = ctx.getSource().getSender();
        LangService ls = GameManager.getInstance().getLangService();

        GameManager.getInstance().getQuestManager().addExtraSlots(amount);
        int bonus = GameManager.getInstance().getQuestManager().getExtraGlobalSlots();
        sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.quest.extraslot_added", amount, bonus));
        return Command.SINGLE_SUCCESS;
    }

    private static int extraSlotRemove(CommandContext<CommandSourceStack> ctx) {
        int amount = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount");
        CommandSender sender = ctx.getSource().getSender();
        LangService ls = GameManager.getInstance().getLangService();

        GameManager.getInstance().getQuestManager().removeExtraSlots(amount);
        int bonus = GameManager.getInstance().getQuestManager().getExtraGlobalSlots();
        sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.quest.extraslot_removed", amount, bonus));
        return Command.SINGLE_SUCCESS;
    }

    private static int extraSlotStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        LangService ls = GameManager.getInstance().getLangService();

        int bonus = GameManager.getInstance().getQuestManager().getExtraGlobalSlots();
        int base = GameManager.getInstance().getQuestManager().getTotalCapacity() - bonus;
        sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.quest.extraslot_status", bonus, base, base + bonus));
        return Command.SINGLE_SUCCESS;
    }
}