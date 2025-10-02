# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Koniavacraft** is a NeoForge 1.21.1 Minecraft mod combining magic and technology, featuring custom machines, ritual systems, mana networks, and biome generation. The mod uses Java 21 and Gradle for build automation.

- **Mod ID**: `koniava`
- **Main Package**: `com.github.nalamodikk`
- **License**: MIT

## Essential Build Commands

All commands use PowerShell with Gradle Wrapper:

```powershell
# Full build with validation
.\gradlew.bat build

# Launch test client/server
.\gradlew.bat runClient
.\gradlew.bat runServer

# Regenerate JSON assets (models, recipes, loot tables)
.\gradlew.bat runData

# Run NeoForge GameTests
.\gradlew.bat runGameTestServer

# Clean build artifacts
.\gradlew.bat clean
```

**IMPORTANT**: Before running `runData`, verify JDK 21 is installed and `JAVA_HOME` is set:
```powershell
powershell.exe -Command "java -version"
```

## Code Architecture

### Package Structure

- **`register/`**: DeferredRegister classes for Blocks, Items, BlockEntities, Menus, Recipes, etc.
- **`common/`**: Server-side and shared logic
  - `common/block/`: Block classes (normal, ritual blocks)
  - `common/block/blockentity/`: BlockEntity implementations
  - `common/network/`: Custom packet handlers
  - `common/datagen/`: Data generation providers
  - `common/capability/`: Energy/mana capability implementations
  - `common/utils/`: Shared utilities
- **`client/`**: Client-only rendering and GUI
  - `client/screenAPI/`: Custom UI framework
  - `client/event/`: Client event handlers
- **`narasystem/`**: Nara UI system with intro/binding screens
- **`biome/`**: Custom biome registration and terrain generation
- **`experimental/`**: Work-in-progress features

### Registration Flow

1. `KoniavacraftMod` constructor registers DeferredRegisters on mod event bus
2. Registers execute during FML lifecycle (Items → Blocks → BlockEntities → Menus → Recipes)
3. `commonSetup()` initializes biome systems via `UniversalBiomeRegistration`
4. Client-specific registration in separate event subscribers

### Energy System

- **Dual Energy**: Blocks store both Mana (custom) and RF (NeoForge standard)
- **ManaStorage**: Custom implementation with `receive()`, `extract()`, `getMana()` interfaces
- **Network Nodes**: Conduit blocks use `NetworkManager` to track connections and IO modes
- **Capabilities**: Exposed via `ModCapabilities.MANA` on block sides

### Ritual System Architecture

**Core Components** (`common/block/ritualblock` + `common/block/blockentity/ritual`):

1. **RitualCoreBlock/BlockEntity**: State machine controller
   - States: `IDLE` → `PREPARING` → `RUNNING` → `COMPLETED`/`FAILED`
   - Validates structure via `RitualStructureValidator`
   - Validates materials via `RitualMaterialValidator`
   - Only consumes catalyst after validation passes
   - Tracks ritual progress and mana consumption

2. **ArcanePedestalBlock/BlockEntity**: Offering platforms
   - Single-slot inventory for ritual offerings
   - Server: Particle effects, core synchronization
   - Client: Rotation/floating animations
   - Syncs via `setChangedAndSync()` on inventory changes
   - Notifies core via `RitualCoreTracker` when offerings change

3. **RuneStoneBlock/BlockEntity**: Ritual modifiers
   - Four types: Efficiency, Celerity, Stability, Augmentation
   - Custom geometry models (not simple cubes) with distinct visual designs
   - Uses `RitualCoreTracker` cache to find nearby cores
   - Calculates stacking bonuses via `RuneType.calculateEffect()`

4. **ManaPylonBlock/BlockEntity**: Energy providers
   - 500k mana capacity, provides energy to rituals
   - Must be within 9 blocks of ritual core
   - Connects to conduit network via capability caching

5. **ChalkGlyphBlock**: Floor decorations
   - Properties: `COLOR` (6 colors), `PATTERN` (multiple variants)
   - Ultra-thin layer (0.5 height), requires solid block beneath
   - Same-color chalk items cycle through patterns on use
   - Textures in `textures/block/ritual/` subdirectory

