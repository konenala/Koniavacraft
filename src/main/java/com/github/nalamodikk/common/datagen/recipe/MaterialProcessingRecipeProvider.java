package com.github.nalamodikk.common.datagen.recipe;

import com.github.nalamodikk.register.ModItems;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 🧪 材料加工配方工具類
 *
 * 職責：
 * - 原魔塵的各種處理方式
 * - 魔力粉的精煉和升級
 * - 汙穢魔力粉的製作和處理
 * - 未來的材料轉化配方
 *
 * 注意：這是工具類，不繼承 RecipeProvider，避免註冊衝突
 */
public class MaterialProcessingRecipeProvider {

    // 🔧 工具方法：創建 "has item" 條件
    private static Criterion<InventoryChangeTrigger.TriggerInstance> hasItem(net.minecraft.world.level.ItemLike item) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }

    // 🎯 主要生成方法
    public static void generate(RecipeOutput output) {
        generateRawManaProcessing(output);
        generateRefinedManaProcessing(output);
        generateCorruptedManaProcessing(output);
        generateEmergencyRecipes(output);
    }

    /**
     * 🔥 原魔塵處理配方
     */
    private static void generateRawManaProcessing(RecipeOutput output) {
        // 🔥 原魔塵 → 魔力粉 (熔爐)
        SimpleCookingRecipeBuilder.smelting(
                        Ingredient.of(ModItems.RAW_MANA_DUST.get()),
                        RecipeCategory.MISC,
                        ModItems.MANA_DUST.get(),
                        0.1f, 300)
                .unlockedBy("has_raw_mana_dust", hasItem(ModItems.RAW_MANA_DUST.get()))
                .save(output, "mana_dust_from_raw_smelting");

        // 🌪️ 原魔塵 → 魔力粉 (高爐)
        SimpleCookingRecipeBuilder.blasting(
                        Ingredient.of(ModItems.RAW_MANA_DUST.get()),
                        RecipeCategory.MISC,
                        ModItems.MANA_DUST.get(),
                        0.2f, 150)
                .unlockedBy("has_raw_mana_dust", hasItem(ModItems.RAW_MANA_DUST.get()))
                .save(output, "mana_dust_from_raw_blasting");
    }

    /**
     * ✨ 精煉魔力粉處理配方 (你的新材料！)
     */
    private static void generateRefinedManaProcessing(RecipeOutput output) {
        // ✨ 魔力粉 → 精煉魔力粉 (熔爐)
        SimpleCookingRecipeBuilder.smelting(
                        Ingredient.of(ModItems.MANA_DUST.get()),
                        RecipeCategory.MISC,
                        ModItems.REFINED_MANA_DUST.get(),
                        0.2f, 200)
                .unlockedBy("has_mana_dust", hasItem(ModItems.MANA_DUST.get()))
                .save(output, "refined_mana_dust_from_smelting");

        // ⚡ 魔力粉 → 精煉魔力粉 (高爐)
        SimpleCookingRecipeBuilder.blasting(
                        Ingredient.of(ModItems.MANA_DUST.get()),
                        RecipeCategory.MISC,
                        ModItems.REFINED_MANA_DUST.get(),
                        0.2f, 100)
                .unlockedBy("has_mana_dust", hasItem(ModItems.MANA_DUST.get()))
                .save(output, "refined_mana_dust_from_blasting");
    }

    /**
     * 🦠 汙穢魔力粉處理配方
     */
    private static void generateCorruptedManaProcessing(RecipeOutput output) {
        // 🦠 主動製作汙穢魔力粉
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.CORRUPTED_MANA_DUST.get(), 2)
                .requires(ModItems.MANA_DUST.get())
                .requires(Items.ROTTEN_FLESH)
                .requires(Items.SPIDER_EYE)
                .unlockedBy("has_mana_dust", hasItem(ModItems.MANA_DUST.get()))
                .save(output, "corrupted_mana_dust_crafting");
    }

    /**
     * 🔥 緊急配方
     */
    private static void generateEmergencyRecipes(RecipeOutput output) {
        // 🔥 緊急魔力粉合成
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MANA_DUST.get())
                .requires(ModItems.RAW_MANA_DUST.get(), 3)
                .requires(Items.COAL)
                .unlockedBy("has_raw_mana_dust", hasItem(ModItems.RAW_MANA_DUST.get()))
                .save(output, "emergency_mana_dust_from_raw");
    }
}