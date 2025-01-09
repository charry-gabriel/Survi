package fr.miuby.survi.role;

import net.kyori.adventure.text.TextComponent;

import java.util.List;

public record Role(ERole type, TextComponent displayName, List<RoleAttribute> attributes, String roleId) { }
