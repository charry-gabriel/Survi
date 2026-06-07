package fr.miuby.survi.listener.job;

import fr.miuby.survi.job.EJob;
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
 * Applique le seuil de chute sans dégâts de l'aventurier via un modifier
 * transient ADD_NUMBER sur {@code safe_fall_distance} (base vanilla = 3 blocs).
 *
 * <h3>Modifier par niveau :</h3>
 * <pre>
 *  niv.0 → -2 → seuil = 1 → dégâts à partir de 2 blocs
 *  niv.1 → -1 → seuil = 2 → dégâts à partir de 3 blocs
 *  niv.2 →  0 → seuil = 3 → dégâts à partir de 4 blocs (vanilla)
 *  niv.N → N - 2
 * </pre>
 *
 * <p>Le modifier est transient : reappliqué à chaque connexion.
 * La base n'est jamais modifiée ; le calcul des dégâts et Feather Falling
 * sont entièrement délégués au moteur vanilla.</p>
 */
public class AventurierListener implements Listener {

    private static final double VANILLA_SAFE_FALL = 3.0;

    private final NamespacedKey modifierKey;

    public AventurierListener(JavaPlugin plugin) {
        this.modifierKey = new NamespacedKey(plugin, "aventurier_safe_fall_distance");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        AlphaPlayer alpha = AlphaPlayer.get(event.getPlayer().getUniqueId());
        applyFallModifier(event.getPlayer(), alpha.getJobLevel(EJob.AVENTURIER));
    }

    @EventHandler
    public void onJobLevelUp(AlphaPlayerJobLevelUpEvent event) {
        if (event.getJob() != EJob.AVENTURIER) return;
        Player player = event.getAlphaPlayer().getPlayer();
        if (player == null || !player.isOnline()) return;
        applyFallModifier(player, event.getNewLevel());
    }

    private void applyFallModifier(Player player, int level) {
        AttributeInstance attr = player.getAttribute(Attribute.SAFE_FALL_DISTANCE);
        if (attr == null) return;

        AttributeModifier existing = attr.getModifier(modifierKey);
        if (existing != null) attr.removeModifier(existing);

        double delta = (level + 0.7) - VANILLA_SAFE_FALL;
        attr.addTransientModifier(new AttributeModifier(modifierKey, delta, AttributeModifier.Operation.ADD_NUMBER));
    }
}