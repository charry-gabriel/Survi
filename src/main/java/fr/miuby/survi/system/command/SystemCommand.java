package fr.miuby.survi.system.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.SurviConfig;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.world.config.VillageZoneConfig;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.time.TimeManager;
import fr.miuby.survi.world.VillageZoneManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.logging.Level;

public class SystemCommand {
    private static final String levelArgument = "level";

    private SystemCommand() {
        /* This utility class should not be instantiated */
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("survi")
                .requires(sender -> sender.getSender().isOp())

                // === LOG COMMANDS ===
                .then(Commands.literal("log")

                        // /survi log status
                        .then(Commands.literal("status")
                                .executes(ctx -> {
                                    var sender = ctx.getSource().getSender();
                                    var lm = MLLogManager.getInstance();

                                    sender.sendMessage(Component.text("╔═══════════════════════════╗", NamedTextColor.GOLD));
                                    sender.sendMessage(Component.text("║    LOG STATUS             ║", NamedTextColor.GOLD));
                                    sender.sendMessage(Component.text("╠═══════════════════════════╣", NamedTextColor.GOLD));

                                    // Tags
                                    sender.sendMessage(Component.text("║ TAGS:", NamedTextColor.YELLOW));
                                    for (var entry : lm.getAllTagStates().entrySet()) {
                                        NamedTextColor color = Boolean.TRUE.equals(entry.getValue()) ? NamedTextColor.GREEN : NamedTextColor.RED;
                                        String icon = Boolean.TRUE.equals(entry.getValue()) ? "✓" : "✗";
                                        sender.sendMessage(
                                                Component.text("║   " + icon + " ", color)
                                                        .append(Component.text(entry.getKey(), NamedTextColor.WHITE))
                                        );
                                    }

                                    sender.sendMessage(Component.text("║", NamedTextColor.GOLD));

                                    // Levels
                                    sender.sendMessage(Component.text("║ LEVELS:", NamedTextColor.YELLOW));
                                    for (var entry : lm.getAllLevelStates().entrySet()) {
                                        NamedTextColor color = Boolean.TRUE.equals(entry.getValue()) ? NamedTextColor.GREEN : NamedTextColor.RED;
                                        String icon = Boolean.TRUE.equals(entry.getValue()) ? "✓" : "✗";
                                        sender.sendMessage(
                                                Component.text("║   " + icon + " ", color)
                                                        .append(Component.text(entry.getKey().getName(), NamedTextColor.WHITE))
                                        );
                                    }

                                    sender.sendMessage(Component.text("╚═══════════════════════════╝", NamedTextColor.GOLD));
                                    return Command.SINGLE_SUCCESS;
                                })
                        )

                        // /survi log tag <toggle/enable/disable> <TAG>
                        .then(Commands.literal("tag")
                                .then(Commands.literal("toggle")
                                        .then(Commands.argument("tag", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    EnumSet.allOf(ELogTag.class)
                                                            .forEach(tag -> builder.suggest(tag.name()));
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> {
                                                    String tagName = StringArgumentType.getString(ctx, "tag");
                                                    ELogTag tag = ELogTag.valueOf(tagName);
                                                    MLLogManager.getInstance().toggleTag(tag);
                                                    boolean enabled = MLLogManager.getInstance().isTagEnabled(tag);

                                                    ctx.getSource().getSender().sendMessage(
                                                            Component.text("Tag [" + tag + "] ", NamedTextColor.YELLOW)
                                                                    .append(Component.text(enabled ? "activé" : "désactivé",
                                                                            enabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                                                    );
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                                .then(Commands.literal("enable")
                                        .then(Commands.argument("tag", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    EnumSet.allOf(ELogTag.class)
                                                            .forEach(tag -> builder.suggest(tag.name()));
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> {
                                                    String tagName = StringArgumentType.getString(ctx, "tag");
                                                    ELogTag tag = ELogTag.valueOf(tagName);
                                                    MLLogManager.getInstance().setTagEnabled(tag, true);

                                                    ctx.getSource().getSender().sendMessage(
                                                            Component.text("Tag [" + tag + "] activé", NamedTextColor.GREEN)
                                                    );
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                                .then(Commands.literal("disable")
                                        .then(Commands.argument("tag", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    EnumSet.allOf(ELogTag.class)
                                                            .forEach(tag -> builder.suggest(tag.name()));
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> {
                                                    String tagName = StringArgumentType.getString(ctx, "tag");
                                                    ELogTag tag = ELogTag.valueOf(tagName);
                                                    MLLogManager.getInstance().setTagEnabled(tag, false);

                                                    ctx.getSource().getSender().sendMessage(
                                                            Component.text("Tag [" + tag + "] désactivé", NamedTextColor.RED)
                                                    );
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        )

                        // /survi log level <toggle/enable/disable> <LEVEL>
                        .then(Commands.literal("level")
                                .then(Commands.literal("toggle")
                                        .then(Commands.argument(levelArgument, StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    builder.suggest("INFO");
                                                    builder.suggest("WARNING");
                                                    builder.suggest("SEVERE");
                                                    builder.suggest("CONFIG");
                                                    builder.suggest("FINE");
                                                    builder.suggest("FINER");
                                                    builder.suggest("FINEST");
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> {
                                                    String levelName = StringArgumentType.getString(ctx, levelArgument);
                                                    Level level = Level.parse(levelName);
                                                    MLLogManager.getInstance().toggleLevel(level);
                                                    boolean enabled = MLLogManager.getInstance().isLevelEnabled(level);

                                                    ctx.getSource().getSender().sendMessage(
                                                            Component.text("Level [" + level.getName() + "] ", NamedTextColor.YELLOW)
                                                                    .append(Component.text(enabled ? "activé" : "désactivé",
                                                                            enabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                                                    );
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                                .then(Commands.literal("enable")
                                        .then(Commands.argument(levelArgument, StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    builder.suggest("INFO");
                                                    builder.suggest("WARNING");
                                                    builder.suggest("SEVERE");
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> {
                                                    String levelName = StringArgumentType.getString(ctx, levelArgument);
                                                    Level level = Level.parse(levelName);
                                                    MLLogManager.getInstance().setLevelEnabled(level, true);

                                                    ctx.getSource().getSender().sendMessage(
                                                            Component.text("Level [" + level.getName() + "] activé", NamedTextColor.GREEN)
                                                    );
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                                .then(Commands.literal("disable")
                                        .then(Commands.argument(levelArgument, StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    builder.suggest("INFO");
                                                    builder.suggest("WARNING");
                                                    builder.suggest("SEVERE");
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> {
                                                    String levelName = StringArgumentType.getString(ctx, levelArgument);
                                                    Level level = Level.parse(levelName);
                                                    MLLogManager.getInstance().setLevelEnabled(level, false);

                                                    ctx.getSource().getSender().sendMessage(
                                                            Component.text("Level [" + level.getName() + "] désactivé", NamedTextColor.RED)
                                                    );
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        )

                        // /survi log mode <production/debug/quiet>
                        .then(Commands.literal("mode")
                                .then(Commands.literal("production")
                                        .executes(ctx -> {
                                            MLLogManager.getInstance().setProductionMode();
                                            ctx.getSource().getSender().sendMessage(
                                                    Component.text("Mode PRODUCTION activé (WARNING + SEVERE)", NamedTextColor.GREEN)
                                            );
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                                .then(Commands.literal("debug")
                                        .executes(ctx -> {
                                            MLLogManager.getInstance().setDebugMode();
                                            ctx.getSource().getSender().sendMessage(
                                                    Component.text("Mode DEBUG activé (tout)", NamedTextColor.GREEN)
                                            );
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                                .then(Commands.literal("quiet")
                                        .executes(ctx -> {
                                            MLLogManager.getInstance().setQuietMode();
                                            ctx.getSource().getSender().sendMessage(
                                                    Component.text("Mode QUIET activé (seulement SEVERE)", NamedTextColor.RED)
                                            );
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                )

                // === TIME COMMANDS (ton code existant) ===
                .then(Commands.literal("time")
                        .then(Commands.literal("info")
                                .executes(ctx -> {
                                    TimeManager tm = GameManager.getInstance().getTimeManager();
                                    if (tm == null) {
                                        ctx.getSource().getSender().sendMessage(
                                                Component.text("TimeManager non initialisé !", NamedTextColor.RED)
                                        );
                                        return Command.SINGLE_SUCCESS;
                                    }

                                    var sender = ctx.getSource().getSender();
                                    sender.sendMessage(Component.text("═══ Time Manager ═══", NamedTextColor.GOLD));
                                    sender.sendMessage(
                                            Component.text("Timezone: ", NamedTextColor.YELLOW)
                                                    .append(Component.text(tm.getServerTimezone().getId(), NamedTextColor.WHITE))
                                    );
                                    sender.sendMessage(
                                            Component.text("A reset aujourd'hui: ", NamedTextColor.YELLOW)
                                                    .append(Component.text(tm.hasResetToday() ? "Oui" : "Non", NamedTextColor.WHITE))
                                    );
                                    sender.sendMessage(
                                            Component.text("Dernier reset: jour ", NamedTextColor.YELLOW)
                                                    .append(Component.text(tm.getLastResetDay(), NamedTextColor.WHITE))
                                    );
                                    sender.sendMessage(
                                            Component.text("Prochain reset: ", NamedTextColor.YELLOW)
                                                    .append(Component.text(
                                                            tm.getNextResetTime().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")),
                                                            NamedTextColor.WHITE
                                                    ))
                                    );
                                    return Command.SINGLE_SUCCESS;
                                })
                        )

                        .then(Commands.literal("reset")
                                .requires(sender -> sender.getSender().hasPermission("survi.time.admin"))
                                .executes(ctx -> {
                                    TimeManager tm = GameManager.getInstance().getTimeManager();
                                    if (tm == null) {
                                        ctx.getSource().getSender().sendMessage(
                                                Component.text("TimeManager non initialisé !", NamedTextColor.RED)
                                        );
                                        return Command.SINGLE_SUCCESS;
                                    }

                                    ctx.getSource().getSender().sendMessage(
                                            Component.text("Force le reset quotidien...", NamedTextColor.GOLD)
                                    );
                                    tm.forceReset();
                                    ctx.getSource().getSender().sendMessage(
                                            Component.text("✓ Reset effectué avec succès !", NamedTextColor.GREEN)
                                    );
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )

                // === ZONE COMMANDS ===
                .then(Commands.literal("zone")

                        // /survi zone start — lance le timer de partie
                        .then(Commands.literal("start")
                                .executes(ctx -> {
                                    var sender = ctx.getSource().getSender();
                                    boolean ok = GameManager.getInstance().getVillageZoneManager().start();
                                    if (ok) {
                                        sender.sendMessage(Component.text(
                                                "✓ Partie démarrée ! Zone village active (palier 0).",
                                                NamedTextColor.GREEN));
                                    } else {
                                        sender.sendMessage(Component.text(
                                                "⚠ Le timer est déjà en cours. Utilisez /survi zone reset pour le relancer.",
                                                NamedTextColor.YELLOW));
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })
                        )

                        // /survi zone reset — remet le timer à zéro (tests)
                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    GameManager.getInstance().getVillageZoneManager().reset();
                                    ctx.getSource().getSender().sendMessage(Component.text(
                                            "✓ Timer réinitialisé — palier 0 restauré (rayon "
                                                    + GameManager.getInstance().getVillageZoneManager().getCurrentRadius()
                                                    + " blocs).",
                                            NamedTextColor.GREEN));
                                    return Command.SINGLE_SUCCESS;
                                })
                        )

                        // /survi zone status — affiche l'état courant
                        .then(Commands.literal("status")
                                .executes(ctx -> {
                                    var sender = ctx.getSource().getSender();
                                    VillageZoneManager vzm = GameManager.getInstance().getVillageZoneManager();
                                    VillageZoneConfig cfg =
                                            SurviConfig.getInstance().getVillageZoneConfig();

                                    sender.sendMessage(Component.text("╔═══════════════════════════════╗", NamedTextColor.GOLD));
                                    sender.sendMessage(Component.text("║    VILLAGE ZONE STATUS        ║", NamedTextColor.GOLD));
                                    sender.sendMessage(Component.text("╠═══════════════════════════════╣", NamedTextColor.GOLD));

                                    sender.sendMessage(
                                            Component.text("║ Démarrée : ", NamedTextColor.YELLOW)
                                                    .append(Component.text(vzm.isStarted() ? "Oui" : "Non",
                                                            vzm.isStarted() ? NamedTextColor.GREEN : NamedTextColor.RED)));

                                    if (vzm.isStarted()) {
                                        float elapsed = vzm.getElapsedMinutes() / 60f;
                                        sender.sendMessage(
                                                Component.text("║ Temps écoulé : ", NamedTextColor.YELLOW)
                                                        .append(Component.text(String.format("%.2f", elapsed) + "h", NamedTextColor.WHITE)));

                                        sender.sendMessage(
                                                Component.text("║ Palier actuel : ", NamedTextColor.YELLOW)
                                                        .append(Component.text(vzm.getCurrentStageIndex(), NamedTextColor.WHITE)));

                                        sender.sendMessage(
                                                Component.text("║ Rayon : ", NamedTextColor.YELLOW)
                                                        .append(Component.text(vzm.getCurrentRadius() + " blocs", NamedTextColor.WHITE)));

                                        sender.sendMessage(
                                                Component.text("║ Centre : ", NamedTextColor.YELLOW)
                                                        .append(Component.text("(" + cfg.centerX() + ", " + cfg.centerZ() + ")", NamedTextColor.WHITE)));

                                        // Prochain palier
                                        var stages = cfg.stages();
                                        int next = vzm.getCurrentStageIndex() + 1;
                                        if (next < stages.size()) {
                                            float hoursLeft = stages.get(next).afterHours() - elapsed;
                                            sender.sendMessage(
                                                    Component.text("║ Prochain palier dans : ", NamedTextColor.YELLOW)
                                                            .append(Component.text(String.format("%.2f", hoursLeft) + "h → rayon "
                                                                    + stages.get(next).radius() + " blocs", NamedTextColor.WHITE)));
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