package com.github.nalamodikk.common.compat.JEI.Machine.managenerator;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.recipe.fuel.FuelRecipe;
import com.mojang.blaze3d.systems.RenderSystem;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class FuelRecipeCategory implements IRecipeCategory<FuelRecipe> {
    public static final ResourceLocation UID = new ResourceLocation(MagicalIndustryMod.MOD_ID, "fuel");
    private static final ResourceLocation TEXTURE = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/jei_fuel.png");
    public static final RecipeType<FuelRecipe> RECIPE_TYPE = new RecipeType<>(UID, FuelRecipe.class);

    private final IDrawable background;
    private final IDrawable icon; // ✅ 修改類型為 IDrawable

    public FuelRecipeCategory(IDrawableStatic background, IDrawable icon) {
        this.background = background;
        this.icon = icon;
    }

    @Override
    public RecipeType<FuelRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.magical_industry.fuel");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FuelRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 10)
                .addItemStack(new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(recipe.getItemId()))));
    }


    @Override
    public void draw(FuelRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        graphics.blit(TEXTURE, 0, 0, 0, 0, 176, 85);

        // 取得 Minecraft 字型
        Minecraft mc = Minecraft.getInstance();

        // 使用本地化鍵
        graphics.drawString(mc.font, Component.translatable("jei.magical_industry.fuel.mana", recipe.getManaRate()), 100, 15, 0xFFFFFF, false);
        graphics.drawString(mc.font, Component.translatable("jei.magical_industry.fuel.energy", recipe.getEnergyRate()), 100, 25, 0xFFFFFF, false);
        graphics.drawString(mc.font, Component.translatable("jei.magical_industry.fuel.burn_time", recipe.getBurnTime()), 100, 35, 0xFFFFFF, false);
    }

    public static ResourceLocation getTexture() {
        return TEXTURE;
    }

}
