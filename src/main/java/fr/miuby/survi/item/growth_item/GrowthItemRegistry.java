package fr.miuby.survi.item.growth_item;

import fr.miuby.survi.item.growth_item.config.GrowthConfig;

import java.util.HashMap;

import java.util.Map;

public final class GrowthItemRegistry {
    private static final Map<String, GrowthConfig> REGISTRY = new HashMap<>();

    public static void register(String id, GrowthConfig config) {
        REGISTRY.putIfAbsent(id, config);
    }

    public static GrowthConfig get(String id) {
        return REGISTRY.get(id);
    }

    private GrowthItemRegistry() {}
}
