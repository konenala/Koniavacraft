package com.github.nalamodikk.item;

import com.github.nalamodikk.block.ModBlocks;
import com.github.nalamodikk.magical_industry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB , magical_industry.MOD_ID);

public static final RegistryObject<CreativeModeTab> MAGICAL_INDUSTRY_TAB = CREATIVE_MODE_TABS.register("magical_industry_tab",
        () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.MANA_DUST.get()))
                .title(Component.translatable("creativetab.magical_industry_tab"))
                .displayItems((pParameters, pOutput) -> {
                    pOutput.accept(ModItems.MANA_DUST.get());
                    pOutput.accept(ModItems.MANA_INGOT.get());


                    pOutput.accept(ModBlocks.MANA_BLOCK.get());

                })
                .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
