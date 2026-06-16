package fr.miuby.survi.villager;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.command.CommandErrors;
import fr.miuby.survi.system.command.argument.VillagerArgument;
import fr.miuby.survi.villager.trader.Trader;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import fr.miuby.survi.world.WorldInitializer;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.FinePositionResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;

public class VillagerCommand {
    private static final String playerArgument = "player";
    private static final String locationArgument = "location";
    private static final String villagerArgument = "villager";

    private VillagerCommand() {
        /* This utility class should not be instantiated */
    }

    @SuppressWarnings("UnstableApiUsage")
    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("villager")
                .requires(sender -> sender.getSender().isOp())
                .then(Commands.argument(villagerArgument, VillagerArgument.villager())
                        .then(Commands.literal("teleport")
                                .executes(ctx -> VillagerCommand.villagerExecuteTeleport(ctx, ctx.getSource().getLocation()))
                                .then(Commands.argument(playerArgument, ArgumentTypes.player())
                                        .executes(ctx -> VillagerCommand.villagerExecuteTeleport(ctx, ctx.getArgument(playerArgument, PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst().getLocation()))
                                )
                                .then(Commands.argument(locationArgument, ArgumentTypes.finePosition(true))
                                        .executes(ctx -> VillagerCommand.villagerExecuteTeleport(ctx, ctx.getArgument(locationArgument, FinePositionResolver.class).resolve(ctx.getSource()).toLocation(WorldInitializer.getDefaultWorld())))
                                        .then(Commands.argument("yaw", DoubleArgumentType.doubleArg(0, 360))
                                                .then(Commands.argument("pitch", DoubleArgumentType.doubleArg(0, 360))
                                                        .executes(ctx -> {
                                                            Location location = ctx.getArgument(locationArgument, FinePositionResolver.class).resolve(ctx.getSource()).toLocation(WorldInitializer.getDefaultWorld());
                                                            location.setYaw((float) DoubleArgumentType.getDouble(ctx, "yaw"));
                                                            location.setPitch((float) DoubleArgumentType.getDouble(ctx, "pitch"));
                                                            return VillagerCommand.villagerExecuteTeleport(ctx, location);
                                                        })
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("levelup")
                                .executes(VillagerCommand::villagerExecuteLevelUp)
                        )
                        .then(Commands.literal("info")
                                .executes(VillagerCommand::villagerExecuteInfo)
                        )
                        .then(Commands.literal("unlock")
                                .executes(VillagerCommand::villagerExecuteUnlock)
                        )
                        .then(Commands.literal("reset")
                                .executes(VillagerCommand::villagerExecuteReset)
                        )
                );
    }

    private static int villagerExecuteReset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        AVillager villager = VillagerArgument.getVillager(ctx, villagerArgument);

        if (!(villager instanceof VillagerLevel villagerLevel)) {
            throw CommandErrors.NOT_A_LEVEL_VILLAGER.create();
        }

        villagerLevel.resetLevel();

        var ls = GameManager.getInstance().getLangService();
        var sender = ctx.getSource().getSender();
        sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.villager.reset_done",
                Placeholder.component("villager", villagerLevel.getDisplayName())));
        return Command.SINGLE_SUCCESS;
    }

    private static int villagerExecuteUnlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        AVillager villager = VillagerArgument.getVillager(ctx, villagerArgument);

        if (villager instanceof VillagerLevel villagerLevel) {
            if (villagerLevel.unlock()) {
                var ls = GameManager.getInstance().getLangService();
                var sender = ctx.getSource().getSender();
                sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.villager.unlocked"));
            }
        } else {
            throw CommandErrors.NOT_A_LEVEL_VILLAGER.create();
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int villagerExecuteInfo(CommandContext<CommandSourceStack> ctx) {
        AVillager villager = VillagerArgument.getVillager(ctx, villagerArgument);

        Component text = Component.text("Nom : ").append(villager.getDisplayName());
        if (villager instanceof VillagerLevel villagerLevel) {
            text = text
                    .appendNewline()
                    .append(Component.text("Level : " + villagerLevel.getLevel()))
                    .appendNewline()
                    .append(Component.text("unlock : " + villagerLevel.getRemainingLock()));
        } else if (villager instanceof Trader trader) {
            text = text
                    .appendNewline()
                    .append(Component.text("Job : " + trader.getJob()));
        }
        ctx.getSource().getSender().sendMessage(text);
        return Command.SINGLE_SUCCESS;
    }

    private static int villagerExecuteTeleport(CommandContext<CommandSourceStack> ctx, Location location) {
        AVillager villager = VillagerArgument.getVillager(ctx, villagerArgument);

        GameManager.getInstance().getDatabase().villagers().updateLocation(villager.getVillager().getUniqueId(), location);
        villager.getVillager().teleport(location);
        var ls = GameManager.getInstance().getLangService();
        var sender = ctx.getSource().getSender();
        sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.villager.teleported"));
        return Command.SINGLE_SUCCESS;
    }

    private static int villagerExecuteLevelUp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        AVillager villager = VillagerArgument.getVillager(ctx, villagerArgument);

        if (villager instanceof VillagerLevel villagerLevel) {
            if (!villagerLevel.levelUp()) {
                var ls = GameManager.getInstance().getLangService();
                var sender = ctx.getSource().getSender();
                sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.villager.already_max"));
            }
        } else {
            throw CommandErrors.NOT_A_LEVEL_VILLAGER.create();
        }
        return Command.SINGLE_SUCCESS;
    }
}