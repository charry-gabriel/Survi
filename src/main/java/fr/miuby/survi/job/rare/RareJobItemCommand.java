package fr.miuby.survi.job.rare;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.miuby.lib.MiubyLib;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.command.argument.AlphaPlayerArgument;
import fr.miuby.survi.system.command.argument.JobArgument;
import fr.miuby.survi.system.log.ELogTag;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/**
 * /rare <player>             — vue d'ensemble de tous les métiers
 * /rare <player> <job>       — détail d'un métier (seuil, actions effectives, chance exacte)
 * /rare reset <player> <job> — remet à zéro le compteur et l'état has_item (admin)
 *
 * Fonctionne pour les joueurs connectés (données en mémoire) et hors ligne (lecture DB async).
 */
public class RareJobItemCommand {

    private static final String PLAYER_ARG = "player";
    private static final String JOB_ARG    = "job";
    private static final Locale FR         = Locale.FRANCE;

    private RareJobItemCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("rare")
                .requires(source -> source.getSender().isOp())

                .then(Commands.literal("reset")
                        .then(Commands.argument(PLAYER_ARG, AlphaPlayerArgument.alphaPlayer())
                                .then(Commands.argument(JOB_ARG, JobArgument.job())
                                        .executes(ctx -> {
                                            executeReset(ctx.getSource().getSender(),
                                                    AlphaPlayerArgument.getAlphaPlayer(ctx, PLAYER_ARG),
                                                    JobArgument.getJob(ctx, JOB_ARG));
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                )

                .then(Commands.argument(PLAYER_ARG, AlphaPlayerArgument.alphaPlayer())
                        .executes(ctx -> {
                            executeOverview(ctx.getSource().getSender(),
                                    AlphaPlayerArgument.getAlphaPlayer(ctx, PLAYER_ARG));
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument(JOB_ARG, JobArgument.job())
                                .executes(ctx -> {
                                    executeDetail(ctx.getSource().getSender(),
                                            AlphaPlayerArgument.getAlphaPlayer(ctx, PLAYER_ARG),
                                            JobArgument.getJob(ctx, JOB_ARG));
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                );
    }

    // ─── Handlers ────────────────────────────────────────────────────────────────

    private static void executeOverview(CommandSender sender, AlphaPlayer target) {
        RareJobItemService svc = GameManager.getInstance().getRareJobItemService();
        if (svc == null) { sender.sendMessage(Component.text("Service non initialisé.", NamedTextColor.RED)); return; }

        Map<EJob, long[]> snapshot = svc.getMemorySnapshot(target.getUuid());
        if (snapshot != null) {
            sendOverview(sender, target.getPseudo(), false, snapshot);
        } else {
            sender.sendMessage(Component.text("Chargement depuis la DB…", NamedTextColor.DARK_GRAY));
            MiubyLib.runAsync(() -> {
                Map<EJob, long[]> dbData = GameManager.getInstance().getDatabase().rareJobItems().loadPlayer(target.getUuid());
                for (EJob job : EJob.values()) dbData.computeIfAbsent(job, k -> new long[]{0L, 0L});
                sendOverview(sender, target.getPseudo(), true, dbData);
            });
        }
    }

    private static void executeDetail(CommandSender sender, AlphaPlayer target, EJob job) {
        RareJobItemService svc = GameManager.getInstance().getRareJobItemService();
        if (svc == null) { sender.sendMessage(Component.text("Service non initialisé.", NamedTextColor.RED)); return; }

        Map<EJob, long[]> snapshot = svc.getMemorySnapshot(target.getUuid());
        if (snapshot != null) {
            sendDetail(sender, target.getPseudo(), false, job, snapshot.get(job));
        } else {
            sender.sendMessage(Component.text("Chargement depuis la DB…", NamedTextColor.DARK_GRAY));
            MiubyLib.runAsync(() -> {
                Map<EJob, long[]> dbData = GameManager.getInstance().getDatabase().rareJobItems().loadPlayer(target.getUuid());
                sendDetail(sender, target.getPseudo(), true, job, dbData.get(job));
            });
        }
    }

    private static void executeReset(CommandSender sender, AlphaPlayer target, EJob job) {
        RareJobItemService svc = GameManager.getInstance().getRareJobItemService();
        if (svc == null) { sender.sendMessage(Component.text("Service non initialisé.", NamedTextColor.RED)); return; }

        svc.resetJobData(target.getUuid(), job);
        sender.sendMessage(
                Component.text("[RareItem] ", NamedTextColor.GOLD)
                        .append(job.toComponent().decoration(TextDecoration.BOLD, false))
                        .append(Component.text(" de ", NamedTextColor.GRAY))
                        .append(Component.text(target.getPseudo(), NamedTextColor.WHITE))
                        .append(Component.text(" remis à zéro.", NamedTextColor.GRAY))
        );
        MLLogManager.getInstance().log(Level.INFO, ELogTag.ITEM,
                "[RareJobItem] Reset par commande : " + target.getPseudo() + " / " + job.name()
                        + " (opérateur : " + sender.getName() + ")");
    }

    // ─── Affichage ───────────────────────────────────────────────────────────────

    private static void sendOverview(CommandSender sender, String pseudo, boolean offline, Map<EJob, long[]> data) {
        sender.sendMessage(sep());
        sender.sendMessage(
                Component.text("  Objets rares — ", NamedTextColor.GOLD)
                        .append(Component.text(pseudo, NamedTextColor.WHITE))
                        .append(offline ? Component.text(" (hors ligne)", NamedTextColor.DARK_GRAY) : Component.empty())
        );
        sender.sendMessage(sep());
        for (EJob job : EJob.values()) {
            long[]  jd          = data.getOrDefault(job, new long[]{0L, 0L});
            long    actionCount = jd[0];
            boolean hasItem     = jd[1] == 1L;
            double  chance      = RareJobItemService.computeChance(job, actionCount);

            sender.sendMessage(
                    Component.text("  ")
                            .append(job.toComponent().decoration(TextDecoration.BOLD, false))
                            .append(Component.text("  " + fmt(actionCount) + " actions", NamedTextColor.WHITE))
                            .append(Component.text("  |  ", NamedTextColor.DARK_GRAY))
                            .append(Component.text(String.format(FR, "%.4f%%", chance * 100),
                                    chance > 0 ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY))
                            .append(Component.text("  |  ", NamedTextColor.DARK_GRAY))
                            .append(hasItem
                                    ? Component.text("✓ obtenu", NamedTextColor.GREEN)
                                    : Component.text("non obtenu", NamedTextColor.GRAY))
            );
        }
        sender.sendMessage(sep());
    }

    private static void sendDetail(CommandSender sender, String pseudo, boolean offline, EJob job, long[] jd) {
        long    actionCount = jd != null ? jd[0] : 0L;
        boolean hasItem     = jd != null && jd[1] == 1L;
        long    threshold   = RareJobItemService.getThreshold(job);
        long    effective   = Math.max(0L, actionCount - threshold);
        double  chance      = RareJobItemService.computeChance(job, actionCount);

        sender.sendMessage(sep());
        sender.sendMessage(
                Component.text("  ")
                        .append(job.toComponent().decoration(TextDecoration.BOLD, false))
                        .append(Component.text(" — " + pseudo, NamedTextColor.WHITE))
                        .append(offline ? Component.text(" (hors ligne)", NamedTextColor.DARK_GRAY) : Component.empty())
        );
        sender.sendMessage(sep());
        sender.sendMessage(row("Actions totales",    Component.text(fmt(actionCount),                                                 NamedTextColor.WHITE)));
        sender.sendMessage(row("Seuil",              Component.text(fmt(threshold),                                                   NamedTextColor.WHITE)));
        sender.sendMessage(row("Actions effectives", Component.text(fmt(effective),                                                   NamedTextColor.WHITE)));
        sender.sendMessage(row("Chance actuelle",    Component.text(String.format(FR, "%.6f%%", chance * 100),
                chance > 0 ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY)));
        sender.sendMessage(row("Objet obtenu",       hasItem
                ? Component.text("oui", NamedTextColor.GREEN)
                : Component.text("non", NamedTextColor.GRAY)));
        sender.sendMessage(sep());
    }

    private static Component row(String label, Component value) {
        return Component.text("  " + label, NamedTextColor.GRAY)
                .append(Component.text(" : ", NamedTextColor.DARK_GRAY))
                .append(value);
    }

    private static Component sep() {
        return Component.text("─────────────────────────────────────", NamedTextColor.DARK_GRAY);
    }

    private static String fmt(long n) {
        return String.format(FR, "%,d", n);
    }
}