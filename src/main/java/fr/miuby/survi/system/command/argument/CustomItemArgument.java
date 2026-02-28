package fr.miuby.survi.system.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.miuby.survi.item.ECustomItem;
import fr.miuby.survi.system.command.CommandErrors;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class CustomItemArgument implements CustomArgumentType.Converted<ItemStack, String> {

    public static CustomItemArgument customItem() {
        return new CustomItemArgument();
    }

    @Override
    public ItemStack convert(String nativeType) throws CommandSyntaxException {
        ECustomItem customItem = ECustomItem.fromString(nativeType);

        if (customItem == null) {
            throw CommandErrors.CUSTOM_ITEM_NOT_FOUND.create(nativeType);
        }

        return customItem.getItemStack();
    }

    @Override
    public @NonNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    @Override
    public <S> @NonNull CompletableFuture<Suggestions> listSuggestions(@NonNull CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();

        Arrays.stream(ECustomItem.values()).map(Enum::toString).filter(name -> name.toLowerCase().startsWith(remaining)).forEach(builder::suggest);

        return builder.buildFuture();
    }

    public static ItemStack getItemStack(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, ItemStack.class);
    }
}