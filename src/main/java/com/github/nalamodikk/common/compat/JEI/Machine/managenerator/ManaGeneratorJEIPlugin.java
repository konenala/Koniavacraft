package com.github.nalamodikk.common.compat.JEI.Machine.managenerator;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.recipe.fuel.FuelRecipe;
import com.github.nalamodikk.common.register.ModBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
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
        registration.addRecipeCategories(new FuelRecipeCategory(
                registration.getJeiHelpers().getGuiHelper().createDrawable(FuelRecipeCategory.getTexture(), 0, 0, 176, 85),
                registration.getJeiHelpers().getGuiHelper().createDrawableItemStack(new ItemStack(ModBlocks.MANA_GENERATOR.get()))
        ));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            LOGGER.warn("[JEI] Minecraft Level 為 null，無法載入 FuelRecipe。");
            return;
        }

        List<FuelRecipe> fuelRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(FuelRecipe.FuelRecipeType.INSTANCE);

        LOGGER.info("[JEI] 找到了 {} 個燃料配方！", fuelRecipes.size());

        if (!fuelRecipes.isEmpty()) {
            registration.addRecipes(FuelRecipeCategory.RECIPE_TYPE, fuelRecipes);
            LOGGER.info("[JEI] 成功註冊 FuelRecipeCategory 配方！");
        } else {
            LOGGER.warn("[JEI] 沒有找到 FuelRecipe，請檢查 JSON 配方文件！");
        }
    }
}
