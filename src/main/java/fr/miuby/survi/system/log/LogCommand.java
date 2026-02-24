package fr.miuby.survi.system.log;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.EnumSet;

public class LogCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("survi")
            .requires(sender -> sender.getSender().isOp())
            .then(Commands.literal("log")
                .then(Commands.argument("tag", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        EnumSet.allOf(LogManager.ETagLog.class).stream().map(Enum::toString).forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .executes(ctx -> {
                        LogManager.ETagLog tag = LogManager.ETagLog.valueOf(StringArgumentType.getString(ctx, "tag"));
                        LogManager.getInstance().toggleLog(tag);
                        boolean enabled = LogManager.getInstance().isLogEnabled(tag);
                        ctx.getSource().getSender().sendMessage(Component.text("Log [" + tag + "] est maintenant " + (enabled ? "activé" : "désactivé"),
                            enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    })
                )
            );
    }
}
