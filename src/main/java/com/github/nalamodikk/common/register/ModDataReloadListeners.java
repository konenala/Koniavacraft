package com.github.nalamodikk.common.register;

import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModDataReloadListeners {

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
    }
}
