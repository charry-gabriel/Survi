package fr.miuby.survi.villager;

import fr.miuby.lib.villager.MLVillager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class VillagerPostLoadActions {
    private static final Map<String, List<Consumer<MLVillager>>> actionsByVillagerId = new ConcurrentHashMap<>();

    private VillagerPostLoadActions() {
    }

    public static void add(String villagerId, Consumer<MLVillager> action) {
        actionsByVillagerId.computeIfAbsent(villagerId, ignored -> new CopyOnWriteArrayList<>()).add(action);
    }

    public static void executeAndClear(MLVillager villager) {
        if (villager == null) {
            return;
        }

        List<Consumer<MLVillager>> actions = actionsByVillagerId.remove(villager.getNameId());
        if (actions == null) {
            return;
        }

        for (Consumer<MLVillager> action : actions) {
            action.accept(villager);
        }
    }
}
