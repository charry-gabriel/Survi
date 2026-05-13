package fr.miuby.survi.world;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.miuby.lib.world.MLWorld;
import fr.miuby.survi.system.command.argument.AlphaPlayerArgument;
import fr.miuby.survi.system.command.argument.WorldArgument;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.FinePositionResolver;
import org.bukkit.Location;

public class WorldCommand {
    private static final String playerArgument = "player";
    private static final String worldArgument = "MLworld";

    private WorldCommand() {
        /* This utility class should not be instantiated */
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createWorldResetCommand() {
        return Commands.literal("worldreset")
                .requires(source -> source.getSender().isOp())
                .executes(ctx -> {
                    WorldResetManager.getInstance().performReset();
                    return Command.SINGLE_SUCCESS;
                });
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createTeleportToCommand() {
        return Commands.literal("teleportTo")
                .requires(source -> source.getSender().isOp())
                .then(Commands.argument(playerArgument, AlphaPlayerArgument.alphaPlayer())
                        .then(Commands.argument(worldArgument, WorldArgument.world())
                                .executes(ctx -> {
                                    MLWorld mlWorld = WorldArgument.getWorld(ctx, worldArgument);
                                    AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument).getPlayer().teleport(mlWorld.getWorld().getSpawnLocation());
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(Commands.argument("location", ArgumentTypes.finePosition(true))
                                        .executes(ctx -> {
                                            Location location = ctx.getArgument("location", FinePositionResolver.class)
                                                    .resolve(ctx.getSource())
                                                    .toLocation(WorldArgument.getWorld(ctx, worldArgument).getWorld());
                                            AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument).getPlayer().teleport(location);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                );
    }
}