**Validation Flow**:
- Structure check: Pedestal positions/facing, pylon distance, chalk counts by color/pattern
- Material check: Offering types/quantities against recipe requirements
- Results stored in `RitualValidationContext.structureSummary` map
- Recipe `structure_requirements` uses keys like `pedestal.north`, `glyph.color.white`, `pylon.total`

## Data Generation Workflow

1. Implement providers in `common/datagen/`:
   - `ModBlockStateProvider`: Block models and blockstates
   - `ModItemModelProvider`: Item models
   - `ModRecipeProvider`: Crafting/smelting recipes
   - `ModLootTableProvider`: Block drops

2. Run `.\gradlew.bat runData` to generate JSON files into `src/generated/resources/`

3. **Review with `git diff`** before committing to avoid overwriting manual assets

4. **Critical**: Ensure loot tables exist for all blocks (e.g., `chalk_glyph.json`) to prevent generation failures

## Resource Organization

- **Textures**: `src/main/resources/assets/koniava/textures/`
  - Ritual assets in `block/ritual/` subdirectory
  - Use `modLoc("block/ritual/...")` in datagen for ritual textures

- **Models**: Auto-generated in `src/generated/resources/` or manual in `src/main/resources/`

- **Localization**: `assets/koniava/lang/`
  - `en_us.json` (English)
  - `zh_tw.json` (Traditional Chinese)
  - **Always use `Component.translatable()` for UI text**, never hardcode Chinese

## Documentation Maintenance

When making changes, update in order:

1. `spec.md` - Technical specification and data models
2. `api.md` - API versioning and external interfaces (currently no REST endpoints)
3. `AGENTS.md` (root and `docs/`) - Collaboration guidelines (keep in sync)
4. `todolist.md` - Task tracking
5. `個人開發者小記錄/ritual/祭壇符文系統.md` - Ritual system details (if applicable)

## Coding Standards

- **Indentation**: 4 spaces (not tabs)
- **Naming**:
  - Classes: `UpperCamelCase`
  - Methods/Variables: `lowerCamelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Resource files: `lower_snake_case`

- **Comments**: Use Traditional Chinese to explain purpose, inputs, outputs
- **Logging**: Use `KoniavacraftMod.LOGGER`, remove `System.out.println`
- **Localization**: All user-facing text via `translatable` keys

## Critical Synchronization Rules

### BlockEntity Sync Pattern
```java
// Server-side state changes MUST call:
setChangedAndSync(); // Triggers ClientboundBlockEntityDataPacket

// Override saveOptional() to avoid serializing empty/default values
@Override
protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    if (!itemStack.isEmpty()) {
        tag.put("Item", itemStack.save(registries));
    }
}
```

### Nara UI Performance
- Cache layouts and textures in `init()`, not `render()`
- Avoid per-frame widget creation or texture reloading
- Pre-calculate positions to minimize GPU overhead

## Testing Requirements

1. **Before PR**: Run `.\gradlew.bat build` successfully
2. **GUI Changes**: Test with `runClient`, verify translations render correctly
3. **BlockEntity Changes**: Test with both `runClient` and `runServer` for sync
4. **Network/Energy**: Stress-test with multi-node conduit setups
5. **Data Generation**: Verify JSON output with `git diff`, test recipes in-game

## Common Pitfalls

- **Empty ItemStack serialization**: Always check `!stack.isEmpty()` before NBT save
- **Chalk glyph loot tables**: Missing loot table blocks data generation
- **Ritual texture paths**: Use `block/ritual/` subdirectory, not root `block/`
- **JDK version**: Data generation requires JDK 21 with `JAVA_HOME` set
- **Hard-coded Chinese**: Replace with `Component.translatable("key.koniava.xyz")`

## Branch Strategy

- **master**: Production releases
- **dev/test1.21.1**: Active development branch (current)
- Fork for features, submit PR with:
  - Change purpose
  - Test steps/results
  - Screenshots/videos for GUI/effects
  - Updated spec/api/todo docs

## Development Philosophy

⚠️ **DO NOT implement unrequested features** - Confirm requirements before coding
- Start with minimal viable version, iterate based on feedback
- Prioritize visual demonstrations (screenshots/videos) over lengthy descriptions
- Ask before assuming - design discussion precedes implementation