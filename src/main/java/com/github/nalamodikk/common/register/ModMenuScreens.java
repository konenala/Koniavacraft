package com.github.nalamodikk.common.register;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.collector.manacollector.SolarManaCollectorScreen;
import com.github.nalamodikk.common.block.mana_crafting.ManaCraftingScreen;
import com.github.nalamodikk.common.block.mana_generator.ManaGeneratorMenu;
import com.github.nalamodikk.common.block.mana_generator.ManaGeneratorScreen;
import com.github.nalamodikk.common.screen.shared.UniversalConfigMenu;
import com.github.nalamodikk.common.screen.shared.UniversalConfigScreen;
import com.github.nalamodikk.common.screen.shared.UpgradeMenu;
import com.github.nalamodikk.common.screen.shared.UpgradeScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = MagicalIndustryMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModMenuScreens {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.MANA_CRAFTING_MENU.get(), ManaCraftingScreen::new);
        event.register(ModMenuTypes.MANA_GENERATOR_MENU.get(), ManaGeneratorScreen::new);
        event.register(ModMenuTypes.SOLAR_MANA_COLLECTOR_MENU.get(), SolarManaCollectorScreen::new);
        event.register(ModMenuTypes.UNIVERSAL_CONFIG_MENU.get(), UniversalConfigScreen::new);
        event.register(ModMenuTypes.UPGRADE_MENU.get(), UpgradeScreen::new);
    }

}
