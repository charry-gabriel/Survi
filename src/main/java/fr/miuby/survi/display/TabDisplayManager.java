package fr.miuby.survi.display;

import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.VillagerLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class TabDisplayManager {

    private final DecimalFormat df = new DecimalFormat("#.##");

    public TabDisplayManager() {
        Bukkit.getScheduler().runTaskTimer(GameManager.getInstance().getPlugin(), this::updateTabList, 0L, 20L); // Met à jour toutes les secondes
    }

    private void updateTabList() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            AlphaPlayer alphaPlayer = AlphaPlayer.get(player.getUniqueId());
            if (alphaPlayer != null && alphaPlayer.getPlayer() != null) {
                player.sendPlayerListHeaderAndFooter(buildHeader(alphaPlayer), buildFooter(alphaPlayer));
            }
        }
    }

    private Component buildHeader(AlphaPlayer alphaPlayer) {
        return Component.text("\nServeur Survi | ", NamedTextColor.GOLD)
                .append(alphaPlayer.getRole().displayName())
                .append(Component.text("\n"));
    }

    private Component buildFooter(AlphaPlayer alphaPlayer) {
        Component footer = Component.text("\n");

        footer = footer.append(buildVillagerLevelsLine());
        footer = footer.append(Component.newline());

        footer = footer.append(formatStat(alphaPlayer, Attribute.MAX_HEALTH, "Vie"));
        footer = footer.append(Component.text(" | ", NamedTextColor.DARK_GRAY));
        footer = footer.append(formatStat(alphaPlayer, Attribute.ATTACK_DAMAGE, "Force"));
        footer = footer.append(Component.text(" | ", NamedTextColor.DARK_GRAY));
        footer = footer.append(formatStat(alphaPlayer, Attribute.ARMOR, "Armure"));
        footer = footer.append(Component.newline());
        footer = footer.append(formatStat(alphaPlayer, Attribute.MOVEMENT_SPEED, "Vitesse"));
        footer = footer.append(Component.text(" | ", NamedTextColor.DARK_GRAY));
        footer = footer.append(formatStat(alphaPlayer, Attribute.ATTACK_SPEED, "Vit. d'Attaque"));
        footer = footer.append(Component.text(" | ", NamedTextColor.DARK_GRAY));
        footer = footer.append(formatStat(alphaPlayer, Attribute.LUCK, "Chance"));

        return footer.append(Component.text("\n"));
    }

    /**
     * Builds a line showing each VillagerLevel's display name and level (from DB / in-memory).
     */
    private Component buildVillagerLevelsLine() {
        List<Component> parts = new ArrayList<>();
        for (var mlVillager : VillagerRegistry.getAll()) {
            if (mlVillager instanceof VillagerLevel vl) {
                if (!parts.isEmpty()) {
                    parts.add(Component.text(" | ", NamedTextColor.DARK_GRAY));
                }
                parts.add(Component.text(vl.getDisplayName().content() + ": ", NamedTextColor.GRAY));
                parts.add(Component.text(String.valueOf(vl.getLevel()), NamedTextColor.AQUA));
            }
        }
        if (parts.isEmpty()) {
            return Component.empty();
        }
        Component line = Component.empty();
        for (Component part : parts) {
            line = line.append(part);
        }
        return line;
    }

    private String formatDouble(double d) {
        return df.format(d);
    }

    private Component formatStat(AlphaPlayer alphaPlayer, Attribute attributeType, String statName) {
        Player player = alphaPlayer.getPlayer();
        if (player == null) return Component.empty();

        AttributeInstance attributeInstance = player.getAttribute(attributeType);
        if (attributeInstance == null) {
            return Component.empty();
        }

        double baseValue = alphaPlayer.getBaseAttributes().getOrDefault(attributeType, attributeInstance.getBaseValue());
        double currentValue = attributeInstance.getValue();
        double diff = currentValue - baseValue;

        String sign = "";
        NamedTextColor color;
        if (Math.abs(diff) < 0.001) {
            diff = 0;
            color = NamedTextColor.GRAY;
        } else if (diff > 0) {
            color = NamedTextColor.GREEN;
            sign = "+";
        } else {
            color = NamedTextColor.RED;
        }

        return Component.text(statName + ": ", NamedTextColor.WHITE)
                .append(Component.text(sign + formatDouble(diff), color));
    }
}
