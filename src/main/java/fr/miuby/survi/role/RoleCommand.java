package fr.miuby.survi.role;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;

import fr.miuby.survi.system.command.AlphaPlayerArgument;
import fr.miuby.survi.system.command.RoleArgument;
import fr.miuby.survi.system.command.SubRoleArgument;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

public class RoleCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> createRoleCommand() {
        return Commands.literal("role")
                .requires(source -> source.getSender().isOp())
                .then(Commands.argument("player", AlphaPlayerArgument.alphaPlayer())
                        .then(Commands.argument("role", RoleArgument.role())
                                .executes(RoleCommand::executeRole)
                        )
                );
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createSubRoleCommand() {
        return Commands.literal("subrole")
                .requires(source -> source.getSender().isOp())
                .then(Commands.literal("add")
                        .then(Commands.argument("player", AlphaPlayerArgument.alphaPlayer())
                                .then(Commands.argument("subrole", SubRoleArgument.subrole())
                                        .executes(ctx -> executeSubRole(ctx, true))
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("player", AlphaPlayerArgument.alphaPlayer())
                                .then(Commands.argument("subrole", SubRoleArgument.subrole())
                                        .executes(ctx -> executeSubRole(ctx, false))
                                )
                        )
                );
    }

    private static int executeRole(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer alphaPlayer = AlphaPlayerArgument.getAlphaPlayer(ctx, "player");
        Role role = RoleArgument.getRole(ctx, "role");

        if (GameManager.getInstance().getRoleManagementService().changeMainRole(alphaPlayer, role)) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Le role ").color(NamedTextColor.GREEN)
                            .append(role.displayName())
                            .append(Component.text(" a ete ajouté a " + alphaPlayer.getPseudo()).color(NamedTextColor.GREEN))
            );
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int executeSubRole(CommandContext<CommandSourceStack> ctx, boolean isAdd) {
        AlphaPlayer alphaPlayer = AlphaPlayerArgument.getAlphaPlayer(ctx, "player");
        Role role = SubRoleArgument.getSubRole(ctx, "subrole");

        RoleManagementService service = GameManager.getInstance().getRoleManagementService();

        TextComponent message = Component.text("Le sous-role ").color(NamedTextColor.GREEN).append(role.displayName());
        boolean actionDone;
        if (isAdd) {
            message = message.append(Component.text(" a ete ajouté a ").color(NamedTextColor.GREEN));
            actionDone = service.addSubRole(alphaPlayer, role);
        } else {
            message = message.append(Component.text(" a ete supprimé a ").color(NamedTextColor.GREEN));
            actionDone = service.removeSubRole(alphaPlayer, role);
        }

        if (actionDone)
            ctx.getSource().getSender().sendMessage(message.append(Component.text(alphaPlayer.getPseudo() + " !").color(NamedTextColor.GREEN)));
        return Command.SINGLE_SUCCESS;
    }
}