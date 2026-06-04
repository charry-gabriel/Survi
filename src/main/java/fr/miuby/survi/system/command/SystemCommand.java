package fr.miuby.survi.system.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.miuby.lib.command.MLLogCommand;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.SurviConfig;
import fr.miuby.survi.system.time.TimeManager;
import fr.miuby.survi.world.VillageZoneManager;
import fr.miuby.survi.world.config.VillageZoneConfig;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.format.DateTimeFormatter;

public class SystemCommand {
    private SystemCommand() {
        /* This utility class should not be instantiated */
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("survi")
                .requires(sender -> sender.getSender().isOp())

                // === LOG COMMANDS — gérés par MLLogCommand dans MiubyLib ===
                .then(MLLogCommand.create())

                // === RELOAD COMMANDS ===
                .then(ReloadCommand.create())

                // === TIME COMMANDS ===
                .then(Commands.literal("time")
                        .then(Commands.literal("info")
                                .executes(ctx -> {
                                    TimeManager tm = GameManager.getInstance().getTimeManager();
                                    if (tm == null) {
                                        ctx.getSource().getSender().sendMessage(Component.text("TimeManager non initialisé !", NamedTextColor.RED));
                                        return Command.SINGLE_SUCCESS;
                                    }

                                    var sender = ctx.getSource().getSender();
                                    sender.sendMessage(Component.text("═══ Time Manager ═══", NamedTextColor.GOLD));
                                    sender.sendMessage(Component.text("Timezone: ", NamedTextColor.YELLOW).append(Component.text(tm.getServerTimezone().getId(), NamedTextColor.WHITE)));
                                    sender.sendMessage(Component.text("A reset aujourd'hui: ", NamedTextColor.YELLOW).append(Component.text(tm.hasResetToday() ? "Oui" : "Non", NamedTextColor.WHITE)));
                                    sender.sendMessage(Component.text("Dernier reset: jour ", NamedTextColor.YELLOW).append(Component.text(tm.getLastResetDay(), NamedTextColor.WHITE)));
                                    sender.sendMessage(Component.text("Prochain reset: ", NamedTextColor.YELLOW)
                                            .append(Component.text(tm.getNextResetTime().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")), NamedTextColor.WHITE)));
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .then(Commands.literal("reset")
                                .requires(sender -> sender.getSender().hasPermission("survi.time.admin"))
                                .executes(ctx -> {
                                    TimeManager tm = GameManager.getInstance().getTimeManager();
                                    if (tm == null) {
                                        ctx.getSource().getSender().sendMessage(Component.text("TimeManager non initialisé !", NamedTextColor.RED));
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    ctx.getSource().getSender().sendMessage(Component.text("Force le reset quotidien...", NamedTextColor.GOLD));
                                    tm.forceReset();
                                    ctx.getSource().getSender().sendMessage(Component.text("✓ Reset effectué avec succès !", NamedTextColor.GREEN));
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )

                // === ZONE COMMANDS ===
                .then(Commands.literal("zone")
                        .then(Commands.literal("start")
                                .executes(ctx -> {
                                    var sender = ctx.getSource().getSender();
                                    boolean ok = GameManager.getInstance().getVillageZoneManager().start();
                                    if (ok) {
                                        sender.sendMessage(Component.text("✓ Partie démarrée ! Zone village active (palier 0).", NamedTextColor.GREEN));
                                    } else {
                                        sender.sendMessage(Component.text("⚠ Le timer est déjà en cours. Utilisez /survi zone reset pour le relancer.", NamedTextColor.YELLOW));
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    GameManager.getInstance().getVillageZoneManager().reset();
                                    ctx.getSource().getSender().sendMessage(Component.text(
                                            "✓ Timer réinitialisé — palier 0 restauré (rayon "
                                                    + GameManager.getInstance().getVillageZoneManager().getCurrentRadius() + " blocs).",
                                            NamedTextColor.GREEN));
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .then(Commands.literal("status")
                                .executes(ctx -> {
                                    var sender = ctx.getSource().getSender();
                                    VillageZoneManager vzm = GameManager.getInstance().getVillageZoneManager();
                                    VillageZoneConfig cfg = SurviConfig.getInstance().getVillageZoneConfig();

                                    sender.sendMessage(Component.text("╔═══════════════════════════════╗", NamedTextColor.GOLD));
                                    sender.sendMessage(Component.text("║    VILLAGE ZONE STATUS        ║", NamedTextColor.GOLD));
                                    sender.sendMessage(Component.text("╠═══════════════════════════════╣", NamedTextColor.GOLD));
                                    sender.sendMessage(Component.text("║ Démarrée : ", NamedTextColor.YELLOW)
                                            .append(Component.text(vzm.isStarted() ? "Oui" : "Non", vzm.isStarted() ? NamedTextColor.GREEN : NamedTextColor.RED)));

                                    if (vzm.isStarted()) {
                                        float elapsed = vzm.getElapsedMinutes() / 60f;
                                        sender.sendMessage(Component.text("║ Temps écoulé : ", NamedTextColor.YELLOW).append(Component.text(String.format("%.2f", elapsed) + "h", NamedTextColor.WHITE)));
                                        sender.sendMessage(Component.text("║ Palier actuel : ", NamedTextColor.YELLOW).append(Component.text(vzm.getCurrentStageIndex(), NamedTextColor.WHITE)));
                                        sender.sendMessage(Component.text("║ Rayon : ", NamedTextColor.YELLOW).append(Component.text(vzm.getCurrentRadius() + " blocs", NamedTextColor.WHITE)));
                                        sender.sendMessage(Component.text("║ Centre : ", NamedTextColor.YELLOW).append(Component.text("(" + cfg.centerX() + ", " + cfg.centerZ() + ")", NamedTextColor.WHITE)));

                                        var stages = cfg.stages();
                                        int next = vzm.getCurrentStageIndex() + 1;
                                        if (next < stages.size()) {
                                            float hoursLeft = stages.get(next).afterHours() - elapsed;
                                            sender.sendMessage(Component.text("║ Prochain palier dans : ", NamedTextColor.YELLOW)
                                                    .append(Component.text(String.format("%.2f", hoursLeft) + "h → rayon " + stages.get(next).radius() + " blocs", NamedTextColor.WHITE)));
                                        } else {
                                            sender.sendMessage(Component.text("║ Palier final atteint.", NamedTextColor.AQUA));
                                        }
                                    }

                                    sender.sendMessage(Component.text("╚═══════════════════════════════╝", NamedTextColor.GOLD));
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                );
    }
}