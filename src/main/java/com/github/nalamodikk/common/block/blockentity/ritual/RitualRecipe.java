package com.github.nalamodikk.common.block.blockentity.ritual;

import com.github.nalamodikk.register.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;

/**
 * 儀式配方 - 定義儀式的所有要求和產出
 */
public class RitualRecipe implements Recipe<RitualRecipe.RitualInput> {
    
    private final ResourceLocation id;
    private final String name;
    private final RitualTier tier;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final List<ItemStack> additionalResults;
    private final int manaCost;
    private final int ritualTime;
    private final float failureChance;
    private final Map<String, Integer> structureRequirements;
    
    public RitualRecipe(ResourceLocation id, String name, RitualTier tier,
                       NonNullList<Ingredient> ingredients, ItemStack result,
                       List<ItemStack> additionalResults, int manaCost,
                       int ritualTime, float failureChance,
                       Map<String, Integer> structureRequirements) {
        this.id = id;
        this.name = name;
        this.tier = tier;
        this.ingredients = ingredients;
        this.result = result;
        this.additionalResults = additionalResults;
        this.manaCost = manaCost;
        this.ritualTime = ritualTime;
        this.failureChance = failureChance;
        this.structureRequirements = structureRequirements;
    }

    @Override
    public boolean matches(RitualInput input, Level level) {
        // 檢查祭品是否匹配
        List<ItemStack> availableItems = input.getIngredients();
        if (availableItems.size() < ingredients.size()) {
            return false;
        }
        
        // 檢查每個必需材料是否可用
        for (Ingredient ingredient : ingredients) {
            boolean found = false;
            for (ItemStack available : availableItems) {
                if (ingredient.test(available)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        
        // 檢查魔力是否充足
        if (input.getAvailableMana() < manaCost) {
            return false;
        }
        
        return true;
    }

    @Override
    public ItemStack assemble(RitualInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true; // 儀式不受尺寸限制
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.RITUAL_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.RITUAL_TYPE.get();
    }

    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    // Getters
    public String getName() { return name; }
    public RitualTier getTier() { return tier; }
    public List<ItemStack> getAdditionalResults() { return additionalResults; }
    public int getManaCost() { return manaCost; }
    public int getRitualTime() { return ritualTime; }
    public float getFailureChance() { return failureChance; }
    public Map<String, Integer> getStructureRequirements() { return structureRequirements; }

    /**
     * 儀式輸入容器
     */
    public static class RitualInput implements RecipeInput {
        private final List<ItemStack> ingredients;
        private final int availableMana;
        private final Map<String, Integer> availableStructure;
        
        public RitualInput(List<ItemStack> ingredients, int availableMana, 
                          Map<String, Integer> availableStructure) {
            this.ingredients = ingredients;
            this.availableMana = availableMana;
            this.availableStructure = availableStructure;
        }

        @Override
        public ItemStack getItem(int index) {
            return index < ingredients.size() ? ingredients.get(index) : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return ingredients.size();
        }
        
        public List<ItemStack> getIngredients() { return ingredients; }
        public int getAvailableMana() { return availableMana; }
        public Map<String, Integer> getAvailableStructure() { return availableStructure; }
    }

    /**
     * 儀式等級枚舉
     */
    public enum RitualTier {
        BASIC(1, "基礎"),
        INTERMEDIATE(2, "中級"), 
        ADVANCED(3, "高級"),
        MASTER(4, "大師"),
        FORBIDDEN(5, "禁忌");
        
        public final int level;
        public final String displayName;
        
        RitualTier(int level, String displayName) {
            this.level = level;
            this.displayName = displayName;
        }
    }
}