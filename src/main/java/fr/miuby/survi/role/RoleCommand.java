package fr.miuby.survi.role;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.player.event.AlphaPlayerRoleChangeEvent;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class RoleCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> createRoleCommand() {
        return Commands.literal("role")
                .requires(source -> source.getSender().isOp())
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            GameManager.getInstance().getAlphaPlayerFactory().getAllPseudo().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("role", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    GameManager.getInstance().getRoleRegistry().getRoles().stream()
                                            .map(r -> r.type().toString())
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(RoleCommand::executeRole)
                        )
                );
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createSubRoleCommand() {
        return Commands.literal("subrole")
                .requires(source -> source.getSender().isOp())
                .then(createSubRoleAddTree())
                .then(createSubRoleRemoveTree());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createSubRoleAddTree() {
        return Commands.literal("add")
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            GameManager.getInstance().getAlphaPlayerFactory().getAllPseudo().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("role", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    String pseudo = StringArgumentType.getString(context, "player");
                                    for (String r : getSubRoleSuggestions(pseudo, true)) {
                                        builder.suggest(r);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> executeSubRole(ctx, true))
                        )
                );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createSubRoleRemoveTree() {
        return Commands.literal("remove")
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            GameManager.getInstance().getAlphaPlayerFactory().getAllPseudo().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("role", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    String pseudo = StringArgumentType.getString(context, "player");
                                    for (String r : getSubRoleSuggestions(pseudo, false)) {
                                        builder.suggest(r);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> executeSubRole(ctx, false))
                        )
                );
    }

    private static List<String> getSubRoleSuggestions(String pseudo, boolean isAdd) {
        AlphaPlayer alphaPlayer;
        try {
            alphaPlayer = GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayer(pseudo);
        } catch (Exception e) {
            return new ArrayList<>();
        }

        List<String> roles = new ArrayList<>();
        if (isAdd) {
            roles.addAll(GameManager.getInstance().getRoleRegistry().getRoles().stream().map(r -> r.type().toString()).toList());
            roles.removeAll(alphaPlayer.getSubRoles().stream().map(r -> r.type().toString()).toList());
            if (alphaPlayer.getRole() != null) {
                roles.remove(alphaPlayer.getRole().type().toString());
            }
        } else {
            roles.addAll(alphaPlayer.getSubRoles().stream().map(r -> r.type().toString()).toList());
        }
        return roles;
    }

    private static int executeRole(CommandContext<CommandSourceStack> ctx) {
        String pseudo = StringArgumentType.getString(ctx, "player");
        String roleType = StringArgumentType.getString(ctx, "role");

        AlphaPlayer alphaPlayer;
        try {
            alphaPlayer = GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayer(pseudo);
        } catch (Exception e) {
            ctx.getSource().getSender().sendMessage(Component.text("Joueur introuvable !"));
            return Command.SINGLE_SUCCESS;
        }

        Role role = GameManager.getInstance().getRoleRegistry().getRole(roleType);
        if (role == null) {
            ctx.getSource().getSender().sendMessage(Component.text("Role introuvable !"));
            return Command.SINGLE_SUCCESS;
        }

        AlphaPlayerRoleChangeEvent event = new AlphaPlayerRoleChangeEvent(alphaPlayer, alphaPlayer.getRole(), role);
        GameManager.getInstance().callEvent(event);
        if (event.isCancelled()) {
            return Command.SINGLE_SUCCESS;
        }

        alphaPlayer.setRole(role);
        GameManager.getInstance().getAlphaPlayerFactory().getPersistenceService().updateRole(alphaPlayer);
        if (alphaPlayer.getPlayer() != null && alphaPlayer.getPlayer().isOnline()) {
            GameManager.getInstance().getAlphaPlayerFactory().sendToPlayers(alphaPlayer);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int executeSubRole(CommandContext<CommandSourceStack> ctx, boolean isAdd) {
        String pseudo = StringArgumentType.getString(ctx, "player");
        String roleType = StringArgumentType.getString(ctx, "role");

        AlphaPlayer alphaPlayer;
        try {
            alphaPlayer = GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayer(pseudo);
        } catch (Exception e) {
            ctx.getSource().getSender().sendMessage(Component.text("Joueur introuvable !"));
            return Command.SINGLE_SUCCESS;
        }

        Role role = GameManager.getInstance().getRoleRegistry().getRole(roleType);
        if (role == null) {
            ctx.getSource().getSender().sendMessage(Component.text("Role introuvable !"));
            return Command.SINGLE_SUCCESS;
        }

        AlphaPlayerRoleChangeEvent event;
        if (isAdd) {
            event = new AlphaPlayerRoleChangeEvent(alphaPlayer, null, role);
        } else {
            event = new AlphaPlayerRoleChangeEvent(alphaPlayer, role, null);
        }

        GameManager.getInstance().callEvent(event);
        if (event.isCancelled()) {
            return Command.SINGLE_SUCCESS;
        }

        if (isAdd) {
            alphaPlayer.addSubRole(role);
        } else {
            alphaPlayer.removeSubRole(role);
        }

        GameManager.getInstance().getAlphaPlayerFactory().getPersistenceService().updateSubRoles(alphaPlayer);
        if (alphaPlayer.getPlayer() != null && alphaPlayer.getPlayer().isOnline()) {
            GameManager.getInstance().getAlphaPlayerFactory().sendToPlayers(alphaPlayer);
        }
        return Command.SINGLE_SUCCESS;
    }
}