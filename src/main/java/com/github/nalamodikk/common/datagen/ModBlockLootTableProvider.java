package com.github.nalamodikk.common.datagen;

import com.github.nalamodikk.common.utils.loot.LootTableUtils;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.HashSet;
import java.util.Set;

/**
 * 🎯 模組方塊戰利品表生成器
 *
 * 📊 功能說明：
 * - 自動生成模組方塊的戰利品表 (loot tables)
 * - 支援排除有自定義掉落邏輯的方塊
 * - 處理標準掉落、礦物掉落、絲綢之觸等邏輯
 *
 * 💡 設計理念：
 * - 🔧 靈活的排除系統：避免與自定義掉落邏輯衝突
 * - 📋 統一的生成邏輯：保持戰利品表一致性
 * - ⚡ 效能友善：只處理需要生成的方塊
 *
 * 🚀 使用方法：
 * - 在 generate() 中添加標準方塊：dropSelf(block)
 * - 在構造函數中排除自定義掉落方塊：addExcludedBlock(block)
 * - 礦物方塊使用 LootTableUtils 工具類生成複雜掉落
 */
public class ModBlockLootTableProvider extends BlockLootSubProvider {

    /**
     * 🚫 排除列表 - 有自定義掉落邏輯的方塊
     *
     * 📝 說明：
     * - 這些方塊在代碼中實現了自定義的 onRemove() 邏輯
     * - 不需要 DataGen 生成標準戰利品表
     * - 避免雙重掉落或衝突問題
     */
    private static final Set<Block> EXCLUDED_BLOCKS = new HashSet<>();

    /**
     * 🔧 構造函數 - 初始化戰利品表生成器
     *
     * @param registries 註冊表提供者，用於訪問附魔等註冊內容
     */
    protected ModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);

        // 🎯 添加有自定義掉落邏輯的方塊到排除列表
        addExcludedBlock(ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get()); // 魔力合成台：保存 NBT 到物品

        // 💡 未來如果有其他自定義掉落方塊，在這裡添加：
        // addExcludedBlock(ModBlocks.ANOTHER_CUSTOM_DROP_BLOCK.get());
    }

    /**
     * 🔧 添加方塊到排除列表
     *
     * @param block 要排除的方塊
     */
    private static void addExcludedBlock(Block block) {
        EXCLUDED_BLOCKS.add(block);
    }

    /**
     * 📋 生成戰利品表 - 核心邏輯
     *
     * 🎯 處理的方塊類型：
     * - 標準方塊：直接掉落自身
     * - 礦物方塊：複雜掉落邏輯（多種物品、幸運附魔、絲綢之觸）
     */
    @Override
    protected void generate() {
        // 🏗️ 標準方塊：掉落自身
        dropSelf(ModBlocks.MANA_BLOCK.get());           // 魔力錠磚
        dropSelf(ModBlocks.MANA_GENERATOR.get());       // 魔力發電機
        dropSelf(ModBlocks.SOLAR_MANA_COLLECTOR.get()); // 太陽能魔力收集器
        dropSelf(ModBlocks.ARCANE_CONDUIT.get());       // 奧術導管
        dropSelf(ModBlocks.MANA_SOIL.get());
        dropSelf(ModBlocks.MANA_GRASS_BLOCK.get());

        dropSelf(ModBlocks.DEEP_MANA_SOIL.get());

        dropSelf(ModBlocks.ARCANE_PEDESTAL.get());
        dropSelf(ModBlocks.MANA_PYLON.get());
        dropSelf(ModBlocks.RITUAL_CORE.get());
        dropSelf(ModBlocks.RUNE_STONE_AUGMENTATION.get());
        dropSelf(ModBlocks.RUNE_STONE_CELERITY.get());
        dropSelf(ModBlocks.RUNE_STONE_EFFICIENCY.get());
        dropSelf(ModBlocks.RUNE_STONE_STABILITY.get());


        // ⛏️ 魔法礦 - 複雜掉落邏輯
        this.add(ModBlocks.MAGIC_ORE.get(), block ->
                LootTableUtils.createOreDropsWithBonusAndSilkTouch(
                        block,
                        ModItems.RAW_MANA_DUST.get(),        // 主要掉落物
                        1, 4,                                // 主掉落範圍：1-4 個
                        ModItems.CORRUPTED_MANA_DUST.get(),  // 額外掉落物
                        0.2f,                                // 額外掉落機率：20%
                        this.registries.lookupOrThrow(Registries.ENCHANTMENT)
                ));

        // ⛏️ 深板岩魔法礦 - 更高產量
        this.add(ModBlocks.DEEPSLATE_MAGIC_ORE.get(), block ->
                LootTableUtils.createOreDropsWithBonusAndSilkTouch(
                        block,
                        ModItems.RAW_MANA_DUST.get(),        // 主要掉落物
                        2, 5,                                // 主掉落範圍：2-5 個（更高產量）
                        ModItems.CORRUPTED_MANA_DUST.get(),  // 額外掉落物
                        0.2f,                                // 額外掉落機率：20%
                        this.registries.lookupOrThrow(Registries.ENCHANTMENT)
                ));
    }

    /**
     * 🔧 創建多重礦物掉落邏輯（工具方法）
     *
     * @param pBlock 目標方塊
     * @param item 掉落的物品
     * @param minDrops 最小掉落數量
     * @param maxDrops 最大掉落數量
     * @return 戰利品表建構器
     *
     * 💡 功能：
     * - 支援絲綢之觸：掉落方塊本身
     * - 支援幸運附魔：增加掉落數量
     * - 支援爆炸衰減：爆炸時減少掉落
     */
    protected LootTable.Builder createMultipleOreDrops(Block pBlock, Item item, float minDrops, float maxDrops) {
        HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchDispatchTable(pBlock,
                this.applyExplosionDecay(pBlock, LootItem.lootTableItem(item)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(minDrops, maxDrops)))
                        .apply(ApplyBonusCount.addOreBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))));
    }

    /**
     * 🔍 獲取已知方塊列表 - 用於 DataGen 驗證
     *
     * @return 需要生成戰利品表的方塊列表
     *
     * 📊 邏輯：
     * - 從模組的所有註冊方塊開始
     * - 過濾掉排除列表中的方塊（有自定義掉落邏輯）
     * - 返回需要 DataGen 處理的方塊
     *
     * 💡 這個方法的作用：
     * - DataGen 會檢查這個列表中的每個方塊
     * - 確保 generate() 中為每個方塊都定義了戰利品表
     * - 如果有遺漏，會報錯提醒開發者
     */
    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream()
                .map(Holder::value)                              // 取得實際的方塊物件
                .filter(block -> !EXCLUDED_BLOCKS.contains(block)) // 🎯 排除有自定義掉落的方塊
                .toList();
    }
}