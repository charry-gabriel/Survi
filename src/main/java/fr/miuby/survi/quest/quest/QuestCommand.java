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
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.miuby.survi.GameManager;

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
                );
    }

    /**
     * Donne une nouvelle quête.
     */
    private static int giveQuest(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer alphaPlayer = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);
        Trader trader = TraderArgument.getTrader(ctx, villagerArgument);

        if (alphaPlayer.getCurrentActiveQuest() == null) {
            GameManager.getInstance().getQuestManager().assignQuest(alphaPlayer, trader, true);
        } else {
            ctx.getSource().getSender().sendMessage(Component.text("Une quête existe déjà pour " + alphaPlayer.getPseudo()).color(NamedTextColor.RED));
        }
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Supprime la quête en cours (non réclamée) du joueur.
     * Le joueur récupère un slot et peut en accepter une nouvelle.
     */
    private static int removeQuest(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer alphaPlayer = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);

        if (GameManager.getInstance().getQuestManager().resetQuest(alphaPlayer)) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Quête en cours réinitialisée pour " + alphaPlayer.getPseudo() + ". Il peut en accepter une nouvelle.").color(NamedTextColor.GREEN));
        } else {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Aucune quête en cours pour " + alphaPlayer.getPseudo() + " (toutes déjà réclamées ou aucune quête).").color(NamedTextColor.RED));
        }
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Supprime les quêtes actives (non réclamées) du joueur (reset admin).
     * Les quêtes déjà réclamées restent dans quest_history et comptent dans le total cumulatif.
     */
    private static int resetAllQuests(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer alphaPlayer = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);

        if (alphaPlayer.getActiveQuests().isEmpty()) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Aucune quête active pour " + alphaPlayer.getPseudo() + ".").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        // Supprime uniquement les quêtes non réclamées (les réclamées sont déjà en quest_history)
        for (PlayerQuestData data : new ArrayList<>(alphaPlayer.getActiveQuests())) {
            GameManager.getInstance().getDatabase().quests().deletePlayerQuestSlot(alphaPlayer.getUuid(), data.getSlot());
        }
        alphaPlayer.getActiveQuests().clear();
        GameManager.getInstance().getQuestActionBarService().stopRefresh(alphaPlayer.getUuid());

        QuestGlowService glowService = GameManager.getInstance().getQuestGlowService();
        if (glowService != null) glowService.disableGlow(alphaPlayer);

        if (alphaPlayer.getPlayer() != null) {
            alphaPlayer.getPlayer().sendMessage(Component.text("Vos quêtes actives ont été réinitialisées par un administrateur.", NamedTextColor.YELLOW));
        }
        ctx.getSource().getSender().sendMessage(
                Component.text("Quêtes actives réinitialisées pour " + alphaPlayer.getPseudo() + ".").color(NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Assigne une quête spécifique à un joueur pour la tester (admin).
     * Remplace toute quête active non réclamée. Aucun Trader requis pour la validation.
     */
    private static int testQuest(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer alphaPlayer = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);
        Quest quest = QuestArgument.getQuest(ctx, questArgument);

        GameManager.getInstance().getQuestManager().assignSpecificQuest(alphaPlayer, quest);

        ctx.getSource().getSender().sendMessage(
                Component.text("[TEST] Quête « " + quest.getName() + " » assignée à " + alphaPlayer.getPseudo() + ".").color(NamedTextColor.YELLOW));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Force la réception des récompenses de la quête en cours (admin).
     */
    private static int claimQuest(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer alphaPlayer = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);
        Trader trader = TraderArgument.getTrader(ctx, villagerArgument);

        if (GameManager.getInstance().getQuestManager().claimQuest(alphaPlayer, trader, true)) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Récompenses réclamées pour " + alphaPlayer.getPseudo()).color(NamedTextColor.GREEN));
        } else {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Impossible de réclamer les récompenses pour " + alphaPlayer.getPseudo()
                            + " (aucune quête terminée ou déjà réclamée)").color(NamedTextColor.RED));
        }
        return Command.SINGLE_SUCCESS;
    }

    // =========================================================================
    // /quest history
    // =========================================================================

    /** Affiche l'historique de l'expéditeur (doit être un joueur en jeu). */
    private static int historyself(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Utilisez /quest history <joueur> depuis la console.").color(NamedTextColor.RED));
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
        List<QuestHistoryEntry> entries = GameManager.getInstance().getDatabase().questHistory().getHistory(ap.getUuid(), 10);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  ✦ Historique de " + ap.getPseudo())
                .color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GOLD));

        if (entries.isEmpty()) {
            sender.sendMessage(Component.text("  Aucune quête complétée pour l'instant.").color(NamedTextColor.GRAY));
        } else {
            for (int i = 0; i < entries.size(); i++) {
                QuestHistoryEntry e = entries.get(i);

                // Lookup du nom de la quête dans les pools (daily ou global)
                String questName = null;
                if ("daily".equals(e.questType())) {
                    Quest q = GameManager.getInstance().getQuestManager().getQuest(e.questId());
                    if (q != null) questName = q.getName();
                } else {
                    GlobalQuest gq = GameManager.getInstance().getGlobalQuestManager().getQuest(e.questId());
                    if (gq != null) questName = gq.getName();
                }
                String displayName = questName != null ? questName : e.questId();

                // Libellé de difficulté
                String diffLabel = "daily".equals(e.questType())
                        ? "diff." + e.difficulty()
                        : "mondiale";

                // Libellé de métier
                String jobLabel = e.job() != null ? e.job() : "—";

                Component line = Component.text("  #" + (i + 1) + " ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(e.completedAt().format(fmt) + " ", NamedTextColor.WHITE))
                        .append(Component.text("[" + diffLabel + "] ", NamedTextColor.YELLOW))
                        .append(Component.text("[" + jobLabel + "] ", NamedTextColor.AQUA))
                        .append(Component.text(displayName, NamedTextColor.GRAY));

                if ("global".equals(e.questType()) && e.contribution() > 0) {
                    line = line.append(Component.text(" — contrib:" + e.contribution(), NamedTextColor.DARK_GRAY));
                }

                sender.sendMessage(line);
            }
        }

        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GOLD));
    }
}