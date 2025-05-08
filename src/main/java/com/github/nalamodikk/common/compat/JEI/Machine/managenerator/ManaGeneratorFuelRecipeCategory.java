package com.github.nalamodikk.common.compat.JEI.Machine.managenerator;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.recipe.fuel.ManaGenFuelRecipe;
import com.github.nalamodikk.common.register.ModBlocks;
import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ManaGeneratorFuelRecipeCategory implements IRecipeCategory<ManaGenFuelRecipe> {
    public static final ResourceLocation UID = new ResourceLocation(MagicalIndustryMod.MOD_ID, "mana_fuel");
    private static final ResourceLocation TEXTURE = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/jei_fuel.png");
    private static final ResourceLocation MANA_BAR = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/mana_bar_full.png");
    private static final ResourceLocation ENERGY_BAR = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/energy_bar_full.png");

    public static final RecipeType<ManaGenFuelRecipe> RECIPE_TYPE = new RecipeType<>(UID, ManaGenFuelRecipe.class);
    private static final Logger LOGGER = LoggerFactory.getLogger(ManaGeneratorFuelRecipeCategory.class);

    private final IDrawableStatic background;
    private final IDrawable icon; // ✅ 修改類型為 IDrawable

    public ManaGeneratorFuelRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 182, 80); // 確保與圖片大小一致
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.MANA_GENERATOR.get()));
    }

    @Override
    public RecipeType<ManaGenFuelRecipe> getRecipeType() {
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
    public void setRecipe(IRecipeLayoutBuilder builder, ManaGenFuelRecipe recipe, IFocusGroup focuses) {
        ResourceLocation itemResource = recipe.getItemResource();
        ItemStack inputStack = new ItemStack(BuiltInRegistries.ITEM.get(itemResource));

        if (!inputStack.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 80, 40)
                    .addItemStack(inputStack);
        } else {
            LOGGER.error("[JEI] ❌ 找不到物品 {}，無法添加到 FuelRecipeCategory！", itemResource);
        }
    }

    @Override
    public void draw(ManaGenFuelRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        graphics.blit(TEXTURE, 0, 0, 0, 0, 182, 80);
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        int manaRate = recipe.getManaRate();     // 魔力消耗/t
        int energyRate = recipe.getEnergyRate(); // 能量產出/t
        int burnTime = recipe.getBurnTime(); // ← 你要從這裡取
        Component burnTimeText = Component.translatable("jei.magical_industry.burn_time", burnTime);
        graphics.drawString(font, burnTimeText, 45, 27, 0x555555, false);
        double efficiency = manaRate > 0 ? (double) energyRate / manaRate : 0;

    }



    @Override
    public List<Component> getTooltipStrings(ManaGenFuelRecipe recipe, IRecipeSlotsView slotsView, double mouseX, double mouseY) {
        List<Component> tooltips = new ArrayList<>();

        // 🔹 魔力條區域 (x: 10 ~ 30, y: 10 ~ 70)
        if (mouseX >= 8 && mouseX <= 19 && mouseY >= 10 && mouseY <= 70) {
            tooltips.add(Component.translatable("jei.magical_industry.fuel.mana", recipe.getManaRate()));
        }

        // 🔹 能量條區域 (x: 150 ~ 170, y: 10 ~ 70)
        if (mouseX >= 150 && mouseX <= 170 && mouseY >= 10 && mouseY <= 70) {
            tooltips.add(Component.translatable("jei.magical_industry.fuel.energy", recipe.getEnergyRate()));
        }

        return tooltips;
    }


    public static ResourceLocation getTexture() {
        return TEXTURE;
    }

}
