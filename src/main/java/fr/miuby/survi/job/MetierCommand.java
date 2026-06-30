package fr.miuby.survi.job;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.command.argument.JobArgument;
import fr.miuby.survi.system.database.repository.QuestRepository.ReputationRankEntry;
import fr.miuby.survi.system.lang.ELang;
import fr.miuby.survi.system.lang.LangService;
import fr.miuby.survi.system.log.ELogTag;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.logging.Level;

/**
 * Commande publique (accessible aux non-OP) — classement des joueurs pour un métier donné.
 * Raccourci de {@code /player top job <métier>} (réservé OP, voir {@link fr.miuby.survi.system.command.PlayerCommand}).
 * Ne pas confondre avec {@link JobCommand} (admin, gestion de réputation, OP uniquement).
 */
@SuppressWarnings({"java:S3516", "SameReturnValue"})
public class MetierCommand {

    private static final String JOB_ARG   = "job";

    private MetierCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("metier")
                .then(Commands.argument(JOB_ARG, JobArgument.job())
                        .executes(MetierCommand::topJob)
                );
    }

    private static int topJob(CommandContext<CommandSourceStack> ctx) {
        EJob          job    = JobArgument.getJob(ctx, JOB_ARG);
        CommandSender sender = ctx.getSource().getSender();
        LangService   ls     = GameManager.getInstance().getLangService();
        ELang         lang   = sender instanceof Player p ? ls.resolveLanguage(p) : ls.getServerDefault();

        List<ReputationRankEntry> top = GameManager.getInstance().getDatabase().quests().getTopByJob(job.name(), 10);

        MLLogManager.getInstance().log(Level.FINE, ELogTag.JOB,
                "[Metier] demandé par " + sender.getName() + " job=" + job.name() + " résultats=" + top.size());

        if (top.isEmpty()) {
            sender.sendMessage(ls.text(lang, "cmd.player.no_data"));
            return Command.SINGLE_SUCCESS;
        }

        String[]         medals = {"⚜ ", "✦ ", "● "};
        NamedTextColor[] colors = {NamedTextColor.GOLD, NamedTextColor.GRAY, NamedTextColor.DARK_AQUA};
        Component         sep   = ls.text(lang, "cmd.player.separator");

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
}