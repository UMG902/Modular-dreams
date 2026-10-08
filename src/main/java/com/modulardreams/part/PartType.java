package com.modulardreams.part;

/**
 * Tool parts of Modular Dreams v2: the universal handle plus one head per
 * tool type. Every tool is exactly handle + head.
 *
 * <p>Costs are in material units consumed by the Part Picker: heads cost
 * their vanilla head cost (pickaxe 3, axe 3, shovel 1, hoe 2, sword 2), the
 * spear head costs 2, and the handle costs 1 - so a modular pickaxe costs
 * roughly what a vanilla one does.
 */
public enum PartType {
    HANDLE("handle", 1),
    PICKAXE_HEAD("pickaxe_head", 3),
    AXE_HEAD("axe_head", 3),
    SHOVEL_HEAD("shovel_head", 1),
    HOE_HEAD("hoe_head", 2),
    SWORD_HEAD("sword_head", 2),
    SPEAR_HEAD("spear_head", 2);

    public final String id;
    public final int cost;

    PartType(String id, int cost) {
        this.id = id;
        this.cost = cost;
    }

    public boolean isHandle() {
        return this == HANDLE;
    }

    public boolean isHead() {
        return this != HANDLE;
    }

    /** @return the head part belonging to the given tool id, e.g. "pickaxe" -> PICKAXE_HEAD. */
    public static PartType headOfTool(String toolId) {
        return valueOf(toolId.toUpperCase(java.util.Locale.ROOT) + "_HEAD");
    }

    public String translationKey() {
        return "part.modular_dreams." + id;
    }
}
