package fr.miuby.survi.quest;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.Trader;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.Component;

public class QuestCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("quest")
                .then(Commands.literal("accept")
                        .then(Commands.argument("traderId", StringArgumentType.string())
                                .executes(QuestCommand::acceptQuest)
                        )
                )
                .then(Commands.literal("remove")
                        .requires(source -> source.getSender().isOp())
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(QuestCommand::removeQuest)
                        )
                );
    }

    private static int removeQuest(CommandContext<CommandSourceStack> context) {
        AlphaPlayer alphaPlayer = GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayer(context.getSource().getSender().getName());

        if (alphaPlayer == null) {
            context.getSource().getSender().sendMessage(Component.text("§cJoueur introuvable."));
            return Command.SINGLE_SUCCESS;
        }

        QuestManager.getInstance().resetQuest(alphaPlayer);

        context.getSource().getSender().sendMessage(Component.text("§aQuête réinitialisée pour " + alphaPlayer.getPseudo()));
        return Command.SINGLE_SUCCESS;
    }

    private static int acceptQuest(CommandContext<CommandSourceStack> context) {
        AlphaPlayer alphaPlayer = GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayer(context.getSource().getSender().getName());
        if (alphaPlayer == null) {
            context.getSource().getSender().sendMessage(Component.text("§cJoueur introuvable."));
            return Command.SINGLE_SUCCESS;
        }

        String traderId = StringArgumentType.getString(context, "traderId");
        Trader trader = (Trader) VillagerRegistry.get(traderId);

        if (trader == null) {
            alphaPlayer.getPlayer().sendMessage("§cTrader introuvable.");
            return Command.SINGLE_SUCCESS;
        }

        QuestManager.getInstance().assignQuest(alphaPlayer, trader);

        return Command.SINGLE_SUCCESS;
    }
}
