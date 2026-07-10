# Auric

Auric is a Minecraft magic mod for NeoForge. It does not have one single focus; instead, it collects small magical systems, useful tools, and strange little interactions that make the world feel more enchanted.

The mod is currently early in development. Expect features to grow, shift, and connect more tightly over time.

## Project Facts

- Mod id: `auric`
- Current version: `0.1.0`
- Target: Minecraft 1.21.1, NeoForge 21.1.234, Java 21
- Optional integration: Jade 15+
- Server config: `config/auric-server.toml`

## Current Features

- Scented Candles that carry potion effects and apply them as a nearby scent.
- Potion cauldron brewing for brewing potions directly in heated cauldrons.
- An Imbuing Table for applying potion effects to eligible tools and weapons.
- Sculk Bottles for storing and throwing small amounts of experience.
- Camouflage tools for copying block appearances and disguising blocks.
- A Block Palette for compact builder block storage and random placement.
- Sword in Stone blocks and rare Forgotten Blade Shrine worldgen.
- Data-driven imbuing rules under `data/<namespace>/auric/imbue_effects/` so packs can add or replace eligible potion-effect behavior.
- Jade potion-cauldron diagnostics showing the stored potion, fill level, heat state, and visible effects.
- Configurable imbuing strength, XP cost, disallowed effects, potion candle mixing, and shrine generation.

## Design Direction

Auric is meant to be a broad magic mod, not a mod about only potions, only sculk, only building, or only combat. If a feature feels magical, useful, and a little unusual, it can belong here.

The rough pillars are:

- Potion-based utility and ambient effects.
- Magical building tools.
- Sculk and experience manipulation.
- Tool imbuing and enchantment-adjacent systems.
- Small discoveries that reward experimentation.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.234 or newer
- Java 21

## Configuration

The server config controls imbuing strength and XP cost, Dragon's Breath imbuing level caps, disallowed effects, mixed-effect candle stacking, and Forgotten Blade Shrine generation.

## Minecraft Beyond Integration

Minecraft Beyond uses Auric as its broad magic-and-utility layer and supplies Jade for readable potion-cauldron state. The pack also provides external ruby and sapphire materials used by other local projects; Auric's own systems remain deliberately data-driven and do not hard-depend on those projects.

## Building

Run:

```sh
./gradlew build
```

The built jars will be generated in `build/libs/`.

## Status

Auric is under active development and is not feature-complete. The current branch is playable enough for internal pack testing, but balance, final art, and stronger links between the magic systems are still in progress.

## Known Limitations

- Potion candle and imbuing balance are still early.
- Forgotten Blade Shrine generation is config-gated but still needs broader worldgen testing.
- Camouflage support is intentionally conservative; unsupported blocks are rejected instead of copied badly.

## License

All rights reserved.
