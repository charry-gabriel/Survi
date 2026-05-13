package fr.miuby.survi.job;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.command.argument.AlphaPlayerArgument;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings({"java:S3516", "SameReturnValue"})
public class JobCommand {
    private JobCommand() {
        /* This utility class should not be instantiated */
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("job")
                // /job  — le joueur voit ses propres métiers
                .executes(JobCommand::executeSelf)
                // /job <player>  — op seulement
                .then(Commands.argument("player", AlphaPlayerArgument.alphaPlayer())
                        .requires(source -> source.getSender().isOp())
                        .executes(JobCommand::executeOther)
                );
    }

    // /job (sans argument)
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

    // /job <player> (op uniquement)
    private static int executeOther(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer target = AlphaPlayerArgument.getAlphaPlayer(ctx, "player");
        CommandSender sender = ctx.getSource().getSender();

        Player senderPlayer = (sender instanceof Player p) ? p : null;
        sendJobDisplay(senderPlayer != null ? senderPlayer : null, target, false);

        if (senderPlayer == null) {
            // Appelé depuis console : afficher en texte brut
            sender.sendMessage("=== Métiers de " + target.getPseudo() + " ===");
            for (EJob job : EJob.values()) {
                int level = target.getJobLevel(job);
                int rep = target.getReputation(
                        fr.miuby.survi.GameManager.getInstance().getVillagerFactory().getTraders().stream()
                                .filter(t -> job.equals(t.getJob()))
                                .findFirst()
                                .map(t -> t.getNameId())
                                .orElse("")
                );
                sender.sendMessage(String.format("  %-15s niv.%d — %s  (rep: %d)",
                        job.getDisplayName(), level, JobLevelConfig.getLevelName(level), rep));
            }
        }

        ctx.getSource().getSender().sendMessage(
                Component.text("Métiers de " + target.getPseudo() + " affichés.").color(NamedTextColor.GRAY));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Envoie l'affichage complet des métiers au joueur (ou à la cible si c'est un inspect op).
     *
     * @param recipient joueur qui reçoit le message (peut être null si console)
     * @param subject   joueur dont on affiche les métiers
     * @param isSelf    true si le recipient et le subject sont le même joueur
     */
    private static void sendJobDisplay(Player recipient, AlphaPlayer subject, boolean isSelf) {
        if (recipient == null) return;

        // ── En-tête ──────────────────────────────────────────────────────────
        String title = isSelf ? "✦ Vos Métiers" : "✦ Métiers de " + subject.getPseudo();
        recipient.sendMessage(
                Component.text("────────────────────────").color(NamedTextColor.DARK_GRAY)
        );
        recipient.sendMessage(
                Component.text("  " + title).color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true)
        );
        recipient.sendMessage(
                Component.text("────────────────────────").color(NamedTextColor.DARK_GRAY)
        );

        // ── Une ligne par métier ──────────────────────────────────────────────
        for (EJob job : EJob.values()) {
            int level = subject.getJobLevel(job);
            int rep = getRepForJob(subject, job);
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

        // ── Pied ─────────────────────────────────────────────────────────────
        recipient.sendMessage(
                Component.text("────────────────────────").color(NamedTextColor.DARK_GRAY)
        );
        if (isSelf) {
            recipient.sendMessage(
                    Component.text("  Complétez des quêtes auprès des marchands").color(NamedTextColor.GRAY)
            );
            recipient.sendMessage(
                    Component.text("  pour progresser dans chaque métier.").color(NamedTextColor.GRAY)
            );
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Récupère la réputation du joueur auprès du Trader lié à un métier.
     * Retourne 0 si aucun trader n'est associé.
     */
    private static int getRepForJob(AlphaPlayer player, EJob job) {
        return fr.miuby.survi.GameManager.getInstance().getVillagerFactory().getTraders().stream()
                .filter(t -> job.equals(t.getJob()))
                .findFirst()
                .map(t -> player.getReputation(t.getNameId()))
                .orElse(0);
    }
}