package com.github.nalamodikk.common.ComponentSystem.recipe.component;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.util.*;

public class AssemblyRecipeManager extends SimplePreparableReloadListener<Map<ResourceLocation, JsonObject>> {
    private static final Map<ResourceLocation, AssemblyRecipe> RECIPES = new HashMap<>();
    public static final AssemblyRecipeManager INSTANCE = new AssemblyRecipeManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Logger LOGGER = LogUtils.getLogger();

    private AssemblyRecipeManager() {}

    @Override
    protected Map<ResourceLocation, JsonObject> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonObject> map = new HashMap<>();
        String prefix = "recipes/assembly";

        manager.listResources(prefix, path -> path.getPath().endsWith(".json")).forEach((location, resource) -> {
            try (var stream = resource.open()) {
                JsonObject json = GSON.fromJson(new InputStreamReader(stream), JsonObject.class);
                ResourceLocation id = new ResourceLocation(location.getNamespace(), location.getPath().substring(prefix.length()));
                map.put(id, json);
            } catch (Exception e) {
                LOGGER.error("❌ Failed to read assembly recipe JSON: {}", location, e);
            }
        });

        return map;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonObject> objectMap, ResourceManager manager, ProfilerFiller profiler) {
        RECIPES.clear();
        objectMap.forEach((id, json) -> {
            try {
                AssemblyRecipe recipe = AssemblyRecipe.fromJson(id, json);
                RECIPES.put(id, recipe);
            } catch (Exception e) {
                LOGGER.error("❌ Failed to parse recipe {}: {}", id, e.getMessage());
            }
        });

        LOGGER.info("✅ Loaded {} assembly recipes", RECIPES.size());
    }

    public static List<AssemblyRecipe> getAllRecipes() {
        return new ArrayList<>(RECIPES.values());
    }


    public static @Nullable AssemblyRecipe findMatchingRecipe(List<ItemStack> inputs) {
        outer:
        for (AssemblyRecipe recipe : RECIPES.values()) {
            List<CountedIngredient> required = recipe.getInputItems();

            // ⚠️ 配方需要的材料格數不能大於實際輸入材料格數（支援 >=，可擴展）
            if (required.size() > inputs.size()) continue;

            // 複製輸入避免污染原資料
            List<ItemStack> tempInputs = new ArrayList<>();
            for (ItemStack original : inputs) {
                tempInputs.add(original.copy());
            }

            for (CountedIngredient counted : required) {
                boolean matched = false;

                for (ItemStack stack : tempInputs) {
                    if (counted.getIngredient().test(stack) && stack.getCount() >= counted.getCount()) {
                        // ✅ 消耗對應數量
                        stack.shrink(counted.getCount());

                        // 若已用光，從清單中移除
                        if (stack.isEmpty()) {
                            tempInputs.remove(stack);
                        }

                        matched = true;
                        break;
                    }
                }

                if (!matched) continue outer; // 有一個對不上就跳過這個配方
            }

            return recipe;
        }

        return null;
    }

}
