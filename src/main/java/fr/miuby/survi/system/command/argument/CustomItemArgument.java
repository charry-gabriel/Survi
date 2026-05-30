package fr.miuby.survi.system.command.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.miuby.lib.command.MLStringArgument;
import fr.miuby.survi.item.ECustomItem;
import fr.miuby.survi.system.command.CommandErrors;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collection;

public class CustomItemArgument extends MLStringArgument<ItemStack> {

    public static CustomItemArgument customItem() {
        return new CustomItemArgument();
    }

    @Override
    public ItemStack convert(String value) throws CommandSyntaxException {
        ECustomItem customItem = ECustomItem.fromString(value);
        if (customItem == null) throw CommandErrors.CUSTOM_ITEM_NOT_FOUND.create(value);
        return customItem.getItemStack();
    }

    @Override
    protected Collection<String> suggestions() {
        return Arrays.stream(ECustomItem.values()).map(Enum::toString).toList();
    }

    public static ItemStack getItemStack(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, ItemStack.class);
    }
}
