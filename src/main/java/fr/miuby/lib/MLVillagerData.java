package fr.miuby.lib;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class MLVillagerData {
    protected final UUID uuid;
    @NotNull
    protected final Location location;
}
