package com.github.nalamodikk.common.datagen;


import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.datagen.recipe.ManaCraftingRecipeProvider;
import com.github.nalamodikk.common.datagen.recipe.ManaFuelRecipeProvider;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ItemLike;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

        @Override
        protected void buildRecipes(RecipeOutput recipeOutput) {
            ManaFuelRecipeProvider.generate(recipeOutput);
            ManaCraftingRecipeProvider.generate(recipeOutput);


            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SOLAR_MANA_COLLECTOR.get())
                    .pattern("GGG")  // 玻璃 玻璃 玻璃
                    .pattern("GMG")  // 玻璃 魔力粉 玻璃
                    .pattern("III")  // 鐵錠 鐵錠 鐵錠
                    .define('G', Items.GLASS)
                    .define('M', ModItems.MANA_DUST.get())
                    .define('I', Items.IRON_INGOT)
                    .unlockedBy(getHasName(ModItems.MANA_DUST.get()), has(ModItems.MANA_DUST.get()))
                    .save(recipeOutput, "solar_mana_collector");

            // 🔗 奧術導管 (批量製作)
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ARCANE_CONDUIT.get(), 4)
                    .pattern("MMM")  // 魔力粉 魔力粉 魔力粉
                    .pattern("IGI")  // 鐵錠 玻璃 鐵錠
                    .pattern("MMM")  // 魔力粉 魔力粉 魔力粉
                    .define('M', ModItems.MANA_DUST.get())
                    .define('I', Items.IRON_INGOT)
                    .define('G', Items.GLASS)
                    .unlockedBy(getHasName(ModItems.MANA_DUST.get()), has(ModItems.MANA_DUST.get()))
                    .save(recipeOutput, "arcane_conduit");


            // ===== 升級模組配方 =====

            // ⚡ 速度升級模組
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SPEED_UPGRADE.get())
                    .pattern("RMR")  // 紅石 魔力粉 紅石
                    .pattern("MGM")  // 魔力粉 金錠 魔力粉
                    .pattern("RMR")  // 紅石 魔力粉 紅石
                    .define('R', Items.REDSTONE)
                    .define('M', ModItems.MANA_DUST.get())
                    .define('G', Items.GOLD_INGOT)
                    .unlockedBy(getHasName(ModItems.MANA_DUST.get()), has(ModItems.MANA_DUST.get()))
                    .save(recipeOutput, "speed_upgrade");

            // 🔋 效率升級模組
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.EFFICIENCY_UPGRADE.get())
                    .pattern("DMD")  // 鑽石 魔力粉 鑽石
                    .pattern("MIM")  // 魔力粉 魔力錠 魔力粉
                    .pattern("DMD")  // 鑽石 魔力粉 鑽石
                    .define('D', Items.DIAMOND)
                    .define('M', ModItems.MANA_DUST.get())
                    .define('I', ModItems.MANA_INGOT.get())
                    .unlockedBy(getHasName(ModItems.MANA_INGOT.get()), has(ModItems.MANA_INGOT.get()))
                    .save(recipeOutput, "efficiency_upgrade");

            // 🔥 原魔塵 → 魔力粉 (熔爐，基礎方式)
            SimpleCookingRecipeBuilder.smelting(
                            Ingredient.of(ModItems.RAW_MANA_DUST.get()),
                            RecipeCategory.MISC,
                            ModItems.MANA_DUST.get(),
                            0.1f,   // 低經驗值
                            300     // 較慢的熔煉時間 (15秒)
                    )
                    .unlockedBy(getHasName(ModItems.RAW_MANA_DUST.get()), has(ModItems.RAW_MANA_DUST.get()))
                    .save(recipeOutput, "mana_dust_from_raw_smelting");

            // 🌪️ 原魔塵 → 魔力粉 (高爐，效率更高)
            SimpleCookingRecipeBuilder.blasting(
                            Ingredient.of(ModItems.RAW_MANA_DUST.get()),
                            RecipeCategory.MISC,
                            ModItems.MANA_DUST.get(),
                            0.2f,   // 稍高經驗值
                            150     // 更快的熔煉時間 (7.5秒)
                    )
                    .unlockedBy(getHasName(ModItems.RAW_MANA_DUST.get()), has(ModItems.RAW_MANA_DUST.get()))
                    .save(recipeOutput, "mana_dust_from_raw_blasting");


            // ===== 汙穢魔力粉獲取方式 =====

            // 🦠 主動製作汙穢魔力粉 (給玩家控制權)
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.CORRUPTED_MANA_DUST.get(), 2)
                    .requires(ModItems.MANA_DUST.get())     // 魔力粉
                    .requires(Items.ROTTEN_FLESH)           // 腐肉
                    .requires(Items.SPIDER_EYE)             // 蜘蛛眼
                    .unlockedBy(getHasName(ModItems.MANA_DUST.get()), has(ModItems.MANA_DUST.get()))
                    .save(recipeOutput, "corrupted_mana_dust_crafting");

            // 🔥 緊急魔力粉合成 (早期臨時解決方案)
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MANA_DUST.get())
                    .requires(ModItems.RAW_MANA_DUST.get(), 3) // 3個原魔塵
                    .requires(Items.COAL)                       // + 煤炭
                    .unlockedBy(getHasName(ModItems.RAW_MANA_DUST.get()), has(ModItems.RAW_MANA_DUST.get()))
                    .save(recipeOutput, "emergency_mana_dust_from_raw");

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.MANA_BLOCK.get())
                    .pattern("SSS").pattern("SSS").pattern("SSS")
                    .define('S', ModItems.MANA_INGOT.get())
                    .unlockedBy(getHasName(ModItems.MANA_INGOT.get()), has(ModItems.MANA_INGOT.get()))
                    .save(recipeOutput);

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MANA_INGOT.get())
                    .pattern("SSS").pattern("SSS").pattern("SSS")
                    .define('S', ModItems.MANA_DUST.get())
                    .unlockedBy(getHasName(ModItems.MANA_DUST.get()), has(ModItems.MANA_DUST.get()))
                    .save(recipeOutput);

            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MANA_INGOT.get(), 9)
                    .requires(ModBlocks.MANA_BLOCK.get())
                    .unlockedBy(getHasName(ModBlocks.MANA_BLOCK.get()), has(ModBlocks.MANA_BLOCK.get()))
                    .save(recipeOutput, "mana_ingot_from_block");

            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MANA_DUST.get(), 9)
                    .requires(ModItems.MANA_INGOT.get())
                    .unlockedBy(getHasName(ModItems.MANA_INGOT.get()), has(ModItems.MANA_INGOT.get()))
                    .save(recipeOutput, "mana_dust_from_ingot");


           ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.MANA_GENERATOR.get())
                    .pattern("AAA").pattern("RMR").pattern("IFI")
                    .define('I', Items.IRON_INGOT)
                    .define('R', Items.REDSTONE_BLOCK)
                    .define('M', Items.DIAMOND_BLOCK)
                    .define('F', Blocks.FURNACE)
                    .define('A', Items.AMETHYST_SHARD)
                    .unlockedBy("has_iron", has(Items.IRON_INGOT))
                    .save(recipeOutput);

            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get())
                    .pattern("GMG")  // 玻璃 魔力粉 玻璃
                    .pattern("RCR")  // 紅石 合成台 紅石
                    .pattern("IMI")  // 鐵錠 魔力粉 鐵錠
                    .define('G', Items.GLASS)
                    .define('M', ModItems.MANA_DUST.get())
                    .define('R', Items.REDSTONE)
                    .define('C', Items.CRAFTING_TABLE)
                    .define('I', Items.IRON_INGOT)
                    .unlockedBy(getHasName(ModItems.MANA_DUST.get()), has(ModItems.MANA_DUST.get()))
                    .save(recipeOutput, "mana_crafting_table_simplified");

            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.BASIC_TECH_WAND.get())
                    .pattern("RMR")  // 紅石 魔力粉 紅石
                    .pattern("CIC")  // 銅錠 鐵錠 銅錠
                    .pattern(" S ")  // 空 棍子 空
                    .define('R', Items.REDSTONE)
                    .define('M', ModItems.MANA_DUST.get())
                    .define('C', Items.COPPER_INGOT)
                    .define('I', Items.IRON_INGOT)
                    .define('S', Items.STICK)
                    .unlockedBy(getHasName(ModItems.MANA_DUST.get()), has(ModItems.MANA_DUST.get()))
                    .save(recipeOutput, "basic_tech_wand_simplified");
            // 🌟 基礎模板：Transmute
