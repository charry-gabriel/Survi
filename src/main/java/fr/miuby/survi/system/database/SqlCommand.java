package fr.miuby.survi.system.database;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.miuby.survi.GameManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

public class SqlCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("sql")
                .requires(sender -> sender.getSender().isOp())
                .requires(sender -> sender.getSender().hasPermission("permission.sql"))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                    .executes(SqlCommand::sqlExecute));
    }

    private static int sqlExecute(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String result = GameManager.getInstance().getDatabase().Request(StringArgumentType.getString(ctx, "args"));
        sender.sendMessage(Component.text(result));
        return Command.SINGLE_SUCCESS;
    }
}