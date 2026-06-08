package fr.miuby.survi.system.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.JobLevelConfig;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.command.argument.AlphaPlayerArgument;
import fr.miuby.survi.system.command.argument.JobArgument;
import fr.miuby.survi.system.database.repository.QuestHistoryRepository.PlayerRankEntry;
import fr.miuby.survi.system.database.repository.QuestRepository.ReputationRankEntry;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Commandes de stats et classements joueurs.
 *
 * <pre>
 *   /player info [joueur]           — profil complet (soi ou autre si op)
 *   /player top quests [N]          — classement par quêtes complétées
 *   /player top reputation [N]      — classement par réputation totale
 *   /player top job &lt;métier&gt; [N]   — classement par métier spécifique
 * </pre>
 */
@SuppressWarnings({"java:S3516", "SameReturnValue"})
public class PlayerCommand {

    private static final String PLAYER_ARG = "player";
    private static final String JOB_ARG    = "job";
    private static final String LIMIT_ARG  = "limit";

    private PlayerCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("player")

                // /player info [joueur]
                .then(Commands.literal("info")
                        .executes(PlayerCommand::infoSelf)
                        .then(Commands.argument(PLAYER_ARG, AlphaPlayerArgument.alphaPlayer())
                                .requires(src -> src.getSender().isOp())
                                .executes(PlayerCommand::infoOther)
                        )
                )

