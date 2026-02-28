package fr.miuby.survi.quest;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.command.argument.AlphaPlayerArgument;
import fr.miuby.survi.system.command.CommandErrors;
import fr.miuby.survi.system.command.argument.VillagerArgument;
import fr.miuby.survi.villager.AVillager;
import fr.miuby.survi.villager.Trader;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class QuestCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("quest")
                .then(Commands.literal("remove")
                    .requires(source -> source.getSender().isOp())
                    .then(Commands.argument("player", AlphaPlayerArgument.alphaPlayer())
                        .executes(QuestCommand::removeQuest)
                    )
                )
                .then(Commands.literal("complete")
                    .requires(source -> source.getSender().isOp())
                    .then(Commands.argument("player", AlphaPlayerArgument.alphaPlayer())
                        .then(Commands.argument("villager", VillagerArgument.villager())
                            .executes(QuestCommand::completeQuest)
                        )
                    )
                );
    }

    private static int removeQuest(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer alphaPlayer = AlphaPlayerArgument.getAlphaPlayer(ctx, "player");

        if (QuestManager.getInstance().resetQuest(alphaPlayer))
            ctx.getSource().getSender().sendMessage(Component.text("Quête réinitialisée pour " + alphaPlayer.getPseudo()).color(NamedTextColor.GREEN));
        else
            ctx.getSource().getSender().sendMessage(Component.text("Pas de quête pour " + alphaPlayer.getPseudo()).color(NamedTextColor.RED));

        return Command.SINGLE_SUCCESS;
    }

    private static int completeQuest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        AlphaPlayer alphaPlayer = AlphaPlayerArgument.getAlphaPlayer(ctx, "player");
        AVillager villager = VillagerArgument.getVillager(ctx, "villager");

        if (villager instanceof Trader trader) {
            if (QuestManager.getInstance().completeQuest(alphaPlayer, trader, true)) {
                ctx.getSource().getSender().sendMessage(Component.text("Quête complété pour " + alphaPlayer.getPseudo()).color(NamedTextColor.GREEN));
            } else {
                ctx.getSource().getSender().sendMessage(Component.text("Impossible de compléter la quête pour " + alphaPlayer.getPseudo()).color(NamedTextColor.RED));
            }
        } else {
            throw CommandErrors.NOT_A_TRADER.create();
        }

        return Command.SINGLE_SUCCESS;
    }
}