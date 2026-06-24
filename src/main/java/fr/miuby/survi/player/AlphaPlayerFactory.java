package fr.miuby.survi.player;

import fr.miuby.lib.player.MLPlayerRegistry;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.display.TutorialBookService;
import fr.miuby.survi.job.ExplorerAttributeService;
import fr.miuby.survi.job.FishermanAttributeService;
import fr.miuby.survi.player.service.PlayerAttributeService;
import fr.miuby.survi.player.service.PlayerPersistenceService;
import fr.miuby.survi.player.service.PlayerEffectRestoreService;
import fr.miuby.survi.role.Role;
import fr.miuby.survi.system.database.EPlayerLoadResult;
import fr.miuby.survi.system.exception.AlphaPlayerNotFoundException;
import fr.miuby.survi.system.log.ELogTag;
import lombok.Getter;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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
    @Getter
    private final FishermanAttributeService fishermanAttributeService = new FishermanAttributeService();
    @Getter
    private final ExplorerAttributeService explorerAttributeService = new ExplorerAttributeService();

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

    public void onPlayerJoin(Player bukkitPlayer) {
        AlphaPlayer alphaPlayer = get(bukkitPlayer.getUniqueId());

        if (alphaPlayer == null) {
            // Absent du registre mémoire : avant de supposer "nouveau joueur", on vérifie directement
            // en BDD. Sans ce filet de sécurité, toute désync mémoire/BDD (ligne ignorée au démarrage,
            // redémarrage entre deux connexions, etc.) ferait passer un joueur existant pour nouveau et
            // écraserait silencieusement son profil (rôle, sous-rôles, morts, succès) par des valeurs
            // par défaut dans le registre — la BDD elle-même n'est touchée que plus tard, au premier
            // appel d'update, mais c'est suffisant pour perdre la progression réelle.
            EPlayerLoadResult result = GameManager.getInstance().getDatabase().players().tryReloadPlayer(bukkitPlayer.getUniqueId());

            switch (result) {
                case FOUND -> alphaPlayer = get(bukkitPlayer.getUniqueId());
                case ERROR -> {
                    MLLogManager.getInstance().log(Level.SEVERE, ELogTag.PLAYER,
                            "Connexion refusée pour " + bukkitPlayer.getName()
                                    + " : impossible de vérifier ses données en base (état indéterminé), pour éviter d'écraser une progression existante.");
                    bukkitPlayer.kick(GameManager.getInstance().getLangService().text(bukkitPlayer, "player.db_unavailable"));
                    return;
                }
                case NOT_FOUND -> {
                    GameManager.getInstance().getDatabase().players().create(bukkitPlayer);
                    alphaPlayer = registerAlphaPlayer(bukkitPlayer.getUniqueId(), bukkitPlayer.getName(), GameManager.getInstance().getRoleLoader().getDefaultRole());

                    GameManager.getInstance().getPlugin().getServer().getScheduler().runTaskLater(
                            GameManager.getInstance().getPlugin(),
                            () -> TutorialBookService.giveTutorialBook(bukkitPlayer),
                            1L
                    );
                }
            }
        }

        alphaPlayer.setPlayer(bukkitPlayer);

        // Vie restaurée par Minecraft depuis le playerdata = vie exacte avant la déco.
        // On la capture avant tout appel à notre code qui pourrait la modifier.
        double savedHealth = !bukkitPlayer.isDead() ? bukkitPlayer.getHealth() : 0;

        // restoreAttributesOnJoin() pose mort/succès/blessing SANS clamp (voir AlphaLife).
        // applyAllRoleAttributesOnJoin() pose les modifiers de rôle SANS regenHealth.
        alphaPlayer.onJoinServer();
        attributeService.applyAllRoleAttributesOnJoin(alphaPlayer);
        fishermanAttributeService.applyAttributes(alphaPlayer);
        explorerAttributeService.applyAttributes(alphaPlayer);

        // Clamp final unique : tous les modifiers sont posés, on ramène la vie sauvegardée
        // dans [1, maxEffectif]. Ignoré si armorMalus actif (vie déjà forcée à 0.01f).
        if (!bukkitPlayer.isDead() && !alphaPlayer.getAlphaLife().isArmorMalus()) {
            AttributeInstance maxHealthAttr = bukkitPlayer.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double finalHealth = Math.clamp(savedHealth, 1.0, maxHealthAttr.getValue());
                bukkitPlayer.setHealth(finalHealth);
                MLLogManager.getInstance().log(Level.FINE, ELogTag.PLAYER,
                        "[onPlayerJoin] " + alphaPlayer.getPseudo()
                                + " saved=" + savedHealth
                                + " final=" + finalHealth
                                + " max=" + maxHealthAttr.getValue());
            }
        }
    }

    public AlphaPlayer registerAlphaPlayer(UUID uuid, String pseudo, Role role) {
        AlphaPlayer alphaPlayer = new AlphaPlayer(uuid);
        alphaPlayer.setPseudo(pseudo);
        alphaPlayer.setRole(role);
        registry.register(alphaPlayer);
        return alphaPlayer;
    }
}