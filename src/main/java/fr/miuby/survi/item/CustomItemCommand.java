package fr.miuby.survi.item;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("UnstableApiUsage")
public class CustomItemCommand {
    
    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("customitem")
            .requires(source -> source.getSender().hasPermission("survi.customitem"))
            .then(Commands.argument("item", StringArgumentType.word())
                .suggests((context, builder) -> {
                    for (ECustomItem item : ECustomItem.values())
                        builder.suggest(item.name());
                    return builder.buildFuture();
                })
                .executes(CustomItemCommand::giveItem)
            );
    }
    
    private static int giveItem(CommandContext<CommandSourceStack> context) {
        String itemName = context.getArgument("item", String.class);

        ItemStack item = ECustomItem.valueOf(itemName).getItemStack();

        if (context.getSource().getSender() instanceof Player player)
            player.getInventory().addItem(item);
        
        return Command.SINGLE_SUCCESS;
    }
}
