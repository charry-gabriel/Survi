package fr.miuby.survi.role;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.player.event.AlphaPlayerRoleChangeEvent;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

/*@SuppressWarnings("UnstableApiUsage")
public class RoleCommand implements BasicCommand {

    @Override
    public void execute(@NotNull CommandSourceStack source, String[] args) {
        AlphaPlayer alphaPlayer = GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayer(args[0]);

        Role role = GameManager.getInstance().getRoleFactory().getRole(args[1]);
        if (role == null) {
            source.getSender().sendMessage(Component.text("Role introuvable !"));
            return;
        }

        // Call event
        AlphaPlayerRoleChangeEvent alphaPlayerRoleChangeEvent = new AlphaPlayerRoleChangeEvent(alphaPlayer, alphaPlayer.getRole(), role);
        GameManager.getInstance().callEvent(alphaPlayerRoleChangeEvent);
        if (alphaPlayerRoleChangeEvent.isCancelled())
            return;

        // Swap role
        GameManager.getInstance().getDatabase().updatePlayer(alphaPlayer.getUUID(), "role", role.type().toString());
        alphaPlayer.setRole(role);

        if (alphaPlayer.getPlayer().isOnline())
            GameManager.getInstance().getAlphaPlayerFactory().sendToPlayers(alphaPlayer);
    }

    @Override
    public boolean canUse(@NotNull CommandSender sender) {
        String permission = this.permission();
        return (permission == null || sender.hasPermission(permission)) && sender.isOp();
    }

    @Override
    public @Nullable String permission() {
        return "role";
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack command, String @NotNull [] args) {
        if (args.length == 1) {
            return GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers().values().stream()
                    .map(AlphaPlayer::getPseudo)
                    .filter(argument -> argument.startsWith(args[0]))
                    .toList();
        } else if (args.length == 2) {
            return GameManager.getInstance().getRoleFactory().getRoles().stream()
                    .map(role -> role.type().toString())
                    .filter(argument -> argument.startsWith(args[1]))
                    .toList();
        }
        return new ArrayList<>();
    }
}*/
/*
@SuppressWarnings("UnstableApiUsage")
public class RoleCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {

        LiteralArgumentBuilder<CommandSourceStack> roleCmd = Commands.literal("role");
        RequiredArgumentBuilder<CommandSourceStack, String> playerArg = Commands.argument("player", StringArgumentType.word());
        RequiredArgumentBuilder<CommandSourceStack, String> roleArg = Commands.argument("role", StringArgumentType.word());

        playerArg.suggests((context, builder) -> {
            GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers().values().stream()
                    .map(AlphaPlayer::getPseudo)
                    .filter(argument -> argument.startsWith(context.getChild().getArgument("player", String.class)))
                    .forEach(builder::suggest);
            return builder.buildFuture();
        });

        roleArg.suggests((context, builder) -> {
            var ok = context.getLastChild();
            GameManager.getInstance().getRoleFactory().getRoles().stream()
                    .map(role -> role.type().toString())
                    .filter(argument -> argument.startsWith(context.getChild().getArgument("role", String.class)))
                    .forEach(builder::suggest);
            return builder.buildFuture();
        });

        return roleCmd
            .requires(sender -> sender.getSender().isOp())
            .requires(sender -> sender.getSender().hasPermission("permission.role"))
            .then(playerArg
                .then(roleArg
                    .executes(RoleCommand::roleExecute)));
    }

    @SuppressWarnings("SameReturnValue")
    private static int roleExecute(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        AlphaPlayer alphaPlayer = GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayer(StringArgumentType.getString(ctx, "player"));

        Role role = GameManager.getInstance().getRoleFactory().getRole(ERole.valueOf(StringArgumentType.getString(ctx, "role")));
        if (role == null) {
            sender.sendMessage(Component.text("Role introuvable !"));
            return Command.SINGLE_SUCCESS;
        }

        // Call event
        AlphaPlayerRoleChangeEvent alphaPlayerRoleChangeEvent = new AlphaPlayerRoleChangeEvent(alphaPlayer, alphaPlayer.getRole(), role);
        GameManager.getInstance().callEvent(alphaPlayerRoleChangeEvent);
        if (alphaPlayerRoleChangeEvent.isCancelled())
            return Command.SINGLE_SUCCESS;

        // Swap role
        GameManager.getInstance().getDatabase().updatePlayer(alphaPlayer.getUUID(), "role", role.type().toString());
        alphaPlayer.setRole(role);

        if (alphaPlayer.getPlayer().isOnline())
            GameManager.getInstance().getAlphaPlayerFactory().sendToPlayers(alphaPlayer);
        return Command.SINGLE_SUCCESS;
    }
}*/