package com.github.nalamodikk.common.coreapi.recipe.jei;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.coreapi.recipe.ProcessingRecipe;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 🔄 加工配方 JEI 整合插件
 *
 * 支援多個機器類型的配方顯示：
 * - grinder (粉碎機)
 * - washer (清洗機)
 * - enricher (富集機)
 * - 等等...
 *
 * 每個機器類型有自己的 JEI 分類頁面
 */
@JeiPlugin
public class ProcessingRecipeJEIPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
            KoniavacraftMod.MOD_ID, "processing_recipe_jei_plugin");
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingRecipeJEIPlugin.class);

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    /**
     * 📝 註冊 JEI 配方分類
     */
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        LOGGER.info("[JEI] 正在註冊加工配方分類...");

        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();

        // 為每個機器類型註冊一個分類
        registration.addRecipeCategories(
                new GrinderRecipeCategory(guiHelper),
                new WasherRecipeCategory(guiHelper),
                new EnricherRecipeCategory(guiHelper)
        );

        LOGGER.info("[JEI] ✅ 加工配方分類註冊完成");
    }

    /**
     * 🔨 註冊催化劑（機器方塊）
     *
     * 在 JEI 中，催化劑是指能執行該配方的機器
     * 用戶點擊配方時，會顯示哪個機器能做這個配方
     */
    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        LOGGER.info("[JEI] 註冊加工機器為配方催化劑...");

        // TODO: 當 ModBlocks 中的 ORE_GRINDER、ORE_WASHER、ORE_ENRICHER 註冊後取消註解
        /*
        // 粉碎機
        if (ModBlocks.ORE_GRINDER.isPresent()) {
            registration.addRecipeCatalyst(
                    new ItemStack(ModBlocks.ORE_GRINDER.get()),
                    GrinderRecipeCategory.RECIPE_TYPE
            );
            LOGGER.debug("[JEI] ✅ 粉碎機已註冊為催化劑");
        }

        // 清洗機
        if (ModBlocks.ORE_WASHER.isPresent()) {
            registration.addRecipeCatalyst(
                    new ItemStack(ModBlocks.ORE_WASHER.get()),
                    WasherRecipeCategory.RECIPE_TYPE
            );
            LOGGER.debug("[JEI] ✅ 清洗機已註冊為催化劑");
        }

        // 富集機
        if (ModBlocks.ORE_ENRICHER.isPresent()) {
            registration.addRecipeCatalyst(
                    new ItemStack(ModBlocks.ORE_ENRICHER.get()),
                    EnricherRecipeCategory.RECIPE_TYPE
            );
            LOGGER.debug("[JEI] ✅ 富集機已註冊為催化劑");
        }
        */

        LOGGER.info("[JEI] ⚠️ 機器方塊催化劑將在 Block 註冊後啟用");
    }

    /**
     * 🖱️ 註冊 GUI 點擊區域
     *
     * 讓玩家點擊 GUI 中特定區域就能打開對應的 JEI 配方
     */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // TODO: 後續添加 GUI 點擊區域
        // registration.addRecipeClickArea(
        //     OreGrinderScreen.class,
        //     79, 35,  // 進度條 X, Y
        //     26, 16,  // 進度條寬度, 高度
        //     GrinderRecipeCategory.RECIPE_TYPE
        // );
    }

    /**
     * 📖 註冊配方到 JEI
     *
     * 從遊戲中讀取所有配方，按機器類型分類後註冊到 JEI
     */
    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            LOGGER.warn("[JEI] Minecraft Level 為 null，無法載入加工配方");
            return;
        }

        // 獲取所有加工配方
        List<ProcessingRecipe> allRecipes = minecraft.level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.PROCESSING_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();

        if (allRecipes.isEmpty()) {
            LOGGER.error("[JEI] ❌ 沒有找到加工配方！請確保配方已生成並加載");
            return;
        }

        LOGGER.info("[JEI] ✅ 找到了 {} 個加工配方，開始按類型分類...", allRecipes.size());

        // 按機器類型分類
        List<ProcessingRecipe> grinderRecipes = allRecipes.stream()
                .filter(r -> "grinder".equals(r.getMachineType()))
                .toList();

        List<ProcessingRecipe> washerRecipes = allRecipes.stream()
                .filter(r -> "washer".equals(r.getMachineType()))
                .toList();

        List<ProcessingRecipe> enricherRecipes = allRecipes.stream()
                .filter(r -> "enricher".equals(r.getMachineType()))
                .toList();

        // 註冊各分類的配方
        if (!grinderRecipes.isEmpty()) {
            LOGGER.info("[JEI] 📝 註冊 {} 個粉碎機配方", grinderRecipes.size());
            registration.addRecipes(GrinderRecipeCategory.RECIPE_TYPE, grinderRecipes);
        }

        if (!washerRecipes.isEmpty()) {
            LOGGER.info("[JEI] 📝 註冊 {} 個清洗機配方", washerRecipes.size());
            registration.addRecipes(WasherRecipeCategory.RECIPE_TYPE, washerRecipes);
        }

        if (!enricherRecipes.isEmpty()) {
            LOGGER.info("[JEI] 📝 註冊 {} 個富集機配方", enricherRecipes.size());
            registration.addRecipes(EnricherRecipeCategory.RECIPE_TYPE, enricherRecipes);
        }

        LOGGER.info("[JEI] ✅ 所有加工配方已註冊到 JEI");
    }
}
