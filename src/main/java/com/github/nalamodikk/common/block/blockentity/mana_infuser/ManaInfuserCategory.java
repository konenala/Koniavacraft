package com.github.nalamodikk.common.block.blockentity.mana_infuser;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModBlocks;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * 🔮 魔力注入機 JEI 配方類別
 * 參考魔力發電機的風格實現
 */
public class ManaInfuserCategory implements IRecipeCategory<ManaInfuserRecipe> {

    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_infuser");

    // 🎯 使用你現有的魔力注入機 GUI 材質
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/jei_mana_infuser_gui.png");

    public static RecipeType<ManaInfuserRecipe> MANA_INFUSER_TYPE =
            new RecipeType<>(UID, ManaInfuserRecipe.class);

    // JEI 配方尺寸
    private static final int RECIPE_WIDTH = 174;
    private static final int RECIPE_HEIGHT = 80;

    private static final Logger LOGGER = LoggerFactory.getLogger(ManaInfuserCategory.class);

    private final IDrawableStatic background;
    private final IDrawable icon;
    private final IDrawableAnimated arrow; // 🆕 動畫箭頭

    public ManaInfuserCategory(IGuiHelper guiHelper) {
        // 🎨 使用你的原版 GUI 材質作為背景
        this.background = guiHelper.drawableBuilder(TEXTURE, 0, 0, RECIPE_WIDTH, RECIPE_HEIGHT)
                .setTextureSize(256, 128) // 你的材質尺寸
                .build();
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.MANA_INFUSER.get()));
        // 🎯 使用正確的箭頭座標和尺寸
        IDrawableStatic staticArrow = guiHelper.drawableBuilder(TEXTURE, 176, 54, 36, 14)
                .setTextureSize(256, 128)  // 整個材質的尺寸
                .build();
        this.arrow = guiHelper.createAnimatedDrawable(staticArrow, 60,
                IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<ManaInfuserRecipe> getRecipeType() {
        return MANA_INFUSER_TYPE;
    }
    @Override
    public Component getTitle() {
        return Component.translatable("block.koniava.mana_infuser");
    }

    @Override
    public int getWidth() {
        return RECIPE_WIDTH;
    }

    @Override
    public int getHeight() {
        return RECIPE_HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ManaInfuserRecipe recipe, IFocusGroup focuses) {
        // 🔧 檢查配方有效性
        if (recipe.getInput().isEmpty()) {
            LOGGER.error("[JEI] ❌ ManaInfuserCategory 中配方無有效輸入物品！");
            return;
        }

        // 📦 輸入槽 (使用你 GUI 中的實際位置)
        builder.addSlot(RecipeIngredientRole.INPUT, 48, 35)
                .addIngredients(recipe.getInput())
                .setSlotName("input");

        // 📦 輸出槽 (使用你 GUI 中的實際位置)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 123, 34)
                .addItemStack(recipe.getResult())
                .setSlotName("output");
    }

    @Override
    public void draw(ManaInfuserRecipe recipe, IRecipeSlotsView slotsView,
                     GuiGraphics graphics, double mouseX, double mouseY) {

        // 🎨 繪製你的 GUI 背景材質
        graphics.blit(TEXTURE, 0, 0, 0, 0, RECIPE_WIDTH, RECIPE_HEIGHT, 256, 128);

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        // 📊 配方數據
        int manaCost = recipe.getManaCost();
        int infusionTime = recipe.getInfusionTime();
        int inputCount = recipe.getInputCount();

        // 🔵 繪製魔力條 (使用你的UV座標)
        drawManaBarFromTexture(graphics, manaCost);

        // ➡️ 繪製動畫箭頭 (使用你的UV座標)
        drawAnimatedArrow(graphics);

        // 📝 繪製配方信息文字
        drawRecipeInfo(graphics, font, manaCost, infusionTime, inputCount);

        // 🔹 處理 tooltip
        handleTooltips(graphics, font, mouseX, mouseY, manaCost, infusionTime, inputCount);
    }

    /**
     * 🔵 從材質繪製魔力條 (使用你的UV座標)
     */
    private void drawManaBarFromTexture(GuiGraphics graphics, int manaCost) {
        // 🎯 使用你提供的魔力條UV座標: 176, 1 到 186, 49
        int manaBarX = 9;   // 你GUI中魔力條的位置
        int manaBarY = 17;
        int manaBarWidth = 10;  // 186-176=10
        int manaBarHeight = 48; // 49-1=48

        // 計算魔力條填充高度
        int maxMana = 200; // 假設最大魔力
        int fillHeight = Math.min(manaBarHeight * manaCost / maxMana, manaBarHeight);

        if (fillHeight > 0) {
            // 從底部往上填充
            graphics.blit(TEXTURE,
                    manaBarX, manaBarY + manaBarHeight - fillHeight,  // 螢幕位置
                    176, 1 + manaBarHeight - fillHeight,               // UV位置
                    manaBarWidth, fillHeight,                          // 尺寸
                    256, 128);                                         // 材質尺寸
        }
    }

    /**
     * ➡️ 繪製動畫箭頭
     */
    private void drawAnimatedArrow(GuiGraphics graphics) {
        int arrowX = 72;  // 你GUI中箭頭的位置
        int arrowY = 38;

        // 🔥 繪製動畫箭頭！
        arrow.draw(graphics, arrowX, arrowY);
    }

    /**
     * 📝 繪製配方信息文字
     */
    private void drawRecipeInfo(GuiGraphics graphics, Font font, int manaCost, int infusionTime, int inputCount) {
        // 魔力消耗 (上方)
        Component manaText = Component.translatable("jei.koniava.mana_cost", manaCost);
        graphics.drawString(font, manaText, 20, 5, 0x404040, false);

        // 注入時間 (下方)
        Component timeText = Component.translatable("jei.koniava.infusion_time", infusionTime / 20.0f);
        graphics.drawString(font, timeText, 20, RECIPE_HEIGHT - 15, 0x404040, false);

        // 如果需要多個物品，顯示數量
        if (inputCount > 1) {
            Component countText = Component.translatable("jei.koniava.input_count", inputCount);
            graphics.drawString(font, countText, 20, 50, 0x666666, false);
        }
    }

    /**
     * 🔹 處理工具提示 (參考魔力發電機的做法)
     */
    private void handleTooltips(GuiGraphics graphics, Font font, double mouseX, double mouseY,
                                int manaCost, int infusionTime, int inputCount) {

        // 🔵 魔力條 tooltip (使用你GUI中的實際位置)
        if (mouseX >= 8 && mouseX <= 18 && mouseY >= 18 && mouseY <= 66) {
            graphics.renderTooltip(font,
                    List.of(
                            Component.translatable("jei.koniava.mana_cost", manaCost)
                    ),
                    Optional.empty(),
                    (int) mouseX, (int) mouseY);
        }

        // ➡️ 進度箭頭 tooltip (使用你GUI中的實際位置)
        if (mouseX >= 78 && mouseX <= 112 && mouseY >= 35 && mouseY <= 46) {
            graphics.renderTooltip(font,
                    List.of(
                            Component.translatable("jei.koniava.infusion_time", infusionTime / 20.0f)
                    ),
                    Optional.empty(),
                    (int) mouseX, (int) mouseY);
        }

        // 📦 輸入槽 tooltip (如果需要多個物品)
        if (inputCount > 1 && mouseX >= 56 && mouseX <= 74 && mouseY >= 35 && mouseY <= 53) {
            graphics.renderTooltip(font,
                    List.of(Component.translatable("jei.koniava.input_count", inputCount)),
                    Optional.empty(),
                    (int) mouseX, (int) mouseY);
        }
    }
}