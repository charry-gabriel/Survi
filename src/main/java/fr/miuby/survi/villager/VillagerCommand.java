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
                    String remaining = builder.getRemaining().toLowerCase();
                    VillagerRegistry.getAll().stream()
                            .map(MLVillager::getNameId)
                            .filter(name -> name.toLowerCase().startsWith(remaining))
                            .forEach(builder::suggest);
                    return builder.buildFuture();
                })
                .then(Commands.literal("teleport")
                    .executes(ctx -> VillagerCommand.villagerExecuteTeleport(ctx, ctx.getSource().getLocation()))
                    .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(ctx -> VillagerCommand.villagerExecuteTeleport(ctx, ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst().getLocation()))
                    )
                    .then(Commands.argument("location", ArgumentTypes.finePosition(true))
                        .executes(ctx -> VillagerCommand.villagerExecuteTeleport(ctx, ctx.getArgument("location", FinePositionResolver.class).resolve(ctx.getSource()).toLocation(WorldInitializer.getDefaultWorld())))
                        .then(Commands.argument("yaw", DoubleArgumentType.doubleArg(0, 360))
                            .then(Commands.argument("pitch", DoubleArgumentType.doubleArg(0, 360))
                                .executes(ctx -> {
                                    Location location = ctx.getArgument("location", FinePositionResolver.class).resolve(ctx.getSource()).toLocation(WorldInitializer.getDefaultWorld());
                                    location.setYaw((float) DoubleArgumentType.getDouble(ctx, "yaw"));
                                    location.setPitch((float) DoubleArgumentType.getDouble(ctx, "pitch"));
                                    return VillagerCommand.villagerExecuteTeleport(ctx, location);
                                })
                            )
                        )
                    )
                )
                .then(Commands.literal("addlevel")
                    .executes(VillagerCommand::villagerExecuteAddLevel)
                )
                .then(Commands.literal("info")
                    .executes(VillagerCommand::villagerExecuteInfo)
                )
                .then(Commands.literal("unlock")
                        .executes(VillagerCommand::villagerExecuteUnlock)
                )
            );
    }

    private static int villagerExecuteUnlock(CommandContext<CommandSourceStack> ctx) {
        AVillager villager = (AVillager) VillagerRegistry.get(StringArgumentType.getString(ctx, "villager"));
        if (villager == null) {
            ctx.getSource().getSender().sendMessage(Component.text("Villager introuvable !"));
            return Command.SINGLE_SUCCESS;
        }

        if (villager instanceof VillagerLevel villagerLevel) {
            villagerLevel.handleUnlockTask();
        } else {
            ctx.getSource().getSender().sendMessage(Component.text("Villager is not a levelable villager !"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int villagerExecuteInfo(CommandContext<CommandSourceStack> ctx) {
        AVillager villager = (AVillager) VillagerRegistry.get(StringArgumentType.getString(ctx, "villager"));
        if (villager == null) {
            ctx.getSource().getSender().sendMessage(Component.text("Villager introuvable !"));
            return Command.SINGLE_SUCCESS;
        }

        Component text = Component.text("Nom : ").append(villager.getDisplayName());
        if (villager instanceof VillagerLevel villagerLevel) {
            text = text
                    .appendNewline()
                    .append(Component.text("Level : " + villagerLevel.getLevel()))
                    .appendNewline()
                    .append(Component.text("unlock : " + villagerLevel.getUnlockedDate()));
        } else if (villager instanceof Trader trader) {
            text = text
                    .appendNewline()
                    .append(Component.text("Reputation : " + GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayer(ctx.getSource().getSender().getName()).getReputation(trader.getNameId())));
        }
        ctx.getSource().getSender().sendMessage(text);
        return Command.SINGLE_SUCCESS;
    }

    private static int villagerExecuteTeleport(CommandContext<CommandSourceStack> ctx, Location location) {
        AVillager villager = (AVillager) VillagerRegistry.get(StringArgumentType.getString(ctx, "villager"));

        if (villager == null) {
            ctx.getSource().getSender().sendMessage(Component.text("Villager introuvable !"));
            return Command.SINGLE_SUCCESS;
        }

        GameManager.getInstance().getDatabase().updateVillagerLocation(villager.getVillager().getUniqueId(), location);
        villager.getVillager().teleport(location);
        return Command.SINGLE_SUCCESS;
    }

    private static int villagerExecuteAddLevel(CommandContext<CommandSourceStack> ctx) {
        AVillager villager = (AVillager) VillagerRegistry.get(StringArgumentType.getString(ctx, "villager"));

        if (villager == null) {
            ctx.getSource().getSender().sendMessage(Component.text("Villager introuvable !"));
            return Command.SINGLE_SUCCESS;
        }

        if (villager instanceof VillagerLevel villagerLevel) {
            if (!villagerLevel.levelUp())
                ctx.getSource().getSender().sendMessage(Component.text("Villager already at max level !"));
        } else {
            ctx.getSource().getSender().sendMessage(Component.text("Villager is not a levelable villager !"));
        }
        return Command.SINGLE_SUCCESS;
    }
}
