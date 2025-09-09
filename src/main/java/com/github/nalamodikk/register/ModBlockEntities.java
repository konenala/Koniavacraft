package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.collector.solarmana.SolarManaCollectorBlockEntity;
import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_crafting.ManaCraftingTableBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.block.blockentity.ritual.*;
import com.github.nalamodikk.common.block.ritual.RuneStoneBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, KoniavacraftMod.MOD_ID);

    public static final Supplier<BlockEntityType<ManaCraftingTableBlockEntity>> MANA_CRAFTING_TABLE_BLOCK_BE =
            BLOCK_ENTITY_TYPES.register("mana_crafting_table_be", () ->
                    BlockEntityType.Builder.of(ManaCraftingTableBlockEntity::new,
                            ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get()).build(null));

    public static final Supplier<BlockEntityType<ManaGeneratorBlockEntity>> MANA_GENERATOR_BE =
            BLOCK_ENTITY_TYPES.register("mana_generator", () ->
                    BlockEntityType.Builder.of(ManaGeneratorBlockEntity::new,
                            ModBlocks.MANA_GENERATOR.get()).build(null));

    public static final Supplier<BlockEntityType<SolarManaCollectorBlockEntity>> SOLAR_MANA_COLLECTOR_BE =
            BLOCK_ENTITY_TYPES.register("solar_mana_collector_be", () ->
                    BlockEntityType.Builder.of(SolarManaCollectorBlockEntity::new,
                            ModBlocks.SOLAR_MANA_COLLECTOR.get()).build(null));

    public static final Supplier<BlockEntityType<ArcaneConduitBlockEntity>> ARCANE_CONDUIT_BE =
            BLOCK_ENTITY_TYPES.register("arcane_conduit", () ->
                    BlockEntityType.Builder.of(ArcaneConduitBlockEntity::new,
                            ModBlocks.ARCANE_CONDUIT.get()).build(null));

    // === 🔮 儀式系統方塊實體 (Ritual System Block Entities) ===
    public static final Supplier<BlockEntityType<RitualCoreBlockEntity>> RITUAL_CORE =
            BLOCK_ENTITY_TYPES.register("ritual_core", () ->
                    BlockEntityType.Builder.of(RitualCoreBlockEntity::new,
                            ModBlocks.RITUAL_CORE.get()).build(null));

    public static final Supplier<BlockEntityType<ArcanePedestalBlockEntity>> ARCANE_PEDESTAL =
            BLOCK_ENTITY_TYPES.register("arcane_pedestal", () ->
                    BlockEntityType.Builder.of(ArcanePedestalBlockEntity::new,
                            ModBlocks.ARCANE_PEDESTAL.get()).build(null));

    public static final Supplier<BlockEntityType<ManaPylonBlockEntity>> MANA_PYLON =
            BLOCK_ENTITY_TYPES.register("mana_pylon", () ->
                    BlockEntityType.Builder.of(ManaPylonBlockEntity::new,
                            ModBlocks.MANA_PYLON.get()).build(null));

    public static final Supplier<BlockEntityType<RuneStoneBlockEntity>> RUNE_STONE =
            BLOCK_ENTITY_TYPES.register("rune_stone", () ->
                    BlockEntityType.Builder.of((pos, state) -> {
                        // 從方塊狀態中提取符文類型
                        if (state.getBlock() instanceof RuneStoneBlock runeBlock) {
                            return new RuneStoneBlockEntity(pos, state, runeBlock.getRuneType());
                        }
                        return new RuneStoneBlockEntity(pos, state, RuneStoneBlock.RuneType.EFFICIENCY);
                    },
                            ModBlocks.RUNE_STONE_EFFICIENCY.get(),
                            ModBlocks.RUNE_STONE_CELERITY.get(),
                            ModBlocks.RUNE_STONE_STABILITY.get(),
                            ModBlocks.RUNE_STONE_AUGMENTATION.get()).build(null));

//    public static final Supplier<BlockEntityType<ModularMachineBlockEntity>> MODULAR_MACHINE_BE =
//            BLOCK_ENTITY_TYPES.register("modular_machine", () ->
//                    BlockEntityType.Builder.of(ModularMachineBlockEntity::new,
//                            ModBlocks.MODULAR_MACHINE_BLOCK.get()).build());


    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
