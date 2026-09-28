package com.modulardreams.part;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModularMaterial;

/**
 * All part shapes of Modular Dreams (overhaul step 1).
 *
 * <p>Every tool is assembled from exactly three parts - handle, binding and
 * head. There is ONE UNIVERSAL HANDLE and ONE UNIVERSAL BINDING:
 * <ul>
 *   <li>the universal handle (simply {@code handle}) fits every tool,</li>
 *   <li>the universal binding (simply {@code binding}) fits the pickaxe,
 *       the axe, the shovel and the hoe,</li>
 *   <li>the sword and the spear keep their own dedicated bindings
 *       ({@code sword_binding} - displayed as the "Guard" - and
 *       {@code spear_binding}).</li>
 * </ul>
 *
 * <p>The sword and the spear additionally have their own handle SHAPES only
 * for RENDERING the assembled item (the item definitions use the
 * sword_handle / spear_handle models for their handle layer); those shapes
 * are not parts - no items, recipes or Part Builder entries exist for them.
 * The same is true for the pickaxe/axe/shovel/hoe binding SHAPES: every tool
 * assembles with the universal binding but keeps rendering its own binding
 * shape. The mace was removed entirely. Part ids follow
 * {@code <tool>_<role>} (e.g. {@code pickaxe_head}) - except the universal
 * handle and binding, which are simply {@code handle} and {@code binding}.
 *
 * <p>Which materials a part can be made of depends on its role:
 * <ul>
 *   <li><b>heads</b>: wood, stone, flint, bone (Part Builder) + copper, iron,
 *       gold (Melting Upgrade)</li>
 *   <li><b>bindings</b>: wood, stone, flint, leather, bone, vine, string,
 *       slime (Part Builder) + copper, iron, gold</li>
 *   <li><b>handles</b>: wood, stone, flint, bone, blaze rod, breeze rod
 *       (Part Builder) + copper, iron, gold</li>
 * </ul>
 */
public enum PartType {
        // the universal handle (fits EVERY tool)
        HANDLE("handle", "any", Role.HANDLE),
        // the universal binding (fits the pickaxe, the axe, the shovel and the hoe)
        BINDING("binding", "any", Role.BINDING),
        // pickaxe
        PICKAXE_HEAD("pickaxe_head", "pickaxe", Role.HEAD),
        // axe
        AXE_HEAD("axe_head", "axe", Role.HEAD),
        // shovel
        SHOVEL_HEAD("shovel_head", "shovel", Role.HEAD),
        // hoe
        HOE_HEAD("hoe_head", "hoe", Role.HEAD),
        // sword (rendered with the sword_handle shape, assembled with HANDLE +
        // its dedicated sword_binding, displayed as the Guard)
        SWORD_HEAD("sword_head", "sword", Role.HEAD),
        SWORD_BINDING("sword_binding", "sword", Role.BINDING),
        // spear (rendered with the spear_handle shape, assembled with HANDLE +
        // its dedicated spear_binding)
        SPEAR_HEAD("spear_head", "spear", Role.HEAD),
        SPEAR_BINDING("spear_binding", "spear", Role.BINDING);

        /** The role a part plays in tool assembly; decides its material set. */
        public enum Role {
                HEAD, BINDING, HANDLE
        }

        /** The tool-agnostic id of the universal handle's toolId field. */
        public static final String ANY_TOOL = "any";

        public final String id;
        public final String toolId;
        public final Role role;

        PartType(String id, String toolId, Role role) {
                this.id = id;
                this.toolId = toolId;
                this.role = role;
        }

        /** @return whether this handle part fits EVERY tool (the universal handle). */
        public boolean isUniversal() {
                return this.toolId.equals(ANY_TOOL);
        }

        public String translationKey() {
                return "part.modular_dreams." + id;
        }

        /** Materials this part can be crafted/melted from. */
        public List<ModularMaterial> allowedMaterials() {
                return switch (role) {
                        case HEAD -> ModMaterials.headMaterials();
                        case BINDING -> ModMaterials.bindingMaterials();
                        case HANDLE -> ModMaterials.handleMaterials();
                };
        }

        /** Material units consumed per part (Legacy's Construct costs). */
        public int cost() {
                return switch (role) {
                        case HEAD -> switch (toolId) {
                                case "shovel" -> 1;
                                case "hoe" -> 2;
                                default -> 3; // pickaxe, axe, sword, spear
                        };
                        case BINDING -> 2; // universal and sword/spear bindings alike
                        case HANDLE -> 2;
                };
        }

        public static Optional<PartType> byId(String id) {
                for (PartType part : values()) {
                        if (part.id.equals(id)) {
                                return Optional.of(part);
                        }
                }
                return Optional.empty();
        }

        /**
         * @return the HANDLE part of a tool: always the universal handle.
         *         The sword and the spear RENDER with their own handle shapes
         *         (see the item definitions), but those shapes are not parts.
         */
        public static PartType handlePartForTool(String toolId) {
                return HANDLE;
        }

        /**
         * @return the BINDING part of a tool: the universal binding for the
         *         pickaxe, the axe, the shovel and the hoe; the dedicated
         *         sword/spear bindings for the sword and the spear. The
         *         per-tool binding SHAPES stay render-only (see the item
         *         definitions).
         */
        public static PartType bindingPartForTool(String toolId) {
                return switch (toolId) {
                        case "sword" -> SWORD_BINDING;
                        case "spear" -> SPEAR_BINDING;
                        default -> BINDING;
                };
        }

        /** All parts of one tool, in assembly order: handle, binding, head. */
        public static List<PartType> partsOfTool(String toolId) {
                List<PartType> parts = new ArrayList<>();
                for (PartType part : values()) {
                        if (part.role == Role.HEAD && part.toolId.equals(toolId)) {
                                parts.add(part);
                        }
                }
                parts.add(bindingPartForTool(toolId));
                parts.add(handlePartForTool(toolId));
                parts.sort((a, b) -> Integer.compare(a.role.ordinal(), b.role.ordinal()));
                return parts;
        }

        public static void initialize() {
                // static init of all enums
                values();
        }
}
