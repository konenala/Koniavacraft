package com.github.nalamodikk.common.register;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.screen.ManaGenerator.ManaGeneratorMenu;
import com.github.nalamodikk.common.screen.UpgradeMenu;
import com.github.nalamodikk.common.screen.manacollector.SolarManaCollectorMenu;
import com.github.nalamodikk.common.screen.manacrafting.ManaCraftingMenu;
import com.github.nalamodikk.common.screen.tool.UniversalConfigMenu;
import com.github.nalamodikk.common.upgrade.api.IUpgradeableMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
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

    public static final RegistryObject<MenuType<UniversalConfigMenu>> UNIVERSAL_CONFIG_MENU =
            MENUS.register("universal_config",
                    () -> IForgeMenuType.create(UniversalConfigMenu::new));

    public static final RegistryObject<MenuType<SolarManaCollectorMenu>> SOLAR_MANA_COLLECTOR_MENU =
            MENUS.register("solar_mana_collector",
                    () -> IForgeMenuType.create(SolarManaCollectorMenu::new));

    public static final RegistryObject<MenuType<UpgradeMenu>> UPGRADE_MENU =
            MENUS.register("upgrade_menu", () ->
                    IForgeMenuType.create((id, playerInv, extraData) -> {
                        BlockPos pos = extraData.readBlockPos();
                        Level level = playerInv.player.level();

                        BlockEntity be = level.getBlockEntity(pos);
                        if (be instanceof IUpgradeableMachine machine) {
                            return new UpgradeMenu(id, playerInv, machine.getUpgradeInventory(), machine);
                        }

                        throw new IllegalStateException("UpgradeMenu: No IUpgradeableMachine at " + pos);
                    }));


    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
