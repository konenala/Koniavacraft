package com.github.nalamodikk.common.ComponentSystem.recipe.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 封裝 Ingredient + count，支援數量判定的材料單位
 */
public class CountedIngredient {
    private final Ingredient ingredient;
    private final int count;

    public CountedIngredient(Ingredient ingredient, int count) {
        this.ingredient = ingredient;
        this.count = count;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public int getCount() {
        return count;
    }

    public boolean matches(ItemStack stack) {
        return ingredient.test(stack) && stack.getCount() >= count;
    }

    /**
     * 解析 JSON 格式：支援 {item:"...", count:N}
     */
    public static CountedIngredient fromJson(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            Ingredient ing = Ingredient.fromJson(obj);
            int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
            return new CountedIngredient(ing, count);
        } else {
            Ingredient ing = Ingredient.fromJson(element);
            return new CountedIngredient(ing, 1);
        }
    }
}
