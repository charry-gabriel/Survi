package fr.miuby.survi.food;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.log.ELogTag;
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
     * Ajuste la saturation ET la faim restaurées par un aliment, à partir du gain réel déjà
     * appliqué par vanilla entre {@code avant} et l'état actuel du joueur (mesuré par le listener) :
     * doublé si {@code food} fait partie de la nourriture du jour, sinon divisé par deux.
     *
     * Pour la saturation, le résultat est plafonné à {@link #MAX_SATURATION} et non à la faim
     * courante : vanilla plafonne lui-même son propre gain à la faim courante, ce qui écraserait
     * presque toujours le doublement (la faim grimpe en même temps que la saturation). Pour la
     * faim, son plafond naturel est déjà {@link #MAX_FOOD_LEVEL} : multiplier le gain déjà observé
     * donne mathématiquement le même résultat que partir d'une valeur nominale fixe.
     */
    public void applyFoodBonus(Player player, Material food, float saturationBeforeEating, int foodLevelBeforeEating) {
        if (!player.isOnline()) return;

        boolean isFoodOfDay = isFoodOfTheDay(food);
        float multiplier = isFoodOfDay ? 2.0f : 0.5f;

        float saturationGained = player.getSaturation() - saturationBeforeEating;
        float newSaturation = Math.clamp(saturationBeforeEating + saturationGained * multiplier, 0.0f, MAX_SATURATION);

        int foodLevelGained = player.getFoodLevel() - foodLevelBeforeEating;
        int newFoodLevel = Math.clamp(foodLevelBeforeEating + Math.round(foodLevelGained * multiplier), 0, MAX_FOOD_LEVEL);

        player.setSaturation(newSaturation);
        player.setFoodLevel(newFoodLevel);

        MLLogManager.getInstance().log(Level.FINE, ELogTag.ITEM,
                "[FoodOfTheDay] " + player.getName() + " mange " + food
                        + " (nourritureDuJour=" + isFoodOfDay + ") x" + multiplier
                        + " -> saturation=" + newSaturation + " faim=" + newFoodLevel);
    }
}