package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.UpgradeItem;
import com.github.nalamodikk.common.item.debug.ManaDebugToolItem;
import com.github.nalamodikk.common.item.tool.BasicTechWandItem;
import com.github.nalamodikk.common.item.ritual.*;
import com.github.nalamodikk.common.utils.upgrade.UpgradeType;
import com.github.nalamodikk.experimental.particle.item.DebugParticleItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KoniavacraftMod.MOD_ID);
    /***
     * 素材類
     */
    public static final DeferredItem<Item>  MANA_DUST = ITEMS.register("mana_dust",() ->  new Item(new Item.Properties()));
    public static final DeferredItem<Item>  CORRUPTED_MANA_DUST = ITEMS.register("corrupted_mana_dust",() ->  new Item(new Item.Properties()));
    public static final DeferredItem<Item>  MANA_INGOT = ITEMS.register("mana_ingot",() ->  new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_MANA_DUST = ITEMS.register("raw_mana_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CONDENSED_MANA_DUST = ITEMS.register("condensed_mana_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MANA_CRYSTAL_FRAGMENT = ITEMS.register("mana_crystal_fragment", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> REFINED_MANA_DUST = ITEMS.register("refined_mana_dust", () -> new Item(new Item.Properties()));
    /***
     * 工具
     */
    public static final DeferredItem<Item> DEBUG_PARTICLE_ITEM = ITEMS.register("debug_particle_item", () -> new DebugParticleItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item>  MANA_DEBUG_TOOL = ITEMS.register("mana_debug_tool",() ->  new ManaDebugToolItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item>  BASIC_TECH_WAND = ITEMS.register("basic_tech_wand",() ->  new BasicTechWandItem(new Item.Properties().stacksTo(1)));

    /**
     * 其他
     */
    public static final DeferredItem<Item> SPEED_UPGRADE = ITEMS.register("speed_upgrade", () -> new UpgradeItem(UpgradeType.SPEED, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> EFFICIENCY_UPGRADE = ITEMS.register("efficiency_upgrade", () -> new UpgradeItem(UpgradeType.EFFICIENCY, new Item.Properties().stacksTo(1)));

    // === 🔮 儀式系統物品 (Ritual System Items) ===
    public static final DeferredItem<Item> RITUALIST_CHALK = ITEMS.register("ritualist_chalk", 
            () -> new RitualistChalkItem(new Item.Properties().stacksTo(1)));
    
    public static final DeferredItem<Item> RESONANT_CRYSTAL = ITEMS.register("resonant_crystal", 
            () -> new ResonantCrystalItem(new Item.Properties().stacksTo(16)));
    
    public static final DeferredItem<Item> VOID_PEARL = ITEMS.register("void_pearl", 
            () -> new VoidPearlItem(new Item.Properties().stacksTo(4))); // 限制堆疊數量以示其稀有性


    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
