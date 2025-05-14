package com.github.nalamodikk.common.ComponentSystem.recipe.component;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.util.*;

/**
 * 載入所有元件對應的 JSON 配方
 */
public class ComponentRecipeLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LoggerFactory.getLogger("ComponentRecipeLoader");
    private static final Map<ResourceLocation, List<AssemblyRecipe>> RECIPE_MAP = new HashMap<>();

    public static final ComponentRecipeLoader INSTANCE = new ComponentRecipeLoader();

    private ComponentRecipeLoader() {
        super(GSON, "component_recipes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        RECIPE_MAP.clear();
        for (var entry : pObject.entrySet()) {
            try {
                ResourceLocation id = entry.getKey();
                JsonArray array = entry.getValue().getAsJsonArray();
                List<AssemblyRecipe> recipes = new ArrayList<>();

                for (int i = 0; i < array.size(); i++) {
                    JsonObject obj = array.get(i).getAsJsonObject();
                    ResourceLocation recipeId = new ResourceLocation(id.getNamespace(), id.getPath() + "/" + i);
                    AssemblyRecipe recipe = AssemblyRecipe.fromJson(recipeId, obj);
                    recipes.add(recipe);
                }

                RECIPE_MAP.put(id, recipes);
                LOGGER.info("✅ Loaded component recipe: {} ({} recipes)", id, recipes.size());
            } catch (Exception e) {
                LOGGER.error("❌ Failed to load component recipe: {}", entry.getKey(), e);
            }
        }
    }


    public static List<AssemblyRecipe> getRecipesForComponent(ResourceLocation id) {
        return RECIPE_MAP.getOrDefault(id, List.of());

    }
}