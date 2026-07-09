package fr.miuby.survi.system.database;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.command.DangerousCommandGuard;
import fr.miuby.survi.system.lang.ELang;
import fr.miuby.survi.system.lang.LangService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class SqlCommand {
    private SqlCommand() {
        /* This utility class should not be instantiated */
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("sql")
                .requires(sender -> sender.getSender().isOp())
                .then(Commands.literal("query")
                        .then(Commands.argument("sql", StringArgumentType.greedyString())
                                .executes(SqlCommand::sqlExecuteQuery)
                        )
                )
                .then(Commands.literal("tables")
                        .executes(SqlCommand::sqlExecuteTables)
                )
                .then(Commands.literal("schema")
                        .then(Commands.argument("table", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    builder.suggest("player");
                                    builder.suggest("villager");
                                    builder.suggest("planted_crops");
                                    builder.suggest("player_quest");
                                    builder.suggest("player_reputation");
                                    builder.suggest("server_data");
                                    return builder.buildFuture();
                                })
                                .executes(SqlCommand::sqlExecuteSchema)
                        )
                );
    }

    private static int sqlExecuteSchema(CommandContext<CommandSourceStack> ctx) {
        String table = StringArgumentType.getString(ctx, "table");
        String result = GameManager.getInstance().getDatabase().executeRaw("PRAGMA table_info(" + table + ")");

        ctx.getSource().getSender().sendMessage(Component.text("═══ Schema: " + table + " ═══", NamedTextColor.GOLD));
        for (String line : result.split("\n")) {
            ctx.getSource().getSender().sendMessage(Component.text("  " + line, NamedTextColor.WHITE));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int sqlExecuteTables(CommandContext<CommandSourceStack> ctx) {
        String result = GameManager.getInstance().getDatabase().executeRaw("SELECT name FROM sqlite_master WHERE type='table'");

        ctx.getSource().getSender().sendMessage(Component.text("═══ Tables ═══", NamedTextColor.GOLD));
        for (String line : result.split("\n")) {
            ctx.getSource().getSender().sendMessage(Component.text("  • " + line, NamedTextColor.YELLOW));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int sqlExecuteQuery(CommandContext<CommandSourceStack> ctx) {
        LangService ls = GameManager.getInstance().getLangService();
        ELang lang = ls.resolveOrDefault(ctx.getSource().getSender());
        if (!DangerousCommandGuard.confirm(ctx, "sql.query", ls.text(lang, "cmd.sql.confirm_desc"))) {
            return Command.SINGLE_SUCCESS;
        }

        String sql = StringArgumentType.getString(ctx, "sql");

        ctx.getSource().getSender().sendMessage(Component.text("Executing: ", NamedTextColor.YELLOW).append(Component.text(sql, NamedTextColor.WHITE)));

        String result = GameManager.getInstance().getDatabase().executeRaw(sql);

        if (result.isEmpty()) {
            ctx.getSource().getSender().sendMessage(Component.text("(empty result)", NamedTextColor.GRAY));
        } else {
            ctx.getSource().getSender().sendMessage(Component.text("Result:", NamedTextColor.GREEN));
            for (String line : result.split("\n")) {
                ctx.getSource().getSender().sendMessage(Component.text("  " + line, NamedTextColor.WHITE));
            }
        }
        return Command.SINGLE_SUCCESS;
    }
}