package com.github.nalamodikk.common.block.blockentity.ritual.validator;

import com.github.nalamodikk.common.block.blockentity.ritual.ArcanePedestalBlockEntity;
import com.github.nalamodikk.common.block.blockentity.ritual.RitualRecipe;
import com.github.nalamodikk.register.ModRecipes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;
import java.util.Optional;

/**
 * 儀式材料驗證器：檢查基座祭品是否足夠並找到適用的儀式配方。
 */
public class RitualMaterialValidator {

    public Optional<RitualRecipe> validate(RitualValidationContext context, int availableMana, RecipeManager recipeManager) {
        List<ArcanePedestalBlockEntity> pedestals = context.getPedestals();
        boolean hasEmptyPedestal = pedestals.stream().anyMatch(p -> p.getOffering().isEmpty());
        if (hasEmptyPedestal) {
            context.addError(Component.translatable("message.koniavacraft.ritual.error.missing_ingredients"));
            return Optional.empty();
        }

        List<ItemStack> offerings = context.getPedestalOfferings();
        if (offerings.isEmpty()) {
            context.addError(Component.translatable("message.koniavacraft.ritual.error.missing_ingredients"));
            return Optional.empty();
        }

        var recipeHolders = recipeManager.getAllRecipesFor(ModRecipes.RITUAL_TYPE.get());
        RitualRecipe.RitualInput input = new RitualRecipe.RitualInput(
                offerings,
                availableMana,
                context.getStructureSummary()
        );

        RitualRecipe.StructureRequirementStatus unmetStructure = null;
        for (var holder : recipeHolders) {
            RitualRecipe recipe = holder.value();
            if (!recipe.matchesIngredientsAndMana(input)) {
                continue;
            }
            Optional<RitualRecipe.StructureRequirementStatus> missing =
                    recipe.findFirstUnmetStructureRequirement(input.getAvailableStructure());
            if (missing.isPresent()) {
                if (unmetStructure == null) {
                    unmetStructure = missing.get();
                }
                continue;
            }
            return Optional.of(recipe);
        }

        if (unmetStructure != null) {
            context.addError(Component.translatable(
                    "message.koniavacraft.ritual.error.structure_requirement",
                    describeStructureKey(unmetStructure.key()),
                    unmetStructure.required(),
                    unmetStructure.actual()
            ));
        } else {
            context.addError(Component.translatable("message.koniavacraft.ritual.error.no_recipe"));
        }
        return Optional.empty();
    }

    private Component describeStructureKey(String key) {
        return Component.translatable("structure.koniavacraft." + key);
    }
}
