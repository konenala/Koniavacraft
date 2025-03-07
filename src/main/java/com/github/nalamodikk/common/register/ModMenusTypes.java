package com.github.nalamodikk.common.register;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.screen.ManaGenerator.ManaGeneratorMenu;
import com.github.nalamodikk.common.screen.manacrafting.ManaCraftingMenu;
import com.github.nalamodikk.common.screen.tool.UniversalConfigMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.common.extensions.IForgeMenuType;

public class ModMenusTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MagicalIndustryMod.MOD_ID);


    public static final RegistryObject<MenuType<ManaCraftingMenu>> MANA_CRAFTING_MENU =
            MENUS.register("mana_crafting",
                    () -> IForgeMenuType.create((windowId, inv, buf) -> ManaCraftingMenu.create(windowId, inv, buf)));

    public static final RegistryObject<MenuType<ManaGeneratorMenu>> MANA_GENERATOR_MENU =
            MENUS.register("mana_generator_menu",
                    () -> IForgeMenuType.create((windowId, inv, buf) -> new ManaGeneratorMenu(windowId, inv, buf)));

    public static final RegistryObject<MenuType<UniversalConfigMenu>> UNIVERSAL_CONFIG =
            MENUS.register("universal_config",
                    () -> IForgeMenuType.create((windowId, inv, buf) -> new UniversalConfigMenu(windowId, inv, buf)));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
