# Modular Dreams

**A Tinkers' Construct / Construct's Armory-inspired modular equipment system for Minecraft Java Edition 26.3 (Fabric), built strictly from vanilla Minecraft tools, weapons, armor and resources.**

Modular Dreams turns vanilla gear into modular equipment: assemble tools and armor from parts made of vanilla materials, repair them with their component materials instead of re-crafting them, and upgrade them with Tinkers-style modifiers. No custom ores, no new materials — everything is built from what vanilla already gives you.

> **Note about the project template:** the original template ZIP (`modular-dreams-template-26.3.zip`) did not arrive with the request (the upload directory was empty), so this project is built on the **official FabricMC `fabric-example-mod` template, branch `26.3`** — the exact same scaffold the fabricmc.net template generator produces, including its Gradle/Loom configuration, split main/client source sets and the Fabric API data-generation setup (`fabricApi { configureDataGeneration() { client = true } }` + `fabric-datagen` entrypoint). The template's Gradle/build configuration was preserved and adapted; nothing was replaced with an older-version setup.

---

## Requirements

| Component | Version |
|---|---|
| Minecraft | **26.3** (stable) |
| Fabric Loader | **>= 0.19.5** |
| Fabric API | **0.161.0+26.3** |
| Java | **>= 25** (Minecraft 26.3 requires Java 25) |
| Fabric Loom | 1.17-SNAPSHOT (via Gradle wrapper 9.5.1) |

Minecraft 26.1+ ships **unobfuscated** — Fabric no longer uses Yarn/intermediary for these versions, and neither does this project. All code targets Mojang's real (shipped) class names, e.g. `net.minecraft.resources.Identifier`.

## Building

```bash
# requires a JDK 25 toolchain (JAVA_HOME must point at JDK 25)
./gradlew build           # compiles + packages build/libs/modular-dreams-1.0.0.jar
./gradlew runDatagen      # regenerates src/main/generated (models, lang, recipes, tags, advancements)
```

The Gradle wrapper downloads Gradle 9.5.1 automatically. Make sure `JAVA_HOME` points to a Java 25 installation, since the mod compiles with `options.release = 25`.

## What's in the mod

### Modular tools & weapons (7 types)

Pickaxe, Axe, Shovel, Hoe, Sword, **Mace** and **Spear** — including Minecraft's mace (keeps its full smash-attack mechanics) and the vanilla spear (keeps its charged dash / kinetic weapon behavior). A tool's stats come from its parts:

- **Head** — defines mining speed, mining tier, and contributes the most attack damage
- **Handle** — multiplies durability, can adjust attack speed
- **Binding / Guard** — adds further durability and can grant material traits

Attack damage and attack speed are balanced for vanilla parity: an iron-headed modular sword hits exactly like a vanilla iron sword, a diamond-headed pickaxe mines exactly like a vanilla diamond pickaxe, etc.

### Modular armor (4 pieces)

Helmet, chestplate, leggings and boots assembled from **plates** (main material: defense, toughness, knockback resistance, durability) plus a **lining** (padding: utility attribute bonuses and extra enchantability). Armor built from a material *looks* like its vanilla counterpart — the per-instance `EQUIPPABLE` component reuses the vanilla equipment assets — and is fully trim- and enchantment-compatible.

### Materials (all vanilla)

- **19 tool materials**: 7 woods (oak, spruce, birch, cherry, bamboo, crimson, warped), bone, flint, stone, deepslate, blackstone, obsidian, amethyst, copper, iron, gold, diamond, netherite
- **7 plate materials**: copper, iron, gold, diamond, netherite, turtle scute (helmet only — it is a shell, not sheet metal), armadillo scute
- **5 lining materials**: leather, rabbit hide, phantom membrane, wool, slime ball

### Traits (vanilla-logical, Tinkers-inspired)

No arbitrary fantasy effects — each trait derives from what the material already *means* in vanilla:

| Trait | Material | Effect |
|---|---|---|
| Fiery | netherite | strikes ignite enemies |
| Dense | obsidian | 20% less durability damage |
| Featherweight | bone | +0.15 attack speed (handle) |
| Resonant | amethyst | +10% mining speed (binding) |
| Precious | diamond | cosmetic glint (binding) |
| Gilded | gold | +3 enchantability (binding) |
| Rooted | all woods | also repairable with sticks |
| Spiky | flint | +0.5 attack damage |
| Sturdy | iron | +5% durability per sturdy part |
| Fireproof | netherite | item immune to fire/lava |
| Cushioned | slime lining | increased safe fall distance |
| Featherlight | phantom membrane lining | -15% fall damage |
| Cozy | wool lining | +15% sneaking speed |
| Aquatic | turtle plate | +20% swim speed |
| Plated | armadillo plate | +5% knockback resistance |
| Soft | leather lining | +0.5 armor |
| Springy | rabbit hide lining | stronger jumps |

### Modifiers (15)

Applied in a crafting grid (assembled item + cost). Costs of 2+ items are deliberate: the cost item often doubles as a repair material, and a single item always means "repair" while 2+ means "upgrade".

