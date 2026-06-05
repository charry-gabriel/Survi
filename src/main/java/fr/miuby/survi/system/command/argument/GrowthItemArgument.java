package fr.miuby.survi.system.command.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.miuby.lib.command.MLStringArgument;
import fr.miuby.survi.item.growth_item.GrowthItemRegistry;
import fr.miuby.survi.system.command.CommandErrors;
import org.jspecify.annotations.NonNull;

import java.util.Collection;

public class GrowthItemArgument extends MLStringArgument<String> {

    public static GrowthItemArgument growthItem() {
        return new GrowthItemArgument();
    }

    /** Valide et retourne l'ID en majuscules (ex. {@code "GROWTH_PICKAXE"}). */
    @Override
    public @NonNull String convert(String value) throws CommandSyntaxException {
        String id = value.toUpperCase();
        if (GrowthItemRegistry.get(id) == null) throw CommandErrors.GROWTH_ITEM_NOT_FOUND.create(value);
        return id;
    }

    @Override
    protected Collection<String> suggestions() {
        return GrowthItemRegistry.getAllIds();
    }

    public static String getId(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, String.class);
    }
}