                // /player top ...
                .then(Commands.literal("top")

                        // /player top quests [N]
                        .then(Commands.literal("quests")
                                .executes(ctx -> topQuests(ctx, 10))
                                .then(Commands.argument(LIMIT_ARG, IntegerArgumentType.integer(1, 50))
                                        .executes(ctx -> topQuests(ctx, IntegerArgumentType.getInteger(ctx, LIMIT_ARG)))
                                )
                        )

                        // /player top reputation [N]
                        .then(Commands.literal("reputation")
                                .executes(ctx -> topReputation(ctx, 10))
                                .then(Commands.argument(LIMIT_ARG, IntegerArgumentType.integer(1, 50))
                                        .executes(ctx -> topReputation(ctx, IntegerArgumentType.getInteger(ctx, LIMIT_ARG)))
                                )
                        )

                        // /player top job <métier> [N]
                        .then(Commands.literal("job")
                                .then(Commands.argument(JOB_ARG, JobArgument.job())
                                        .executes(ctx -> topJob(ctx, 10))
                                        .then(Commands.argument(LIMIT_ARG, IntegerArgumentType.integer(1, 50))
                                                .executes(ctx -> topJob(ctx, IntegerArgumentType.getInteger(ctx, LIMIT_ARG)))
                                        )
                                )
                        )
                );
    }

    // =========================================================================
    // /player info
    // =========================================================================

    private static int infoSelf(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Cette commande est réservée aux joueurs en jeu. Utilisez /player info <joueur> depuis la console.").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        displayPlayerInfo(sender, AlphaPlayer.get(player.getUniqueId()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int infoOther(CommandContext<CommandSourceStack> ctx) {
        displayPlayerInfo(ctx.getSource().getSender(), AlphaPlayerArgument.getAlphaPlayer(ctx, PLAYER_ARG), false);
        return Command.SINGLE_SUCCESS;
    }

    private static void displayPlayerInfo(CommandSender sender, AlphaPlayer ap, boolean isSelf) {
        String title = isSelf ? "✦ Votre Profil" : "✦ Profil de " + ap.getPseudo();

        int totalQuests  = GameManager.getInstance().getDatabase().questHistory().countCompleted(ap.getUuid());
        Map<Integer, Integer> byDiff = GameManager.getInstance().getDatabase().questHistory().countByDifficulty(ap.getUuid());
        Map<String, Integer>  byType = GameManager.getInstance().getDatabase().questHistory().countByType(ap.getUuid());
        int dailyCount  = byType.getOrDefault("daily", 0);
        int globalCount = byType.getOrDefault("global", 0);

        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  " + title).color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GOLD));

        // Rang & stats générales
        sender.sendMessage(
                Component.text("  Rang : ", NamedTextColor.GRAY)
                        .append(ap.getGlobalRank().displayComponent())
                        .append(Component.text("    Morts : ", NamedTextColor.GRAY))
                        .append(Component.text(String.valueOf(ap.getMort()), NamedTextColor.RED))
                        .append(Component.text("    Succès : ", NamedTextColor.GRAY))
                        .append(Component.text(String.valueOf(ap.getSuccess()), NamedTextColor.GREEN))
        );

        // Quêtes
        sender.sendMessage(
                Component.text("  Quêtes : ", NamedTextColor.GRAY)
                        .append(Component.text(totalQuests + " complétée(s)", NamedTextColor.AQUA))
                        .append(Component.text("  (", NamedTextColor.DARK_GRAY))
                        .append(Component.text(dailyCount + " journalières", NamedTextColor.YELLOW))
                        .append(Component.text(" · ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(globalCount + " globales", NamedTextColor.LIGHT_PURPLE))
                        .append(Component.text(")", NamedTextColor.DARK_GRAY))
        );

        if (!byDiff.isEmpty()) {
            Component diffLine = Component.text("  Par difficulté : ", NamedTextColor.GRAY);
            boolean first = true;
            for (Map.Entry<Integer, Integer> entry : byDiff.entrySet()) {
                if (!first) diffLine = diffLine.append(Component.text("  ", NamedTextColor.DARK_GRAY));
                diffLine = diffLine
                        .append(Component.text("diff." + entry.getKey(), NamedTextColor.WHITE))
                        .append(Component.text(" → " + entry.getValue(), NamedTextColor.DARK_GRAY));
                first = false;
            }
            sender.sendMessage(diffLine);
        }

        // Métiers
        sender.sendMessage(Component.text("  ─ Métiers ─", NamedTextColor.DARK_GRAY));
        for (EJob job : EJob.values()) {
            int level = ap.getJobLevel(job);
            TextComponent line = Component.text("    ")
                    .append(job.toComponent())
                    .append(Component.text(" niv." + level, NamedTextColor.WHITE));
            sender.sendMessage(line);
        }

        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GOLD));
    }

    // =========================================================================
    // /player top
    // =========================================================================

    private static int topQuests(CommandContext<CommandSourceStack> ctx, int limit) {
        List<PlayerRankEntry> top = GameManager.getInstance().getDatabase().questHistory().getTopByCompletions(limit);
        sendLeaderboard(ctx.getSource().getSender(), "Classement — Quêtes complétées",
                top.stream().map(e -> Map.entry(e.pseudo(), e.count())).toList(), "quêtes");
        return Command.SINGLE_SUCCESS;
    }

    private static int topReputation(CommandContext<CommandSourceStack> ctx, int limit) {
        List<ReputationRankEntry> top = GameManager.getInstance().getDatabase().quests().getTopByTotalReputation(limit);
        sendLeaderboard(ctx.getSource().getSender(), "Classement — Réputation totale",
                top.stream().map(e -> Map.entry(e.pseudo(), e.value())).toList(), "rep");
        return Command.SINGLE_SUCCESS;
    }

    private static int topJob(CommandContext<CommandSourceStack> ctx, int limit) {
        EJob job = JobArgument.getJob(ctx, JOB_ARG);
        List<ReputationRankEntry> top = GameManager.getInstance().getDatabase().quests().getTopByJob(job.name(), limit);

        CommandSender sender = ctx.getSource().getSender();
        if (top.isEmpty()) {
            sender.sendMessage(Component.text("Aucune donnée disponible pour ce classement.").color(NamedTextColor.GRAY));
            return Command.SINGLE_SUCCESS;
        }

        String[] medals = {"⚜ ", "✦ ", "● "};
        NamedTextColor[] colors = {NamedTextColor.GOLD, NamedTextColor.GRAY, NamedTextColor.DARK_AQUA};

        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  Classement — " + job.getDisplayName()).color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GOLD));

        for (int i = 0; i < top.size(); i++) {
            String medal = i < medals.length ? medals[i] : (i + 1) + ". ";
            NamedTextColor color = i < colors.length ? colors[i] : NamedTextColor.WHITE;
            ReputationRankEntry e = top.get(i);
            int level = JobLevelConfig.computeLevel((int) e.value());
            sender.sendMessage(
                    Component.text("  " + medal, color)
                            .append(Component.text(e.pseudo(), NamedTextColor.WHITE))
                            .append(Component.text("  niv." + level, NamedTextColor.DARK_GRAY))
            );
        }

        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GOLD));
        return Command.SINGLE_SUCCESS;
    }

    private static void sendLeaderboard(CommandSender sender, String title, List<Map.Entry<String, Long>> entries, String unit) {
        if (entries.isEmpty()) {
            sender.sendMessage(Component.text("Aucune donnée disponible pour ce classement.").color(NamedTextColor.GRAY));
            return;
        }

        String[]         medals = {"⚜ ", "✦ ", "● "};
        NamedTextColor[] colors = {NamedTextColor.GOLD, NamedTextColor.GRAY, NamedTextColor.DARK_AQUA};

        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  " + title).color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GOLD));

        for (int i = 0; i < entries.size(); i++) {
            String         medal = i < medals.length ? medals[i] : (i + 1) + ". ";
            NamedTextColor color = i < colors.length ? colors[i] : NamedTextColor.WHITE;
            Map.Entry<String, Long> e = entries.get(i);
            sender.sendMessage(
                    Component.text("  " + medal, color)
                            .append(Component.text(e.getKey(), NamedTextColor.WHITE))
                            .append(Component.text("  " + e.getValue() + " " + unit, NamedTextColor.DARK_GRAY))
            );
        }

        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GOLD));
    }
}
