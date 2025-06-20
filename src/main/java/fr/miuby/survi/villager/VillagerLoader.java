package fr.miuby.survi.villager;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.Survi;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class VillagerLoader {
    private static final Map<String, VillagerConfig> loaded = new HashMap<>();

    public static VillagerConfig load(String id) {
        if (loaded.containsKey(id))
            return loaded.get(id);

        Survi plugin = GameManager.getInstance().getPlugin();
        File villagerFile = new File(plugin.getDataFolder(), "villagers/" + id + ".yml");

        if (!villagerFile.getParentFile().exists()) {
            villagerFile.getParentFile().mkdirs();
        }
        if (!villagerFile.exists()) {
            plugin.saveResource("villagers/" + id + ".yml", false);
        }

        try (InputStream stream = new FileInputStream(villagerFile)) {
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            LoaderOptions options = new LoaderOptions();
            Yaml yaml = new Yaml(new Constructor(VillagerConfig.class, options));
            VillagerConfig config = yaml.load(content);
            loaded.put(id, config);

            return config;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load villager config: " + id, e);
        }
    }
}