//            TransmuteRecipeBuilder.transmute(
//                            RecipeCategory.MISC,
//                            Ingredient.of(Items.SHULKER_BOX),
//                            Ingredient.of(DyeItem.byColor(DyeColor.BLUE)),
//                            ShulkerBoxBlock.getBlockByColor(DyeColor.BLUE).asItem())
//                    .group("shulker_box_dye")
//                    .unlockedBy("has_shulker_box", has(Items.SHULKER_BOX))
//                    .save(recipeOutput, MagicalIndustryMod.MOD_ID + ":blue_shulker_box_transmute");




            // 🌟 基礎模板：Stonecutting
            SingleItemRecipeBuilder.stonecutting(
                            Ingredient.of(Items.ANDESITE),
                            RecipeCategory.BUILDING_BLOCKS,
                            Items.ANDESITE_SLAB,
                            2)
                    .unlockedBy("has_andesite", has(Items.ANDESITE))
                    .save(recipeOutput, KoniavacraftMod.MOD_ID + ":andesite_slab_from_stonecutting");

            // 🌟 基礎模板：Smithing Transform
            SmithingTransformRecipeBuilder.smithing(
                            Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                            Ingredient.of(Items.DIAMOND_AXE),
                            Ingredient.of(Items.NETHERITE_INGOT),
                            RecipeCategory.TOOLS,
                            Items.NETHERITE_AXE)
                    .unlocks("has_netherite_ingot", has(Items.NETHERITE_INGOT))
                    .save(recipeOutput, KoniavacraftMod.MOD_ID + ":netherite_axe_smithing");

