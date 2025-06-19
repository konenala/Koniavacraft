package com.github.nalamodikk.register.client;

import com.github.nalamodikk.client.event.ClientVanillaInvGuiEvents;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public class commonClientInit {
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ClientVanillaInvGuiEvents::register);
    }
}
