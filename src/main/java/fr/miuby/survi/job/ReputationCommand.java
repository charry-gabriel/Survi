package fr.miuby.survi.job;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.command.argument.AlphaPlayerArgument;
import fr.miuby.survi.system.command.argument.JobArgument;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Commande admin pour modifier manuellement la réputation d'un joueur
 * auprès d'un métier donné.
 *
 * Plusieurs traders peuvent partager le même métier : cette commande
 * agit directement sur la réputation du métier, indépendamment des traders.
 *
 * Usage :
 *   /reputation add    <joueur> <métier> <montant>
 *   /reputation remove <joueur> <métier> <montant>
 *   /reputation set    <joueur> <métier> <valeur>
 *   /reputation info   <joueur> <métier>
 */
@SuppressWarnings({"java:S3516", "SameReturnValue"})
public class ReputationCommand {
    private static final String AMOUNT_ARG = "amount";
    private static final String PLAYER_ARG = "player";
    private static final String JOB_ARG    = "job";

    private ReputationCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> createReputationCommand() {
        return Commands.literal("reputation")
                .requires(source -> source.getSender().isOp())

                // /reputation add <player> <job> <amount>
                .then(Commands.literal("add")
                        .then(Commands.argument(PLAYER_ARG, AlphaPlayerArgument.alphaPlayer())
                                .then(Commands.argument(JOB_ARG, JobArgument.job())
                                        .then(Commands.argument(AMOUNT_ARG, IntegerArgumentType.integer(1, 10000))
                                                .executes(ctx -> executeAdd(ctx, true))
                                        )
                                )
                        )
                )

                // /reputation remove <player> <job> <amount>
                .then(Commands.literal("remove")
                        .then(Commands.argument(PLAYER_ARG, AlphaPlayerArgument.alphaPlayer())
                                .then(Commands.argument(JOB_ARG, JobArgument.job())
                                        .then(Commands.argument(AMOUNT_ARG, IntegerArgumentType.integer(1, 10000))
                                                .executes(ctx -> executeAdd(ctx, false))
                                        )
                                )
                        )
                )

                // /reputation set <player> <job> <value>
                .then(Commands.literal("set")
                        .then(Commands.argument(PLAYER_ARG, AlphaPlayerArgument.alphaPlayer())
                                .then(Commands.argument(JOB_ARG, JobArgument.job())
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100000))
                                                .executes(ReputationCommand::executeSet)
                                        )
                                )
                        )
                )

                // /reputation info <player> <job>
                .then(Commands.literal("info")
                        .executes(ReputationCommand::executeSelf)
                        .then(Commands.argument(PLAYER_ARG, AlphaPlayerArgument.alphaPlayer())
                                .requires(source -> source.getSender().isOp())
                                .executes(ReputationCommand::executeOther)
                        )
                );
    }

    private static int executeAdd(CommandContext<CommandSourceStack> ctx, boolean isAdd) {
        AlphaPlayer target = AlphaPlayerArgument.getAlphaPlayer(ctx, PLAYER_ARG);
        EJob        job    = JobArgument.getJob(ctx, JOB_ARG);
        int         amount = IntegerArgumentType.getInteger(ctx, AMOUNT_ARG);

        int before = target.getJobReputation(job);
        int delta  = isAdd ? amount : -amount;

        target.addJobReputation(job, delta);
        int after = target.getJobReputation(job);
        int actualDelta = after - before;

        if (actualDelta == 0 && !isAdd) {
            ctx.getSource().getSender().sendMessage(
                    Component.text(target.getPseudo() + " est déjà à 0 réputation en ")
                            .color(NamedTextColor.YELLOW)
                            .append(job.toComponent())
                            .append(Component.text(".", NamedTextColor.YELLOW))
            );
            return Command.SINGLE_SUCCESS;
        }

        String verb = isAdd ? "Ajouté" : "Retiré";
        NamedTextColor color = isAdd ? NamedTextColor.GREEN : NamedTextColor.GOLD;

        ctx.getSource().getSender().sendMessage(
                Component.text(verb + " ", color)
                        .append(Component.text(Math.abs(actualDelta) + " rep", NamedTextColor.WHITE))
                        .append(Component.text(" à ", color))
                        .append(Component.text(target.getPseudo(), NamedTextColor.YELLOW))
                        .append(Component.text(" en métier ", color))
                        .append(job.toComponent())
                        .append(Component.text(" (" + before + " → " + after + ")", NamedTextColor.GRAY))
        );

        if (target.getPlayer() != null && target.getPlayer().isOnline()) {
            target.getPlayer().sendMessage(
                    Component.text(isAdd ? "✦ Un administrateur vous a accordé " : "✦ Un administrateur vous a retiré ")
                            .color(isAdd ? NamedTextColor.GREEN : NamedTextColor.GOLD)
                            .append(Component.text(Math.abs(actualDelta) + " rep", NamedTextColor.WHITE))
                            .append(Component.text(" en métier ", isAdd ? NamedTextColor.GREEN : NamedTextColor.GOLD))
                            .append(job.toComponent())
                            .append(Component.text(".", isAdd ? NamedTextColor.GREEN : NamedTextColor.GOLD))
            );
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int executeSet(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer target = AlphaPlayerArgument.getAlphaPlayer(ctx, PLAYER_ARG);
        EJob        job    = JobArgument.getJob(ctx, JOB_ARG);
        int         value  = IntegerArgumentType.getInteger(ctx, "value");

        int before = target.getJobReputation(job);
        int delta  = value - before;

        target.addJobReputation(job, delta);

        ctx.getSource().getSender().sendMessage(
                Component.text("Réputation de ", NamedTextColor.GREEN)
                        .append(Component.text(target.getPseudo(), NamedTextColor.YELLOW))
                        .append(Component.text(" en métier ", NamedTextColor.GREEN))
                        .append(job.toComponent())
                        .append(Component.text(" fixée à ", NamedTextColor.GREEN))
                        .append(Component.text(value, NamedTextColor.WHITE))
                        .append(Component.text(" (était " + before + ")", NamedTextColor.GRAY))
        );

        if (target.getPlayer() != null && target.getPlayer().isOnline()) {
            target.getPlayer().sendMessage(
                    Component.text("✦ Un administrateur a fixé votre réputation en métier ", NamedTextColor.GREEN)
                            .append(job.toComponent())
                            .append(Component.text(" à " + value + ".", NamedTextColor.GREEN))
            );
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int executeSelf(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Seul un joueur peut utiliser cette commande.").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        AlphaPlayer alphaPlayer = AlphaPlayer.get(player.getUniqueId());
        if (alphaPlayer == null) {
            sender.sendMessage(Component.text("Joueur non trouvé.").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        sendJobDisplay(player, alphaPlayer, true);
        return Command.SINGLE_SUCCESS;
    }

    private static int executeOther(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer target = AlphaPlayerArgument.getAlphaPlayer(ctx, PLAYER_ARG);
        CommandSender sender = ctx.getSource().getSender();

        Player senderPlayer = (sender instanceof Player p) ? p : null;
        sendJobDisplay(senderPlayer, target, false);

        if (senderPlayer == null) {
            // Appelé depuis la console : afficher en texte brut
            sender.sendMessage("=== Métiers de " + target.getPseudo() + " ===");
            for (EJob job : EJob.values()) {
                int level = target.getJobLevel(job);
                int rep = target.getJobReputation(job);
                sender.sendMessage(String.format("  %-15s niv.%d — %s  (rep: %d)",
                        job.getDisplayName(), level, JobLevelConfig.getLevelName(level), rep));
            }
        }

        ctx.getSource().getSender().sendMessage(
                Component.text("Métiers de " + target.getPseudo() + " affichés.").color(NamedTextColor.GRAY));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Envoie l'affichage complet des métiers au joueur.
     *
     * @param recipient joueur qui reçoit le message (peut être null si console)
     * @param subject   joueur dont on affiche les métiers
     * @param isSelf    true si recipient et subject sont le même joueur
     */
    private static void sendJobDisplay(Player recipient, AlphaPlayer subject, boolean isSelf) {
        if (recipient == null) return;

        String title = isSelf ? "✦ Vos Métiers" : "✦ Métiers de " + subject.getPseudo();
        recipient.sendMessage(Component.text("────────────────────────").color(NamedTextColor.DARK_GRAY));
        recipient.sendMessage(Component.text("  " + title).color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true));
        recipient.sendMessage(Component.text("────────────────────────").color(NamedTextColor.DARK_GRAY));

        for (EJob job : EJob.values()) {
            int level = subject.getJobLevel(job);
            int rep = subject.getJobReputation(job);
            String levelName = JobLevelConfig.getLevelName(level);
            int nextThresh = JobLevelConfig.getNextThreshold(rep);
            String progress = nextThresh >= 0 ? rep + "/" + nextThresh : rep + " (MAX)";

            TextComponent line = Component.text("  ")
                    .append(job.toComponent().decoration(TextDecoration.BOLD, false))
                    .append(Component.text(" — ").color(NamedTextColor.DARK_GRAY))
                    .append(Component.text("niv." + level + " ").color(NamedTextColor.WHITE))
                    .append(Component.text(levelName).color(NamedTextColor.YELLOW))
                    .append(Component.text("  " + progress + " rep").color(NamedTextColor.GRAY));

            recipient.sendMessage(line);
        }

        recipient.sendMessage(Component.text("────────────────────────").color(NamedTextColor.DARK_GRAY));
        if (isSelf) {
            recipient.sendMessage(Component.text("  Complétez des quêtes auprès des marchands").color(NamedTextColor.GRAY));
            recipient.sendMessage(Component.text("  pour progresser dans chaque métier.").color(NamedTextColor.GRAY));
        }
    }
}