//            // 🌟 基礎模板：Smithing Trim
//            SmithingTrimRecipeBuilder.smithingTrim(
//                            Ingredient.of(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE),
//                            tag(ItemTags.TRIMMABLE_ARMOR),
//                            tag(ItemTags.TRIM_MATERIALS),
//                            RecipeCategory.MISC)
//                    .unlocks("has_trim_template", has(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE))
//                    .save(recipeOutput, MagicalIndustryMod.MOD_ID + ":trim_example");
//
//            // 🌟 基礎模板：Special Recipe（例：盔甲染色）
//            SpecialRecipeBuilder.special(ArmorDyeRecipe::new)
//                    .save(recipeOutput, MagicalIndustryMod.MOD_ID + ":armor_dye");
        }

        protected static void oreSmelting(RecipeOutput recipeOutput, List<ItemLike> ingredients, RecipeCategory category, ItemLike result,
                                          float xp, int time, String group) {
            oreCooking(recipeOutput, RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new, ingredients, category, result, xp, time, group, "_from_smelting");
        }

        protected static void oreBlasting(RecipeOutput recipeOutput, List<ItemLike> ingredients, RecipeCategory category, ItemLike result,
                                          float xp, int time, String group) {
            oreCooking(recipeOutput, RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new, ingredients, category, result, xp, time, group, "_from_blasting");
        }

        protected static <T extends AbstractCookingRecipe> void oreCooking(RecipeOutput recipeOutput, RecipeSerializer<T> serializer, AbstractCookingRecipe.Factory<T> factory,
                                                                           List<ItemLike> ingredients, RecipeCategory category, ItemLike result, float xp, int time, String group, String suffix) {
            for (ItemLike item : ingredients) {
                SimpleCookingRecipeBuilder.generic(Ingredient.of(item), category, result, xp, time, serializer, factory)
                        .group(group).unlockedBy(getHasName(item), has(item))
                        .save(recipeOutput, KoniavacraftMod.MOD_ID + ":" + getItemName(result) + suffix + "_" + getItemName(item));
            }
        }
    }


