package com.github.nalamodikk.common.register;

import com.github.nalamodikk.common.MagicalIndustryMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB , MagicalIndustryMod.MOD_ID);

    public static final Supplier<CreativeModeTab> MAGICAL_INDUSTRY_ITEMS_TAB =  CREATIVE_MODE_TABS.register("magical_industry_items_tab",
            ()-> CreativeModeTab.builder().icon(()-> new ItemStack(ModItems.MANA_DUST.get()))
                    .title(Component.translatable("creativetab.magical_industry_items"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.MANA_DUST);
                        output.accept(ModItems.CORRUPTED_MANA_DUST);
                        output.accept(ModItems.MANA_INGOT);
                        output.accept(ModItems.SPEED_UPGRADE);
                        output.accept(ModItems.EFFICIENCY_UPGRADE);
                        output.accept(ModItems.BASIC_TECH_WAND);
                        output.accept(ModItems.MANA_DEBUG_TOOL);

                    })
                    .build());
 public static final Supplier<CreativeModeTab> MAGICAL_INDUSTRY_BLOCKS_TAB =  CREATIVE_MODE_TABS.register("magical_industry_blocks_tab",
            ()-> CreativeModeTab.builder().icon(()-> new ItemStack(ModBlocks.MANA_BLOCK.get()))
                    .title(Component.translatable("creativetab.magical_industry_blocks"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.MAGIC_ORE);
                        output.accept(ModBlocks.DEEPSLATE_MAGIC_ORE);
                        output.accept(ModBlocks.MANA_BLOCK);
                        output.accept(ModBlocks.MANA_CRAFTING_TABLE_BLOCK);
                        output.accept(ModBlocks.MANA_GENERATOR);
                        output.accept(ModBlocks.SOLAR_MANA_COLLECTOR);

                    })
                    .build());

    public static void register(IEventBus eventBus){
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
