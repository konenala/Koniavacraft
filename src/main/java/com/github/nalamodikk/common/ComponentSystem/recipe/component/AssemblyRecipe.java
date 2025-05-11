package com.github.nalamodikk.common.ComponentSystem.recipe.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

/**
 * 定義單筆拼裝配方：輸入、輸出、mana、冷卻
 */
public class AssemblyRecipe {
    private final ResourceLocation id;
    private final List<Ingredient> inputItems;
    private final ItemStack output;
    private final int manaCost;
    private final int cooldownTicks;

    public AssemblyRecipe(ResourceLocation id, List<Ingredient> inputItems, ItemStack output, int manaCost, int cooldownTicks) {
        this.id = id;
        this.inputItems = inputItems;
        this.output = output;
        this.manaCost = manaCost;
        this.cooldownTicks = cooldownTicks;
    }

    public ResourceLocation getId() {
        return id;
    }

    public List<Ingredient> getInputItems() {
        return inputItems;
    }

    public ItemStack getOutput() {
        return output;
    }

    public int getManaCost() {
        return manaCost;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    /**
     * 工具函式：讀 JSON 建構配方
     */
    public static AssemblyRecipe fromJson(ResourceLocation id, JsonObject json) {
        List<Ingredient> inputs = new ArrayList<>();
        JsonArray inputArray = json.getAsJsonArray("inputs");
        for (int i = 0; i < inputArray.size(); i++) {
            inputs.add(Ingredient.fromJson(inputArray.get(i)));
        }

        ItemStack output = ItemStack.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json.get("output"))
                .result()
                .orElseThrow(() -> new IllegalArgumentException("Invalid output in assembly recipe: " + id));

        int mana = json.has("mana") ? json.get("mana").getAsInt() : 0;
        int cooldown = json.has("cooldown") ? json.get("cooldown").getAsInt() : 0;

        return new AssemblyRecipe(id, inputs, output, mana, cooldown);
    }
}
