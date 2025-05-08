package com.github.nalamodikk.common.register;

import com.github.nalamodikk.common.recipe.fuel.loader.ManaGenFuelRateLoader;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModDataReloadListeners {

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        // 註冊 FuelRateLoader 作為資源重載監聽器

        event.addListener(new ManaGenFuelRateLoader());

    }
}
