package fr.miuby.lib;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VillagerRegistry {
    private static final Map<UUID, MLVillager> villagers = new HashMap<>();
    private static final Map<String, MLVillager> byName = new HashMap<>();

    private VillagerRegistry() {}

    public static void register(MLVillager villager) {
        villagers.put(villager.getVillager().getUniqueId(), villager);
        byName.put(villager.getNameId(), villager);
    }

    public static MLVillager get(UUID uuid) {
        return villagers.get(uuid);
    }

    public static MLVillager get(String name) {
        return byName.get(name);
    }

    public static Collection<MLVillager> getAll() {
        return new ArrayList<>(villagers.values());
    }
}

