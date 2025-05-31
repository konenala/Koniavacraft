package com.github.nalamodikk.common.block.mana_crafting;

import com.github.nalamodikk.client.screenAPI.DynamicTooltip;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.register.ModBlocks;
import com.github.nalamodikk.common.register.ModItems;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class ManaCraftingTableCategory implements IRecipeCategory<ManaCraftingTableRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "mana_crafting");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "textures/gui/mana_crafting_table_gui.png");
    public static final ResourceLocation MANA_BAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "textures/gui/mana_bar_full.png");
    public static final int WIDTH = 173;
    public static final int HEIGHT = 80;

    public static final RecipeType<ManaCraftingTableRecipe> MANA_CRAFTING_TYPE =
            new RecipeType<>(UID, ManaCraftingTableRecipe.class);

    private final IDrawable icon;
    private final IDrawableStatic manaCostDrawable;

    public ManaCraftingTableCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get()));
        // 使用完整的魔力条纹理
        this.manaCostDrawable = helper.createDrawable(MANA_BAR_TEXTURE, 0, 0, 16, 16);  // 使用整张魔力条纹理，指定区域大小
    }

    @Override
    public RecipeType<ManaCraftingTableRecipe> getRecipeType() {
        return MANA_CRAFTING_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.magical_industry.mana_crafting_table");
    }

    @Override
    public int getWidth() {
        return WIDTH; // 你自己定義背景寬度
    }

    @Override
    public int getHeight() {
        return HEIGHT; // 你自己定義背景高度
    }


    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ManaCraftingTableRecipe recipe, IFocusGroup focuses) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();

        if (recipe.isShaped()) {
            // 3x3 格擺放（照 pattern）
            for (int i = 0; i < ingredients.size(); i++) {
                int xPos = 30 + (i % 3) * 18;
                int yPos = 17 + (i / 3) * 18;
                builder.addSlot(RecipeIngredientRole.INPUT, xPos, yPos).addIngredients(ingredients.get(i));
            }
        } else {
            // 無序合成：橫向擺放
            for (int i = 0; i < ingredients.size(); i++) {
                int xPos = 30 + i * 18;
                builder.addSlot(RecipeIngredientRole.INPUT, xPos, 17).addIngredients(ingredients.get(i));
            }
        }

        // 合成結果槽位
        builder.addSlot(RecipeIngredientRole.OUTPUT, 124, 35).addItemStack(recipe.getResultItem(null));

        // 魔力消耗提示（Catalyst 插槽，實際沒功能，只是 tooltip 顯示）

    }

    @Override
    public void draw(ManaCraftingTableRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        int bgWidth = getWidth();
        int bgHeight = getHeight();

        // ✅ 正確的 blit 呼叫：從 texture 左上角 (0,0) 開始繪製整張圖
        graphics.blit(TEXTURE, 0, 0, 0, 0, bgWidth, bgHeight);

        // 🔵 Mana 條繪製邏輯
        int manaCost = recipe.getManaCost();
        int barX = 11;
        int barY = 19;
        int barWidth = 8;
        int barHeight = 47;

        int filledHeight = (int)((manaCost / (float) ManaCraftingTableBlockEntity.MAX_MANA) * barHeight);
        int filledY = barY + barHeight - filledHeight;
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f); // 重設為白色，避免影響之後的 blit/draw

        Minecraft.getInstance().getTextureManager().bindForSetup(MANA_BAR_TEXTURE);
        graphics.blit(MANA_BAR_TEXTURE, barX, filledY, 0, barHeight - filledHeight, barWidth,filledHeight);

        // 檢查滑鼠是否在指定區域內
        int x = 11;
        int y = 19;
        int width = 8;
        int height = 47;

        boolean inArea = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        if (inArea) {
            DynamicTooltip tooltip = new DynamicTooltip(() ->
                    List.of(Component.translatable("tooltip.magical_industry.mana_cost", recipe.getManaCost()))
            );

            graphics.renderTooltip(
                    Minecraft.getInstance().font,
                    tooltip.toComponentList(),
                    Optional.empty(),
                    (int) mouseX,
                    (int) mouseY
            );
        }
    }


}
