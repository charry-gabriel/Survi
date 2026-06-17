package fr.miuby.survi.system.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.miuby.lib.command.MLLogCommand;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.listener.PlacedBlockTracker;
import fr.miuby.survi.system.lang.ELang;
import fr.miuby.survi.system.lang.LangService;
import fr.miuby.survi.system.perf.PerfTimer;
import fr.miuby.survi.system.time.TimeManager;
import fr.miuby.survi.world.VillageZoneManager;
import fr.miuby.survi.world.config.VillageZoneConfig;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.format.DateTimeFormatter;

public class SystemCommand {
    private SystemCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("survi")
                .requires(sender -> sender.getSender().isOp())

                .then(MLLogCommand.create())
                .then(ReloadCommand.create())

                // === BLOCK TRACKER ===
                .then(Commands.literal("blocktracker")
                        .then(Commands.literal("on").executes(ctx -> {
                            PlacedBlockTracker.setEnabled(true);
                            ctx.getSource().getSender().sendMessage(ls(ctx).text(lang(ctx), "cmd.system.blocktracker.on"));
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(Commands.literal("off").executes(ctx -> {
                            PlacedBlockTracker.setEnabled(false);
                            ctx.getSource().getSender().sendMessage(ls(ctx).text(lang(ctx), "cmd.system.blocktracker.off"));
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(Commands.literal("status").executes(ctx -> {
                            boolean on = PlacedBlockTracker.isEnabled();
                            ctx.getSource().getSender().sendMessage(ls(ctx).text(lang(ctx), on ? "cmd.system.blocktracker.status_on" : "cmd.system.blocktracker.status_off"));
                            return Command.SINGLE_SUCCESS;
                        }))
                )

                // === PERF ===
                .then(Commands.literal("perf")
                        .then(Commands.literal("on").executes(ctx -> {
                            PerfTimer.setEnabled(true);
                            ctx.getSource().getSender().sendMessage(ls(ctx).text(lang(ctx), "cmd.system.perf.on"));
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(Commands.literal("off").executes(ctx -> {
                            PerfTimer.setEnabled(false);
                            ctx.getSource().getSender().sendMessage(ls(ctx).text(lang(ctx), "cmd.system.perf.off"));
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(Commands.literal("status").executes(ctx -> {
                            boolean on = PerfTimer.isEnabled();
                            ctx.getSource().getSender().sendMessage(ls(ctx).text(lang(ctx), on ? "cmd.system.perf.status_on" : "cmd.system.perf.status_off"));
                            return Command.SINGLE_SUCCESS;
                        }))
                )

                // === TIME ===
                .then(Commands.literal("time")
                        .then(Commands.literal("info").executes(ctx -> {
                            TimeManager tm = GameManager.getInstance().getTimeManager();
                            CommandSender sender = ctx.getSource().getSender();
                            LangService ls = ls(ctx);
                            ELang lang = lang(ctx);
                            if (tm == null) { sender.sendMessage(ls.text(lang, "cmd.system.time.not_init")); return Command.SINGLE_SUCCESS; }
                            sender.sendMessage(ls.text(lang, "cmd.system.time.header"));
                            sender.sendMessage(ls.text(lang, "cmd.system.time.timezone",   tm.getServerTimezone().getId()));
                            sender.sendMessage(ls.text(lang, "cmd.system.time.has_reset",  ls.getString(lang, tm.hasResetToday() ? "cmd.system.time.yes" : "cmd.system.time.no")));
                            sender.sendMessage(ls.text(lang, "cmd.system.time.last_reset", tm.getLastResetDay()));
                            sender.sendMessage(ls.text(lang, "cmd.system.time.next_reset", tm.getNextResetTime().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))));
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(Commands.literal("reset")
                                .requires(sender -> sender.getSender().hasPermission("survi.time.admin"))
                                .executes(ctx -> {
                                    TimeManager tm = GameManager.getInstance().getTimeManager();
                                    CommandSender sender = ctx.getSource().getSender();
                                    LangService ls = ls(ctx);
                                    ELang lang = lang(ctx);
                                    if (tm == null) { sender.sendMessage(ls.text(lang, "cmd.system.time.not_init")); return Command.SINGLE_SUCCESS; }
                                    sender.sendMessage(ls.text(lang, "cmd.system.time.force_reset"));
                                    tm.forceReset();
                                    sender.sendMessage(ls.text(lang, "cmd.system.time.reset_done"));
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )

                // === ZONE ===
                .then(Commands.literal("zone")
                        .then(Commands.literal("start").executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            LangService ls = ls(ctx);
                            ELang lang = lang(ctx);
                            boolean ok = GameManager.getInstance().getVillageZoneManager().start();
                            sender.sendMessage(ls.text(lang, ok ? "cmd.system.zone.started" : "cmd.system.zone.already_started"));
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(Commands.literal("reset").executes(ctx -> {
                            VillageZoneManager vzm = GameManager.getInstance().getVillageZoneManager();
                            vzm.reset();
                            ctx.getSource().getSender().sendMessage(ls(ctx).text(lang(ctx), "cmd.system.zone.reset", vzm.getCurrentHalfWidth(), vzm.getCurrentHalfDepth()));
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(Commands.literal("status").executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            LangService ls = ls(ctx);
                            ELang lang = lang(ctx);
                            VillageZoneManager vzm = GameManager.getInstance().getVillageZoneManager();
                            VillageZoneConfig  cfg = vzm.getConfig();

                            sender.sendMessage(ls.text(lang, "cmd.system.zone.status_header"));
                            sender.sendMessage(ls.text(lang, "cmd.system.zone.status_title"));
                            sender.sendMessage(ls.text(lang, "cmd.system.zone.status_sep"));
                            sender.sendMessage(ls.text(lang, vzm.isStarted() ? "cmd.system.zone.status_started_yes" : "cmd.system.zone.status_started_no"));

                            if (vzm.isStarted()) {
                                float elapsed = vzm.getElapsedMinutes() / 60f;
                                int stageIdx  = vzm.getCurrentStageIndex();
                                VillageZoneConfig.VillageZoneStage currentStage = cfg.stages().get(stageIdx);

                                sender.sendMessage(ls.text(lang, "cmd.system.zone.status_elapsed", String.format("%.2f", elapsed)));
                                sender.sendMessage(ls.text(lang, "cmd.system.zone.status_stage",   stageIdx));
                                sender.sendMessage(ls.text(lang, "cmd.system.zone.status_radius",  vzm.getCurrentHalfWidth(), vzm.getCurrentHalfDepth()));
                                sender.sendMessage(ls.text(lang, "cmd.system.zone.status_center",  currentStage.centerX(), currentStage.centerZ()));

                                int next = stageIdx + 1;
                                if (next < cfg.stages().size()) {
                                    VillageZoneConfig.VillageZoneStage nextStage = cfg.stages().get(next);
                                    float hoursLeft = nextStage.afterHours() - elapsed;
                                    sender.sendMessage(ls.text(lang, "cmd.system.zone.status_next", String.format("%.2f", hoursLeft), nextStage.halfWidth(), nextStage.halfDepth()));
                                } else {
                                    sender.sendMessage(ls.text(lang, "cmd.system.zone.status_final"));
                                }
                            }

                            sender.sendMessage(ls.text(lang, "cmd.system.zone.status_footer"));
                            return Command.SINGLE_SUCCESS;
                        }))
                );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static LangService ls(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        return GameManager.getInstance().getLangService();
    }

    private static ELang lang(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        LangService ls = GameManager.getInstance().getLangService();
        CommandSender sender = ctx.getSource().getSender();
        return sender instanceof Player p ? ls.resolveLanguage(p) : ls.getServerDefault();
    }
}