package com.github.nalamodikk.common.registry;

import com.github.nalamodikk.common.ComponentSystem.register.component.ComponentRegistry;
import com.github.nalamodikk.common.MagicalIndustryMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import static com.github.nalamodikk.common.ComponentSystem.item.ModuleItem.createModuleItem;

public class ModCreativeModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB , MagicalIndustryMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAGICAL_INDUSTRY_ITEMS_TAB = CREATIVE_MODE_TABS.register("magical_industry_items_tab",
        () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.MANA_DUST.get()))
                .title(Component.translatable("creativetab.magical_industry_items"))
                .displayItems((pParameters, pOutput) -> {

                    pOutput.accept(ModItems.MANA_DEBUG_TOOL.get());
                    pOutput.accept(ModItems.BASIC_TECH_WAND.get());
                    pOutput.accept(ModItems.MANA_DUST.get());
                    pOutput.accept(ModItems.MANA_INGOT.get());
                    pOutput.accept(ModItems.SOLAR_MANA_UPGRADE.get());

                    pOutput.accept(ModItems.CORRUPTED_MANA_DUST.get());



                })
                .build());



    public static final RegistryObject<CreativeModeTab> MAGICAL_INDUSTRY_BLOCKS_TAB = CREATIVE_MODE_TABS.register("magical_industry_blocks_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.MANA_BLOCK.get()))
                    .title(Component.translatable("creativetab.magical_industry_blocks"))
                    .displayItems((pParameters, pOutput) -> {

                        pOutput.accept(ModBlocks.MANA_BLOCK.get());
                        pOutput.accept(ModBlocks.MAGIC_ORE.get());
                        pOutput.accept(ModBlocks.DEEPSLATE_MAGIC_ORE.get());
                        pOutput.accept(ModBlocks.DEEPSLATE_MAGIC_ORE.get());

                        pOutput.accept(ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get());
                        pOutput.accept(ModItems.MANA_GENERATOR_BLOCK_ITEM.get());
                        pOutput.accept(ModBlocks.SOLAR_MANA_COLLECTOR.get());
                        pOutput.accept(ModBlocks.MODULAR_MACHINE_BLOCK.get());

                        pOutput.accept(ModBlocks.MANA_CONDUIT.get());


                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> MAGICAL_INDUSTRY_MODULE_TAB = CREATIVE_MODE_TABS.register("magical_industry_module_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.MODULE_ITEM.get()))
                    .title(Component.translatable("creativetab.magical_industry_modules"))
                            .displayItems((parameters, output) -> {
                                ComponentRegistry.getAllComponentIds().forEach(componentId -> {
                                    ItemStack stack = createModuleItem(componentId); // ✅ 改這裡
                                    output.accept(stack);
                                });
                            })
                            .build()
            );


    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
