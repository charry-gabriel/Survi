package fr.miuby.survi.quest;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.villager.trader.Trader;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gère l'effet de glow (brillance) per-joueur sur le Trader chez qui le joueur
 * a une quête terminée non réclamée.
 *
 * <p>Utilise ProtocolLib pour intercepter les paquets ENTITY_METADATA sortants
 * et injecter le flag Glowing (bit 6 de l'octet de flags partagés — index 0 des
 * metadata d'entité) uniquement pour le joueur concerné.
 *
 * <p><b>Cycle de vie :</b> initialiser dans {@code GameManager.initAfterWorldsLoad()},
 * avant {@code VillagerFactory}. Arrêter via {@link #shutdown()} dans {@code onDisable()}.
 */
public class QuestGlowService {

    /** Bit "Glowing" dans l'octet de flags partagés (Entity Metadata index 0). */
    private static final byte GLOW_BIT = (byte) 0x40;

    /** playerUUID → entityId du Trader qui doit briller pour ce joueur. Thread-safe. */
    private final Map<UUID, Integer> glowingEntityIdByPlayer = new ConcurrentHashMap<>();
    private PacketListener metadataInterceptor;

    public QuestGlowService(Plugin plugin) {
        ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        metadataInterceptor = new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_METADATA) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Integer targetEntityId = glowingEntityIdByPlayer.get(event.getPlayer().getUniqueId());
                if (targetEntityId == null) return;
                if (event.getPacket().getIntegers().read(0) != targetEntityId) return;
                injectGlowFlag(event.getPacket());
            }
        };
        pm.addPacketListener(metadataInterceptor);
    }

    /**
     * Active le glow du Trader identifié par {@code traderId} pour ce joueur.
     * Envoie immédiatement un paquet ENTITY_METADATA pour que l'effet soit visible.
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
        MLLogManager.getInstance().log(Level.FINE, ELogTag.QUEST, "[QuestGlow] Activé pour " + alphaPlayer.getPseudo() + " → " + traderId);
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
            MLLogManager.getInstance().log(Level.FINE, ELogTag.QUEST, "[QuestGlow] Désactivé pour " + alphaPlayer.getPseudo());
        }
    }

    /**
     * Nettoyage mémoire lors d'une déconnexion (aucun paquet envoyé, joueur parti).
     */
    public void cleanupOnQuit(UUID playerUuid) {
        glowingEntityIdByPlayer.remove(playerUuid);
    }

    /** Désenregistre le listener ProtocolLib. Appeler dans {@code onDisable()}. */
    public void shutdown() {
        if (metadataInterceptor != null) {
            ProtocolLibrary.getProtocolManager().removePacketListener(metadataInterceptor);
            metadataInterceptor = null;
        }
    }

    // =========================================================================
    // Internals
    // =========================================================================

    /** Injecte le bit glow dans l'octet de flags partagés (index 0) du paquet de metadata. */
    private void injectGlowFlag(PacketContainer packet) {
        List<WrappedDataValue> original = packet.getDataValueCollectionModifier().read(0);
        List<WrappedDataValue> modified = new ArrayList<>(original.size() + 1);
        boolean found = false;
        for (WrappedDataValue dv : original) {
            if (dv.getIndex() == 0) {
                byte current = (byte) dv.getValue();
                modified.add(new WrappedDataValue(0, dv.getSerializer(), (byte) (current | GLOW_BIT)));
                found = true;
            } else {
                modified.add(dv);
            }
        }
        if (!found) {
            // Index 0 absent du paquet : on l'ajoute avec uniquement le bit glow
            modified.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), GLOW_BIT));
        }
        packet.getDataValueCollectionModifier().write(0, modified);
    }

    /**
     * Envoie un paquet ENTITY_METADATA synthétique pour forcer la mise à jour du glow côté client.
     * {@code filters = false} → bypass du propre intercepteur pour éviter le double-traitement.
     */
    private void sendGlowPacket(Player player, int entityId, boolean glowing) {
        try {
            ProtocolManager pm = ProtocolLibrary.getProtocolManager();
            PacketContainer packet = pm.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            packet.getIntegers().write(0, entityId);
            byte flags = glowing ? GLOW_BIT : (byte) 0x00;
            List<WrappedDataValue> values = List.of(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), flags));
            packet.getDataValueCollectionModifier().write(0, values);
            pm.sendServerPacket(player, packet, false);
        } catch (Exception e) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.QUEST, "[QuestGlow] Erreur lors de l'envoi du paquet : " + e.getMessage());
        }
    }
}
