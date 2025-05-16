package com.github.nalamodikk.common.recipe.fuel;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.google.gson.JsonObject;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class ManaGenFuelRecipeBuilder implements FinishedRecipe {
    private final ResourceLocation id;
    private final String itemId;
    private final int manaRate;
    private final int energyRate;
    private final int burnTime; // 新增 burnTime 屬性

    public ManaGenFuelRecipeBuilder(String itemId, int manaRate, int energyRate, int burnTime, ResourceLocation id) {
        this.itemId = itemId;
        this.manaRate = manaRate;
        this.energyRate = energyRate;
        this.burnTime = burnTime; // 初始化 burnTime
        this.id = id;
    }

    public static ManaGenFuelRecipeBuilder create(String namespace, String path, int manaRate, int energyRate, int burnTime, String recipeId) {
        return new ManaGenFuelRecipeBuilder(namespace + ":" + path, manaRate, energyRate, burnTime,
                new ResourceLocation(namespace, recipeId));
    }


    public void save(Consumer<FinishedRecipe> consumer) {
        ResourceLocation saveLocation = new ResourceLocation(MagicalIndustryMod.MOD_ID, "mana_recipes/mana_fuel/" + id.getPath());
        consumer.accept(new ManaGenFuelRecipeBuilder(this.itemId, this.manaRate, this.energyRate, this.burnTime, saveLocation));
    }


    @Override
    public void serializeRecipeData(JsonObject json) {
        json.addProperty("type", MagicalIndustryMod.MOD_ID+ ":mana_fuel");
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("item", itemId); // 確保 "item" 屬性正確生成
        json.add("ingredient", ingredient);
        json.addProperty("mana", manaRate);
        json.addProperty("energy", energyRate);
        json.addProperty("burn_time", burnTime); // 新增 burn_time 到 JSON 文件
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getType() {
        return ManaGenFuelRecipe.FuelRecipeSerializer.INSTANCE;
    }

    @Nullable
    @Override
    public JsonObject serializeAdvancement() {
        return null; // 這個方法可以返回 null，因為它是可選的
    }

    @Nullable
    @Override
    public ResourceLocation getAdvancementId() {
        return null; // 這個方法也可以返回 null
    }
}
