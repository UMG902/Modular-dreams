# Modular Dreams v2 — fresh start

A clean-slate rebuild of the Modular Dreams overhaul for **Minecraft 26.3 (Fabric)**.
The v1 repo (https://github.com/UMG902/Modular-dreams) is used as an **API reference
only** — no features, no ideas, ever.

Requires **Fabric Loader ≥ 0.19.5** and **Fabric API** for 26.3, on **Java 25**.

## The core loop

1. **Mine materials** — wood (any planks), stone (**every stone variant counts as
   cobblestone** in crafting), copper, iron, gold, **rose gold** (copper ingot +
   gold ingot, shapeless → 2), diamond, netherite.
2. **Part Picker** (5 tiers) — shapes **handles and heads**. Each tier works its own
   material plus the next tier's: wood → wood + stone, stone → + copper,
   copper → + iron/gold/rose gold, iron → + diamond, diamond → + **netherite**.
   Costs match vanilla heads (pickaxe 3, axe 3, shovel 1, hoe 2, sword 2, spear 2,
   handle 1). Stone picker recipes accept any stone variant.
3. **Assembler** — put a handle + head in, take your tool out. It also **converts
   vanilla tools** (head of the tool's material + wooden handle; netherite tools
   convert with a netherite handle) while keeping enchantments, custom name and
   relative wear. Vanilla spears (wooden, copper) convert too.
4. **Tools** — pickaxe, axe, shovel, hoe, sword and the **spear** (26.3 vanilla
   spear mechanics: charge-stab, lunge-enchantable — wooden heads use the wooden
   spear sounds). Stats come from the head (Tinkers'-style values); the handle
   multiplies durability and some handles boost speed and attack.

**Station crafting recipes:** the wood Part Picker is a crafting table ringed by
planks; every higher tier is its material ringed around the previous picker.
The Assembler is iron ingots around a diamond, over a stone-material block
(pattern: ` I ` / `IDI` / ` S `).

**Part Picker option list per material:** the handle + one head per tool type
(6) + the 4 armor platings where the material has them → **11 options for a full
material**; rods offer only their handle; netherite offers tool parts but no
platings. Armor plating costs are the vanilla armor material costs:
**helmet 5, chestplate 8, leggings 7, boots 4**.

### Handle stats

| Handle | Durability × | Speed/Attack × |
|---|---|---|
| wood | 1.0 | 1.0 |
| stone | 0.9 | 1.0 |
| copper | 0.9 | 1.0 |
| iron | 1.1 | 1.0 |
| gold | 1.0 | 1.0 |
| rose gold | 1.1 | 1.05 |
| diamond | 1.3 | 1.1 |
| netherite | 1.4 | 1.1 |
| blaze rod | 1.1 | 1.0 |
| breeze rod | 1.1 | 1.0 |

Formulas (baked by the `StatsEngine`): durability = head durability × handle
multiplier, mining speed = head speed × handle boost, attack = head attack bonus ×
handle boost + tool base damage, harvest tier and enchantability from the head.

## Traits

Every material owns a trait **that shares its name**: the trait of wooden parts
is `wood`, a rose gold head carries `rose_gold`, a blaze rod handle carries
`blaze_rod` — 10 full/handle materials. Raw materials join the same vocabulary
through the smithing table: `quartz`, `redstone`, `lapis`, `emerald`,
`prismarine`, `amethyst`, `slime`, `flint`, `bone` — plus the lining traits
`wool`, `magma_cream`, `phantom_membrane`.

- **Inherent traits** follow from a tool's parts (head + handle) or an armor
  piece's parts (lining + plating) and are never written down.
- **Applied traits** are granted at the smithing table and stored on the tool.
- Re-applying a trait the tool already carries (inherent OR applied) is refused
  — no materials are ever wasted on a no-op.
- The tool tooltip lists them: inherent in gray, applied in aqua.
- **All trait effects are implemented** (see "Trait effects" below).
- `string` and `leather` are retired as smithing modifiers (the leather trait
  lives on via the leather lining).
- Trait-enchant mutual exclusion rules are still a future milestone.

## Smithing table modifiers

The **vanilla smithing table** (no custom menu): 1 modular tool in the base
slot + materials in the addition slot. Taking the result applies the material's
trait to the tool and re-bakes the tool's stats. 16 trait modifiers — modifier
recipes refuse to show until the full material amount is present:

| Modifier | Material | Cost |
|---|---|---|
| quartz / redstone / lapis / emerald / prismarine / amethyst / slime / flint / bone | the raw material | 3 |
| wood | any planks | 3 |
| stone | any stone variant | 3 |
| iron / gold / diamond | the ingot/gem | 3 |
| rose gold | the mod's ingot | 3 |
| **netherite** | netherite ingot | **1**, plus the **vanilla `netherite_upgrade_smithing_template`** in the template slot, consumed like vanilla |

A custom mixin makes the vanilla menu consume exactly the amount in the table;
vanilla smithing recipes are untouched and always consume 1. Modifier recipes
without a template require the template slot to be EMPTY, so vanilla template
recipes stay unambiguous.

### Modifier caps & unlocks

A tool takes at most **3 applied modifiers**. Three strictly sequential unlock
recipes (each consumed once) raise the cap:

| Unlock | Material | Cap |
|---|---|---|
| 1 | nether star | 4 |
| 2 | elytra | 5 |
| 3 | dragon egg | 6 |

**The dragon-egg legendary:** a tool carrying the egg is indestructible as an
item entity (fire, lava, cactus, explosions), survives `/kill`, and falling out
of the world returns it to the world spawn instead of discarding it. Near Nether
heat it keeps its FULL durability damage (the netherite reduction is waived for
it). The extraction recipe — the egg tool ALONE in a crafting grid — returns the
egg and deletes the tool's LAST applied modifier (cap back down one step), so
the egg is one-per-world and can never be duplicated.

## Trait effects (fully implemented)

Every trait now does something. Tick-driven traits re-derive what each player
carries every tick from their worn armor, held tools and inventory — nothing
is stored on the player.

**Material traits**

| Trait | Effect |
|---|---|
| wood (Regenerative) | Feet in water + outside in daylight + no rain + sky visible: every damaged wood-trait stack in the whole inventory repairs 1 durability per 3 s |
| stone (Shadowed) | Below Y 63 with no sky above: +10 flat mining speed on held tools / +2 armor points on worn armor |
| copper (Powered) | An active redstone source within 10 blocks (±4 vertically, re-checked every 2 s): copper/rose-gold items take no durability damage for 10 s (refreshes while near) |
| iron (Last Stand) | At 4 HP or less: Strength I while holding an iron-trait tool, Resistance I while wearing iron-trait armor (maintained while low) |
| gold (Piglin's Favor) | In the Nether: Absorption I maintained — 5 min 10 s window, re-applied every 5 min; hearts LOST to damage only come back at the next refresh. ANY gold-trait item in the inventory pacifies piglins |
| rose gold (Defiant) | In the Nether: the gold effects, but piglins are always hostile; in the Overworld: the copper Powered effect |
| diamond (Fortunate) | 50% durability saves: mining lower-tier blocks, hitting non-boss mobs at 20 HP or less, and armor hits under 2 damage |
| netherite (Netherforged) | The ITEMS are fire/lava-proof (vanilla `DamageResistant`, dropped netherite stacks too) — the player is NOT fire-immune. In the Nether, with a lava or fire block within 8 blocks (±4 vertically, re-checked every 2 s): the held netherite weapon deals +20% attack damage, the held netherite tool mines +20% faster, and every netherite item takes 50% less durability damage (dragon-egg tools exempt) |
| blaze rod (Blazing, handle) | Mobs hit are set on fire (3 s); broken blocks auto-smelt when a smelting recipe exists |
| breeze rod (Windborne, handle) | Right-click launches the player upward with a gust at their feet (1.1 launch power, 2 s cooldown, mace combo) — right-click on a SPEAR keeps the vanilla charge-stab |

**Lining traits**

| Lining | Effect |
|---|---|
| leather (Frostward) | Immune to freezing damage (vanilla leather semantics — the buildup never progresses) |
| wool (Silent) | Every game event the player causes is dropped — sculk sensors and wardens cannot detect them |
| slime (Bouncy) | No fall damage and no elytra wall-impact damage; the player bounces upward instead — bounce = impact speed × 0.7, clamped to 0.6–2.0 (tuned, FLAGGED FOR USER); a hit that deals no damage gives no bounce |
| phantom membrane (Phantom) | Full phantom-lining set: void fall teleports to the spawn point instead of killing — the save consumes exactly ONE lining (a full set is four saves), platings always stay. Any phantom lining in the End: Levitation immunity; crouching grants Slow Falling (4 s) |
| magma cream (Heatproof) | Fire/lava damage reduced by 50% |

**Smithing modifier traits** — quartz Power (+1 attack damage), flint Swift
(+0.25 attack speed), redstone Haste (+15% mining speed), emerald Reinforced
(+20% max durability) are **baked into the tool components** at apply time (and
survive re-bakes); lapis Luck acts as **Fortune I for mining and Looting I for
kills** by riding the vanilla enchantment lookups (no enchantment is written
onto the item); prismarine Aquatic removes the underwater mining penalty;
amethyst Resonance gains +0.1 attack speed per chained hit inside a 3 s window
(cap +0.6 at 6 chained hits); slime Vacuum pulls broken blocks straight into the
inventory (leftovers drop normally); bone Piercing ignores 20% of the target's
armor reduction and 20% of the target's Protection reduction — two separate
defense layers, each pierced by its own fraction, computed as exact vanilla-math
pre-amplification and capped at 4× the input.

All tunable numbers live in `TraitTuning` — values marked `FLAGGED FOR USER`
are balance defaults awaiting playtesting.

## Armor linings (first armor milestone)

Five linings — **leather, wool, slime, magma cream, phantom membrane** —
crafted from 2× their source material (shapeless). A lining:

- is **wearable on its own**: right-click equips it into the first *empty*
  armor slot (head → chest → legs → feet; occupied slots are never swapped
  out), and every armor slot of the vanilla inventory accepts it;
- has **no armor value**: no protection, no attributes, no durability — its
  only identity is its **trait, which is its own name** (`leather`, `wool`,
  `slime`, `magma_cream`, `phantom_membrane`), shown in the tooltip;
- shows a recolored cloth layer on the body while worn (equipment assets);
- is the padding layer the **armor plating** assembles with.

Implementation note: 26.3's `Equippable` component holds exactly one slot per
item, so a small mixin (`LivingEntityMixin`) opens all four humanoid armor
slots for lining items — vanilla gating stays untouched for everything else.

## Armor plating (second armor milestone)

Twenty-eight platings — the 4 armor slots × the 7 full materials
**wood, stone, copper, iron, gold, rose gold, diamond** — shaped at the
**Part Picker** like tool parts (no crafting recipes). Item ids follow the part
order: `helmet_plating_iron`, `chestplate_plating_rose_gold`, ...

A plating:

- is **slot-specific**: each plating type belongs to exactly one armor slot
  (`PlatingType.HELMET_PLATING.slot == HEAD`, etc.);
- carries its **material trait** (the trait is the material's name), shown in
  the gray tooltip line like the linings — but grants **no armor value**
  on its own and is **not wearable** (that is the assembled piece's job).

**No netherite plating, by design:** platings stop at diamond. Netherite
armor is reachable ONLY through the smithing upgrade (netherite ingot +
vanilla template), and that upgrade only applies to armor with **diamond
plating** — vanilla parity. The modifier system does not touch armor yet.

## Armor assembly (third armor milestone)

The **Assembler** also builds armor: a **lining in the handle slot + a plating
in the head slot** → the piece the plating is shaped for. Assembly consumes
both parts.

- **The plating decides everything protective** — armor value, toughness,
  knockback resistance, durability (`slot unit × material multiplier`,
  vanilla formula), enchantability, repair items, equip sound and the worn
  layer — through a vanilla `ArmorMaterial` record per material, so the
  baked components are byte-identical in shape to vanilla armor.
- **The lining contributes its trait.** A piece's inherent traits = lining
  trait + plating trait, shown in the tooltip (same style as tools).

Ladder (durability multiplier, defense helmet/chestplate/leggings/boots,
enchantability — armor enchantability reuses the material's tool value):

| Material | Dur. × | Defense | Ench | Notes |
|---|---|---|---|---|
| wood | 5 | 1/3/2/1 | 15 | leather tier, leather equip sound |
| stone | 8 | 1/4/3/1 | 5 | chain equip sound |
| copper | 11 | 1/4/3/2 | 13 | vanilla copper |
| iron | 15 | 2/6/5/2 | 14 | vanilla iron |
| gold | 7 | 1/5/3/2 | 22 | vanilla gold |
| rose gold | 13 | 2/5/4/2 | 18 | between copper and iron, iron equip sound |
| diamond | 33 | 3/8/6/3 | 10 | vanilla diamond, toughness 2 |
| netherite | 37 | 3/8/6/3 | 15 | **upgrade-only** — toughness 3, knockback resistance 0.1, fire resistant |

- **Netherite upgrade (the diamond-plating gate):** a DIAMOND-plated piece +
  1 netherite ingot + the vanilla smithing template becomes the same piece
  with netherite plating (the netherite row above), keeping its lining and
  enchantments. Iron-plated or worse refuses.
- Worn look: per-material humanoid armor layers (vanilla textures where
  they exist, recolored iron layers for wood/stone/rose gold); the
  inventory icon layers the lining padding under the plating silhouette.

## Kiln & Fletching Table

- **Kiln** — the third furnace (8 bricks in the furnace pattern). It smelts
  *everything a regular furnace smelts except what belongs to the smoker or
  the blast furnace* — anything with a smelting recipe but no smoking/blasting
  recipe (sand, cobble, clay, logs, cactus, ...), including modded smelting.
  No recipe entries of its own; matching is dynamic. Cooks at twice the
  furnace speed (100 t). Fuel, hoppers, XP and the lit state reuse the vanilla
  furnace engine. Non-kilnable items are refused by the input slot, so hoppers
  cannot jam on smoker food or ores.
- **Fletching Table** — the vanilla block now opens a menu: 2 potion slots +
  an arrow slot. Each potion coats up to 32 arrows, so 2 potions + 64 arrows
  → 64 tipped arrows in one take (1 potion + 32 arrows works; mixed potions
  are rejected; effectless bases — water, mundane, thick, awkward — don't
  coat). Drinkable, splash and lingering potions all work.

## Build

```
./gradlew build        # Gradle wrapper 9.7.1, JDK 25
```

The jar lands in `build/libs/`. Requires Fabric Loader ≥ 0.19.5 and **Fabric API**
for 26.3 (`0.162.0+26.3`). Loom 1.18-SNAPSHOT, Java 25. Mod version:
`2.0.0-mc26.3`.

Headless verification: `MODULAR_DREAMS_SELFTEST=true` (or
`-Dmodular_dreams.selftest=true`) runs **129 end-to-end checks** at server start —
smithing modifiers, netherite template requirement, the lining equip rules, the
plating rules, armor assembly and the netherite armor upgrade included.

## Layout

- `src/main/java/com/modulardreams/` — materials, parts, components, stats engine,
  stations, menus, networking, linings, platings, armor pieces
- `src/client/java/` — screens, the `modular_dreams:modular_tool` layered item
  model type (Tinkers'-style per-material layering)
- `src/main/resources/assets/modular_dreams/` — textures (asset pack integrated +
  generated missing pieces), item definitions, equipment assets, blockstates, lang
- `src/main/resources/data/` — recipes, tags, loot tables
- `scripts/` (project root sibling) — `gen_textures.py`, `gen_lining_assets.py`,
  `gen_plating_assets.py`, `gen_armor_assets.py`, `gen_data.py` regenerate all
  derived assets and JSON

## Next milestones (per the v2 plan)

- Trait-enchant mutual exclusion rules
- Magic resistance modifier, balance pass
- Tuning pass on the flagged trait numbers in `TraitTuning`
