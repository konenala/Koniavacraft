package com.github.nalamodikk.common.register.event;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.register.ModCapabilities;
import com.github.nalamodikk.system.nara.api.INaraData;
import com.github.nalamodikk.system.nara.data.NaraData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = MagicalIndustryMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModCommonEvents {
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.<INaraData, Void, Player>registerEntity(ModCapabilities.NARA_DATA, EntityType.PLAYER, (player, ctx) -> new NaraData());



    }
}
