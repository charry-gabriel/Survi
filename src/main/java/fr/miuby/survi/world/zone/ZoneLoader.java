package fr.miuby.survi.world.zone;

import fr.miuby.survi.world.config.VillageZoneConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ZoneLoader {
    private ZoneLoader() {}

    @SuppressWarnings("unchecked")
    public static VillageZoneConfig load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "zone.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        List<VillageZoneConfig.VillageZoneStage> stages = new ArrayList<>();
        List<?> rawStages = cfg.getList("stages");
        if (rawStages != null) {
            for (Object obj : rawStages) {
                if (obj instanceof java.util.Map<?, ?> stageMap) {
                    float afterHours = ((Number) stageMap.get("after-hours")).floatValue();
                    int centerX     = ((Number) stageMap.get("center-x")).intValue();
                    int centerZ     = ((Number) stageMap.get("center-z")).intValue();
                    int halfWidth   = ((Number) stageMap.get("half-width")).intValue();
                    int halfDepth   = ((Number) stageMap.get("half-depth")).intValue();

                    java.util.Map<String, Object> spawnMap = (java.util.Map<String, Object>) stageMap.get("spawn");
                    VillageZoneConfig.VillageZoneSpawn spawn = new VillageZoneConfig.VillageZoneSpawn(
                            ((Number) spawnMap.get("x")).floatValue(),
                            ((Number) spawnMap.get("y")).floatValue(),
                            ((Number) spawnMap.get("z")).floatValue(),
                            spawnMap.containsKey("yaw")   ? ((Number) spawnMap.get("yaw")).floatValue()   : 0f,
                            spawnMap.containsKey("pitch") ? ((Number) spawnMap.get("pitch")).floatValue() : 0f
                    );

                    java.util.Map<String, Object> portalMap = (java.util.Map<String, Object>) stageMap.get("portal");
                    VillageZoneConfig.VillageZonePortal portal = new VillageZoneConfig.VillageZonePortal(
                            ((Number) portalMap.get("min-x")).intValue(),
                            ((Number) portalMap.get("min-y")).intValue(),
                            ((Number) portalMap.get("min-z")).intValue(),
                            ((Number) portalMap.get("max-x")).intValue(),
                            ((Number) portalMap.get("max-y")).intValue(),
                            ((Number) portalMap.get("max-z")).intValue()
                    );

                    stages.add(new VillageZoneConfig.VillageZoneStage(afterHours, centerX, centerZ, halfWidth, halfDepth, spawn, portal));
                }
            }
        }

        return new VillageZoneConfig(stages);
    }
}