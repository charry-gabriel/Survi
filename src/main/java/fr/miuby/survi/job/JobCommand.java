package fr.miuby.survi.job;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.command.argument.AlphaPlayerArgument;
import fr.miuby.survi.system.command.argument.JobArgument;
import fr.miuby.survi.system.lang.ELang;
import fr.miuby.survi.system.lang.LangService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings({"java:S3516", "SameReturnValue"})
public class JobCommand {
    private static final String AMOUNT_ARG = "amount";
    private static final String PLAYER_ARG = "player";
    private static final String JOB_ARG    = "job";

    private JobCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> createReputationCommand() {
        return Commands.literal("job")
                .requires(source -> source.getSender().isOp())
                .then(Commands.argument(JOB_ARG, JobArgument.job())
                        .then(Commands.argument(PLAYER_ARG, AlphaPlayerArgument.alphaPlayer())

                                .then(Commands.literal("add")
                                        .then(Commands.argument(AMOUNT_ARG, IntegerArgumentType.integer(1, 10000))
                                                .executes(ctx -> executeAdd(ctx, true))
                                        )
                                )

                                .then(Commands.literal("remove")
                                        .then(Commands.argument(AMOUNT_ARG, IntegerArgumentType.integer(1, 10000))
                                                .executes(ctx -> executeAdd(ctx, false))
                                        )
                                )

                                .then(Commands.literal("set")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100000))
                                                .executes(JobCommand::executeSet)
                                        )
                                )

                                .then(Commands.literal("info")
                                        .executes(JobCommand::executeOther)
                                )
                        )
                );
    }

    private static int executeAdd(CommandContext<CommandSourceStack> ctx, boolean isAdd) {
        AlphaPlayer target = AlphaPlayerArgument.getAlphaPlayer(ctx, PLAYER_ARG);
        EJob        job    = JobArgument.getJob(ctx, JOB_ARG);
        int         amount = IntegerArgumentType.getInteger(ctx, AMOUNT_ARG);

        int before = target.getJobReputation(job);
        int delta  = isAdd ? amount : -amount;

        target.addJobReputation(job, delta);
        int after = target.getJobReputation(job);
        int actualDelta = after - before;

        CommandSender sender = ctx.getSource().getSender();
        LangService   ls     = GameManager.getInstance().getLangService();
        ELang         lang   = sender instanceof Player p ? ls.resolveLanguage(p) : ls.getServerDefault();

        if (actualDelta == 0 && !isAdd) {
            sender.sendMessage(ls.text(lang, "cmd.job.already_zero",
                    Placeholder.unparsed("player", target.getPseudo()),
                    Placeholder.component("job", job.toComponent())));
            return Command.SINGLE_SUCCESS;
        }

        String cmdKey = isAdd ? "cmd.job.added" : "cmd.job.removed";
        sender.sendMessage(ls.text(lang, cmdKey,
                Placeholder.unparsed("amount", String.valueOf(Math.abs(actualDelta))),
                Placeholder.unparsed("player", target.getPseudo()),
                Placeholder.component("job", job.toComponent()),
                Placeholder.unparsed("before", String.valueOf(before)),
                Placeholder.unparsed("after", String.valueOf(after))));

        if (target.getPlayer() != null && target.getPlayer().isOnline()) {
            String notifyKey = isAdd ? "cmd.job.notify.added" : "cmd.job.notify.removed";
            target.getPlayer().sendMessage(ls.text(target.getPlayer(), notifyKey,
                    Placeholder.component("job", job.toComponent())));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int executeSet(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer target = AlphaPlayerArgument.getAlphaPlayer(ctx, PLAYER_ARG);
        EJob        job    = JobArgument.getJob(ctx, JOB_ARG);
        int         value  = IntegerArgumentType.getInteger(ctx, "value");

        int before = target.getJobReputation(job);
        target.addJobReputation(job, value - before);

        CommandSender sender = ctx.getSource().getSender();
        LangService   ls     = GameManager.getInstance().getLangService();
        ELang         lang   = sender instanceof Player p ? ls.resolveLanguage(p) : ls.getServerDefault();

        sender.sendMessage(ls.text(lang, "cmd.job.set",
                Placeholder.unparsed("player", target.getPseudo()),
                Placeholder.component("job", job.toComponent()),
                Placeholder.unparsed("value", String.valueOf(value)),
                Placeholder.unparsed("before", String.valueOf(before))));

        if (target.getPlayer() != null && target.getPlayer().isOnline()) {
            target.getPlayer().sendMessage(ls.text(target.getPlayer(), "cmd.job.notify.set",
                    Placeholder.component("job", job.toComponent())));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int executeOther(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer   target = AlphaPlayerArgument.getAlphaPlayer(ctx, PLAYER_ARG);
        CommandSender sender = ctx.getSource().getSender();
        LangService   ls     = GameManager.getInstance().getLangService();
        ELang         lang   = sender instanceof Player p ? ls.resolveLanguage(p) : ls.getServerDefault();

        Player senderPlayer = (sender instanceof Player p) ? p : null;
        sendJobDisplay(ls, lang, senderPlayer, target, false);

        if (senderPlayer == null) {
            sender.sendMessage(ls.text(lang, "cmd.job.console.header", target.getPseudo()));
            for (EJob job : EJob.values()) {
                sender.sendMessage(Component.text("  " + job.getDisplayName())
                        .append(ls.text(lang, "cmd.job.info.level", target.getJobLevel(job))));
            }
        }

        sender.sendMessage(ls.text(lang, "cmd.job.displayed", target.getPseudo()));
        return Command.SINGLE_SUCCESS;
    }

    private static void sendJobDisplay(LangService ls, ELang lang, Player recipient, AlphaPlayer subject, boolean isSelf) {
        if (recipient == null) return;

        Component title = isSelf
                ? ls.text(lang, "cmd.job.info.title_self")
                : ls.text(lang, "cmd.job.info.title_other", subject.getPseudo());

        recipient.sendMessage(ls.text(lang, "cmd.job.info.separator"));
        recipient.sendMessage(title);
        recipient.sendMessage(ls.text(lang, "cmd.job.info.separator"));

        for (EJob job : EJob.values()) {
            int level = subject.getJobLevel(job);
            recipient.sendMessage(Component.text("  ")
                    .append(job.toComponent().decoration(TextDecoration.BOLD, false))
                    .append(ls.text(lang, "cmd.job.info.level", level)));
        }

        recipient.sendMessage(ls.text(lang, "cmd.job.info.separator"));
        if (isSelf) {
            recipient.sendMessage(ls.text(lang, "cmd.job.info.hint_1"));
            recipient.sendMessage(ls.text(lang, "cmd.job.info.hint_2"));
        }
    }
}