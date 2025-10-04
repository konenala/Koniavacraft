package com.github.nalamodikk.common.datagen.recipe.ritual;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.ritual.ritualblockentity.RitualRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 儀式配方構建器 - 用於 DataGen 創建儀式配方
 */
public class RitualRecipeBuilder {
    private final String id;
    private String name = "";
    private RitualRecipe.RitualTier tier = RitualRecipe.RitualTier.BASIC;
    private final NonNullList<Ingredient> ingredients = NonNullList.create();
    private ItemStack result = ItemStack.EMPTY;
    private final List<ItemStack> additionalResults = new ArrayList<>();
    private int manaCost = 1000;
    private int ritualTime = 200;
    private float failureChance = 0.0f;
    private final Map<String, Integer> structureRequirements = new HashMap<>();

    private RitualRecipeBuilder(String id) {
        this.id = id;
        this.name = id; // 默認名稱為ID
    }

    public static RitualRecipeBuilder create(String id) {
        return new RitualRecipeBuilder(id);
    }

    public RitualRecipeBuilder name(String name) {
        this.name = name;
        return this;
    }

    public RitualRecipeBuilder tier(RitualRecipe.RitualTier tier) {
        this.tier = tier;
        return this;
    }

    public RitualRecipeBuilder ingredient(Ingredient ingredient) {
        this.ingredients.add(ingredient);
        return this;
    }

    public RitualRecipeBuilder ingredient(Ingredient ingredient, int count) {
        // 為簡化，這裡只添加一次。實際應用中可能需要根據count添加多次
        for (int i = 0; i < count; i++) {
            this.ingredients.add(ingredient);
        }
        return this;
    }

    public RitualRecipeBuilder ingredient(ItemLike item) {
        return ingredient(Ingredient.of(item));
    }

    public RitualRecipeBuilder ingredient(ItemLike item, int count) {
        return ingredient(Ingredient.of(item), count);
    }

    public RitualRecipeBuilder result(ItemStack result) {
        this.result = result;
        return this;
    }

    public RitualRecipeBuilder result(ItemLike item) {
        return result(new ItemStack(item));
    }

    public RitualRecipeBuilder result(ItemLike item, int count) {
        return result(new ItemStack(item, count));
    }

    public RitualRecipeBuilder additionalResult(ItemStack result) {
        this.additionalResults.add(result);
        return this;
    }

    public RitualRecipeBuilder additionalResult(ItemLike item) {
        return additionalResult(new ItemStack(item));
    }

    public RitualRecipeBuilder additionalResult(ItemLike item, int count) {
        return additionalResult(new ItemStack(item, count));
    }

    public RitualRecipeBuilder manaCost(int manaCost) {
        this.manaCost = manaCost;
        return this;
    }

    public RitualRecipeBuilder ritualTime(int ritualTime) {
        this.ritualTime = ritualTime;
        return this;
    }

    public RitualRecipeBuilder failureChance(float failureChance) {
        this.failureChance = failureChance;
        return this;
    }

    public RitualRecipeBuilder structureRequirement(String component, int count) {
        this.structureRequirements.put(component, count);
        return this;
    }

    /**
     * 保存配方到輸出
     */
    public void save(RecipeOutput output) {
        if (result.isEmpty()) {
            throw new IllegalStateException("儀式配方必須有結果物品: " + id);
        }

        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "ritual/" + id);

        RitualRecipe recipe = new RitualRecipe(
            recipeId,
            name,
            tier,
            ingredients,
            result,
            additionalResults,
            manaCost,
            ritualTime,
            failureChance,
            structureRequirements
        );

        output.accept(recipeId, recipe, null);
    }

    /**
     * 驗證配方是否有效
     */
    private boolean isValid() {
        if (result.isEmpty()) return false;
        if (ingredients.isEmpty()) return false;
        if (manaCost <= 0) return false;
        if (ritualTime <= 0) return false;
        return true;
    }

    /**
     * 預定義的結構要求快捷方法
     */
    public RitualRecipeBuilder basicStructure() {
        return structureRequirement("core", 1)
               .structureRequirement("pedestals", 4);
    }

    public RitualRecipeBuilder intermediateStructure() {
        return structureRequirement("core", 1)
               .structureRequirement("pedestals", 4)
               .structureRequirement("pylons", 1);
    }

    public RitualRecipeBuilder advancedStructure() {
        return structureRequirement("core", 1)
               .structureRequirement("pedestals", 8)
               .structureRequirement("pylons", 2);
    }

    public RitualRecipeBuilder masterStructure() {
        return structureRequirement("core", 1)
               .structureRequirement("pedestals", 12)
               .structureRequirement("pylons", 4);
    }

    /**
     * 符文石要求快捷方法
     */
    public RitualRecipeBuilder requireEfficiencyRunes(int count) {
        return structureRequirement("efficiency_runes", count);
    }

    public RitualRecipeBuilder requireCelerityRunes(int count) {
        return structureRequirement("celerity_runes", count);
    }

    public RitualRecipeBuilder requireStabilityRunes(int count) {
        return structureRequirement("stability_runes", count);
    }

    public RitualRecipeBuilder requireAugmentationRunes(int count) {
        return structureRequirement("augmentation_runes", count);
    }
}
