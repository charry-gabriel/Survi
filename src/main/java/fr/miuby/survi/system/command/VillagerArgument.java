package fr.miuby.survi.system.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.miuby.lib.villager.MLVillager;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.villager.AVillager;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class VillagerArgument implements CustomArgumentType.Converted<AVillager, String> {

    public static VillagerArgument villager() {
        return new VillagerArgument();
    }

    @Override
    public AVillager convert(String nativeType) throws CommandSyntaxException {
        AVillager villager = (AVillager) VillagerRegistry.get(nativeType);

        if (villager == null) {
            throw CommandErrors.VILLAGER_NOT_FOUND.create(nativeType);
        }

        return villager;
    }

    @Override
    public @NonNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    @Override
    public <S> @NonNull CompletableFuture<Suggestions> listSuggestions(@NonNull CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();

        VillagerRegistry.getAll().stream().map(MLVillager::getNameId).filter(name -> name.toLowerCase().startsWith(remaining)).forEach(builder::suggest);

        return builder.buildFuture();
    }

    public static AVillager getVillager(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, AVillager.class);
    }
}