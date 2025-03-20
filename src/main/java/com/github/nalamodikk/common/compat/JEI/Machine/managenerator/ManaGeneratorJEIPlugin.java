package com.github.nalamodikk.common.compat.JEI.Machine.managenerator;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.recipe.fuel.FuelRecipe;
import com.github.nalamodikk.common.register.ModBlocks;
import com.github.nalamodikk.common.screen.ManaGenerator.ManaGeneratorScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@JeiPlugin
public class ManaGeneratorJEIPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(MagicalIndustryMod.MOD_ID, "mana_generator_jei_plugin");
    private static final Logger LOGGER = LoggerFactory.getLogger(ManaGeneratorJEIPlugin.class);

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        LOGGER.info("[JEI] 正在註冊 FuelRecipeCategory...");

        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();

        registration.addRecipeCategories(new FuelRecipeCategory(guiHelper));
    }


    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        LOGGER.info("[JEI] 註冊 Mana Generator 為 FuelRecipeCategory 的催化劑！");
        registration.addRecipeCatalyst(
                new ItemStack(ModBlocks.MANA_GENERATOR.get()), FuelRecipeCategory.RECIPE_TYPE
        );
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(ManaGeneratorScreen.class,30,15 ,25,45,FuelRecipeCategory.RECIPE_TYPE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            LOGGER.warn("[JEI] Minecraft Level 為 null，無法載入 FuelRecipe。");
            return;
        }

        List<FuelRecipe> fuelRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(FuelRecipe.FuelRecipeType.INSTANCE);

        if (fuelRecipes.isEmpty()) {
            LOGGER.error("[JEI] ❌ FuelRecipe 配方數量為 0，請檢查 JSON 是否正確存入 `mana_recipes/fuel/`！");
            return;
        }

        LOGGER.info("[JEI] ✅ 找到了 {} 個燃料配方，開始註冊...", fuelRecipes.size());
        registration.addRecipes(FuelRecipeCategory.RECIPE_TYPE, fuelRecipes);
        LOGGER.info("[JEI] ✅ 成功註冊 FuelRecipeCategory 配方！");
    }

}
