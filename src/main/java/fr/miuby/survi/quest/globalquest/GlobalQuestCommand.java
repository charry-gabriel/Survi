package fr.miuby.survi.quest.globalquest;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.command.argument.GlobalQuestArgument;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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
        GlobalQuest quest = GlobalQuestArgument.getGlobalQuest(ctx, questArgument);
        GlobalQuestManager manager = GameManager.getInstance().getGlobalQuestManager();

        if (manager.getActiveQuest() != null) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Une quête globale est déjà en cours : ")
                            .color(NamedTextColor.RED)
                            .append(Component.text(manager.getActiveQuest().getName(), NamedTextColor.YELLOW))
            );
            return Command.SINGLE_SUCCESS;
        }

        manager.startQuest(quest.getId());
        ctx.getSource().getSender().sendMessage(
                Component.text("Quête globale « " + quest.getName() + " » lancée !").color(NamedTextColor.GREEN)
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int stopQuest(CommandContext<CommandSourceStack> ctx) {
        GlobalQuestManager manager = GameManager.getInstance().getGlobalQuestManager();

        if (manager.getActiveQuest() == null) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Aucune quête globale en cours.").color(NamedTextColor.RED)
            );
            return Command.SINGLE_SUCCESS;
        }

        manager.cancelQuest();
        ctx.getSource().getSender().sendMessage(
                Component.text("Quête globale annulée.").color(NamedTextColor.YELLOW)
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int listQuests(CommandContext<CommandSourceStack> ctx) {
        GlobalQuestManager manager = GameManager.getInstance().getGlobalQuestManager();

        if (manager.getQuestPool().isEmpty()) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Aucune quête globale configurée dans global_quests.yml.").color(NamedTextColor.RED)
            );
            return Command.SINGLE_SUCCESS;
        }

        ctx.getSource().getSender().sendMessage(
                Component.text("── Quêtes globales disponibles ──").color(NamedTextColor.GOLD)
        );
        for (GlobalQuest q : manager.getQuestPool()) {
            String timeStr = GlobalQuestManager.formatSeconds(q.getTimeLimitSeconds());
            ctx.getSource().getSender().sendMessage(
                    Component.text("  " + q.getId(), NamedTextColor.YELLOW)
                            .append(Component.text(" — " + q.getName(), NamedTextColor.WHITE))
                            .append(Component.text(" (objectif: " + q.getGoal()
                                    + " | temps: " + timeStr + ")", NamedTextColor.GRAY))
            );
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int statusQuest(CommandContext<CommandSourceStack> ctx) {
        GlobalQuestManager manager = GameManager.getInstance().getGlobalQuestManager();

        if (manager.getActiveQuest() == null) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Aucune quête globale en cours.").color(NamedTextColor.RED)
            );
            return Command.SINGLE_SUCCESS;
        }

        GlobalQuest q = manager.getActiveQuest();
        String timeLeft = GlobalQuestManager.formatSeconds(manager.getRemainingSeconds());

        ctx.getSource().getSender().sendMessage(
                Component.text("── Quête Globale en cours ──").color(NamedTextColor.GOLD)
        );
        ctx.getSource().getSender().sendMessage(
                Component.text("  Nom : ").color(NamedTextColor.GRAY)
                        .append(Component.text(q.getName(), NamedTextColor.YELLOW))
        );
        ctx.getSource().getSender().sendMessage(
                Component.text("  Progression : ").color(NamedTextColor.GRAY)
                        .append(Component.text(manager.getProgress() + "/" + q.getGoal(), NamedTextColor.AQUA))
        );
        ctx.getSource().getSender().sendMessage(
                Component.text("  Temps restant : ").color(NamedTextColor.GRAY)
                        .append(Component.text(timeLeft, NamedTextColor.AQUA))
        );
        ctx.getSource().getSender().sendMessage(
                Component.text("  Participants : ").color(NamedTextColor.GRAY)
                        .append(Component.text(manager.getParticipants().size(), NamedTextColor.AQUA))
        );
        return Command.SINGLE_SUCCESS;
    }
}
