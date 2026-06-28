package fr.miuby.survi.player.role;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.world.EWorld;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class RoleAttribute {
    @Getter
    private final EWorld world;
    @Getter
    private final Attribute attributeType;
    @Getter
    private final float value;
    @Getter
    private final EOperation operation;

    @Getter @Nullable
    private String name;
    @Getter @Nullable
    private String role;

    public RoleAttribute(EWorld world, Attribute attributeType, float value) {
        this(world, attributeType, value, EOperation.ADD_NUMBER);
    }

    public AttributeModifier createAttributeModifier() {
        assert this.name != null;
        return new AttributeModifier(new NamespacedKey(GameManager.getInstance().getPlugin(), this.name), this.value, AttributeModifier.Operation.valueOf(this.operation.toString()));
    }

    public void setRole(String role) {
        this.role = role;
        this.name = RoleAttribute.createName(this.world.toString(), this.role, this.attributeType.getKey().getKey());
    }

    public static String createName(String world, String role, String attributeKey) {
        return world + "_" + role + "_" + attributeKey;
    }

    public enum EOperation {
        ADD_NUMBER,
        ADD_SCALAR,
        MULTIPLY_SCALAR_1,
        REMOVE
    }
}
