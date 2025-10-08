package com.github.nalamodikk.common.block.blockentity.ritual.jei;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.ritual.ritualblockentity.RitualRecipe;
import com.github.nalamodikk.common.block.blockentity.ritual.structure.RitualStructureBlueprint;
import com.github.nalamodikk.common.block.blockentity.ritual.structure.RitualStructureBlueprintRegistry;
import com.github.nalamodikk.common.block.blockentity.ritual.structure.RitualStructureBlueprints;
import com.github.nalamodikk.register.ModBlocks;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 儀式配方 JEI 類別：負責繪製配方佈局與結構需求資訊
 */
public class RitualRecipeCategory implements IRecipeCategory<RitualRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "ritual");
    public static final RecipeType<RitualRecipe> RITUAL_TYPE = new RecipeType<>(UID, RitualRecipe.class);
    private static final ResourceLocation BLUEPRINT_ID = RitualStructureBlueprints.DEFAULT_BLUEPRINT_ID;

    private static final int WIDTH = 160;
    private static final int HEIGHT = 82;
    private static final int INGREDIENT_COLUMNS = 4;
    private static final int STRUCTURE_LINES = 2;

    private final IDrawable background; // 背景繪製物件
    private final IDrawable icon;       // 類別圖示（使用儀式核心方塊）

    /**
     * 建構子：建立 JEI 背景與圖示
     */
    public RitualRecipeCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = helper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.RITUAL_CORE.get())
        );
    }

    /**
     * 回傳 JEI 使用的配方型別
     */
    @Override
    public RecipeType<RitualRecipe> getRecipeType() {
        return RITUAL_TYPE;
    }

    /**
     * 回傳類別標題
     */
    @Override
    public Component getTitle() {
        return Component.translatable("block.koniava.ritual_core");
    }

    /**
     * 回傳背景寬度
     */
    @Override
    public int getWidth() {
        return WIDTH;
    }

    /**
     * 回傳背景高度
     */
    @Override
    public int getHeight() {
        return HEIGHT;
    }

    /**
     * 回傳顯示於 JEI 類別頁籤的圖示
     */
    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    /**
     * 設定配方槽位：擺放祭品、主要成果與額外成果
     */
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RitualRecipe recipe, IFocusGroup focuses) {
        List<Ingredient> ingredients = recipe.getIngredients();
        // 將祭品依序排成 4 欄格狀陣列
        for (int i = 0; i < ingredients.size(); i++) {
            int column = i % INGREDIENT_COLUMNS;
            int row = i / INGREDIENT_COLUMNS;
            int x = 6 + column * 18;
            int y = 6 + row * 18;
            builder.addSlot(RecipeIngredientRole.INPUT, x, y).addIngredients(ingredients.get(i));
        }

        // 主要成果槽位
        builder.addSlot(RecipeIngredientRole.OUTPUT, 124, 10)
                .addItemStack(recipe.getResultItem(null));

        // 額外成果槽位（最多排列成 2 欄）
        List<ItemStack> additionalResults = recipe.getAdditionalResults();
        for (int i = 0; i < additionalResults.size(); i++) {
            ItemStack stack = additionalResults.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            int column = i % 2;
            int row = i / 2;
            int x = 124 + column * 18;
            int y = 32 + row * 18;
            builder.addSlot(RecipeIngredientRole.OUTPUT, x, y).addItemStack(stack.copy());
        }
    }

    @Override
    public void createRecipeExtras(mezz.jei.api.gui.widgets.IRecipeExtrasBuilder builder, RitualRecipe recipe, IFocusGroup focuses) {
        RitualStructureBlueprint blueprint = RitualStructureBlueprintRegistry.get(BLUEPRINT_ID).orElse(null);
        if (blueprint == null) {
            return;
        }
        RitualBlueprintWidget widget = new RitualBlueprintWidget(92, 6, blueprint);
        builder.addWidget(widget);
        builder.addGuiEventListener(widget);
    }

    /**
     * 繪製額外資訊：魔力消耗、失敗率與結構需求
     */
    @Override
    public void draw(RitualRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        int rows = Math.max(1, (recipe.getIngredients().size() + INGREDIENT_COLUMNS - 1) / INGREDIENT_COLUMNS);
        int infoBaseY = Math.min(36, 6 + rows * 18);
        int infoX = 6;
        Component manaCostText = Component.translatable("tooltip.koniava.mana_cost", recipe.getManaCost());
        graphics.drawString(font, manaCostText, infoX, infoBaseY, 0x404040, false);

        float failureChance = recipe.getFailureChance();
        int failurePercent = Math.round(failureChance * 100.0f);
        Component failureText = Component.translatable("tooltip.koniava.ritual.failure_chance", failurePercent);
        graphics.drawString(font, failureText, infoX, infoBaseY + 10, failurePercent > 0 ? 0x8B1A1A : 0x2E7D32, false);

        Component header = Component.translatable("tooltip.koniava.ritual.structure_header");
        int headerY = infoBaseY + 20;
        graphics.drawString(font, header, infoX, headerY, 0x404040, false);

        List<Map.Entry<String, Integer>> requirements = new ArrayList<>(recipe.getStructureRequirements().entrySet());
        requirements.sort(Comparator.comparing(Map.Entry::getKey));

        int baseY = headerY + 10;
        int linesShown = 0;
        for (Map.Entry<String, Integer> entry : requirements) {
            if (linesShown >= STRUCTURE_LINES) {
                int remaining = requirements.size() - linesShown;
                Component moreText = Component.translatable("tooltip.koniava.ritual.structure_more", remaining);
                graphics.drawString(font, moreText, infoX, baseY + linesShown * 10, 0x404040, false);
                break;
            }
            Component label = translateStructureKey(entry.getKey());
            Component line = Component.translatable(
                    "tooltip.koniava.ritual.structure_entry",
                    label,
                    entry.getValue()
            );
            graphics.drawString(font, line, infoX, baseY + linesShown * 10, 0x404040, false);
            linesShown++;
        }
    }

    /**
     * 將結構統計鍵轉換為語系文字
     */
    private Component translateStructureKey(String key) {
        if (key.startsWith("rune.")) {
            String runeName = key.substring("rune.".length());
            return Component.translatable("block.koniava.rune_stone_" + runeName);
        }
        return Component.translatable("structure.koniavacraft." + key);
    }
}
