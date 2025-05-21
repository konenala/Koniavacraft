package com.github.nalamodikk.register;

import com.github.NalaParticleLib;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;


public class ModCreativeModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB , NalaParticleLib.MOD_ID);


    public static final RegistryObject<CreativeModeTab> PARTICLE_LIB_TAB = CREATIVE_MODE_TABS.register("particlelib_items_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> ModItems.MAGIC_WAND.isPresent() ? new ItemStack(ModItems.MAGIC_WAND.get()) : new ItemStack(Items.BARRIER))
                    .title(Component.translatable("creativetab.nalaparticlelib.items"))
                    .displayItems((pParameters, pOutput) -> {
                        ModItems.MAGIC_WAND.ifPresent(pOutput::accept);
                    })
                    .build());



    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
