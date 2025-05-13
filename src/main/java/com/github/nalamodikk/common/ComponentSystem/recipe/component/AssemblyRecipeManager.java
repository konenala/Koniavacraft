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

    public static Collection<AssemblyRecipe> getAllRecipes() {
        return RECIPES.values();
    }

    public static @Nullable AssemblyRecipe findMatchingRecipe(List<ItemStack> inputs) {
        outer:
        for (AssemblyRecipe recipe : RECIPES.values()) {
            List<Ingredient> required = recipe.getInputItems();
            if (required.size() != inputs.size()) continue;

            List<ItemStack> temp = new ArrayList<>(inputs);

            for (Ingredient ingredient : required) {
                boolean matched = false;
                for (ItemStack stack : temp) {
                    if (ingredient.test(stack)) {
                        temp.remove(stack);
                        matched = true;
                        break;
                    }
                }
                if (!matched) continue outer;
            }

            return recipe;
        }

        return null;
    }
}
