package fr.miuby.survi.quest;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.villager.trader.Trader;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gère l'effet de glow per-joueur sur le Trader chez qui le joueur a une quête
 * terminée non réclamée.
 *
 * <p>Envoie un paquet NMS {@link ClientboundSetEntityDataPacket} ciblant uniquement
 * le joueur concerné — les autres joueurs ne voient aucun changement.</p>
 *
 * <p>Pas de refresh périodique nécessaire : les Traders n'ont ni IA ni combat,
 * le serveur n'émettra jamais un EntityMetadata qui écraserait le flag glow.</p>
 *
 * <p><b>Cycle de vie :</b> initialiser dans {@code GameManager.initAfterWorldsLoad()},
 * avant {@code VillagerFactory}. Pas de {@code shutdown()} requis.</p>
 */
public class QuestGlowService {

    /** Bit "Glowing" dans l'octet de flags partagés (Entity Metadata index 0). */
    private static final byte GLOW_BIT = (byte) 0x40;

    /** playerUUID → entityId du Trader qui doit briller pour ce joueur. Thread-safe. */
    private final Map<UUID, Integer> glowingEntityIdByPlayer = new ConcurrentHashMap<>();

    /**
     * Active le glow du Trader identifié par {@code traderId} pour ce joueur.
     * Si le joueur avait déjà un glow actif (suite à un respawn du villageois), il est mis à jour.
     */
    public void enableGlow(AlphaPlayer alphaPlayer, String traderId) {
        if (traderId == null) return;
        Player player = alphaPlayer.getPlayer();
        if (player == null) return;
        if (!(VillagerRegistry.get(traderId) instanceof Trader trader) || trader.getVillager() == null) return;

        int entityId = trader.getVillager().getEntityId();
        glowingEntityIdByPlayer.put(player.getUniqueId(), entityId);
        sendGlowPacket(player, entityId, true);
        MLLogManager.getInstance().log(Level.FINE, ELogTag.QUEST,
                "[QuestGlow] Activé pour " + alphaPlayer.getPseudo() + " → " + traderId);
    }

    /**
     * Désactive le glow pour ce joueur et envoie un paquet pour supprimer l'effet côté client.
     * No-op si le joueur n'avait pas de glow actif.
     */
    public void disableGlow(AlphaPlayer alphaPlayer) {
        Integer entityId = glowingEntityIdByPlayer.remove(alphaPlayer.getUuid());
        if (entityId == null) return;
        Player player = alphaPlayer.getPlayer();
        if (player != null) {
            sendGlowPacket(player, entityId, false);
            MLLogManager.getInstance().log(Level.FINE, ELogTag.QUEST,
                    "[QuestGlow] Désactivé pour " + alphaPlayer.getPseudo());
        }
    }

    /**
     * Nettoyage mémoire lors d'une déconnexion (aucun paquet envoyé, joueur parti).
     */
    public void cleanupOnQuit(UUID playerUuid) {
        glowingEntityIdByPlayer.remove(playerUuid);
    }

    // =========================================================================
    // Internals
    // =========================================================================

    /**
     * Envoie un paquet NMS {@link ClientboundSetEntityDataPacket} contenant uniquement
     * l'index 0 (shared flags byte) pour activer ou désactiver le bit glow côté client.
     *
     * <p>On n'envoie que l'index 0 — le client merge les valeurs reçues dans son état local,
     * les autres flags (feu, invisibilité…) ne sont donc pas affectés.</p>
     */
    private void sendGlowPacket(Player player, int entityId, boolean glowing) {
        try {
            byte flags = glowing ? GLOW_BIT : (byte) 0x00;
            List<SynchedEntityData.DataValue<?>> values = new ArrayList<>();
            values.add(new SynchedEntityData.DataValue<>(0, EntityDataSerializers.BYTE, flags));
            var packet = new ClientboundSetEntityDataPacket(entityId, values);
            ((CraftPlayer) player).getHandle().connection.send(packet);
        } catch (Exception e) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.QUEST,
                    "[QuestGlow] Erreur lors de l'envoi du paquet : " + e.getMessage());
        }
    }
}