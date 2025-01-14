package fr.miuby.survi.role;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.world.EWorld;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.jetbrains.annotations.Nullable;

public class RoleAttribute {
    private final EWorld world;
    private final Attribute attributeType;
    private final float value;
    private final Operation operation;

    private String name;
    private String role;

    public RoleAttribute(EWorld world, Attribute attributeType, float value) {
        this(world, attributeType, value, Operation.ADD_NUMBER);
    }

    public RoleAttribute(EWorld world, Attribute attributeType, float value, Operation operation) {
        this.world = world;
        this.attributeType = attributeType;
        this.value = value;
        this.operation = operation;
    }

    public AttributeModifier createAttributeModifier() {
        return new AttributeModifier(new NamespacedKey(GameManager.getInstance().getPlugin(), this.name), this.value, AttributeModifier.Operation.valueOf(this.operation.toString()));
    }

    @Nullable
    public String getName() {
        return this.name;
    }

    public Attribute getAttributeType() {
        return attributeType;
    }

    public float getValue() {
        return this.value;
    }

    public EWorld getWorld() {
        return this.world;
    }

    public Operation getOperation() {
        return this.operation;
    }

    @Nullable
    public String getRole() {
        return this.role;
    }

    public void setRole(String role) {
        this.role = role;
        this.name = RoleAttribute.createName(this.world.toString(), this.role, this.attributeType.getKey().getKey());
    }

    public static String createName(String world, String role, String attributeKey) {
        return world + "_" + role + "_" + attributeKey;
    }

    public enum Operation {
        ADD_NUMBER,
        ADD_SCALAR,
        MULTIPLY_SCALAR_1,
        REMOVE
    }
}
