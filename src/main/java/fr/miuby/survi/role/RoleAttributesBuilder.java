package fr.miuby.survi.role;

import fr.miuby.survi.world.EWorld;
import org.bukkit.attribute.Attribute;

import java.util.*;

public class RoleAttributesBuilder {
    private final Map<EWorld, List<RoleAttribute>> attributesByWorld = new EnumMap<>(EWorld.class);
    
    public RoleAttributesBuilder add(EWorld world, Attribute attribute, float value) {
        return add(world, attribute, value, RoleAttribute.Operation.ADD_SCALAR);
    }
    
    public RoleAttributesBuilder add(EWorld world, Attribute attribute, float value, RoleAttribute.Operation operation) {
        attributesByWorld.computeIfAbsent(world, k -> new ArrayList<>())
                        .add(new RoleAttribute(world, attribute, value, operation));
        return this;
    }
    
    public List<RoleAttribute> build() {
        return attributesByWorld.values().stream()
                .flatMap(List::stream)
                .toList();
    }
}
