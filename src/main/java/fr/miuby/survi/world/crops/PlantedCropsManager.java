package fr.miuby.survi.world.crops;

import fr.miuby.survi.system.database.Database;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;

public class PlantedCropsManager {

    /** key(location) → niveau FERMIER au moment de la plantation. */
    private final Map<String, Integer> plantedCrops = new HashMap<>();
    private final Database database;
    private boolean loaded = false;

    public PlantedCropsManager(Database database) {
        this.database = database;
    }

    public void load() {
        if (loaded) return;
        loaded = database.crops().loadAll(plantedCrops);
    }

    public void addPlantedCrop(Location location, int farmLevel) {
        PlantedCrop crop = new PlantedCrop(location, farmLevel);
        String key = crop.getKey();
        if (!plantedCrops.containsKey(key)) {
            plantedCrops.put(key, farmLevel);
            database.crops().save(crop);
        }
    }

    public void removePlantedCrop(Location location) {
        PlantedCrop crop = new PlantedCrop(location, 0);
        String key = crop.getKey();
        if (plantedCrops.remove(key) != null) {
            database.crops().remove(crop);
        }
    }

    /**
     * Retourne le niveau FERMIER stocké pour cette culture, ou {@code null}
     * si la culture n'a pas été plantée par un fermier.
     */
    public Integer getFarmLevel(Location location) {
        return plantedCrops.get(new PlantedCrop(location, 0).getKey());
    }

    public boolean isPlantedByFarmer(Location location) {
        return plantedCrops.containsKey(new PlantedCrop(location, 0).getKey());
    }

    // ─── Détection du bloc cible à l'action de planter ───────────────────────────

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

    //region private helpers
    private Block handleSaplingPlanting(Block clicked, BlockFace face) {
        Block targetBlock = clicked.getType().isSolid() ? clicked.getRelative(face) : clicked;
        return isValidSaplingTarget(targetBlock) ? targetBlock : null;
    }

    private Block handleCocoaPlanting(Block clicked, BlockFace face, org.bukkit.entity.Player player) {
        if (!isValidCocoaLocation(clicked, face)) return null;
        Block targetBlock = clicked.getRelative(face);
        if (!player.hasPermission("survi.build")) return null;
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
        if (clicked.getType() != Material.FARMLAND) return null;
        Block targetBlock = clicked.getRelative(BlockFace.UP);
        return targetBlock.getType() == Material.AIR ? targetBlock : null;
    }

    private static boolean isValidSaplingTarget(Block targetBlock) {
        if (targetBlock.getType() != Material.AIR && targetBlock.getType() != Material.GRASS_BLOCK) return false;
        Block below = targetBlock.getRelative(BlockFace.DOWN);
        Material b = below.getType();
        return b == Material.GRASS_BLOCK || b == Material.DIRT || b == Material.COARSE_DIRT
                || b == Material.PODZOL || b == Material.FARMLAND;
    }

    private static boolean isValidCocoaLocation(Block clicked, BlockFace face) {
        if (face == BlockFace.UP || face == BlockFace.DOWN) return false;
        Material t = clicked.getType();
        boolean isJungleLog = t.toString().contains("JUNGLE") &&
                (t.toString().contains("LOG") || t.toString().contains("WOOD") || t.toString().contains("STEM"));
        return isJungleLog && clicked.getRelative(face).getType() == Material.AIR;
    }

    private static boolean isValidSugarCaneOrBambooLocation(Block block, Material type) {
        Block below = block.getRelative(BlockFace.DOWN);
        return (type != Material.SUGAR_CANE || below.getType() == Material.GRASS_BLOCK
                || below.getType() == Material.DIRT || below.getType() == Material.COARSE_DIRT
                || below.getType() == Material.PODZOL || below.getType() == Material.SAND
                || below.getType() == Material.RED_SAND)
                && (isWaterNearby(block) || isWaterSourceNearby(block));
    }

    private static boolean isValidSweetBerryLocation(Block block) {
        Block below = block.getRelative(BlockFace.DOWN);
        return below.getType() == Material.GRASS_BLOCK || below.getType() == Material.DIRT
                || below.getType() == Material.COARSE_DIRT || below.getType() == Material.PODZOL
                || below.getType() == Material.FARMLAND;
    }

    private static boolean isValidWaterPlantLocation(Block block, Material type) {
        Block below = block.getRelative(BlockFace.DOWN);
        if (type == Material.KELP) {
            return (below.getType() == Material.KELP_PLANT || below.getType() == Material.KELP)
                    && block.getType() == Material.WATER;
        }
        return (below.getType() == Material.DIRT || below.getType() == Material.SAND
                || below.getType() == Material.CLAY || below.getType().name().endsWith("TERRACOTTA")
                || below.getType().name().endsWith("CONCRETE_POWDER"))
                && block.getType() == Material.WATER;
    }

    private static boolean isWaterNearby(Block block) {
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
            Material t = block.getRelative(face).getType();
            if (t == Material.WATER || t == Material.CAVE_AIR) return true;
        }
        return false;
    }

    private static boolean isWaterSourceNearby(Block block) {
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
            if (block.getRelative(face).getType() == Material.WATER) return true;
        }
        return false;
    }
    //endregion
}