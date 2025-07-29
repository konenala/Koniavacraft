package com.github.nalamodikk.common.block.blockentity.mana_infuser;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 🔮 魔力注入機 JEI 兼容插件
 *
 * 功能：
 * - 註冊魔力注入配方類別
 * - 顯示所有魔力注入配方
 * - 與原版 JEI 共存但不重疊
 */
@JeiPlugin
public class ManaInfuserJEIPlugin implements IModPlugin {

    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_infuser");

    private static final Logger LOGGER = LoggerFactory.getLogger(ManaInfuserJEIPlugin.class);

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        // 註冊魔力注入配方類別
        LOGGER.info("[JEI] 正在註冊 ManaInfuserCategory...");

        registration.addRecipeCategories(new ManaInfuserCategory(
                registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // 註冊魔力注入機作為配方的催化劑（JEI 中顯示的機器圖標）
        registration.addRecipeCatalyst(
                new ItemStack(ModBlocks.MANA_INFUSER.get()),
                ManaInfuserCategory.MANA_INFUSER_TYPE
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            LOGGER.warn("[JEI] Minecraft 尚未進入世界，跳過 mana_infuser 配方註冊！");
            return;
        }

        RecipeManager recipeManager = minecraft.level.getRecipeManager();

        // 獲取所有魔力注入配方
        List<ManaInfuserRecipe> manaInfuserRecipes =
                recipeManager.getAllRecipesFor(ModRecipes.MANA_INFUSER_TYPE.get())
                        .stream()
                        .map(RecipeHolder::value)
                        .toList();

        // 註冊到 JEI
        registration.addRecipes(ManaInfuserCategory.MANA_INFUSER_TYPE, manaInfuserRecipes);

        LOGGER.info("[JEI] 成功註冊 {} 筆 mana_infuser 配方", manaInfuserRecipes.size());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // 註冊 GUI 的點擊區域，讓玩家能在 JEI 中點擊進度條查看配方
        registration.addRecipeClickArea(
                ManaInfuserScreen.class,
                149, 4, 21, 15, // 進度條位置 (x, y, width, height)
                ManaInfuserCategory.MANA_INFUSER_TYPE
        );


    }
}