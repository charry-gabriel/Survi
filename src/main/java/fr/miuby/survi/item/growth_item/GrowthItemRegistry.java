package fr.miuby.survi.item.growth_item;

import fr.miuby.lib.utils.MultiKeyRegistry;
import fr.miuby.survi.item.growth_item.config.GrowthConfig;

public final class GrowthItemRegistry {
    private static final MultiKeyRegistry<GrowthConfig> INSTANCE = new MultiKeyRegistry<>();

    public static void register(String id, GrowthConfig config) {
        INSTANCE.register(config, id);
    }

    public static GrowthConfig get(String id) {
        return INSTANCE.get(id);
    }

    /** Vide le registre. À appeler avant un rechargement à chaud via {@link GrowthItemLoader#reload()}. */
    public static void clear() {
        INSTANCE.clear();
    }

    private GrowthItemRegistry() {}
}