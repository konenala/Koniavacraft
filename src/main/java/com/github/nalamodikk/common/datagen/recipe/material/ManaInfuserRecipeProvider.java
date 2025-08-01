package com.github.nalamodikk.common.datagen.recipe.material;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.mana_infuser.ManaInfuserRecipe;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 🔮 魔力注入機配方數據生成器
 *
 * 功能：
 * - 生成基礎材料注入配方
 * - 生成武器裝備強化配方
 * - 生成食物強化配方
 * - 生成特殊物品注入配方
 */
public class ManaInfuserRecipeProvider {

    /**
     * 🎯 生成所有魔力注入配方
     */
    public static void generate(RecipeOutput output) {
        // === 🧪 基礎材料注入 ===
        generateBasicMaterialRecipes(output);

        // === ⚔️ 武器裝備強化 ===

        // === 🍞 食物強化 ===
        generateFoodEnhancementRecipes(output);

        // === 💎 特殊物品注入 ===
        generateSpecialItemRecipes(output);

        // === 📚 附魔相關 ===
        generateEnchantmentRecipes(output);
    }

    /**
     * 🧪 基礎材料注入配方
     */
    private static void generateBasicMaterialRecipes(RecipeOutput output) {
        // 鐵錠 → 魔力錠
        createManaInfuserRecipe(output,
                "iron_to_mana_ingot",
                Ingredient.of(Items.IRON_INGOT),
                new ItemStack(ModItems.MANA_INGOT.get()),
                5000,  // 魔力消耗
                40,  // 注入時間 (2秒)
                1    // 輸入數量
        );

        // 魔力粉 → 濃縮魔力粉
        createManaInfuserRecipe(output,
                "mana_dust_to_condensed",
                Ingredient.of(ModItems.MANA_DUST.get()),
                new ItemStack(ModItems.CONDENSED_MANA_DUST.get()),
                2500,  // 魔力消耗
                30,  // 注入時間
                2    // 需要2個魔力粉
        );

        // 濃縮魔力粉 → 精煉魔力粉
        createManaInfuserRecipe(output,
                "condensed_to_refined_mana_dust",
                Ingredient.of(ModItems.CONDENSED_MANA_DUST.get()),
                new ItemStack(ModItems.REFINED_MANA_DUST.get(),2),
                7500,  // 魔力消耗
                60,  // 注入時間 (3秒)
                1
        );


    }

    /**
     * ⚔️ 武器裝備強化配方
     */


    /**
     * 🍞 食物強化配方
     */
    private static void generateFoodEnhancementRecipes(RecipeOutput output) {
        // 麵包 → 魔力麵包 (更高飽食度)


        // 蘋果 → 金蘋果
        createManaInfuserRecipe(output,
                "apple_to_golden_apple",
                Ingredient.of(Items.APPLE),
                new ItemStack(Items.GOLDEN_APPLE),
                8000,  // 魔力消耗
                60,  // 注入時間
                1
        );

        // 胡蘿蔔 → 金胡蘿蔔
        createManaInfuserRecipe(output,
                "carrot_to_golden_carrot",
                Ingredient.of(Items.CARROT),
                new ItemStack(Items.GOLDEN_CARROT),
                6000,  // 魔力消耗
                50,  // 注入時間
                1
        );
    }

    /**
     * 💎 特殊物品注入配方
     */
    private static void generateSpecialItemRecipes(RecipeOutput output) {
        // 玻璃 → 強化玻璃 (更多數量)

        // 石頭 → 石磚
        createManaInfuserRecipe(output,
                "stone_to_stone_bricks",
                Ingredient.of(Items.STONE),
                new ItemStack(Items.STONE_BRICKS),
                2000,  // 魔力消耗
                25,  // 注入時間
                1
        );

        // 沙子 → 玻璃 (無需熔爐)
        createManaInfuserRecipe(output,
                "sand_to_glass",
                Ingredient.of(Items.SAND),
                new ItemStack(Items.GLASS),
                1500,  // 魔力消耗
                30,  // 注入時間
                1
        );

        // 圓石 → 石頭 (無需熔爐)
        createManaInfuserRecipe(output,
                "cobblestone_to_stone",
                Ingredient.of(Items.COBBLESTONE),
                new ItemStack(Items.STONE),
                1000,  // 魔力消耗
                20,  // 注入時間
                1
        );
    }

    /**
     * 📚 附魔相關配方
     */
    private static void generateEnchantmentRecipes(RecipeOutput output) {
        // 書 → 附魔書 (隨機附魔)
        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
        // 這裡可以添加特定的附魔效果

        createManaInfuserRecipe(output,
                "book_to_enchanted_book",
                Ingredient.of(Items.BOOK),
                enchantedBook,
                120000, // 魔力消耗
                80,  // 注入時間
                1
        );

        // 經驗瓶 → 更多經驗瓶
        createManaInfuserRecipe(output,
                "experience_bottle_multiplication",
                Ingredient.of(Items.EXPERIENCE_BOTTLE),
                new ItemStack(Items.EXPERIENCE_BOTTLE, 2),
                6000,  // 魔力消耗
                45,  // 注入時間
                1
        );
    }

    /**
     * 🔧 創建魔力注入配方的輔助方法
     */
    private static void createManaInfuserRecipe(RecipeOutput output,
                                                String name,
                                                Ingredient input,
                                                ItemStack result,
                                                int manaCost,
                                                int infusionTime,
                                                int inputCount) {

        // 創建配方
        ManaInfuserRecipe recipe = new ManaInfuserRecipe(
                input, result, manaCost, infusionTime, inputCount
        );

        // 創建資源位置
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(
                KoniavacraftMod.MOD_ID,
                "mana_infuser/" + name
        );

        // 保存配方 (暫時不包含advancement，可以後續添加)
        output.accept(recipeId, recipe, null);
    }

    /**
     * 🎯 創建帶advancement的魔力注入配方
     */
    private static void createManaInfuserRecipeWithAdvancement(RecipeOutput output,
                                                               String name,
                                                               Ingredient input,
                                                               ItemStack result,
                                                               int manaCost,
                                                               int infusionTime,
                                                               int inputCount,
                                                               String criterionName,
                                                               Ingredient criterionItem) {

        // 創建配方
        ManaInfuserRecipe recipe = new ManaInfuserRecipe(
                input, result, manaCost, infusionTime, inputCount
        );

        // 創建資源位置
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(
                KoniavacraftMod.MOD_ID,
                "mana_infuser/" + name
        );

        // 創建advancement
        var advancement = output.advancement()
                .addCriterion("has_the_recipe",
                        net.minecraft.advancements.critereon.RecipeUnlockedTrigger.unlocked(recipeId))
                .addCriterion(criterionName,
                        net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance
                                .hasItems(criterionItem.getItems()[0].getItem()))
                .rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(recipeId))
                .requirements(net.minecraft.advancements.AdvancementRequirements.Strategy.OR);

        // 保存配方
        output.accept(recipeId, recipe, advancement.build(recipeId.withPrefix("recipes/")));
    }
}