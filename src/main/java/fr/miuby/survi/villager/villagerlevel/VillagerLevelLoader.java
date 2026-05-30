package fr.miuby.survi.villager.villagerlevel;

import fr.miuby.lib.resource.MLResourceManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.villager.VillagerConfig;

/**
 * Charge les configs de villageois depuis {@code villagers/<id>.yml}.
 * Le cache est géré par {@link MLResourceManager} — pas de doublon en mémoire.
 */
public final class VillagerLevelLoader {

    private VillagerLevelLoader() {}

    public static VillagerConfig load(String id) {
        return MLResourceManager.loadPojo(GameManager.getInstance().getPlugin(), "villagers", id, VillagerConfig.class);
    }
}