package fr.miuby.survi.system.command.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.miuby.lib.command.MLStringArgument;
import fr.miuby.lib.villager.MLVillager;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.system.command.CommandErrors;
import fr.miuby.survi.villager.AVillager;

import java.util.Collection;

public class VillagerArgument extends MLStringArgument<AVillager> {

    public static VillagerArgument villager() {
        return new VillagerArgument();
    }

    @Override
    public AVillager convert(String value) throws CommandSyntaxException {
        AVillager villager = (AVillager) VillagerRegistry.get(value);
        if (villager == null) throw CommandErrors.VILLAGER_NOT_FOUND.create(value);
        return villager;
    }

    @Override
    protected Collection<String> suggestions() {
        return VillagerRegistry.getAll().stream().map(MLVillager::getNameId).toList();
    }

    public static AVillager getVillager(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, AVillager.class);
    }
}
