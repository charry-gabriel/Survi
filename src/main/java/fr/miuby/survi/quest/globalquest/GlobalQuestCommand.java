package fr.miuby.survi.quest.globalquest;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.miuby.survi.GameManager;
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
        sender.sendMessage(ls.text(lang, "cmd.globalquest.status_progress",    manager.getProgress(), q.getGoal()));
        sender.sendMessage(ls.text(lang, "cmd.globalquest.status_time",        timeLeft));
        sender.sendMessage(ls.text(lang, "cmd.globalquest.status_participants",manager.getParticipants().size()));
        return Command.SINGLE_SUCCESS;
    }
}