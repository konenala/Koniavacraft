package com.github.nalamodikk.common.datagen;


import com.github.nalamodikk.MagicalIndustryMod;
import com.github.nalamodikk.common.datagen.recipe.ManaCraftingRecipeProvider;
import com.github.nalamodikk.common.datagen.recipe.ManaFuelRecipeProvider;
import com.github.nalamodikk.common.register.ModBlocks;
import com.github.nalamodikk.common.register.ModItems;
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
                    .pattern("IDI").pattern("IMI").pattern("ICI")
                    .define('I', Items.IRON_INGOT)
                    .define('D', Items.DIAMOND)
                    .define('M', ModItems.BASIC_TECH_WAND.get())
                    .define('C', Items.CRAFTING_TABLE)
                    .unlockedBy("has_iron", has(Items.IRON_INGOT))
                    .save(recipeOutput);

            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.BASIC_TECH_WAND.get())
                    .pattern("RAR").pattern("CIC").pattern(" D ")
                    .define('I', ModItems.CORRUPTED_MANA_DUST.get())
                    .define('R', Items.REDSTONE_BLOCK)
                    .define('D', Blocks.DIAMOND_BLOCK)
                    .define('C', Items.COPPER_INGOT)
                    .define('A', Items.AMETHYST_SHARD)
                    .unlockedBy("has_iron", has(Items.IRON_INGOT))
                    .save(recipeOutput);

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
                    .save(recipeOutput, MagicalIndustryMod.MOD_ID + ":andesite_slab_from_stonecutting");

            // 🌟 基礎模板：Smithing Transform
            SmithingTransformRecipeBuilder.smithing(
                            Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                            Ingredient.of(Items.DIAMOND_AXE),
                            Ingredient.of(Items.NETHERITE_INGOT),
                            RecipeCategory.TOOLS,
                            Items.NETHERITE_AXE)
                    .unlocks("has_netherite_ingot", has(Items.NETHERITE_INGOT))
                    .save(recipeOutput, MagicalIndustryMod.MOD_ID + ":netherite_axe_smithing");

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
                        .save(recipeOutput, MagicalIndustryMod.MOD_ID + ":" + getItemName(result) + suffix + "_" + getItemName(item));
            }
        }
    }


