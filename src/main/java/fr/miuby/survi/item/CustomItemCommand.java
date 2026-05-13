package fr.miuby.survi.item;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.miuby.survi.system.command.argument.CustomItemArgument;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CustomItemCommand {
    private CustomItemCommand() {
        /* This utility class should not be instantiated */
    }
    
    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("customitem")
            .requires(source -> source.getSender().isOp())
            .then(Commands.argument("item", CustomItemArgument.customItem())
                .executes(CustomItemCommand::giveItem)
            );
    }
    
    private static int giveItem(CommandContext<CommandSourceStack> context) {
        ItemStack item = CustomItemArgument.getItemStack(context, "item");

        if (context.getSource().getSender() instanceof Player player)
            player.getInventory().addItem(item);
        
        return Command.SINGLE_SUCCESS;
    }
}
