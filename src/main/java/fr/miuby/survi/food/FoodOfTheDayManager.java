package fr.miuby.survi.food;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.log.ELogTag;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.FoodProperties;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Tire au sort, chaque jour au reset (6h), une nourriture par {@link EFoodCategory}
 * (viande cuite, plante, nourriture craftée).
 *
 * Manger une des trois nourritures du jour double le gain de saturation et de faim ; manger
 * toute autre nourriture comestible les divise par deux. Le tirage est persisté en DB
 * ({@code server_data}) pour survivre à un redémarrage du serveur.
 */
public class FoodOfTheDayManager {

    private static final String SEPARATOR = ":";
    /** Saturation maximale vanilla (capacité de la barre de faim). */
    private static final float MAX_SATURATION = 20.0f;
    /** Faim maximale vanilla. */
    private static final int MAX_FOOD_LEVEL = 20;

    private final Map<EFoodCategory, Material> currentPicks = new EnumMap<>(EFoodCategory.class);

    public FoodOfTheDayManager() {
        load();
    }

    // ─── Chargement / persistance ────────────────────────────────────────────

    private void load() {
        String raw = GameManager.getInstance().getDatabase().system().getFoodOfTheDay();

        if (raw != null && !raw.isBlank()) {
            try {
                String[] parts = raw.split(SEPARATOR);
                EFoodCategory[] categories = EFoodCategory.values();
                for (int i = 0; i < categories.length; i++) {
                    currentPicks.put(categories[i], Material.valueOf(parts[i]));
                }
                MLLogManager.getInstance().log(Level.INFO, ELogTag.ITEM,
                        "[FoodOfTheDay] Chargé depuis la DB : " + currentPicks);
                return;
            } catch (Exception e) {
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.ITEM,
                        "[FoodOfTheDay] Donnée invalide en DB (\"" + raw + "\"), nouveau tirage forcé", e);
            }
        } else {
            MLLogManager.getInstance().log(Level.INFO, ELogTag.ITEM,
                    "[FoodOfTheDay] Aucune donnée en DB, premier tirage");
        }
        performDailyDraw();
    }

    /** Tire au sort une nouvelle nourriture pour chaque catégorie. Appelé depuis {@code ServerListener.onDailyReset}. */
    public void performDailyDraw() {
        for (EFoodCategory category : EFoodCategory.values()) {
            currentPicks.put(category, category.drawRandom());
        }
        save();
        MLLogManager.getInstance().log(Level.INFO, ELogTag.ITEM,
                "[FoodOfTheDay] Nouveau tirage : " + currentPicks);
    }

    private void save() {
        StringBuilder sb = new StringBuilder();
        for (EFoodCategory category : EFoodCategory.values()) {
            if (!sb.isEmpty()) sb.append(SEPARATOR);
            sb.append(currentPicks.get(category).name());
        }
        GameManager.getInstance().getDatabase().system().saveFoodOfTheDay(sb.toString());
    }

    // ─── Accès ────────────────────────────────────────────────────────────────

    public Material getFoodOfTheDay(EFoodCategory category) {
        return currentPicks.get(category);
    }

    public Collection<Material> getAllFoodOfTheDay() {
        return currentPicks.values();
    }

    public boolean isFoodOfTheDay(Material material) {
        return currentPicks.containsValue(material);
    }

    // ─── Saturation ───────────────────────────────────────────────────────────

    /**
     * Ajuste la saturation ET la faim restaurées par un aliment en partant des valeurs
     * <em>nominales</em> de l'item (lues via {@link DataComponentTypes#FOOD}), pas du gain
     * déjà appliqué par vanilla.
     *
     * Vanilla plafonne son propre gain à la faim courante avant que ce code s'exécute.
     * Utiliser le gain vanilla observé entraînerait un double-plafonnement : quand le joueur
     * est à faim=16, vanilla n'accorde que 4 de faim au lieu de 8, et le x0.5 s'appliquerait
     * sur ce 4 réduit, donnant 2 au lieu de 4.
     *
     * En partant de la valeur nominale ({@code nutrition}) et de l'état avant manger, le
     * multiplicateur s'applique toujours sur la pleine valeur de l'item, puis le résultat est
     * plafonné une seule fois à {@link #MAX_FOOD_LEVEL} / {@link #MAX_SATURATION}.
     */
    public void applyFoodBonus(Player player, Material food, float saturationBeforeEating, int foodLevelBeforeEating) {
        if (!player.isOnline()) return;

        boolean isFoodOfDay = isFoodOfTheDay(food);
        float multiplier = isFoodOfDay ? 2.0f : 0.5f;

        FoodProperties foodProps = food.asItemType().getDefaultData(DataComponentTypes.FOOD);
        int nominalNutrition = foodProps.nutrition();
        float nominalSatGain = nominalNutrition * foodProps.saturation() * 2.0f;

        int newFoodLevel = Math.clamp(foodLevelBeforeEating + Math.round(nominalNutrition * multiplier), 0, MAX_FOOD_LEVEL);
        float newSaturation = Math.clamp(saturationBeforeEating + nominalSatGain * multiplier, 0.0f, MAX_SATURATION);

        player.setSaturation(newSaturation);
        player.setFoodLevel(newFoodLevel);

        MLLogManager.getInstance().log(Level.FINE, ELogTag.ITEM,
                "[FoodOfTheDay] " + player.getName() + " mange " + food
                        + " (nourritureDuJour=" + isFoodOfDay + ") x" + multiplier
                        + " nutrition=" + nominalNutrition
                        + " -> saturation=" + newSaturation + " faim=" + newFoodLevel);
    }
}