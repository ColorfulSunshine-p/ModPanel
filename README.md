# ModPanel

Client-side Fabric mod (Minecraft 1.21.1) that generates configuration GUIs from verified mod settings.

## Usage
- Install Fabric Loader, Fabric API, Cloth Config, and (optional) Mod Menu / Text Placeholder API.
- Launch game, press M or use Mod Menu to open ModPanel.
- Select a supported mod and edit settings.

## Supported integrations

| Mod | Configuration file | Format | Versions tested | Editable |
|-----|--------------------|--------|-----------------|----------|
| Lithium | `config/lithium.properties` | properties | 0.15.0 – 0.15.4 (verified on 0.15.4 / MC 1.21.1) | Yes |
| BadOptimizations | `config/badoptimizations.txt` | properties (`key: value`) | 2.4.1 | Yes |
| FerriteCore | `config/ferritecore.mixin.properties` | properties | 7.0.3 | Yes |
| ModernFix | `config/modernfix-mixins.properties` | documented properties | 5.25.1 | Yes |
| C2ME | `config/c2me.toml` | toml | 0.4.0-alpha.0.30 | Yes |
| EntityCulling | `config/entityculling.json` | json | 1.12.1 | Yes |
| ScalableLux | `config/scalablelux.properties` | properties | 0.1.0.1 | Yes |
| Krypton | – | – | 0.2.8 | No configuration file in this version |
| Noisium | – | – | 2.7.0 | No configuration file in this version |
| Generic Properties | `config/<modid>.properties` | properties | any | Read-only |
| Generic JSON | `config/<modid>.json` | json | any | Read-only |
| Generic TOML | `config/<modid>.toml` | toml | any | Read-only |
| Generic colon text | `config/<modid>.txt` | colon text | any | Read-only |

The screen of a supported mod is generated from that mod's own configuration file: setting names,
descriptions and default values are read from the file, nothing is guessed, and settings a mod does
not provide are never shown. Mods without a configuration file (Krypton, Noisium) are reported as
such instead of being given an invented file.

Rules ModPanel follows:
- A setting is editable only when its current value, type and write path are known.
- Keys that a target mod version does not define are never shown as editable.
- Writes back up the source, verify it did not change externally (SHA-256 fingerprint), and write atomically.
- Comments and unrelated entries of the file are preserved when saving.

## Baseline

| Component | Version |
|-----------|---------|
| Minecraft | 1.21.1 |
| Fabric Loader | 0.20.1 |
| Fabric API | 0.116.17 |
| Cloth Config | 15.0.140 |
| Mod Menu | 11.0.5 (optional) |
| Text Placeholder API | 2.4.2 (optional) |
| Lithium | 0.15.4 |
| Java | 21 |

## Build
`./gradlew build` (Java 21 target, Gradle 9.7.1 with Loom 1.18.4).
