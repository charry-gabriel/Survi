package fr.miuby.survi.player;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.lib.player.MLPlayerRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.display.TutorialBookService;
import fr.miuby.survi.player.service.PlayerAttributeService;
import fr.miuby.survi.player.service.PlayerPersistenceService;
import fr.miuby.survi.player.service.PlayerEffectRestoreService;
import fr.miuby.survi.role.Role;
import fr.miuby.survi.system.exception.AlphaPlayerNotFoundException;
import fr.miuby.survi.system.log.ELogTag;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;

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

    public AlphaPlayer getAlphaPlayer(UUID uuid) {
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

    /**
     * Ajoute tous les joueurs en ligne dans le scoreboard fourni (celui du joueur qui vient de rejoindre).
     * Chaque joueur est placé dans une équipe colorée selon son monde et son rôle.
     * Les erreurs individuelles sont absorbées pour ne pas interrompre l'ensemble.
     */
    public void setPlayersToTeam(AlphaScoreboard scoreboard) {
        for (AlphaPlayer alphaPlayer : registry.getAll()) {
            if (alphaPlayer.getPlayer() == null) continue;
            try {
                scoreboard.getTeam(alphaPlayer).addPlayer(alphaPlayer);
            } catch (Exception e) {
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.PLAYER,
                        "[AlphaTeam] Impossible de créer l'équipe pour " + alphaPlayer.getPseudo(), e);
            }
        }
    }

    /**
     * Ajoute le joueur {@code player} dans le scoreboard de chacun des autres joueurs en ligne.
     * <p>
     * Note : on exclut intentionnellement le propre scoreboard de {@code player} — c'est
     * {@link #setPlayersToTeam(AlphaScoreboard)} (appelé juste après) qui s'en charge,
     * évitant ainsi la création de deux équipes redondantes pour ce joueur.
     */
    public void sendToPlayers(AlphaPlayer player) {
        for (AlphaPlayer alphaPlayer : registry.getAll()) {
            if (alphaPlayer.getPlayer() == null) continue;
            // On saute le propre scoreboard du joueur qui rejoint — setPlayersToTeam le couvre.
            if (alphaPlayer.getUuid().equals(player.getUuid())) continue;
            try {
                alphaPlayer.getScoreboard().getTeam(player).addPlayer(player);
            } catch (Exception e) {
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.PLAYER,
                        "[AlphaTeam] Impossible d'ajouter " + player.getPseudo() + " au scoreboard de " + alphaPlayer.getPseudo(), e);
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