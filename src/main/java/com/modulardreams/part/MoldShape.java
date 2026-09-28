package com.modulardreams.part;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

import java.util.Optional;

/**
 * The {@code shape} blockstate property of both mold blocks: which part the
 * placed mold is shaped into. The shape lives IN THE BLOCK STATE so the
 * client renders the carved mold straight from the blockstate's model
 * variant - no block entity sync, no block entity renderer. Every shaped
 * value points at a block model whose top face uses a cutout texture with
 * the part's silhouette removed (a real mold cavity).
 *
 * <p>{@code NONE} is the fresh, unshaped mold.
 */
public enum MoldShape implements StringRepresentable {
        NONE("none"),
        HANDLE("handle"),
        BINDING("binding"),
        PICKAXE_HEAD("pickaxe_head"),
        AXE_HEAD("axe_head"),
        SHOVEL_HEAD("shovel_head"),
        HOE_HEAD("hoe_head"),
        SWORD_HEAD("sword_head"),
        SWORD_BINDING("sword_binding"),
        SPEAR_HEAD("spear_head"),
        SPEAR_BINDING("spear_binding");

        public static final Codec<MoldShape> CODEC = StringRepresentable.fromEnum(MoldShape::values);

        private final String id;

        MoldShape(String id) {
                this.id = id;
        }

        @Override
        public String getSerializedName() {
                return this.id;
        }

        /** @return the part type this shape represents, or empty for NONE. */
        public Optional<PartType> part() {
                return PartType.byId(this.id);
        }

        /** @return the shape property value of a shaped part. */
        public static MoldShape of(PartType part) {
                for (MoldShape shape : values()) {
                        if (shape.id.equals(part.id)) {
                                return shape;
                        }
                }
                return NONE;
        }
}
