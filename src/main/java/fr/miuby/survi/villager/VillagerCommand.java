package fr.miuby.survi.villager;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.miuby.lib.villager.MLVillager;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.world.WorldInitializer;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.FinePositionResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;

public class VillagerCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("villager")
            .requires(sender -> sender.getSender().isOp())
            .then(Commands.argument("villager", StringArgumentType.word())
                .suggests((context, builder) -> {
                    VillagerRegistry.getAll().stream()
                            .map(MLVillager::getNameId)
                            .forEach(builder::suggest);
                    return builder.buildFuture();
                })
                .then(Commands.literal("teleport")
                    .executes(ctx -> VillagerCommand.villagerExecute(ctx, ctx.getSource().getLocation()))
                    .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(ctx -> VillagerCommand.villagerExecute(ctx, ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst().getLocation()))
                    )
                    .then(Commands.argument("location", ArgumentTypes.finePosition(true))
                        .executes(ctx -> VillagerCommand.villagerExecute(ctx, ctx.getArgument("location", FinePositionResolver.class).resolve(ctx.getSource()).toLocation(WorldInitializer.getDefaultWorld())))
                        .then(Commands.argument("yaw", DoubleArgumentType.doubleArg(0, 360))
                            .then(Commands.argument("pitch", DoubleArgumentType.doubleArg(0, 360))
                                .executes(ctx -> {
                                    Location location = ctx.getArgument("location", FinePositionResolver.class).resolve(ctx.getSource()).toLocation(WorldInitializer.getDefaultWorld());
                                    location.setYaw((float) DoubleArgumentType.getDouble(ctx, "yaw"));
                                    location.setPitch((float) DoubleArgumentType.getDouble(ctx, "pitch"));
                                    return VillagerCommand.villagerExecute(ctx, location);
                                })
                            )
                        )
                    )
                )
            );
    }

    private static int villagerExecute(CommandContext<CommandSourceStack> ctx, Location location) {
        AVillager villager = (AVillager) VillagerRegistry.get(StringArgumentType.getString(ctx, "villager"));

        if (villager == null) {
            ctx.getSource().getSender().sendMessage(Component.text("Villager introuvable !"));
            return Command.SINGLE_SUCCESS;
        }

        GameManager.getInstance().getDatabase().updateVillagerLocation(villager.getVillager().getUniqueId(), location);
        villager.getVillager().teleport(location);
        return Command.SINGLE_SUCCESS;
    }
}
