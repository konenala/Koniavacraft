package com.github.nalamodikk.item;

import com.github.nalamodikk.MagicalIndustryMod;
import com.github.nalamodikk.item.debug.ManaDebugToolItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MagicalIndustryMod.MOD_ID);

    public static final RegistryObject<Item> MANA_DUST = ITEMS.register("mana_dust",
        () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MANA_INGOT = ITEMS.register("mana_ingot",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CORRUPTED_MANA_DUST = ITEMS.register("corrupted_mana_dust",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MANA_DEBUG_TOOL = ITEMS.register("mana_debug_tool",
            () -> new ManaDebugToolItem(new Item.Properties().stacksTo(1)));




    public static void register(IEventBus eventBus) {
      ITEMS.register(eventBus);
  }

}
