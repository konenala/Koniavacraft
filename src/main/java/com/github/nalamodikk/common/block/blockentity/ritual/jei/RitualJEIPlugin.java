package com.github.nalamodikk.common.block.blockentity.ritual.jei;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.ritual.ritualblockentity.RitualRecipe;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 儀式配方 JEI 插件：負責註冊類別、配方與催化物
 */
@JeiPlugin
public class RitualJEIPlugin implements IModPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(RitualJEIPlugin.class);
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "ritual");

    /**
     * 回傳 JEI 插件識別碼
     */
    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    /**
     * 註冊儀式配方類別
     */
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new RitualRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    /**
     * 註冊催化物：儀式核心方塊
     */
    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                new ItemStack(ModBlocks.RITUAL_CORE.get()),
                RitualRecipeCategory.RITUAL_TYPE
        );
    }

    /**
     * 註冊所有儀式配方
     */
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            LOGGER.warn("[JEI] Minecraft 尚未載入世界，暫時略過 Ritual 配方註冊。");
            return;
        }

        RecipeManager recipeManager = minecraft.level.getRecipeManager();
        List<RitualRecipe> ritualRecipes = recipeManager.getAllRecipesFor(ModRecipes.RITUAL_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();

        registration.addRecipes(RitualRecipeCategory.RITUAL_TYPE, ritualRecipes);
        LOGGER.info("[JEI] 已註冊 {} 筆 Ritual 配方至 JEI。", ritualRecipes.size());
    }
}
