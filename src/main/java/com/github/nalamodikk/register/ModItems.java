package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.UpgradeItem;
import com.github.nalamodikk.common.item.debug.ManaDebugToolItem;
import com.github.nalamodikk.common.item.ritual.ResonantCrystalItem;
import com.github.nalamodikk.common.item.tool.BasicTechWandItem;
import com.github.nalamodikk.common.item.ritual.RitualistChalkItem;
import com.github.nalamodikk.common.block.blockentity.ritual.ritualblock.ChalkGlyphBlock;
import com.github.nalamodikk.common.utils.upgrade.UpgradeType;
import com.github.nalamodikk.experimental.particle.item.DebugParticleItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KoniavacraftMod.MOD_ID);
    /**
     * 素材類
     */
    public static final DeferredItem<Item> MANA_DUST = ITEMS.register("mana_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CORRUPTED_MANA_DUST = ITEMS.register("corrupted_mana_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MANA_INGOT = ITEMS.register("mana_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_MANA_DUST = ITEMS.register("raw_mana_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CONDENSED_MANA_DUST = ITEMS.register("condensed_mana_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MANA_CRYSTAL_FRAGMENT = ITEMS.register("mana_crystal_fragment", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> REFINED_MANA_DUST = ITEMS.register("refined_mana_dust", () -> new Item(new Item.Properties()));

    /**
     * 符文魔法系統
     */
    public static final DeferredItem<Item> BLANK_RUNE = ITEMS.register("blank_rune", () -> new Item(new Item.Properties()));

    /**
     * 儀式系統
     */
    public static final DeferredItem<Item> RESONANT_CRYSTAL = ITEMS.register("resonant_crystal", () -> new ResonantCrystalItem(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> VOID_PEARL = ITEMS.register("void_pearl", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> RITUALIST_CHALK = ITEMS.register("ritualist_chalk", () -> new RitualistChalkItem(new Item.Properties(), ChalkGlyphBlock.ChalkColor.WHITE));

    /**
     * 魔法材料
     */
    public static final DeferredItem<Item> ARCANE_ALLOY_INGOT = ITEMS.register("arcane_alloy_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LIVINGWOOD_LOG = ITEMS.register("livingwood_log", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MANA_LENS = ITEMS.register("mana_lens", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> STARMETAL_INGOT = ITEMS.register("starmetal_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> HEART_OF_THE_WORLD = ITEMS.register("heart_of_the_world", () -> new Item(new Item.Properties().stacksTo(1)));

    /**
     * 工具
     */
    public static final DeferredItem<Item> DEBUG_PARTICLE_ITEM = ITEMS.register("debug_particle_item", () -> new DebugParticleItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> MANA_DEBUG_TOOL = ITEMS.register("mana_debug_tool", () -> new ManaDebugToolItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BASIC_TECH_WAND = ITEMS.register("basic_tech_wand", () -> new BasicTechWandItem(new Item.Properties().stacksTo(1)));

    /**
     * 升級物品
     */
    public static final DeferredItem<Item> SPEED_UPGRADE = ITEMS.register("speed_upgrade", () -> new UpgradeItem(UpgradeType.SPEED, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> EFFICIENCY_UPGRADE = ITEMS.register("efficiency_upgrade", () -> new UpgradeItem(UpgradeType.EFFICIENCY, new Item.Properties().stacksTo(1)));

    /**
     * 魔力發電機升級物品
     */
    public static final DeferredItem<Item> ACCELERATED_PROCESSING_UPGRADE = ITEMS.register("accelerated_processing_upgrade", () -> new UpgradeItem(UpgradeType.ACCELERATED_PROCESSING, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> EXPANDED_FUEL_CHAMBER_UPGRADE = ITEMS.register("expanded_fuel_chamber_upgrade", () -> new UpgradeItem(UpgradeType.EXPANDED_FUEL_CHAMBER, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> CATALYTIC_CONVERTER_UPGRADE = ITEMS.register("catalytic_converter_upgrade", () -> new UpgradeItem(UpgradeType.CATALYTIC_CONVERTER, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> DIAGNOSTIC_DISPLAY_UPGRADE = ITEMS.register("diagnostic_display_upgrade", () -> new UpgradeItem(UpgradeType.DIAGNOSTIC_DISPLAY, new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
