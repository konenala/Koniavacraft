package com.github.nalamodikk.common.datagen.recipe.ritual;

import com.github.nalamodikk.common.block.blockentity.ritual.ritualblockentity.RitualRecipe;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 儀式配方數據生成器 - 根據GDD生成所有儀式配方
 */
public class RitualRecipeProvider {

    public static void generate(RecipeOutput output) {
        generateTier1Rituals(output);
        generateTier2Rituals(output);
        generateTier3Rituals(output);
        generateTier4Rituals(output);
    }

    /**
     * 生成初級儀式配方 (Tier 1)
     */
    private static void generateTier1Rituals(RecipeOutput output) {
        // 奧術合金的誕生
        RitualRecipeBuilder.create("arcane_alloy_birth")
            .name("奧術合金的誕生")
            .tier(RitualRecipe.RitualTier.BASIC)
            .ingredient(Ingredient.of(Items.IRON_INGOT), 4)
            .ingredient(Ingredient.of(Items.LAPIS_LAZULI), 4)
            .result(ModItems.MANA_INGOT.get(), 4) // 使用現有的魔力錠作為奧術合金
            .manaCost(5000)
            .ritualTime(200) // 10秒
            .structureRequirement("pedestal.total", 4)
            .save(output);

        // 活化木材
        RitualRecipeBuilder.create("livingwood_creation")
            .name("活化木材")
            .tier(RitualRecipe.RitualTier.BASIC)
            .ingredient(Ingredient.of(Items.OAK_LOG, Items.BIRCH_LOG, Items.SPRUCE_LOG), 16)
            .ingredient(Ingredient.of(Items.EMERALD), 2)
            .result(Items.OAK_LOG, 16) // 暫時使用橡木代替活化木材
            .manaCost(2000)
            .ritualTime(160) // 8秒
            .structureRequirement("pedestal.total", 2)
            .save(output);
    }

    /**
     * 生成中級儀式配方 (Tier 2)
     */
    private static void generateTier2Rituals(RecipeOutput output) {
        // 魔力透鏡的精煉
        RitualRecipeBuilder.create("mana_lens_refinement")
            .name("魔力透鏡的精煉")
            .tier(RitualRecipe.RitualTier.INTERMEDIATE)
            .ingredient(Ingredient.of(ModItems.MANA_INGOT.get()), 4) // 奧術合金錠
            .ingredient(Ingredient.of(Items.AMETHYST_SHARD), 4)
            .ingredient(Ingredient.of(Items.GLASS), 1)
            .result(Items.SPYGLASS, 1) // 暫時使用望遠鏡代替魔力透鏡
            .manaCost(25000)
            .ritualTime(400) // 20秒
            .structureRequirement("pedestal.total", 4)
            .structureRequirement("pylon.total", 1)
            .save(output);

        // 效率符文的刻印
        RitualRecipeBuilder.create("efficiency_rune_inscription")
            .name("效率符文的刻印")
            .tier(RitualRecipe.RitualTier.INTERMEDIATE)
            .ingredient(Ingredient.of(Items.OBSIDIAN), 1)
            .ingredient(Ingredient.of(ModItems.MANA_INGOT.get()), 4)
            .ingredient(Ingredient.of(Items.REDSTONE), 4)
            .result(Items.DEEPSLATE_TILES, 1) // 暫時使用深板岩磚代替符文石
            .manaCost(50000)
            .ritualTime(600) // 30秒
            .structureRequirement("pedestal.total", 1)
            .save(output);
    }

    /**
     * 生成高級儀式配方 (Tier 3)
     */
    private static void generateTier3Rituals(RecipeOutput output) {
        // 星辰金屬的鍛造
        RitualRecipeBuilder.create("starmetal_forging")
            .name("星辰金屬的鍛造")
            .tier(RitualRecipe.RitualTier.ADVANCED)
            .ingredient(Ingredient.of(Items.NETHER_STAR), 1)
            .ingredient(Ingredient.of(Items.NETHERITE_INGOT), 4)
            .ingredient(Ingredient.of(Items.SPYGLASS), 8) // 使用望遠鏡代替魔力透鏡
            .result(Items.NETHERITE_INGOT, 2) // 暫時使用下界合金代替星辰金屬
            .manaCost(500000)
            .ritualTime(1200) // 60秒
            .structureRequirement("pedestal.total", 8)
            .structureRequirement("pylon.total", 2)
            .structureRequirement("rune.celerity", 4)
            .save(output);

        // 禁忌 - 虛空珍珠的裂解
        RitualRecipeBuilder.create("void_pearl_fracture")
            .name("禁忌 - 虛空珍珠的裂解")
            .tier(RitualRecipe.RitualTier.FORBIDDEN)
            .ingredient(Ingredient.of(Items.ENDER_PEARL), 1)
            .ingredient(Ingredient.of(Items.CHORUS_FRUIT), 4)
            .result(ModItems.VOID_PEARL.get(), 1)
            .manaCost(100000)
            .ritualTime(800) // 40秒
            .failureChance(0.3f) // 30%失敗率
            .structureRequirement("pedestal.total", 4)
            .save(output);
    }

    /**
     * 生成終極儀式配方 (Tier 4)
     */
    private static void generateTier4Rituals(RecipeOutput output) {
        // 世界之心
        RitualRecipeBuilder.create("heart_of_the_world")
            .name("世界之心")
            .tier(RitualRecipe.RitualTier.MASTER)
            .ingredient(Ingredient.of(Items.DRAGON_EGG), 1)
            .ingredient(Ingredient.of(Items.HEART_OF_THE_SEA), 1)
            .ingredient(Ingredient.of(Items.NETHERITE_BLOCK), 1) // 暫時使用下界合金塊代替星辰金屬塊
            .ingredient(Ingredient.of(Items.DIAMOND_BLOCK), 4)
            .result(Items.BEACON, 1) // 暫時使用信標代替世界之心
            .manaCost(10000000) // 10M 魔力
            .ritualTime(2400) // 120秒 (2分鐘)
            .structureRequirement("pedestal.total", 12)
            .structureRequirement("pylon.total", 4)
            .structureRequirement("rune.efficiency", 4)
            .structureRequirement("rune.celerity", 4)
            .structureRequirement("rune.stability", 4)
            .structureRequirement("rune.augmentation", 4)
            .save(output);
    }
}
