package fr.miuby.survi.item.growth_item;

import fr.miuby.lib.utils.MultiKeyRegistry;
import fr.miuby.survi.item.growth_item.config.GrowthConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GrowthItemRegistry {
    private static final MultiKeyRegistry<GrowthConfig> INSTANCE = new MultiKeyRegistry<>();
    private static final List<String> ALL_IDS = new ArrayList<>();

    public static void register(String id, GrowthConfig config) {
        INSTANCE.register(config, id);
        ALL_IDS.add(id);
    }

    public static GrowthConfig get(String id) {
        return INSTANCE.get(id);
    }

    /** Retourne tous les IDs enregistrés (dans l'ordre de chargement). */
    public static List<String> getAllIds() {
        return Collections.unmodifiableList(ALL_IDS);
    }

    /** Vide le registre. À appeler avant un rechargement à chaud via {@link GrowthItemLoader#reload()}. */
    public static void clear() {
        INSTANCE.clear();
        ALL_IDS.clear();
    }

    private GrowthItemRegistry() {}
}