package fr.miuby.survi.system.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.miuby.lib.command.MLLogCommand;
import fr.miuby.lib.utils.Rect;
import fr.miuby.lib.world.MLWorld;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.listener.PlacedBlockTracker;
import fr.miuby.survi.system.lang.ELang;
import fr.miuby.survi.system.lang.LangService;
import fr.miuby.survi.system.perf.PerfTimer;
import fr.miuby.survi.system.time.TimeManager;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.zone.VillageZoneManager;
import fr.miuby.survi.world.config.VillageZoneConfig;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
                        .then(Commands.literal("stop").executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            LangService ls = ls(ctx);
                            ELang lang = ls.resolveOrDefault(sender);
                            GameManager.getInstance().getVillageZoneManager().stop();
                            sender.sendMessage(ls.text(lang, "cmd.system.zone.stopped"));
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
                )

                // === CONTAINERS ===
                .then(Commands.literal("containers")
                        .then(Commands.literal("clear").executes(ctx -> {
                            clearVillageContainers(ctx.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        }))
                );
    }

    // ── Conteneurs village ──────────────────────────────────────────────────

    /** Nombre de chunks à charger "à froid" par tick. Les chunks déjà chargés sont traités sans limite (coût négligeable). */
    private static final int VILLAGE_CHUNK_LOAD_BUDGET_PER_TICK = 8;

    private static void clearVillageContainers(CommandSender sender) {
        MLWorld village = WorldRegistry.get(EWorld.VILLAGE);
        World world = village.getWorld();
        Rect bounds = village.getLimit();

        int minChunkX = Math.floorDiv(bounds.xMin(), 16);
        int maxChunkX = Math.floorDiv(bounds.xMax(), 16);
        int minChunkZ = Math.floorDiv(bounds.zMin(), 16);
        int maxChunkZ = Math.floorDiv(bounds.zMax(), 16);

        List<int[]> chunkCoords = new ArrayList<>();
        for (int x = minChunkX; x <= maxChunkX; x++)
            for (int z = minChunkZ; z <= maxChunkZ; z++)
                chunkCoords.add(new int[]{x, z});

        LangService ls = GameManager.getInstance().getLangService();
        ELang lang = sender instanceof Player p ? ls.resolveLanguage(p) : ls.getServerDefault();
        sender.sendMessage(ls.text(lang, "cmd.system.containers.clear_start", chunkCoords.size()));

        new BukkitRunnable() {
            int index = 0;
            int cleared = 0;

            @Override
            public void run() {
                int loadBudget = VILLAGE_CHUNK_LOAD_BUDGET_PER_TICK;

                while (index < chunkCoords.size() && loadBudget > 0) {
                    int[] coord = chunkCoords.get(index++);
                    boolean wasLoaded = world.isChunkLoaded(coord[0], coord[1]);
                    Chunk chunk = world.getChunkAt(coord[0], coord[1]);

                    // useSnapshot=false : état lié directement au tile entity réel.
                    // (en snapshot, getInventory() renvoie une copie détachée et le
                    //  clear() peut ne pas être correctement réappliqué par update())
                    for (BlockState state : chunk.getTileEntities(false)) {
                        if (state instanceof Container container) {
                            container.getInventory().clear();
                            container.update();
                            cleared++;
                        }
                    }

                    if (!wasLoaded) {
                        world.unloadChunk(chunk.getX(), chunk.getZ(), true);
                        loadBudget--;
                    }
                }

                if (index >= chunkCoords.size()) {
                    sender.sendMessage(ls.text(lang, "cmd.system.containers.clear_done", cleared));
                    cancel();
                }
            }
        }.runTaskTimer(GameManager.getInstance().getPlugin(), 0L, 1L);
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