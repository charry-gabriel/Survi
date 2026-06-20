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
import fr.miuby.survi.system.lang.ELang;
import fr.miuby.survi.system.lang.LangService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

@SuppressWarnings({"java:S3516", "SameReturnValue"})
public class PlayerCommand {

    private static final String PLAYER_ARG = "player";
    private static final String JOB_ARG    = "job";
    private static final String LIMIT_ARG  = "limit";
    private static final String AMOUNT_ARG = "amount";

    private PlayerCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("player")

                .then(Commands.literal("info")
                        .executes(PlayerCommand::infoSelf)
                        .then(Commands.argument(PLAYER_ARG, AlphaPlayerArgument.alphaPlayer())
                                .requires(src -> src.getSender().isOp())
                                .executes(PlayerCommand::infoOther)
                        )
                )

                .then(Commands.literal("top")

                        .then(Commands.literal("quests")
                                .executes(ctx -> topQuests(ctx, 10))
                                .then(Commands.argument(LIMIT_ARG, IntegerArgumentType.integer(1, 50))
                                        .executes(ctx -> topQuests(ctx, IntegerArgumentType.getInteger(ctx, LIMIT_ARG)))
                                )
                        )

                        .then(Commands.literal("job")
                                .then(Commands.argument(JOB_ARG, JobArgument.job())
                                        .executes(ctx -> topJob(ctx, 10))
                                        .then(Commands.argument(LIMIT_ARG, IntegerArgumentType.integer(1, 50))
                                                .executes(ctx -> topJob(ctx, IntegerArgumentType.getInteger(ctx, LIMIT_ARG)))
                                        )
                                )
                        )

                        .then(Commands.literal("trades")
                                .executes(ctx -> topTrades(ctx, 10))
                                .then(Commands.argument(LIMIT_ARG, IntegerArgumentType.integer(1, 50))
                                        .executes(ctx -> topTrades(ctx, IntegerArgumentType.getInteger(ctx, LIMIT_ARG)))
                                )
                        )
                )

