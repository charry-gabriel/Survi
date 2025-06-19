package fr.miuby.survi.role;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.function.Consumer;

public class RoleDefinition {
    @Getter
    private final ERole type;
    private final String displayName;
    private final NamedTextColor color;
    private final String id;
    private final List<RoleAttribute> attributes;
    
    public RoleDefinition(ERole type, String displayName, NamedTextColor color, String id, 
                         Consumer<RoleAttributesBuilder> attributesBuilder) {
        this.type = type;
        this.displayName = displayName;
        this.color = color;
        this.id = id;
        
        RoleAttributesBuilder builder = new RoleAttributesBuilder();
        attributesBuilder.accept(builder);
        this.attributes = builder.build();
    }
    
    public Role toRole() {
        return new Role(
            type, 
            Component.text("[" + displayName + "]").color(color),
            attributes, 
            id
        );
    }

}
