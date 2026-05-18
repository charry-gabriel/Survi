package fr.miuby.survi.system.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.miuby.lib.villager.MLVillager;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.system.command.CommandErrors;
import fr.miuby.survi.villager.trader.Trader;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class TraderArgument implements CustomArgumentType.Converted<Trader, String> {

    public static TraderArgument trader() {
        return new TraderArgument();
    }

    @Override
    public Trader convert(String nativeType) throws CommandSyntaxException {
        var villager = VillagerRegistry.get(nativeType);

        if (villager instanceof Trader trader) {
            return trader;
        }

        throw CommandErrors.VILLAGER_NOT_FOUND.create(nativeType);
    }

    @Override
    public @NonNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    @Override
    public <S> @NonNull CompletableFuture<Suggestions> listSuggestions(@NonNull CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();

        VillagerRegistry.getAll().stream()
                .filter(v -> v instanceof Trader)
                .map(MLVillager::getNameId)
                .filter(name -> name.toLowerCase().startsWith(remaining))
                .forEach(builder::suggest);

        return builder.buildFuture();
    }

    public static Trader getTrader(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, Trader.class);
    }
}