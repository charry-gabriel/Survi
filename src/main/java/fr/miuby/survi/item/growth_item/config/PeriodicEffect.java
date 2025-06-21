package fr.miuby.survi.item.growth_item.config;

import fr.miuby.survi.item.growth_item.effect.ItemEffect;

import java.util.List;

public record PeriodicEffect(int everyUses, List<ItemEffect> effects) { }
