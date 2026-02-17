package fr.miuby.survi.player.service;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.database.PlayerColumn;
import fr.miuby.survi.player.AlphaPlayer;

public final class PlayerPersistenceService {

    public void updateMort(AlphaPlayer alphaPlayer) {
        GameManager.getInstance().getDatabase().updatePlayer(alphaPlayer.getUuid(), PlayerColumn.MORT, String.valueOf(alphaPlayer.getMort()));
    }

    public void updateSuccess(AlphaPlayer alphaPlayer) {
        GameManager.getInstance().getDatabase().updatePlayer(alphaPlayer.getUuid(), PlayerColumn.SUCCESS, String.valueOf(alphaPlayer.getSuccess()));
    }

    public void updateRole(AlphaPlayer alphaPlayer) {
        if (alphaPlayer.getRole() == null) return;
        GameManager.getInstance().getDatabase().updatePlayer(alphaPlayer.getUuid(), PlayerColumn.ROLE, alphaPlayer.getRole().type().toString());
    }

    public void updateSubRoles(AlphaPlayer alphaPlayer) {
        String value = String.join(",", alphaPlayer.getSubRoles().stream().map(subrole -> subrole.type().toString()).toList());
        GameManager.getInstance().getDatabase().updatePlayer(alphaPlayer.getUuid(), PlayerColumn.SUBROLES, value);
    }
}
