package fr.miuby.survi.quest.globalquest;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.quest.ETargetsMode;
import fr.miuby.survi.system.command.argument.GlobalQuestArgument;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

@SuppressWarnings({"java:S3516", "SameReturnValue"})
public class GlobalQuestCommand {

    private static final String questArgument = "quest";

    private GlobalQuestCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("globalquest")
                .requires(source -> source.getSender().isOp())

                // /globalquest start <id>
                .then(Commands.literal("start")
                        .then(Commands.argument(questArgument, GlobalQuestArgument.globalQuest())
                                .executes(GlobalQuestCommand::startQuest)
                        )
                )

                // /globalquest stop
                .then(Commands.literal("stop")
                        .executes(GlobalQuestCommand::stopQuest)
                )

                // /globalquest list
                .then(Commands.literal("list")
                        .executes(GlobalQuestCommand::listQuests)
                )

                // /globalquest status
                .then(Commands.literal("status")
                        .executes(GlobalQuestCommand::statusQuest)
                )

                // /globalquest progress add|remove|set
                .then(Commands.literal("progress")
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                        .executes(ctx -> adjustProgress(ctx, true))
                                )
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                        .executes(ctx -> adjustProgress(ctx, false))
                                )
                        )
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
                                        .executes(GlobalQuestCommand::setProgress)
                                )
                        )
                );
    }

    private static int startQuest(CommandContext<CommandSourceStack> ctx) {
        var sender  = ctx.getSource().getSender();
        var manager = GameManager.getInstance().getGlobalQuestManager();
        var ls      = GameManager.getInstance().getLangService();
        var lang    = ls.resolveOrDefault(sender);

        if (manager.getActiveQuest() != null) {
            sender.sendMessage(ls.text(lang, "cmd.globalquest.already_active",
                    manager.getActiveQuest().getName()));
            return Command.SINGLE_SUCCESS;
        }

        GlobalQuest quest = GlobalQuestArgument.getGlobalQuest(ctx, questArgument);
        manager.startQuest(quest.getId());
        sender.sendMessage(ls.text(lang, "cmd.globalquest.started", quest.getName()));
        return Command.SINGLE_SUCCESS;
    }

    private static int stopQuest(CommandContext<CommandSourceStack> ctx) {
        var sender  = ctx.getSource().getSender();
        var manager = GameManager.getInstance().getGlobalQuestManager();
        var ls      = GameManager.getInstance().getLangService();
        var lang    = ls.resolveOrDefault(sender);

        if (manager.getActiveQuest() == null) {
            sender.sendMessage(ls.text(lang, "cmd.globalquest.none_active"));
            return Command.SINGLE_SUCCESS;
        }

        manager.cancelQuest();
        sender.sendMessage(ls.text(lang, "cmd.globalquest.cancelled"));
        return Command.SINGLE_SUCCESS;
    }

    private static int listQuests(CommandContext<CommandSourceStack> ctx) {
        var sender  = ctx.getSource().getSender();
        var manager = GameManager.getInstance().getGlobalQuestManager();
        var ls      = GameManager.getInstance().getLangService();
        var lang    = ls.resolveOrDefault(sender);

        if (manager.getQuestPool().isEmpty()) {
            sender.sendMessage(ls.text(lang, "cmd.globalquest.no_config"));
            return Command.SINGLE_SUCCESS;
        }

        sender.sendMessage(ls.text(lang, "cmd.globalquest.list_header"));
        for (GlobalQuest q : manager.getQuestPool()) {
            String timeStr = GlobalQuestManager.formatSeconds(q.getTimeLimitSeconds());
            sender.sendMessage(ls.text(lang, "cmd.globalquest.list_entry",
                    q.getId(), q.getName(), q.getGoal(), timeStr));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int statusQuest(CommandContext<CommandSourceStack> ctx) {
        var sender  = ctx.getSource().getSender();
        var manager = GameManager.getInstance().getGlobalQuestManager();
        var ls      = GameManager.getInstance().getLangService();
        var lang    = ls.resolveOrDefault(sender);

        if (manager.getActiveQuest() == null) {
            sender.sendMessage(ls.text(lang, "cmd.globalquest.none_active"));
            return Command.SINGLE_SUCCESS;
        }

        GlobalQuest q        = manager.getActiveQuest();
        String      timeLeft = GlobalQuestManager.formatSeconds(manager.getRemainingSeconds());

        sender.sendMessage(ls.text(lang, "cmd.globalquest.status_header"));
        sender.sendMessage(ls.text(lang, "cmd.globalquest.status_name",        q.getName()));
        if (q.getTargetsMode() == ETargetsMode.ALL) {
            sender.sendMessage(ls.text(lang, "cmd.globalquest.status_progress_targets", q.formatTargetProgressBreakdown(manager.getTargetProgress())));
        } else {
            sender.sendMessage(ls.text(lang, "cmd.globalquest.status_progress",    manager.getProgress(), q.getGoal()));
        }
        sender.sendMessage(ls.text(lang, "cmd.globalquest.status_time",        timeLeft));
        sender.sendMessage(ls.text(lang, "cmd.globalquest.status_participants",manager.getParticipants().size()));
        return Command.SINGLE_SUCCESS;
    }

    private static int adjustProgress(CommandContext<CommandSourceStack> ctx, boolean isAdd) {
        var sender  = ctx.getSource().getSender();
        var manager = GameManager.getInstance().getGlobalQuestManager();
        var ls      = GameManager.getInstance().getLangService();
        var lang    = ls.resolveOrDefault(sender);

        GlobalQuest quest = manager.getActiveQuest();
        if (quest == null) {
            sender.sendMessage(ls.text(lang, "cmd.globalquest.none_active"));
            return Command.SINGLE_SUCCESS;
        }

        int amount = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount");
        int before = manager.getProgress();
        int goal   = quest.getGoal();
        String questName = quest.getName();

        manager.adjustProgress(isAdd ? amount : -amount);

        if (manager.getActiveQuest() == null) {
            sender.sendMessage(ls.text(lang, "cmd.globalquest.progress_finished", questName));
        } else {
            sender.sendMessage(ls.text(lang, "cmd.globalquest.progress_changed", before, manager.getProgress(), goal));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int setProgress(CommandContext<CommandSourceStack> ctx) {
        var sender  = ctx.getSource().getSender();
        var manager = GameManager.getInstance().getGlobalQuestManager();
        var ls      = GameManager.getInstance().getLangService();
        var lang    = ls.resolveOrDefault(sender);

        GlobalQuest quest = manager.getActiveQuest();
        if (quest == null) {
            sender.sendMessage(ls.text(lang, "cmd.globalquest.none_active"));
            return Command.SINGLE_SUCCESS;
        }

        int value = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "value");
        int goal  = quest.getGoal();
        String questName = quest.getName();

        manager.setProgress(value);

        if (manager.getActiveQuest() == null) {
            sender.sendMessage(ls.text(lang, "cmd.globalquest.progress_finished", questName));
        } else {
            sender.sendMessage(ls.text(lang, "cmd.globalquest.progress_set", manager.getProgress(), goal));
        }
        return Command.SINGLE_SUCCESS;
    }
}