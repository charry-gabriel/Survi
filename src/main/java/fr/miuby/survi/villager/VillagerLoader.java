package fr.miuby.survi.villager;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.Yaml;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class VillagerLoader {
    private static final Map<String, VillagerConfig> loaded = new HashMap<>();

    public static VillagerConfig load(String id) {
        if (loaded.containsKey(id))
            return loaded.get(id);

        try (InputStream stream = VillagerLoader.class.getResourceAsStream("/villagers/" + id + ".yml")) {
            if (stream == null)
                throw new FileNotFoundException("YAML file not found for villager: " + id);

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