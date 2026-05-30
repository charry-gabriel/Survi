package fr.miuby.survi.system.command.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.miuby.lib.command.MLStringArgument;
import fr.miuby.lib.world.MLWorld;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.system.command.CommandErrors;

import java.util.Collection;

public class WorldArgument extends MLStringArgument<MLWorld> {

    public static WorldArgument world() {
        return new WorldArgument();
    }

    @Override
    public MLWorld convert(String value) throws CommandSyntaxException {
        MLWorld world = WorldRegistry.get(value);
        if (world == null) throw CommandErrors.WORLD_NOT_FOUND.create(value);
        return world;
    }

    @Override
    protected Collection<String> suggestions() {
        return WorldRegistry.getAll().stream().map(MLWorld::getName).toList();
    }

    public static MLWorld getWorld(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, MLWorld.class);
    }
}
