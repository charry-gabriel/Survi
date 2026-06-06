package fr.miuby.survi.player;

import fr.miuby.lib.player.MLPlayerRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.display.TutorialBookService;
import fr.miuby.survi.player.service.PlayerAttributeService;
import fr.miuby.survi.player.service.PlayerPersistenceService;
import fr.miuby.survi.player.service.PlayerEffectRestoreService;
import fr.miuby.survi.role.Role;
import fr.miuby.survi.system.exception.AlphaPlayerNotFoundException;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

public class AlphaPlayerFactory {
    private final MLPlayerRegistry<AlphaPlayer> registry = new MLPlayerRegistry<>();
    @Getter
    private final PlayerAttributeService attributeService = new PlayerAttributeService();
    @Getter
    private final PlayerPersistenceService persistenceService = new PlayerPersistenceService(GameManager.getInstance().getDatabase().players());
    @Getter
    private final PlayerEffectRestoreService effectRestoreService = new PlayerEffectRestoreService();

    public AlphaPlayer get(UUID uuid) {
        return registry.get(uuid);
    }

    public Collection<AlphaPlayer> getAlphaPlayers() {
        return registry.getAll();
    }

    public Collection<String> getAllPseudo() {
        return registry.getAll().stream().map(AlphaPlayer::getPseudo).toList();
    }

    public AlphaPlayer getAlphaPlayer(UUID uuid){
        AlphaPlayer alphaPlayer = registry.get(uuid);
        if (alphaPlayer == null)
            throw new AlphaPlayerNotFoundException(uuid);
        return alphaPlayer;
    }

    public AlphaPlayer getAlphaPlayer(String pseudo) {
        AlphaPlayer alphaPlayer = registry.get(pseudo);
        if (alphaPlayer == null)
            throw new AlphaPlayerNotFoundException(pseudo);
        return alphaPlayer;
    }

    public void setPlayersToTeam(AlphaScoreboard scoreboard) {
        for(AlphaPlayer alphaPlayer : registry.getAll()) {
            if(alphaPlayer.getPlayer() != null)
                scoreboard.getTeam(alphaPlayer).addPlayer(alphaPlayer);
        }
    }

    public void sendToPlayers(AlphaPlayer player) {
        for(AlphaPlayer alphaPlayer : registry.getAll()) {
            if(alphaPlayer.getPlayer() != null) {
                alphaPlayer.getScoreboard().getTeam(player).addPlayer(player);
            }
        }
    }

    public void onPlayerJoin(Player bukkitPlayer) {
        AlphaPlayer alphaPlayer = get(bukkitPlayer.getUniqueId());

        // if player doesn't exist in database, create it
        if (alphaPlayer == null) {
            GameManager.getInstance().getDatabase().players().create(bukkitPlayer);
            alphaPlayer = registerAlphaPlayer(bukkitPlayer.getUniqueId(), bukkitPlayer.getName(), GameManager.getInstance().getRoleLoader().getDefaultRole());

            GameManager.getInstance().getPlugin().getServer().getScheduler().runTaskLater(
                    GameManager.getInstance().getPlugin(),
                    () -> TutorialBookService.giveTutorialBook(bukkitPlayer),
                    1L
            );
        }

        alphaPlayer.setPlayer(bukkitPlayer);
        alphaPlayer.onJoinServer();
        attributeService.applyAllRoleAttributes(alphaPlayer);
    }

    public AlphaPlayer registerAlphaPlayer(UUID uuid, String pseudo, Role role) {
        AlphaPlayer alphaPlayer = new AlphaPlayer(uuid);
        alphaPlayer.setPseudo(pseudo);
        alphaPlayer.setRole(role);
        registry.register(alphaPlayer);
        return alphaPlayer;
    }
}