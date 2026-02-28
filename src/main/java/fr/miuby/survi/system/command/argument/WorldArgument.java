package fr.miuby.survi.system.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.miuby.lib.world.MLWorld;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.system.command.CommandErrors;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class WorldArgument implements CustomArgumentType.Converted<MLWorld, String> {

    public static WorldArgument world() {
        return new WorldArgument();
    }

    @Override
    public MLWorld convert(String nativeType) throws CommandSyntaxException {
        MLWorld world = WorldRegistry.get(nativeType);

        if (world == null) {
            throw CommandErrors.WORLD_NOT_FOUND.create(nativeType);
        }

        return world;
    }

    @Override
    public @NonNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    @Override
    public <S> @NonNull CompletableFuture<Suggestions> listSuggestions(@NonNull CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();

        WorldRegistry.getAll().stream().map(MLWorld::getName).filter(name -> name.toLowerCase().startsWith(remaining)).forEach(builder::suggest);

        return builder.buildFuture();
    }

    public static MLWorld getWorld(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, MLWorld.class);
    }
}