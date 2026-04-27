package fr.miuby.survi.quest;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.command.argument.AlphaPlayerArgument;
import fr.miuby.survi.system.command.argument.TraderArgument;
import fr.miuby.survi.villager.Trader;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import fr.miuby.survi.GameManager;

public class QuestCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("quest")
                .then(Commands.literal("remove")
                        .requires(source -> source.getSender().isOp())
                        .then(Commands.argument("player", AlphaPlayerArgument.alphaPlayer())
                                .executes(QuestCommand::removeQuest)
                        )
                )
                .then(Commands.literal("reset")
                        .requires(source -> source.getSender().isOp())
                        .then(Commands.argument("player", AlphaPlayerArgument.alphaPlayer())
                                .executes(QuestCommand::resetAllQuests)
                        )
                )
                .then(Commands.literal("complete")
                        .requires(source -> source.getSender().isOp())
                        .then(Commands.argument("player", AlphaPlayerArgument.alphaPlayer())
                                .then(Commands.argument("villager", TraderArgument.trader())
                                        .executes(QuestCommand::completeQuest)
                                )
                        )
                );
    }

    /**
     * Supprime la quête en cours (non réclamée) du joueur.
     * Le joueur récupère un slot et peut en accepter une nouvelle.
     */
    private static int removeQuest(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer alphaPlayer = AlphaPlayerArgument.getAlphaPlayer(ctx, "player");

        if (QuestManager.getInstance().resetQuest(alphaPlayer)) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Quête en cours réinitialisée pour " + alphaPlayer.getPseudo() + ". Il peut en accepter une nouvelle.").color(NamedTextColor.GREEN));
        } else {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Aucune quête en cours pour " + alphaPlayer.getPseudo() + " (toutes déjà réclamées ou aucune quête).").color(NamedTextColor.RED));
        }
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Supprime TOUTES les quêtes du jour du joueur (reset complet journalier).
     * Utile pour recommencer la journée à zéro.
     */
    private static int resetAllQuests(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer alphaPlayer = AlphaPlayerArgument.getAlphaPlayer(ctx, "player");

        if (alphaPlayer.getActiveQuests().isEmpty()) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Aucune quête aujourd'hui pour " + alphaPlayer.getPseudo() + ".").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        // Retirer les buffs de toutes les quêtes réclamées
        for (PlayerQuestData data : alphaPlayer.getActiveQuests()) {
            if (data.isClaimed() && alphaPlayer.getPlayer() != null) {
                var quest = QuestManager.getInstance().getQuest(data.getQuestId());
                if (quest != null) {
                    quest.getRewards().forEach(e -> alphaPlayer.getPlayer().removePotionEffect(e.getType()));
                }
            }
        }

        alphaPlayer.getActiveQuests().clear();
        GameManager.getInstance().getDatabase().quests().clearAllPlayerQuests(alphaPlayer.getUuid());

        if (alphaPlayer.getPlayer() != null) {
            alphaPlayer.getPlayer().sendMessage("§eToutes vos quêtes du jour ont été réinitialisées par un administrateur.");
        }
        ctx.getSource().getSender().sendMessage(
                Component.text("Toutes les quêtes du jour réinitialisées pour " + alphaPlayer.getPseudo() + ".").color(NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Force la complétion de la quête en cours (admin).
     */
    private static int completeQuest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        AlphaPlayer alphaPlayer = AlphaPlayerArgument.getAlphaPlayer(ctx, "player");
        Trader trader = TraderArgument.getTrader(ctx, "villager");

        if (QuestManager.getInstance().completeQuest(alphaPlayer, trader, true)) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Quête complétée pour " + alphaPlayer.getPseudo()).color(NamedTextColor.GREEN));
        } else {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Impossible de compléter la quête pour " + alphaPlayer.getPseudo()
                            + " (aucune quête en cours ou déjà réclamée)").color(NamedTextColor.RED));
        }
        return Command.SINGLE_SUCCESS;
    }
}