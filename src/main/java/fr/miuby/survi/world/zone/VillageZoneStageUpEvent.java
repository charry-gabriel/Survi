package fr.miuby.survi.world.zone;

import fr.miuby.survi.world.config.VillageZoneConfig;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * lorsque la zone village passe au palier suivant pendant une partie en cours
 * (pas déclenché lors d'une réapplication forcée — boot, start, reload).
 */
@Getter
public class VillageZoneStageUpEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final int oldStageIndex;
    private final int newStageIndex;
    private final VillageZoneConfig.VillageZoneStage newStage;

    public VillageZoneStageUpEvent(int oldStageIndex, int newStageIndex, VillageZoneConfig.VillageZoneStage newStage) {
        this.oldStageIndex = oldStageIndex;
        this.newStageIndex = newStageIndex;
        this.newStage = newStage;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}