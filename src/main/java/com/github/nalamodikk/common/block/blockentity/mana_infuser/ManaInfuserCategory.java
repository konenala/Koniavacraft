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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 🔮 魔力注入機 JEI 配方類別
 *
 * 定義在 JEI 中如何顯示魔力注入配方
 */
public class ManaInfuserCategory implements IRecipeCategory<ManaInfuserRecipe> {

    public static final RecipeType<ManaInfuserRecipe> MANA_INFUSER_TYPE =
            RecipeType.create(KoniavacraftMod.MOD_ID, "mana_infuser", ManaInfuserRecipe.class);

    // JEI 界面材質
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/jei_mana_infuser_gui.png");

    // JEI 類別尺寸
    private static final int WIDTH = 256;
    private static final int HEIGHT = 128;

    // 槽位位置
    private static final int INPUT_SLOT_X = 20;
    private static final int INPUT_SLOT_Y = 20;
    private static final int OUTPUT_SLOT_X = 110;
    private static final int OUTPUT_SLOT_Y = 20;

    // 進度箭頭位置
    private static final int ARROW_X = 60;
    private static final int ARROW_Y = 20;

    private final IDrawableStatic background;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;
    private final Component title;

    public ManaInfuserCategory(IGuiHelper guiHelper) {
        // 🆕 使用你現有的 GUI 材質，截取一部分作為 JEI 背景
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.MANA_INFUSER.get()));

        // 🔧 修正：創建 IDrawableStatic 然後做成動畫
        IDrawableStatic staticArrow = guiHelper.createDrawable(TEXTURE, 176, 52, 24, 11);
        this.arrow = guiHelper.createAnimatedDrawable(staticArrow, 60,
                IDrawableAnimated.StartDirection.LEFT, false);

        this.title = Component.translatable("block.koniava.mana_infuser");
    }

    @Override
    public RecipeType<ManaInfuserRecipe> getRecipeType() {
        return MANA_INFUSER_TYPE;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    // 🆕 新 API：覆寫 getWidth 和 getHeight 而不是 getBackground
    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ManaInfuserRecipe recipe, IFocusGroup focuses) {
        // 輸入槽
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_SLOT_X, INPUT_SLOT_Y)
                .addIngredients(recipe.getInput())
                .setSlotName("input");

        // 輸出槽
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_SLOT_X, OUTPUT_SLOT_Y)
                .addItemStack(recipe.getResult())
                .setSlotName("output");
    }

    @Override
    public void draw(ManaInfuserRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {

        // 繪製背景（如果需要）
        if (background != null) {
            background.draw(guiGraphics, 0, 0);
        }

        // 繪製動畫箭頭
        arrow.draw(guiGraphics, ARROW_X, ARROW_Y);

        // 繪製魔力消耗信息
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.font != null) {
            Component manaText = Component.translatable("tooltip.koniava.mana_infuser.mana_cost",
                    recipe.getManaCost());
            guiGraphics.drawString(minecraft.font, manaText, 5, 5, 0x404040, false);

            // 繪製注入時間
            Component timeText = Component.translatable("tooltip.koniava.mana_infuser.infusion_time",
                    recipe.getInfusionTime());
            guiGraphics.drawString(minecraft.font, timeText, 5, HEIGHT - 15, 0x404040, false);
        }
    }

    // 🆕 現在使用預設的 getRegistryName(T recipe) 實現
    // 不需要覆寫，JEI 會自動從 RecipeHolder 中獲取
}