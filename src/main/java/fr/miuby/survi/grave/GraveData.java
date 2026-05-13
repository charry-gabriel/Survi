package fr.miuby.survi.grave;

import org.bukkit.Location;

import java.util.UUID;

public record GraveData(UUID id, UUID ownerId, Location location) {}