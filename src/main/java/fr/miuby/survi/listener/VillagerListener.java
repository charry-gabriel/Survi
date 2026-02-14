package fr.miuby.survi.listener;

import fr.miuby.lib.villager.MLVillager;
import fr.miuby.lib.villager.VillagerLoadedEvent;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.villager.VillagerPostLoadActions;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class VillagerListener implements Listener {

    @EventHandler
    public void onVillagerLoaded(VillagerLoadedEvent event) {
        MLVillager villager = event.getVillager();
        VillagerRegistry.register(villager);
        VillagerPostLoadActions.executeAndClear(villager);
    }
}