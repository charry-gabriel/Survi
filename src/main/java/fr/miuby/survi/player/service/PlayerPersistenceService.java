package fr.miuby.survi.player.service;

import fr.miuby.survi.system.database.EPlayerColumn;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.database.repository.PlayerRepository;

public final class PlayerPersistenceService {
    private final PlayerRepository playerRepository;

    public PlayerPersistenceService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public void updateDeath(AlphaPlayer alphaPlayer) {
        playerRepository.update(alphaPlayer.getUuid(), EPlayerColumn.DEATH, String.valueOf(alphaPlayer.getDeath()));
    }

    public void updateSuccess(AlphaPlayer alphaPlayer) {
        playerRepository.update(alphaPlayer.getUuid(), EPlayerColumn.SUCCESS, String.valueOf(alphaPlayer.getSuccess()));
    }

    public void updateRole(AlphaPlayer alphaPlayer) {
        if (alphaPlayer.getRole() == null) return;
        playerRepository.update(alphaPlayer.getUuid(), EPlayerColumn.ROLE, alphaPlayer.getRole().type().toString());
    }

    public void updateSubRoles(AlphaPlayer alphaPlayer) {
        String value = String.join(",", alphaPlayer.getSubRoles().stream().map(subrole -> subrole.type().toString()).toList());
        playerRepository.update(alphaPlayer.getUuid(), EPlayerColumn.SUBROLES, value);
    }
}
