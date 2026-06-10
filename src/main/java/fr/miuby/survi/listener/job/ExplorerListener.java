package fr.miuby.survi.listener.job;

import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.config.JobsConfig;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.player.event.AlphaPlayerJobLevelUpEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Applique le seuil de chute sans dégâts de l'explorateur via un modifier
 * transient ADD_NUMBER sur {@code safe_fall_distance} (base vanilla = 3 blocs).
 *
 * <h3>Modifier par niveau (valeurs par défaut) :</h3>
 * <pre>
 *  niv.0 → −2,3 → seuil ≈ 0,7 → dégâts à partir de 2 blocs
 *  niv.1 → −1,3 → seuil ≈ 1,7 → dégâts à partir de 3 blocs
 *  niv.2 → −0,3 → seuil ≈ 2,7 → dégâts à partir de 4 blocs (vanilla)
 *  niv.N → safeFallDistance[N] − 3.0
 * </pre>
 *
 * <p>La distance par niveau est configurable dans {@code explorer.yml → explorer.safe-fall-distance}.
 * Le modifier est transient : réappliqué à chaque connexion.
 * La base n'est jamais modifiée ; le calcul des dégâts et Feather Falling
 * sont entièrement délégués au moteur vanilla.</p>
 */
public class ExplorerListener implements Listener {

    private static final double VANILLA_SAFE_FALL = 3.0;

    private final NamespacedKey modifierKey;

    public ExplorerListener(JavaPlugin plugin) {
        this.modifierKey = new NamespacedKey(plugin, "explorer_safe_fall_distance");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        AlphaPlayer alpha = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (alpha == null) return;
        applyFallModifier(event.getPlayer(), alpha.getJobLevel(EJob.EXPLORER));
    }

    @EventHandler
    public void onJobLevelUp(AlphaPlayerJobLevelUpEvent event) {
        if (event.getJob() != EJob.EXPLORER) return;
        Player player = event.getAlphaPlayer().getPlayer();
        if (player == null || !player.isOnline()) return;
        applyFallModifier(player, event.getNewLevel());
    }

    private void applyFallModifier(Player player, int level) {
        AttributeInstance attr = player.getAttribute(Attribute.SAFE_FALL_DISTANCE);
        if (attr == null) return;

        AttributeModifier existing = attr.getModifier(modifierKey);
        if (existing != null) attr.removeModifier(existing);

        double targetFall = JobsConfig.getInstance().getExplorer().getSafeFallDistance()[level];
        double delta = targetFall - VANILLA_SAFE_FALL;
        attr.addTransientModifier(new AttributeModifier(modifierKey, delta, AttributeModifier.Operation.ADD_NUMBER));
    }
}
