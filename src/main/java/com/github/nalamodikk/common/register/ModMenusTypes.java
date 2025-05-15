package com.github.nalamodikk.common.register;

import com.github.nalamodikk.common.ComponentSystem.screen.ModularMachineMenu;
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
                    () -> IForgeMenuType.create(ManaCraftingMenu::create));

    public static final RegistryObject<MenuType<ManaGeneratorMenu>> MANA_GENERATOR_MENU =
            MENUS.register("mana_generator_menu",
                    () -> IForgeMenuType.create(ManaGeneratorMenu::new));

    public static final RegistryObject<MenuType<UniversalConfigMenu>> UNIVERSAL_CONFIG =
            MENUS.register("universal_config",
                    () -> IForgeMenuType.create(UniversalConfigMenu::new));

    public static final RegistryObject<MenuType<ModularMachineMenu>> MODULAR_MACHINE_MENU =
            MENUS.register("modular_machine",
                    () -> IForgeMenuType.create(ModularMachineMenu::create));



    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
