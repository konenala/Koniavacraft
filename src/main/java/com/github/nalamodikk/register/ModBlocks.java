package com.github.nalamodikk.register;


import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.ritual.arcanematrix.arcanepedestal.ArcanePedestalBlock;
import com.github.nalamodikk.common.block.ritual.arcanematrix.ritualcore.RitualCoreBlock;
import com.github.nalamodikk.common.block.blockentity.collector.solarmana.SolarManaCollectorBlock;
import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlock;
import com.github.nalamodikk.common.block.blockentity.mana_crafting.ManaCraftingTableBlock;
import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorBlock;
import com.github.nalamodikk.common.block.blockentity.mana_infuser.ManaInfuserBlock;
import com.github.nalamodikk.common.block.normal.DeepManaSoilBlock;
import com.github.nalamodikk.common.block.normal.ManaGrassBlock;
import com.github.nalamodikk.common.block.normal.ManaSoilBlock;
import com.github.nalamodikk.common.block.ritual.ManaPylonBlock;
import com.github.nalamodikk.common.block.ritual.ChalkGlyphBlock;
import com.github.nalamodikk.common.block.ritual.RuneStoneBlock;
import com.github.nalamodikk.common.block.ritual.RuneType;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(KoniavacraftMod.MOD_ID);


    public static final DeferredBlock<Block> MANA_BLOCK =
            registerBlock("mana_block", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static final DeferredBlock<Block> MANA_CRAFTING_TABLE_BLOCK =
            registerBlock("mana_crafting_table", () -> new ManaCraftingTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_WOOD)));

    public static final DeferredBlock<Block> MANA_GENERATOR =
            registerBlock("mana_generator", () -> new ManaGeneratorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final DeferredBlock<Block> ARCANE_CONDUIT =
            registerBlock("arcane_conduit", () -> new ArcaneConduitBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
                            .strength(1.5f)
                            .noOcclusion()
                            .lightLevel(state -> 7) // 發光等級
            ));

    public static final DeferredBlock<Block> SOLAR_MANA_COLLECTOR =
            registerBlock("solar_mana_collector", () -> new SolarManaCollectorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));


    public static final DeferredBlock<Block> MAGIC_ORE =
            registerBlock("magic_ore", () -> new DropExperienceBlock(UniformInt.of(3, 6), BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                                .strength(2f).requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> DEEPSLATE_MAGIC_ORE =
            registerBlock("deepslate_magic_ore", () -> new DropExperienceBlock(UniformInt.of(3, 8), BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)
                                .strength(4f).requiresCorrectToolForDrops()));

    // 🌍 魔力生態系統 - 土壤層
    public static final DeferredBlock<Block> MANA_SOIL =
            registerBlock("mana_soil", () -> new ManaSoilBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)
                    .strength(0.5F)
                    .sound(SoundType.GRAVEL)  // 略微不同的音效
                    .lightLevel((state) -> 2)  // 微弱發光 (2/15)
            ));

    public static final DeferredBlock<Block> DEEP_MANA_SOIL =
            registerBlock("deep_mana_soil", () -> new DeepManaSoilBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)
                    .strength(0.6F)
                    .sound(SoundType.GRAVEL)
                    .lightLevel((state) -> 1)  // 更微弱的發光
            ));

    public static final DeferredBlock<Block> MANA_GRASS_BLOCK =
            registerBlock("mana_grass_block", () -> new ManaGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK)
                    .strength(0.6F)
                    .sound(SoundType.GRASS)
                    .lightLevel((state) -> 3)  // 比純土壤亮一點
            ));

    /**
     * 魔力注入機
     */
    public static final DeferredBlock<ManaInfuserBlock> MANA_INFUSER = registerBlock("mana_infuser",
            () -> new ManaInfuserBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.5f).sound(SoundType.METAL).lightLevel(state ->
                            state.getValue(ManaInfuserBlock.WORKING) ? 7 : 0)));

    /**
     * 魔法儀式系統
     */
    public static final DeferredBlock<Block> RITUAL_CORE = registerBlock("ritual_core",
            () -> new RitualCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(4.0f).requiresCorrectToolForDrops().noOcclusion()));

    public static final DeferredBlock<Block> ARCANE_PEDESTAL = registerBlock("arcane_pedestal",
            () -> new ArcanePedestalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(2.5f).requiresCorrectToolForDrops().noOcclusion()));

    public static final DeferredBlock<Block> MANA_PYLON = registerBlock("mana_pylon",
            () -> new ManaPylonBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(3.0f).requiresCorrectToolForDrops().noOcclusion()));

    public static final DeferredBlock<Block> CHALK_GLYPH = registerBlock("chalk_glyph",
            () -> new ChalkGlyphBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE)
                    .strength(0.1f).noCollission().noOcclusion()));

    /**
     * 符文石系統
     */
    public static final DeferredBlock<Block> RUNE_STONE_EFFICIENCY = registerBlock("rune_stone_efficiency",
            () -> new RuneStoneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(2.0f).requiresCorrectToolForDrops(), RuneType.EFFICIENCY));

    public static final DeferredBlock<Block> RUNE_STONE_CELERITY = registerBlock("rune_stone_celerity",
            () -> new RuneStoneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(2.0f).requiresCorrectToolForDrops(), RuneType.CELERITY));

    public static final DeferredBlock<Block> RUNE_STONE_STABILITY = registerBlock("rune_stone_stability",
            () -> new RuneStoneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(2.0f).requiresCorrectToolForDrops(), RuneType.STABILITY));

    public static final DeferredBlock<Block> RUNE_STONE_AUGMENTATION = registerBlock("rune_stone_augmentation",
            () -> new RuneStoneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(2.0f).requiresCorrectToolForDrops(), RuneType.AUGMENTATION));


    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }


    private static <T extends Block> DeferredBlock<T> registerBlockWithItem(String name, Supplier<T> block, Function<T, BlockItem> itemFactory) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> itemFactory.apply(toReturn.get()));
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