                .then(Commands.literal("death")
                        .requires(src -> src.getSender().isOp())
                        .then(Commands.argument(PLAYER_ARG, AlphaPlayerArgument.alphaPlayer())

                                .then(Commands.literal("add")
                                        .then(Commands.argument(AMOUNT_ARG, IntegerArgumentType.integer(1, 10000))
                                                .executes(ctx -> executeDeath(ctx, true))
                                        )
                                )
                                .then(Commands.literal("get")
                                        .then(Commands.argument(AMOUNT_ARG, IntegerArgumentType.integer(1, 10000))
                                                .executes(ctx -> {
                                                    AlphaPlayer target = AlphaPlayerArgument.getAlphaPlayer(ctx, PLAYER_ARG);
                                                    CommandSender sender = ctx.getSource().getSender();
                                                    sender.sendMessage(target.getPseudo() + " a " + target.getDeath() + " mort");
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                                .then(Commands.literal("remove")
                                        .then(Commands.argument(AMOUNT_ARG, IntegerArgumentType.integer(1, 10000))
                                                .executes(ctx -> executeDeath(ctx, false))
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
            LangService ls   = GameManager.getInstance().getLangService();
            sender.sendMessage(ls.text(ls.getServerDefault(), "cmd.player.console_only"));
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
        LangService ls   = GameManager.getInstance().getLangService();
        ELang       lang = sender instanceof Player p ? ls.resolveLanguage(p) : ls.getServerDefault();

        int totalQuests  = GameManager.getInstance().getDatabase().questHistory().countCompleted(ap.getUuid());
        Map<Integer, Integer> byDiff = GameManager.getInstance().getDatabase().questHistory().countByDifficulty(ap.getUuid());
        Map<String, Integer>  byType = GameManager.getInstance().getDatabase().questHistory().countByType(ap.getUuid());
        int dailyCount  = byType.getOrDefault("daily", 0);
        int globalCount = byType.getOrDefault("global", 0);

        Component sep = ls.text(lang, "cmd.player.separator");
        sender.sendMessage(sep);
        sender.sendMessage(isSelf ? ls.text(lang, "cmd.player.info.title_self") : ls.text(lang, "cmd.player.info.title_other", ap.getPseudo()));
        sender.sendMessage(sep);

        // Rang & stats
        sender.sendMessage(
                ls.text(lang, "cmd.player.info.rank_label")
                        .append(ap.getGlobalRank().displayComponent())
                        .append(ls.text(lang, "cmd.player.info.deaths_label"))
                        .append(Component.text(String.valueOf(ap.getDeath()), NamedTextColor.RED))
                        .append(ls.text(lang, "cmd.player.info.success_label"))
                        .append(Component.text(String.valueOf(ap.getSuccess()), NamedTextColor.GREEN))
        );

        // Quêtes
        sender.sendMessage(ls.text(lang, "cmd.player.info.quests", totalQuests, dailyCount, globalCount));

        // Par difficulté
        if (!byDiff.isEmpty()) {
            Component diffLine = ls.text(lang, "cmd.player.info.difficulty_label");
            boolean first = true;
            for (Map.Entry<Integer, Integer> entry : byDiff.entrySet()) {
                if (!first) diffLine = diffLine.append(Component.text("  ", NamedTextColor.DARK_GRAY));
                diffLine = diffLine.append(ls.text(lang, "cmd.player.info.difficulty_entry", entry.getKey(), entry.getValue()));
                first = false;
            }
            sender.sendMessage(diffLine);
        }

        // Métiers
        sender.sendMessage(ls.text(lang, "cmd.player.info.jobs_header"));
        for (EJob job : EJob.values()) {
            int level = ap.getJobLevel(job);
            String key = ap.isJobMaxLevel(job) ? "cmd.player.info.job_entry_max" : "cmd.player.info.job_entry";
            sender.sendMessage(Component.text("    ")
                    .append(job.toComponent())
                    .append(ls.text(lang, key, level)));
        }

        // Achats marchands
        int totalTrades = GameManager.getInstance().getDatabase().tradeHistory().countTotal(ap.getUuid());
        if (totalTrades > 0) {
            sender.sendMessage(ls.text(lang, "cmd.player.info.trades", totalTrades));
            Map<String, Integer> byTrader = GameManager.getInstance().getDatabase().tradeHistory().countByTrader(ap.getUuid());
            if (!byTrader.isEmpty()) {
                Component tradeLine = ls.text(lang, "cmd.player.info.trades_by_trader_label");
                boolean first = true;
                for (Map.Entry<String, Integer> entry : byTrader.entrySet()) {
                    if (!first) tradeLine = tradeLine.append(Component.text("  ", NamedTextColor.DARK_GRAY));
                    tradeLine = tradeLine.append(ls.text(lang, "cmd.player.info.trades_by_trader_entry", entry.getKey(), entry.getValue()));
                    first = false;
                }
                sender.sendMessage(tradeLine);
            }
        }

        sender.sendMessage(sep);
    }

    // =========================================================================
    // /player death
    // =========================================================================

    private static int executeDeath(CommandContext<CommandSourceStack> ctx, boolean isAdd) {
        AlphaPlayer target = AlphaPlayerArgument.getAlphaPlayer(ctx, PLAYER_ARG);
        int         amount = IntegerArgumentType.getInteger(ctx, AMOUNT_ARG);

        int before = target.getDeath();
        target.addDeath(isAdd ? amount : -amount);
        int after       = target.getDeath();
        int actualDelta = after - before;

        CommandSender sender = ctx.getSource().getSender();
        LangService   ls     = GameManager.getInstance().getLangService();
        ELang         lang   = sender instanceof Player p ? ls.resolveLanguage(p) : ls.getServerDefault();

        if (actualDelta == 0 && !isAdd) {
            sender.sendMessage(ls.text(lang, "cmd.player.death.already_zero",
                    Placeholder.unparsed("player", target.getPseudo())));
            return Command.SINGLE_SUCCESS;
        }

        String cmdKey = isAdd ? "cmd.player.death.added" : "cmd.player.death.removed";
        sender.sendMessage(ls.text(lang, cmdKey,
                Placeholder.unparsed("amount", String.valueOf(Math.abs(actualDelta))),
                Placeholder.unparsed("player", target.getPseudo()),
                Placeholder.unparsed("before", String.valueOf(before)),
                Placeholder.unparsed("after", String.valueOf(after))));

        if (target.getPlayer() != null && target.getPlayer().isOnline()) {
            String notifyKey = isAdd ? "cmd.player.death.notify.added" : "cmd.player.death.notify.removed";
            target.getPlayer().sendMessage(ls.text(target.getPlayer(), notifyKey));
        }

        return Command.SINGLE_SUCCESS;
    }

    // =========================================================================
    // /player top
    // =========================================================================

    private static int topTrades(CommandContext<CommandSourceStack> ctx, int limit) {
        CommandSender sender = ctx.getSource().getSender();
        LangService   ls     = GameManager.getInstance().getLangService();
        ELang         lang   = sender instanceof Player p ? ls.resolveLanguage(p) : ls.getServerDefault();
        List<fr.miuby.survi.system.database.repository.TradeHistoryRepository.PlayerRankEntry> top =
                GameManager.getInstance().getDatabase().tradeHistory().getTopByPurchases(limit);
        sendLeaderboard(sender, ls, lang,
                ls.text(lang, "cmd.player.top.trades_title"),
                top.stream().map(e -> Map.entry(e.pseudo(), e.count())).toList(),
                ls.getString(lang, "cmd.player.top.unit_trades"),
                null);
        return Command.SINGLE_SUCCESS;
    }

    private static int topQuests(CommandContext<CommandSourceStack> ctx, int limit) {
        CommandSender sender = ctx.getSource().getSender();
        LangService   ls     = GameManager.getInstance().getLangService();
        ELang         lang   = sender instanceof Player p ? ls.resolveLanguage(p) : ls.getServerDefault();
        List<PlayerRankEntry> top = GameManager.getInstance().getDatabase().questHistory().getTopByCompletions(limit);
        sendLeaderboard(sender, ls, lang,
                ls.text(lang, "cmd.player.top.quests_title"),
                top.stream().map(e -> Map.entry(e.pseudo(), e.count())).toList(),
                ls.getString(lang, "cmd.player.top.unit_quests"),
                null);
        return Command.SINGLE_SUCCESS;
    }

    private static int topJob(CommandContext<CommandSourceStack> ctx, int limit) {
        EJob          job    = JobArgument.getJob(ctx, JOB_ARG);
        CommandSender sender = ctx.getSource().getSender();
        LangService   ls     = GameManager.getInstance().getLangService();
        ELang         lang   = sender instanceof Player p ? ls.resolveLanguage(p) : ls.getServerDefault();

        List<ReputationRankEntry> top = GameManager.getInstance().getDatabase().quests().getTopByJob(job.name(), limit);
        if (top.isEmpty()) {
            sender.sendMessage(ls.text(lang, "cmd.player.no_data"));
            return Command.SINGLE_SUCCESS;
        }

        String[]         medals = {"⚜ ", "✦ ", "● "};
        NamedTextColor[] colors = {NamedTextColor.GOLD, NamedTextColor.GRAY, NamedTextColor.DARK_AQUA};
        Component        sep    = ls.text(lang, "cmd.player.separator");

        sender.sendMessage(sep);
        sender.sendMessage(ls.text(lang, "cmd.player.top.job_title", job.getDisplayName()));
        sender.sendMessage(sep);

        for (int i = 0; i < top.size(); i++) {
            String         medal = i < medals.length ? medals[i] : (i + 1) + ". ";
            NamedTextColor color = i < colors.length ? colors[i] : NamedTextColor.WHITE;
            ReputationRankEntry e = top.get(i);
            int level = JobLevelConfig.computeLevel((int) e.value());
            sender.sendMessage(
                    Component.text("  " + medal, color)
                            .append(Component.text(e.pseudo(), NamedTextColor.WHITE))
                            .append(ls.text(lang, "cmd.player.top.entry_level", level))
            );
        }

        sender.sendMessage(sep);
        return Command.SINGLE_SUCCESS;
    }

    private static void sendLeaderboard(CommandSender sender, LangService ls, ELang lang,
                                        Component title, List<Map.Entry<String, Long>> entries,
                                        String unit, @SuppressWarnings("unused") Object unused) {
        if (entries.isEmpty()) {
            sender.sendMessage(ls.text(lang, "cmd.player.no_data"));
            return;
        }

        String[]         medals = {"⚜ ", "✦ ", "● "};
        NamedTextColor[] colors = {NamedTextColor.GOLD, NamedTextColor.GRAY, NamedTextColor.DARK_AQUA};
        Component        sep    = ls.text(lang, "cmd.player.separator");

        sender.sendMessage(sep);
        sender.sendMessage(title);
        sender.sendMessage(sep);

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

        sender.sendMessage(sep);
    }
}