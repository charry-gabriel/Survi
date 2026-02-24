package fr.miuby.survi.crops;

import fr.miuby.survi.system.database.Database;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.Set;

public class PlantedCropsManager {
    public static final double WILD_GROWTH_CHANCE = 0.05; // 5% de chance de pousser pour les plantes sauvages

    private final Set<String> plantedCrops = new HashSet<>();
    private final Database database;
    private boolean loaded = false;

    public PlantedCropsManager(Database database) {
        this.database = database;
    }

    public void load() {
        if (loaded)
            return;

        loaded = database.selectCrop(plantedCrops);
    }

    public void addPlantedCrop(Location location) {
        PlantedCrop crop = new PlantedCrop(location);
        String key = crop.getKey();
        
        if (plantedCrops.add(key)) {
            database.saveCrop(crop);
        }
    }

    public void removePlantedCrop(Location location) {
        PlantedCrop crop = new PlantedCrop(location);
        String key = crop.getKey();
        
        if (plantedCrops.remove(key)) {
            database.removeCrop(crop);
        }
    }

    public boolean isPlantedByFarmer(Location location) {
        return plantedCrops.contains(new PlantedCrop(location).getKey());
    }

    public Block getTargetBlockForPlanting(PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        Material itemType = event.getItem().getType();
        BlockFace face = event.getBlockFace();

        if (PlantedCropUtils.isSapling(itemType)) {
            return handleSaplingPlanting(clicked, face);
        }

        return switch (itemType) {
            case COCOA_BEANS -> handleCocoaPlanting(clicked, face, event.getPlayer());
            case SUGAR_CANE, BAMBOO -> handleSugarCaneOrBambooPlanting(clicked, face, itemType);
            case CACTUS -> handleCactusPlanting(clicked, face);
            case SWEET_BERRIES -> handleSweetBerryPlanting(clicked, face);
            case KELP, SEAGRASS -> handleWaterPlantPlanting(clicked, face, itemType);
            default -> handleDefaultCropPlanting(clicked);
        };
    }

    //region private method
    private Block handleSaplingPlanting(Block clicked, BlockFace face) {
        Block targetBlock;

        if (clicked.getType().isSolid())
            targetBlock = clicked.getRelative(face);
        else
            targetBlock = clicked;

        if (!isValidSaplingTarget(targetBlock))
            return null;

        return targetBlock;
    }

    private Block handleCocoaPlanting(Block clicked, BlockFace face, org.bukkit.entity.Player player) {
        if (!isValidCocoaLocation(clicked, face))
            return null;

        Block targetBlock = clicked.getRelative(face);

        if (!player.hasPermission("survi.build"))
            return null;

        return targetBlock;
    }

    private Block handleSugarCaneOrBambooPlanting(Block clicked, BlockFace face, Material type) {
        Block targetBlock = clicked.getRelative(face);
        return isValidSugarCaneOrBambooLocation(targetBlock, type) ? targetBlock : null;
    }

    private Block handleCactusPlanting(Block clicked, BlockFace face) {
        Block targetBlock = clicked.getRelative(face);
        Block below = targetBlock.getRelative(BlockFace.DOWN);
        return (below.getType() == Material.SAND || below.getType() == Material.RED_SAND) ? targetBlock : null;
    }

    private Block handleSweetBerryPlanting(Block clicked, BlockFace face) {
        Block targetBlock = clicked.getRelative(face);
        return isValidSweetBerryLocation(targetBlock) ? targetBlock : null;
    }

    private Block handleWaterPlantPlanting(Block clicked, BlockFace face, Material type) {
        Block targetBlock = clicked.getRelative(face);
        return isValidWaterPlantLocation(targetBlock, type) ? targetBlock : null;
    }

    private Block handleDefaultCropPlanting(Block clicked) {
        if (clicked.getType() != Material.FARMLAND)
            return null;

        Block targetBlock = clicked.getRelative(BlockFace.UP);
        return targetBlock.getType() == Material.AIR ? targetBlock : null;
    }

