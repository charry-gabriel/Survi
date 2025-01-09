package fr.miuby.survi.villager;

import fr.miuby.survi.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class VillagerTabCompleter implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("villager"))
        {
            if (args.length == 1)
            {
                List<String> commands = new ArrayList<>();
                commands.add("teleport");

                return StringUtil.copyPartialMatches(args[0],
                        commands,
                        new ArrayList<>());
            }
            else if (args.length == 2)
            {
                if (args[0].equals("teleport"))
                {
                    return StringUtil.copyPartialMatches(args[1],
                            GameManager.getInstance().getVillagerFactory().getVillagers().values().stream().map(villager -> villager.nameId).toList(),
                            new ArrayList<>());
                }
            }
        }
        return null;
    }
}
