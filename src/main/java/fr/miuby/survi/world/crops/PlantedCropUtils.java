package fr.miuby.survi.world.crops;

import org.bukkit.Material;

public class PlantedCropUtils {
    public static boolean isPlantable(Material material) {
        return isSeed(material) || isSapling(material) ||
                material == Material.BAMBOO || material == Material.SUGAR_CANE ||
                material == Material.CACTUS || material == Material.KELP ||
                material == Material.SEAGRASS;
    }

    public static boolean isSapling(Material material) {
        return material.toString().endsWith("_SAPLING") || material == Material.MANGROVE_PROPAGULE;
    }

    public static boolean isSeed(Material material) {
        return switch (material) {
            // Graines de base
            case WHEAT_SEEDS, BEETROOT_SEEDS, MELON_SEEDS, PUMPKIN_SEEDS -> true;
            // Carottes et pommes de terre
            case CARROT, POTATO, BAKED_POTATO, POISONOUS_POTATO -> true;
            // Graines d'arbres
            case OAK_SAPLING, SPRUCE_SAPLING, BIRCH_SAPLING, JUNGLE_SAPLING,
                 ACACIA_SAPLING, DARK_OAK_SAPLING, MANGROVE_PROPAGULE,
                 CHERRY_SAPLING -> true;
            // Graines spéciales
            case COCOA_BEANS, SWEET_BERRIES, GLOW_BERRIES, TORCHFLOWER_SEEDS,
                 PITCHER_POD, NETHER_WART -> true;
            // Champignons
            case BROWN_MUSHROOM, RED_MUSHROOM, CRIMSON_FUNGUS, WARPED_FUNGUS -> true;
            // Autres plantes
            case SUGAR_CANE, CACTUS, BAMBOO, KELP, TWISTING_VINES,
                 WEEPING_VINES, GLOW_LICHEN -> true;
            default -> false;
        };
    }

    public static boolean isCrop(Material material) {
        return switch (material) {
            // Cultures de base
            case WHEAT, CARROTS, POTATOES, BEETROOTS, NETHER_WART -> true;
            // Pousses d'arbres
            case OAK_SAPLING, SPRUCE_SAPLING, BIRCH_SAPLING, JUNGLE_SAPLING,
                 ACACIA_SAPLING, DARK_OAK_SAPLING, MANGROVE_PROPAGULE,
                 CHERRY_SAPLING -> true;
            // Plantes à tiges
            case MELON_STEM, ATTACHED_MELON_STEM,
                 PUMPKIN_STEM, ATTACHED_PUMPKIN_STEM -> true;
            // Cultures spéciales
            case COCOA, SWEET_BERRY_BUSH, CAVE_VINES, CAVE_VINES_PLANT,
                 TORCHFLOWER_CROP, PITCHER_CROP, PITCHER_PLANT -> true;
            // Champignons
            case BROWN_MUSHROOM, RED_MUSHROOM, CRIMSON_FUNGUS, WARPED_FUNGUS -> true;
            // Autres plantes
            case SUGAR_CANE, CACTUS, BAMBOO, KELP, KELP_PLANT,
                 TWISTING_VINES, WEEPING_VINES, GLOW_LICHEN -> true;
            default -> false;
        };
    }
}