    private static boolean isValidSaplingTarget(Block targetBlock) {
        // Vérifie que le bloc cible est de l'air ou de l'herbe
        if (targetBlock.getType() != Material.AIR && targetBlock.getType() != Material.GRASS_BLOCK)
            return false;

        // Vérifie que le bloc en dessous est un bloc de terre valide
        Block below = targetBlock.getRelative(BlockFace.DOWN);
        Material belowType = below.getType();

        return belowType == Material.GRASS_BLOCK ||
                belowType == Material.DIRT ||
                belowType == Material.COARSE_DIRT ||
                belowType == Material.PODZOL ||
                belowType == Material.FARMLAND;
    }

    private static boolean isValidCocoaLocation(Block clicked, BlockFace face) {
        // Le cacao doit être placé sur le côté d'un bloc de jungle
        if (face == BlockFace.UP || face == BlockFace.DOWN) {
            return false;
        }

        // Le bloc sur lequel on clique doit être un tronc de jungle
        Material clickedType = clicked.getType();
        boolean isJungleLog = clickedType.toString().contains("JUNGLE") &&
                (clickedType.toString().contains("LOG") ||
                clickedType.toString().contains("WOOD") ||
                clickedType.toString().contains("STEM"));

        // Le bloc adjacent dans la direction du clic doit être de l'air
        Block targetBlock = clicked.getRelative(face);
        boolean isTargetAir = targetBlock.getType() == Material.AIR;

        return isJungleLog && isTargetAir;
    }

    private static boolean isValidSugarCaneOrBambooLocation(Block block, Material type) {
        // Doit être sur de la terre, sable, etc. et près de l'eau
        Block below = block.getRelative(BlockFace.DOWN);
        return (type != Material.SUGAR_CANE || below.getType() == Material.GRASS_BLOCK ||
                below.getType() == Material.DIRT ||
                below.getType() == Material.COARSE_DIRT ||
                below.getType() == Material.PODZOL ||
                below.getType() == Material.SAND ||
                below.getType() == Material.RED_SAND) &&
                (isWaterNearby(block) || isWaterSourceNearby(block));
    }

    private static boolean isValidSweetBerryLocation(Block block) {
        // Les baies sucrées doivent être sur de la terre, de l'herbe, etc.
        Block below = block.getRelative(BlockFace.DOWN);
        return below.getType() == Material.GRASS_BLOCK ||
                below.getType() == Material.DIRT ||
                below.getType() == Material.COARSE_DIRT ||
                below.getType() == Material.PODZOL ||
                below.getType() == Material.FARMLAND;
    }

    private static boolean isValidWaterPlantLocation(Block block, Material type) {
        // Vérifie si c'est un bon emplacement pour les plantes aquatiques
        Block below = block.getRelative(BlockFace.DOWN);
        if (type == Material.KELP) {
            return (below.getType() == Material.KELP_PLANT ||
                    below.getType() == Material.KELP) &&
                    block.getType() == Material.WATER;
        }
        // Pour la zostère marine
        return (below.getType() == Material.DIRT ||
                below.getType() == Material.SAND ||
                below.getType() == Material.CLAY ||
                below.getType().name().endsWith("TERRACOTTA") ||
                below.getType().name().endsWith("CONCRETE_POWDER")) &&
                block.getType() == Material.WATER;
    }

    private static boolean isWaterNearby(Block block) {
        // Vérifie s'il y a de l'eau à côté (pas forcément une source)
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
            if (block.getRelative(face).getType() == Material.WATER || block.getRelative(face).getType() == Material.CAVE_AIR)
                return true;
        }
        return false;
    }

    private static boolean isWaterSourceNearby(Block block) {
        // Vérifie s'il y a une source d'eau à côté
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
            Block relative = block.getRelative(face);
            if (relative.getType() == Material.WATER)
                return true;
        }
        return false;
    }
    //endregion
}
