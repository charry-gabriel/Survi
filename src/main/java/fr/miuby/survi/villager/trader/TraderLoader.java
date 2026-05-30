package fr.miuby.survi.villager.trader;

import fr.miuby.lib.resource.MLResourceManager;
import fr.miuby.survi.GameManager;

import java.util.List;

/**
 * Charge les configs de traders depuis {@code traders/<id>.yml}.
 * Le cache est géré par {@link MLResourceManager} — pas de doublon en mémoire.
 */
public final class TraderLoader {

    private TraderLoader() {}

    public static TraderConfig load(String id) {
        return MLResourceManager.loadPojo(GameManager.getInstance().getPlugin(), "traders", id, TraderConfig.class);
    }

    public static List<TraderConfig> loadAll() {
        return MLResourceManager.loadPojoAll(GameManager.getInstance().getPlugin(), "traders", TraderConfig.class);
    }
}