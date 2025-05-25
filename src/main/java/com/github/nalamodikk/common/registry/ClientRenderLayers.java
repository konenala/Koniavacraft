package com.github.nalamodikk.common.registry;

import com.github.nalamodikk.common.MagicalIndustryMod;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid = MagicalIndustryMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientRenderLayers {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            //  註冊透明材質方塊使用 cutout 層
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SOLAR_MANA_COLLECTOR.get(), RenderType.translucent());

            // 如果未來還有更多透明材質方塊，可以繼續加在這邊
            // ItemBlockRenderTypes.setRenderLayer(ModBlocks.XXX.get(), RenderType.translucent());
        });
    }
}
