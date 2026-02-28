package fr.miuby.survi.display;

import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.quest.PlayerQuestData;
import fr.miuby.survi.quest.Quest;
import fr.miuby.survi.quest.QuestManager;
import fr.miuby.survi.villager.VillagerLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import fr.miuby.survi.player.GlobalRank;
import java.text.DecimalFormat;
import java.time.LocalDate;
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
        GlobalRank rank = alphaPlayer.getGlobalRank();
        int totalRep = alphaPlayer.getTotalReputation();

        return Component.text("\nServeur Survi | ", NamedTextColor.GOLD)
                .appendNewline()
                .append(Component.text("Difficulté : ", NamedTextColor.RED))
                //.append(Component.text(MobStatManager.getInstance().getDifficultyName()))
                .appendNewline()
                .append(rank.displayComponent())
                .append(Component.text("  (Rép. : ", NamedTextColor.GRAY))
                .append(Component.text(totalRep, rank.getColor()))
                .append(Component.text(")", NamedTextColor.GRAY))
                .appendNewline();
    }

    private Component buildFooter(AlphaPlayer alphaPlayer) {
        Component footer = Component.empty()
                .append(buildVillagerLevelsLine())
                .appendNewline()
                .appendNewline()
                .append(formatStat(alphaPlayer, Attribute.MAX_HEALTH, "Vie"))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(formatStat(alphaPlayer, Attribute.ATTACK_DAMAGE, "Force"))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(formatStat(alphaPlayer, Attribute.ARMOR, "Armure"))
                .appendNewline()
                .append(formatStat(alphaPlayer, Attribute.MOVEMENT_SPEED, "Vitesse"))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(formatStat(alphaPlayer, Attribute.ATTACK_SPEED, "Vit. d'Attaque"))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(formatStat(alphaPlayer, Attribute.LUCK, "Chance"))
                .appendNewline();

        Component questLine = buildQuestLine(alphaPlayer);
        if (!questLine.equals(Component.empty())) {
            footer = footer.append(Component.newline());
            footer = footer.append(questLine);
        }

        return footer.appendNewline();
    }

    private Component buildQuestLine(AlphaPlayer alphaPlayer) {
        PlayerQuestData data = alphaPlayer.getCurrentActiveQuest();
        if (data == null || data.isClaimed()) {
            return Component.empty();
        }

        // On ne montre la quête que si elle est d'aujourd'hui
        if (!data.getLastAccepted().isEqual(LocalDate.now())) {
            return Component.empty();
        }

        Quest quest = QuestManager.getInstance().getQuest(data.getQuestId());
        if (quest == null) {
            return Component.empty();
        }

        Component questLine = Component.text("Quête: ", NamedTextColor.GOLD)
                .append(Component.text(quest.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" (", NamedTextColor.GRAY))
                .append(Component.text(data.getProgress(), data.isCompleted() ? NamedTextColor.GREEN : NamedTextColor.AQUA))
                .append(Component.text("/", NamedTextColor.GRAY))
                .append(Component.text(quest.getGoal(), NamedTextColor.AQUA))
                .append(Component.text(")", NamedTextColor.GRAY));

        if (data.isCompleted()) {
            questLine = questLine.append(Component.text(" - Allez voir le Trader !", NamedTextColor.GREEN));
        }

        return questLine;
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

        String sign = "+";
        NamedTextColor color;
        if (Math.abs(diff) < 0.001) {
            diff = 0;
            color = NamedTextColor.GRAY;
        } else if (diff > 0) {
            color = NamedTextColor.GREEN;
            sign = "+";
        } else {
            color = NamedTextColor.RED;
            sign = "";
        }

        return Component.text(statName + ": ", NamedTextColor.WHITE)
                .append(Component.text(formatDouble(baseValue) + sign + formatDouble(diff), color));
    }
}