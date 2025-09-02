package com.github.nalamodikk.common.datagen;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.datagen.recipe.MaterialProcessingRecipeProvider;
import com.github.nalamodikk.common.datagen.recipe.material.ManaCraftingRecipeProvider;
import com.github.nalamodikk.common.datagen.recipe.material.ManaFuelRecipeProvider;
import com.github.nalamodikk.common.datagen.recipe.material.ManaInfuserRecipeProvider;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }


    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        // === 🔥 委派給專門的提供者 ===
        MaterialProcessingRecipeProvider.generate(recipeOutput);  // 處理所有材料加工
        ManaFuelRecipeProvider.generate(recipeOutput);
        ManaCraftingRecipeProvider.generate(recipeOutput);
        ManaInfuserRecipeProvider.generate(recipeOutput);

        // === 📋 剩餘的主要配方類別 ===
        generateMachineRecipes(recipeOutput);
        generateUpgradeRecipes(recipeOutput);
        generateToolRecipes(recipeOutput);
        generateStorageRecipes(recipeOutput);
        generateExperimentalRecipes(recipeOutput);
        normalBlock(recipeOutput);
    }


    // === 🧪 材料配方 ===
    private void normalBlock(RecipeOutput output) {
        // 🔆 魔力土壤
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.MANA_SOIL.get(), 4)
                .define('D', Blocks.DIRT)
                .define('M', ModItems.MANA_DUST.get())
                .pattern("DD ")
                .pattern("DM ")
                .pattern("   ")
                .unlockedBy("has_mana_dust", has(ModItems.MANA_DUST.get()))
                .save(output);
    }

    // === 🏭 機器配方 ===
    private void generateMachineRecipes(RecipeOutput output) {
        // 🔆 太陽能魔力收集器
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SOLAR_MANA_COLLECTOR.get())
                .pattern("GGG")
                .pattern("GMG")
                .pattern("III")
                .define('G', Items.GLASS)
                .define('M', ModItems.MANA_DUST.get())
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_mana_dust", has(ModItems.MANA_DUST.get()))
                .save(output, "solar_mana_collector");

        // 🔗 奧術導管 (批量製作)
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ARCANE_CONDUIT.get(), 4)
                .pattern("MMM")
                .pattern("IGI")
                .pattern("MMM")
                .define('M', ModItems.MANA_DUST.get())
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .unlockedBy("has_mana_dust", has(ModItems.MANA_DUST.get()))
                .save(output, "arcane_conduit");

        // 🔥 魔力發電機
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.MANA_GENERATOR.get())
                .pattern("AAA")
                .pattern("RMR")
                .pattern("IFI")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE_BLOCK)
                .define('M', Items.DIAMOND_BLOCK)
                .define('F', Blocks.FURNACE)
                .define('A', Items.AMETHYST_SHARD)
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(output, "mana_generator");

        // 🧪 魔力合成台
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get())
                .pattern("GMG")
                .pattern("RCR")
                .pattern("IMI")
                .define('G', Items.GLASS)
                .define('M', ModItems.MANA_DUST.get())
                .define('R', Items.REDSTONE)
                .define('C', Items.CRAFTING_TABLE)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_mana_dust", has(ModItems.MANA_DUST.get()))
                .save(output, "mana_crafting_table");
    }

    // === ⚡ 升級模組配方 ===
    private void generateUpgradeRecipes(RecipeOutput output) {
        // ⚡ 速度升級模組
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SPEED_UPGRADE.get())
                .pattern("RMR")
                .pattern("MGM")
                .pattern("RMR")
                .define('R', Items.REDSTONE)
                .define('M', ModItems.MANA_DUST.get())
                .define('G', Items.GOLD_INGOT)
                .unlockedBy("has_mana_dust", has(ModItems.MANA_DUST.get()))
                .save(output, "speed_upgrade");

        // 🔋 效率升級模組
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.EFFICIENCY_UPGRADE.get())
                .pattern("DMD")
                .pattern("MIM")
                .pattern("DMD")
                .define('D', Items.DIAMOND)
                .define('M', ModItems.MANA_DUST.get())
                .define('I', ModItems.MANA_INGOT.get())
                .unlockedBy("has_mana_ingot", has(ModItems.MANA_INGOT.get()))
                .save(output, "efficiency_upgrade");
    }


    // === 🔧 工具配方 ===
    private void generateToolRecipes(RecipeOutput output) {
        // 🛠️ 基礎科技魔杖
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.BASIC_TECH_WAND.get())
                .pattern("RMR")
                .pattern("CIC")
                .pattern(" S ")
                .define('R', Items.REDSTONE)
                .define('M', ModItems.MANA_DUST.get())
                .define('C', Items.COPPER_INGOT)
                .define('I', Items.IRON_INGOT)
                .define('S', Items.STICK)
                .unlockedBy("has_mana_dust", has(ModItems.MANA_DUST.get()))
                .save(output, "basic_tech_wand");
    }

    // === 📦 儲存和轉換配方 ===
    private void generateStorageRecipes(RecipeOutput output) {
        // 魔力粉 ↔ 魔力錠
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MANA_INGOT.get())
                .pattern("SSS")
                .pattern("SSS")
                .pattern("SSS")
                .define('S', ModItems.MANA_DUST.get())
                .unlockedBy("has_mana_dust", has(ModItems.MANA_DUST.get()))
                .save(output, "mana_ingot_from_dust");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MANA_DUST.get(), 9)
                .requires(ModItems.MANA_INGOT.get())
                .unlockedBy("has_mana_ingot", has(ModItems.MANA_INGOT.get()))
                .save(output, "mana_dust_from_ingot");

        // 魔力錠 ↔ 魔力方塊
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.MANA_BLOCK.get())
                .pattern("SSS")
                .pattern("SSS")
                .pattern("SSS")
                .define('S', ModItems.MANA_INGOT.get())
                .unlockedBy("has_mana_ingot", has(ModItems.MANA_INGOT.get()))
                .save(output, "mana_block_from_ingots");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MANA_INGOT.get(), 9)
                .requires(ModBlocks.MANA_BLOCK.get())
                .unlockedBy("has_mana_block", has(ModBlocks.MANA_BLOCK.get()))
                .save(output, "mana_ingot_from_block");
    }


    // === 🧪 實驗性/示例配方 ===
    private void generateExperimentalRecipes(RecipeOutput output) {
        // 🌟 切石機示例
        SingleItemRecipeBuilder.stonecutting(
                        Ingredient.of(Items.ANDESITE),
                        RecipeCategory.BUILDING_BLOCKS,
                        Items.ANDESITE_SLAB, 2)
                .unlockedBy("has_andesite", has(Items.ANDESITE))
                .save(output, KoniavacraftMod.MOD_ID + ":andesite_slab_stonecutting");

        // 🌟 鍛造台示例
        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(Items.DIAMOND_AXE),
                        Ingredient.of(Items.NETHERITE_INGOT),
                        RecipeCategory.TOOLS,
                        Items.NETHERITE_AXE)
                .unlocks("has_netherite_ingot", has(Items.NETHERITE_INGOT))
                .save(output, KoniavacraftMod.MOD_ID + ":netherite_axe_smithing_example");
    }

    // === 🛠️ 工具方法 ===
    protected static void oreSmelting(RecipeOutput recipeOutput, List<ItemLike> ingredients,
                                      RecipeCategory category, ItemLike result, float xp, int time, String group) {
        oreCooking(recipeOutput, RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new,
                ingredients, category, result, xp, time, group, "_from_smelting");
    }

    protected static void oreBlasting(RecipeOutput recipeOutput, List<ItemLike> ingredients,
                                      RecipeCategory category, ItemLike result, float xp, int time, String group) {
        oreCooking(recipeOutput, RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new,
                ingredients, category, result, xp, time, group, "_from_blasting");
    }

    protected static <T extends AbstractCookingRecipe> void oreCooking(RecipeOutput recipeOutput,
                                                                       RecipeSerializer<T> serializer,
                                                                       AbstractCookingRecipe.Factory<T> factory,
                                                                       List<ItemLike> ingredients,
                                                                       RecipeCategory category,
                                                                       ItemLike result,
                                                                       float xp, int time,
                                                                       String group, String suffix) {
        for (ItemLike item : ingredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(item), category, result, xp, time, serializer, factory)
                    .group(group)
                    .unlockedBy(getHasName(item), has(item))
                    .save(recipeOutput, KoniavacraftMod.MOD_ID + ":" + getItemName(result) + suffix + "_" + getItemName(item));
        }
    }
}
