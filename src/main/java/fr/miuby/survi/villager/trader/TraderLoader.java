package fr.miuby.survi.villager.trader;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.Survi;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraderLoader {
    private TraderLoader() {
        /* utility class */
    }

    private static final Map<String, TraderConfig> loaded = new HashMap<>();

    public static TraderConfig load(String id) {
        if (loaded.containsKey(id))
            return loaded.get(id);

        Survi plugin = GameManager.getInstance().getPlugin();
        File file = new File(plugin.getDataFolder(), "traders/" + id + ".yml");

        try (InputStream stream = new FileInputStream(file)) {
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            LoaderOptions options = new LoaderOptions();
            Yaml yaml = new Yaml(new Constructor(TraderConfig.class, options));
            TraderConfig config = yaml.load(content);
            loaded.put(id, config);

            return config;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load trader config: " + id, e);
        }
    }

    public static List<TraderConfig> loadAll() {
        Survi plugin = GameManager.getInstance().getPlugin();
        File folder = new File(plugin.getDataFolder(), "traders");
        if (!folder.exists() || !folder.isDirectory()) {
            return List.of();
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return List.of();
        }

        List<TraderConfig> configs = new ArrayList<>();
        for (File file : files) {
            String id = file.getName().replace(".yml", "");
            configs.add(load(id));
        }
        return configs;
    }
}