| Modifier | Cost / level | Max | Effect |
|---|---|---|---|
| Hasty | 3x redstone | 3 | +30% mining speed / level |
| Sharp | 3x quartz | 3 | +1 attack damage / level |
| Lucky | 3x lapis | 3 | Fortune (digging) / Looting (weapons) |
| Silky | 4x amethyst shard | 1 | Silk Touch |
| Fiery | 2x blaze powder | 3 | sets targets on fire |
| Grippy | 3x string | 3 | +0.1 attack speed / level |
| Bouncy | 3x slime ball | 2 | +0.5 attack knockback / level |
| Reinforced | 2x obsidian | 3 | 15% less durability damage / level |
| Diamonded | 2x diamond | 1 | +500 max durability |
| Emeraled | 1x emerald | 1 | +50% max durability |
| Netherited | 2x netherite ingot | 1 | netherite mining tier + fireproof |
| Solid | 2x iron ingot | 2 | +10% knockback resistance / level |
| Featherfall | 3x phantom membrane | 2 | -20% fall damage / level |
| Swift Swim | 3x prismarine crystals | 2 | +25% swim speed / level |
| Sneaky | 3x rabbit hide | 2 | +25% sneak speed / level |

### Guide book: "Materials & You"

Crafted from a book + iron ingot (also in the creative tab). Right-clicking builds the book **dynamically from the live registries** — every material page (stats + traits), every plate/lining page, the modifier list with costs, assembly instructions, repair rules and tips — so the documentation can never go stale (~50 pages). It opens in the vanilla written-book UI; no custom screens.

### Repairing

Damaged modular gear + **one** repair material in a crafting grid restores 10% of max durability, repeatable forever. Repair items are derived per instance (baked `REPAIRABLE` component): heads repair with their material's crafting item, armor with the plate's vanilla armor-repair tag, wood-handle tools also accept sticks (Rooted). Vanilla anvil repair and Mending work too.

## Architecture

Everything rides on **vanilla data components** — the whole system is data-driven:

```
MODULAR_DATA (custom component)          <- part materials + modifier levels
    |
    v  StatsEngine.bake()
MAX_DAMAGE, TOOL (mining rules/speed), ATTRIBUTE_MODIFIERS, ENCHANTABLE,
REPAIRABLE, WEAPON, EQUIPPABLE (armor: slot/sound/vanilla asset),
KINETIC_WEAPON + PIERCING_WEAPON + ATTACK_RANGE + ... (spear),
DAMAGE_RESISTANT (netherite), ITEM_NAME, ITEM_MODEL (per-material look)
```

- **7 tool items + 4 armor items total** — individual gear differs only by its data components, exactly like vanilla 26.3 items.
- **3 custom `CustomRecipe` serializers**: `assembly` (any material combination of parts), `modifier`, `repair` — all singleton code recipes, no JSON duplication.
- **Part items** (202: material x part type) are plain ingredients; assembly accepts any valid combination of them in any grid arrangement.
- Per-material item visuals via the per-instance `ITEM_MODEL` component + 2-layer generated models (neutral base + colored head/plate overlay), one definition per material combination.
- Mace behavior is inherited by extending `MaceItem`; spear behavior is purely component-driven (`KINETIC_WEAPON`/`PIERCING_WEAPON`, replicating vanilla's exact construction parameters).
- Trait hooks: Fiery ignition via `hurtEnemy`, Dense/Reinforced durability reduction via Fabric's `CustomDamageHandler`.

### Data generation

All repetitive data is generated (`src/main/generated`, 1184 files):

- **375 item models** (flat parts, 2-layer tools/armor per material) + **374 client item definitions**
- **en_us.json** with every item name, guide page, trait, modifier and tooltip
- **206 recipes**: 202 part recipes + assembly + modifier + repair + guide book (each with its unlock advancement)
- **item tags**: modular tools/armor join the vanilla enchantable/equipment/trim tags so all vanilla enchantments and armor trims work
- **3 advancements** (root, first tool, three modifiers)
- **375 procedural pixel-art textures** (generated by `scripts/gen_textures.py` from per-material palettes + part silhouettes)

## Project layout

```
src/main/java/com/modulardreams/
    ModularDreams.java          entrypoint
    component/                  custom MODULAR_DATA component (codec/network)
    material/                   material definitions + mining tiers
    part/                       PartType enum
    equipment/                  items, tool/armor types, guide book
    stats/                      StatsEngine + traits
    modifier/                   modifier definitions
    recipe/                     assembly / modifier / repair recipes
    registry/                   helpers, creative tab, registry access
src/client/java/com/modulardreams/
    client/                     client entrypoint
    datagen/                    7 datagen providers
src/main/resources/             fabric.mod.json, classtweaker, icon, textures
src/main/generated/             datagen output (committed)
scripts/gen_textures.py         procedural texture generator (Python)
```

## Build results

The project compiles and builds successfully with the pinned toolchain (Gradle 9.5.1, Loom 1.17-SNAPSHOT, JDK 25, `--no-daemon`). The compiled mod JAR was smoke-tested by launching a dedicated server with Fabric Loader + Fabric API: the mod initializes, all registries/recipes/datapack data load without errors, and the server reaches the "Done" state.

## License

CC0-1.0 (inherited from the Fabric example mod template).
