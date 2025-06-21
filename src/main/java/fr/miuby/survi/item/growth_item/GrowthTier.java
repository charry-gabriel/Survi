package fr.miuby.survi.item.growth_item;

import fr.miuby.survi.item.growth_item.effect.ItemEffect;

import java.util.List;

public record GrowthTier(int requiredUses, List<ItemEffect> effects) { }
