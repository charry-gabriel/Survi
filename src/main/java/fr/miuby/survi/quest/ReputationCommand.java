package fr.miuby.survi.quest;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.miuby.survi.job.JobLevelConfig;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.command.argument.AlphaPlayerArgument;
import fr.miuby.survi.system.command.argument.TraderArgument;
import fr.miuby.survi.villager.trader.Trader;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Commande admin pour modifier manuellement la réputation d'un joueur
 * auprès d'un Trader donné.
 *
 * Usage :
 *   /reputation add    <joueur> <trader> <montant>
 *   /reputation remove <joueur> <trader> <montant>
 *   /reputation set    <joueur> <trader> <valeur>
 *   /reputation info   <joueur> <trader>
 */
@SuppressWarnings({"java:S3516", "SameReturnValue"})
public class ReputationCommand {
    private static final String amountArgument = "amount";
    private static final String playerArgument = "player";
    private static final String traderArgument = "trader";

    private ReputationCommand() {
        /* This utility class should not be instantiated */
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("reputation")
                .requires(source -> source.getSender().isOp())

                // /reputation add <player> <trader> <amount>
                .then(Commands.literal("add")
                        .then(Commands.argument(playerArgument, AlphaPlayerArgument.alphaPlayer())
                                .then(Commands.argument(traderArgument, TraderArgument.trader())
                                        .then(Commands.argument(amountArgument, IntegerArgumentType.integer(1, 10000))
                                                .executes(ctx -> executeAdd(ctx, true))
                                        )
                                )
                        )
                )

                // /reputation remove <player> <trader> <amount>
                .then(Commands.literal("remove")
                        .then(Commands.argument(playerArgument, AlphaPlayerArgument.alphaPlayer())
                                .then(Commands.argument(traderArgument, TraderArgument.trader())
                                        .then(Commands.argument(amountArgument, IntegerArgumentType.integer(1, 10000))
                                                .executes(ctx -> executeAdd(ctx, false))
                                        )
                                )
                        )
                )

                // /reputation set <player> <trader> <value>
                .then(Commands.literal("set")
                        .then(Commands.argument(playerArgument, AlphaPlayerArgument.alphaPlayer())
                                .then(Commands.argument(traderArgument, TraderArgument.trader())
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100000))
                                                .executes(ReputationCommand::executeSet)
                                        )
                                )
                        )
                )

                // /reputation info <player> <trader>
                .then(Commands.literal("info")
                        .then(Commands.argument(playerArgument, AlphaPlayerArgument.alphaPlayer())
                                .then(Commands.argument(traderArgument, TraderArgument.trader())
                                        .executes(ReputationCommand::executeInfo)
                                )
                        )
                );
    }

    // ── /reputation add | remove ──────────────────────────────────────────────

    private static int executeAdd(CommandContext<CommandSourceStack> ctx, boolean isAdd) {
        AlphaPlayer target  = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);
        Trader      trader   = TraderArgument.getTrader(ctx, traderArgument);
        int         amount   = IntegerArgumentType.getInteger(ctx, amountArgument);

        int before = target.getReputation(trader.getNameId());
        int delta  = isAdd ? amount : -amount;

        // On ne descend pas en dessous de 0
        int effective = Math.max(0, before + delta);
        int actualDelta = effective - before;

        if (actualDelta == 0 && !isAdd) {
            ctx.getSource().getSender().sendMessage(
                    Component.text(target.getPseudo() + " est déjà à 0 réputation avec " + trader.getNameId() + ".")
                            .color(NamedTextColor.YELLOW));
            return Command.SINGLE_SUCCESS;
        }

        // addReputation gère la persistence + mise à jour du niveau de métier
        target.addReputation(trader.getNameId(), actualDelta);
        int after = target.getReputation(trader.getNameId());

        String verb = isAdd ? "Ajouté" : "Retiré";
        NamedTextColor color = isAdd ? NamedTextColor.GREEN : NamedTextColor.GOLD;

        ctx.getSource().getSender().sendMessage(
                Component.text(verb + " ", color)
                        .append(Component.text(Math.abs(actualDelta) + " rep", NamedTextColor.WHITE))
                        .append(Component.text(" à ", color))
                        .append(Component.text(target.getPseudo(), NamedTextColor.YELLOW))
                        .append(Component.text(" auprès de ", color))
                        .append(Component.text(trader.getNameId(), NamedTextColor.AQUA))
                        .append(Component.text(" (" + before + " → " + after + ")", NamedTextColor.GRAY))
        );

        // Notifier le joueur cible s'il est en ligne
        if (target.getPlayer() != null && target.getPlayer().isOnline()) {
            String adminMsg = isAdd
                    ? "§aUn administrateur vous a accordé §f" + Math.abs(actualDelta) + " rep§a auprès de §b" + trader.getNameId() + "§a."
                    : "§6Un administrateur vous a retiré §f" + Math.abs(actualDelta) + " rep§6 auprès de §b" + trader.getNameId() + "§6.";
            target.getPlayer().sendMessage(adminMsg);
        }

        return Command.SINGLE_SUCCESS;
    }

    // ── /reputation set ───────────────────────────────────────────────────────

    private static int executeSet(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer target   = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);
        Trader      trader   = TraderArgument.getTrader(ctx, traderArgument);
        int         value    = IntegerArgumentType.getInteger(ctx, "value");

        int before = target.getReputation(trader.getNameId());
        int delta  = value - before;

        target.addReputation(trader.getNameId(), delta);

        ctx.getSource().getSender().sendMessage(
                Component.text("Réputation de ", NamedTextColor.GREEN)
                        .append(Component.text(target.getPseudo(), NamedTextColor.YELLOW))
                        .append(Component.text(" auprès de ", NamedTextColor.GREEN))
                        .append(Component.text(trader.getNameId(), NamedTextColor.AQUA))
                        .append(Component.text(" fixée à ", NamedTextColor.GREEN))
                        .append(Component.text(value, NamedTextColor.WHITE))
                        .append(Component.text(" (était " + before + ")", NamedTextColor.GRAY))
        );

        if (target.getPlayer() != null && target.getPlayer().isOnline()) {
            target.getPlayer().sendMessage(
                    "§aUn administrateur a fixé votre réputation auprès de §b" + trader.getNameId() + "§a à §f" + value + "§a.");
        }

        return Command.SINGLE_SUCCESS;
    }

    // ── /reputation info ──────────────────────────────────────────────────────

    private static int executeInfo(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer target   = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);
        Trader      trader   = TraderArgument.getTrader(ctx, traderArgument);

        int rep = target.getReputation(trader.getNameId());

        Component msg = Component.text("Réputation de ", NamedTextColor.GOLD)
                .append(Component.text(target.getPseudo(), NamedTextColor.YELLOW))
                .append(Component.text(" auprès de ", NamedTextColor.GOLD))
                .append(Component.text(trader.getNameId(), NamedTextColor.AQUA))
                .append(Component.text(" : ", NamedTextColor.GOLD))
                .append(Component.text(rep, NamedTextColor.WHITE));

        // Si ce trader est lié à un métier, afficher le niveau
        if (trader.getJob() != null) {
            var job = trader.getJob();
            int level = target.getJobLevel(job);
            int nextThresh = JobLevelConfig.getNextThreshold(rep);
            String progress = nextThresh >= 0
                    ? " (" + rep + "/" + nextThresh + " pour niv." + (level + 1) + ")"
                    : " (niveau max)";

            msg = msg
                    .appendNewline()
                    .append(Component.text("  → Métier : ", NamedTextColor.GRAY))
                    .append(job.toComponent())
                    .append(Component.text(" — niv." + level + " " + JobLevelConfig.getLevelName(level), NamedTextColor.YELLOW))
                    .append(Component.text(progress, NamedTextColor.DARK_GRAY));
        }

        ctx.getSource().getSender().sendMessage(msg);
        return Command.SINGLE_SUCCESS;
    }
}