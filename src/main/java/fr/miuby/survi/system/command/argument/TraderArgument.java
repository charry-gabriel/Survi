package fr.miuby.survi.system.command.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.miuby.lib.command.MLStringArgument;
import fr.miuby.lib.villager.MLVillager;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.system.command.CommandErrors;
import fr.miuby.survi.villager.trader.Trader;

import java.util.Collection;

public class TraderArgument extends MLStringArgument<Trader> {

    public static TraderArgument trader() {
        return new TraderArgument();
    }

    @Override
    public Trader convert(String value) throws CommandSyntaxException {
        var villager = VillagerRegistry.get(value);
        if (villager instanceof Trader trader) return trader;
        throw CommandErrors.VILLAGER_NOT_FOUND.create(value);
    }

    @Override
    protected Collection<String> suggestions() {
        return VillagerRegistry.getAll().stream()
                .filter(v -> v instanceof Trader)
                .map(MLVillager::getNameId)
                .toList();
    }

    public static Trader getTrader(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, Trader.class);
    }
}
