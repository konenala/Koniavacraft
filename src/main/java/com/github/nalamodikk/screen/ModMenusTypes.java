package com.github.nalamodikk.screen;

import com.github.nalamodikk.MagicalIndustryMod;
import com.github.nalamodikk.screen.ManaCrafting.AdvancedManaCraftingTableMenu;
import com.github.nalamodikk.screen.ManaCrafting.ManaCraftingMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenusTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MagicalIndustryMod.MOD_ID);

    public static final RegistryObject<MenuType<ManaCraftingMenu>> MANA_CRAFTING_MENU = MENUS.register("mana_crafting", () -> IForgeMenuType.create((windowId, inv, data) -> {
        Level level = inv.player.getCommandSenderWorld(); // 获取玩家所在的世界
        return new ManaCraftingMenu(windowId, inv, new ItemStackHandler(10), ContainerLevelAccess.NULL, level);
    }));
    public static final RegistryObject<MenuType<AdvancedManaCraftingTableMenu>> ADVANCED_MANA_CRAFTING_TABLE_MENu =
            registerMenuType("advanced_mana_crafting_table_menu", AdvancedManaCraftingTableMenu::new);


    private static <T extends AbstractContainerMenu>RegistryObject<MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IForgeMenuType.create(factory));
    }
    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
