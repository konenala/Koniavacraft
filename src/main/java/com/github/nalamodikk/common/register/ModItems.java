package com.github.nalamodikk.common.register;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.blocks.blockitem.ManaGeneratorBlockItem;
import com.github.nalamodikk.common.ComponentSystem.item.ModuleItem;
import com.github.nalamodikk.common.item.debug.ManaDebugToolItem;
import com.github.nalamodikk.common.item.tool.BasicTechWandItem;
import com.github.nalamodikk.common.item.upgrade.UpgradeItem;
import com.github.nalamodikk.common.upgrade.UpgradeType;
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


    public static final RegistryObject<Item> SOLAR_MANA_UPGRADE = ITEMS.register("solar_mana_upgrade",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SPEED_UPGRADE = ITEMS.register("speed_upgrade",
            () -> new UpgradeItem(UpgradeType.SPEED, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> EFFICIENCY_UPGRADE = ITEMS.register("efficiency_upgrade",
            () -> new UpgradeItem(UpgradeType.EFFICIENCY, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> CORRUPTED_MANA_DUST = ITEMS.register("corrupted_mana_dust",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MANA_DEBUG_TOOL = ITEMS.register("mana_debug_tool",
            () -> new ManaDebugToolItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BASIC_TECH_WAND = ITEMS.register("basic_tech_wand",
            () -> new BasicTechWandItem(new Item.Properties().stacksTo(1)));

    // 模塊零件
    public static final RegistryObject<Item> MODULE_ITEM = ITEMS.register("module_item",
            () -> new ModuleItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MANA_GENERATOR_BLOCK_ITEM = ITEMS.register("mana_generator",
            () -> new ManaGeneratorBlockItem(ModBlocks.MANA_GENERATOR.get(), new Item.Properties()));

    public static void register(IEventBus eventBus) {
      ITEMS.register(eventBus);
  }

}
