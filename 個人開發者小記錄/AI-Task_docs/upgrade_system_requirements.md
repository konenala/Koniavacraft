# Koniavacraft Mod: Mana Generator Upgrade System Requirements Specification (v1.0)

## 1. Overview

This document defines the upgrade system for the "Mana Generator" in the Koniavacraft mod. The goal of this system is to provide players with a mechanism for progressively improving their generator's performance, adding strategic depth and resource sinks to the mid-to-late game. Players will be able to craft different upgrade modules and install them into the generator to gain specific benefits.

---

## 2. Core Features & Specifications

### 2.1. Upgrade Items

The system includes four craftable upgrade items:

1.  **Accelerated Processing Unit**: Significantly increases the generator's operational speed, which also proportionally increases fuel consumption rate.
2.  **Expanded Fuel Chamber**: Increases the total burn time of a single piece of fuel, effectively improving fuel efficiency.
3.  **Catalytic Converter**: Increases Mana and Energy output by a percentage without altering the fuel consumption rate.
4.  **Diagnostic Display**: Provides no performance boost but displays additional real-time operational data (e.g., projected output/tick) in the generator's GUI.

### 2.2. Upgrade Interface

*   The Mana Generator will provide **4 dedicated upgrade slots**.
*   These slots will only accept items tagged with `koniava:upgrades`.
*   Players can freely install or remove upgrades via the Mana Generator's GUI.

### 2.3. Upgrade Effect Calculation

*   **Accelerated Processing Unit**:
    *   Effects are stackable.
    *   Processing cycles per tick `ticksToProcess = 2 ^ (number of upgrades)`. E.g., 1 upgrade = 2 cycles/tick, 2 upgrades = 4 cycles/tick.
    *   Total output and fuel consumption are proportionally increased.

*   **Expanded Fuel Chamber**:
    *   Effects are stackable.
    *   Final burn time `finalBurnTime = baseBurnTime * (1 + 0.5 * numberOfUpgrades)`. E.g., 1 upgrade adds 50% burn time, 2 upgrades add 100%.

*   **Catalytic Converter**:
    *   Effects are stackable.
    *   Final output efficiency `efficiencyMultiplier = 1 + (0.25 * numberOfUpgrades)`. E.g., 1 upgrade boosts output by 25%, 2 upgrades by 50%.

*   **Diagnostic Display**:
    *   Effect does not stack; one is sufficient.
    *   When active, the GUI's `ContainerData` will be populated with additional runtime data.

---

## 3. Technical Implementation Points

*   **Items & Tags**:
    *   New items must be registered in `ModItems`.
    *   A tag file `data/koniava/tags/item/upgrades.json` must be created.

*   **BlockEntity**:
    *   `ManaGeneratorBlockEntity` needs a new `ItemStackHandler(4)` for upgrades, persisted via NBT.
    *   The `IItemHandler` capability for the upgrade slots must be exposed by `ManaGeneratorBlockEntity`.

*   **Logic Modifications**:
    *   `ManaFuelHandler`: Implement the burn time bonus for the "Expanded Fuel Chamber".
    *   `ManaGeneratorTicker`: Implement the accelerated processing for the "Accelerated Processing Unit" and the efficiency bonus for the "Catalytic Converter".

*   **GUI**:
    *   `ManaGeneratorMenu` and `ManaGeneratorScreen` must be updated to include the 4 upgrade slots for display and interaction.