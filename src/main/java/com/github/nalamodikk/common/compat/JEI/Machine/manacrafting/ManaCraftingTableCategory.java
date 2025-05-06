package com.github.nalamodikk.common.compat.JEI.Machine.manacrafting;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.entity.mana_crafting.ManaCraftingTableBlockEntity;
import com.github.nalamodikk.common.capability.ModCapabilities;
import com.github.nalamodikk.common.register.ModBlocks;
import com.github.nalamodikk.common.register.ModItems;
import com.github.nalamodikk.common.recipe.ManaCraftingTableRecipe;
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
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

public class ManaCraftingTableCategory implements IRecipeCategory<ManaCraftingTableRecipe> {

    public static final ResourceLocation UID = new ResourceLocation(MagicalIndustryMod.MOD_ID, "mana_crafting");
    public static final ResourceLocation TEXTURE = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/mana_crafting_table_gui.png");
    public static final ResourceLocation MANA_BAR_TEXTURE = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/mana_bar_full.png");

    public static final RecipeType<ManaCraftingTableRecipe> MANA_CRAFTING_TYPE =
            new RecipeType<>(UID, ManaCraftingTableRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableStatic manaCostDrawable;

    public ManaCraftingTableCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 25, 15, 120, 60);
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
    public IDrawable getBackground() {
        return this.background;
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
                int xPos = 5 + (i % 3) * 18;
                int yPos = 2 + (i / 3) * 18;
                builder.addSlot(RecipeIngredientRole.INPUT, xPos, yPos).addIngredients(ingredients.get(i));
            }
        } else {
            // 無序合成：橫向擺放
            for (int i = 0; i < ingredients.size(); i++) {
                int xPos = 5 + i * 18;
                builder.addSlot(RecipeIngredientRole.INPUT, xPos, 20).addIngredients(ingredients.get(i));
            }
        }

        // 合成結果槽位
        builder.addSlot(RecipeIngredientRole.OUTPUT, 99, 20).addItemStack(recipe.getResultItem(null));

        // 魔力消耗提示（Catalyst 插槽，實際沒功能，只是 tooltip 顯示）
        builder.addSlot(RecipeIngredientRole.CATALYST, 95, 45)
                .setBackground(this.manaCostDrawable, 0, 0)
                .addItemStack(new ItemStack(ModItems.MANA_DUST.get()))
                .addTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("tooltip.magical_industry.mana_cost", recipe.getManaCost()));
                });
    }

    @Override
    public void draw(ManaCraftingTableRecipe recipe, IRecipeSlotsView slotsView, net.minecraft.client.gui.GuiGraphics graphics, double mouseX, double mouseY) {
        int manaCost = recipe.getManaCost();
        int maxManaBarWidth = 50;

        int barX = 95;
        int barY = 45;
        int barHeight = 8;

        // 畫藍色 mana 條
        int barWidth = Math.min(maxManaBarWidth, (int)((manaCost / (float) ManaCraftingTableBlockEntity.MAX_MANA) * maxManaBarWidth));
        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF00BFFF);


    }

}
