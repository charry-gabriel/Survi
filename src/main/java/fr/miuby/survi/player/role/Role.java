package fr.miuby.survi.player.role;

import net.kyori.adventure.text.TextComponent;

import java.util.List;

public record Role(ERole type, TextComponent displayName, List<RoleAttribute> attributes, String roleId) { }
