package fr.miuby.survi.role;

import fr.miuby.survi.world.EWorld;
import org.bukkit.attribute.Attribute;

public record RoleAttribute(EWorld world, Attribute attributeType, float attributeValue) { }
