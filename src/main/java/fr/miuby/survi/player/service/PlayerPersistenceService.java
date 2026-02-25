package fr.miuby.survi.player.service;

import fr.miuby.survi.system.database.Database;
import fr.miuby.survi.system.database.PlayerColumn;
import fr.miuby.survi.player.AlphaPlayer;

public final class PlayerPersistenceService {
    private final Database database;

    public PlayerPersistenceService(Database database) {
        this.database = database;
    }

    public void updateMort(AlphaPlayer alphaPlayer) {
        database.players().update(alphaPlayer.getUuid(), PlayerColumn.MORT, String.valueOf(alphaPlayer.getMort()));
    }

    public void updateSuccess(AlphaPlayer alphaPlayer) {
        database.players().update(alphaPlayer.getUuid(), PlayerColumn.SUCCESS, String.valueOf(alphaPlayer.getSuccess()));
    }

    public void updateRole(AlphaPlayer alphaPlayer) {
        if (alphaPlayer.getRole() == null) return;
        database.players().update(alphaPlayer.getUuid(), PlayerColumn.ROLE, alphaPlayer.getRole().type().toString());
    }

    public void updateSubRoles(AlphaPlayer alphaPlayer) {
        String value = String.join(",", alphaPlayer.getSubRoles().stream().map(subrole -> subrole.type().toString()).toList());
        database.players().update(alphaPlayer.getUuid(), PlayerColumn.SUBROLES, value);
    }
}
