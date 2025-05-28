package com.github.nalamodikk.common.register;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.mana_crafting.ManaCraftingTableBlockEntity;
import com.github.nalamodikk.common.block.mana_generator.ManaGeneratorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MagicalIndustryMod.MOD_ID);

    public static final Supplier<BlockEntityType<ManaCraftingTableBlockEntity>> MANA_CRAFTING_TABLE_BLOCK_BE =
            BLOCK_ENTITY_TYPES.register("mana_crafting_table_be", () ->
                    BlockEntityType.Builder.of(ManaCraftingTableBlockEntity::new,
                            ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get()).build(null));

    public static final Supplier<BlockEntityType<ManaGeneratorBlockEntity>> MANA_GENERATOR_BE =
            BLOCK_ENTITY_TYPES.register("mana_generator", () ->
                    BlockEntityType.Builder.of(ManaGeneratorBlockEntity::new,
                            ModBlocks.MANA_GENERATOR.get()).build(null));

//    public static final Supplier<BlockEntityType<ManaConduitBlockEntity>> MANA_CONDUIT_BE =
//            BLOCK_ENTITY_TYPES.register("mana_conduit", () ->
//                    BlockEntityType.Builder.of(ManaConduitBlockEntity::new,
//                            ModBlocks.MANA_CONDUIT.get()).build());
//
//    public static final Supplier<BlockEntityType<SolarManaCollectorBlockEntity>> SOLAR_MANA_COLLECTOR_BE =
//            BLOCK_ENTITY_TYPES.register("solar_mana_collector_be", () ->
//                    BlockEntityType.Builder.of(SolarManaCollectorBlockEntity::new,
//                            ModBlocks.SOLAR_MANA_COLLECTOR.get()).build());

//    public static final Supplier<BlockEntityType<ModularMachineBlockEntity>> MODULAR_MACHINE_BE =
//            BLOCK_ENTITY_TYPES.register("modular_machine", () ->
//                    BlockEntityType.Builder.of(ModularMachineBlockEntity::new,
//                            ModBlocks.MODULAR_MACHINE_BLOCK.get()).build());


    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
