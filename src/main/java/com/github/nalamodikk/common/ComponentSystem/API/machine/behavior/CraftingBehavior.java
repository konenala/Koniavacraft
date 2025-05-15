package com.github.nalamodikk.common.ComponentSystem.API.machine.behavior;


import com.github.nalamodikk.common.ComponentSystem.API.machine.IComponentBehavior;
import com.github.nalamodikk.common.ComponentSystem.API.machine.IControllableBehavior;
import com.github.nalamodikk.common.ComponentSystem.API.machine.IGridComponent;
import com.github.nalamodikk.common.ComponentSystem.API.machine.grid.ComponentContext;

import com.github.nalamodikk.common.ComponentSystem.API.machine.grid.ComponentGrid;
import com.github.nalamodikk.common.ComponentSystem.recipe.component.*;
import com.github.nalamodikk.common.ComponentSystem.util.AssemblyRecipeUtil;
import com.github.nalamodikk.common.ComponentSystem.util.RecipeUtils;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.capability.IHasMana;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.ComponentSystem.util.helpers.GridIOHelper;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CraftingBehavior implements IComponentBehavior, IControllableBehavior,ICustomRecipeProvider  {
    private int cooldown = 0;
    private boolean enabled = true; // 預設啟用
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onTick(ComponentContext context) {
        Level level = context.getLevel();
        if (level == null || level.isClientSide || !enabled) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        // 收集所有輸入物品（合併自所有輸入端）
        List<ItemStack> inputs = new ArrayList<>();
        List<ItemStackHandler> inputHandlers = GridIOHelper.getAllInputs(context.grid()).stream()
                .map(i -> i.getItemHandler())
                .toList();

        for (ItemStackHandler handler : inputHandlers) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    inputs.add(stack.copy()); // ✅ 深複製避免污染
                }
            }
        }

        if (inputs.isEmpty()) return;

        // 嘗試尋找配方
        List<AssemblyRecipe> recipes = generateRecipes(context.grid());


        AssemblyRecipe recipe = AssemblyRecipeUtil.findMatchingRecipe(recipes, inputs);
        if (recipe == null) {
            LOGGER.debug("[CraftingBehavior] No matching recipe. Inputs = {}",
                    inputs.stream().map(ItemStack::toString).toList());
            return;
        }

        // 嘗試預扣 mana
        int manaRequired = recipe.getManaCost();
        Optional<IHasMana> manaTargetOpt = context.grid().findFirstComponent(IHasMana.class);
        if (manaTargetOpt.isEmpty()) return;

        IHasMana manaTarget = manaTargetOpt.get();
        if (manaTarget.getManaStorage().extractMana(manaRequired, ManaAction.SIMULATE) < manaRequired) {
            return;
        }



        // 材料比對與實際扣除（從所有欄位扣料）
        for (CountedIngredient counted : recipe.getInputItems()) {
            int remaining = counted.getCount();

            for (ItemStackHandler handler : inputHandlers) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (counted.getIngredient().test(stack)) {
                        int remove = Math.min(stack.getCount(), remaining);
                        handler.extractItem(i, remove, false);
                        remaining -= remove;
                        if (remaining <= 0) break;
                    }
                }
                if (remaining <= 0) break;
            }

            if (remaining > 0) {
                int available = counted.getCount() - remaining;

                LOGGER.warn(
                        "❌ Failed to consume required ingredient from grid [{}]: {} x{} (only found {}). Recipe = {}",
                        context.grid().hashCode(),
                        counted.getIngredient(),
                        counted.getCount(),
                        available,
                        recipe.getId()
                );
                return;
            }

        }

        // 扣除 mana（正式）
        manaTarget.getManaStorage().extractMana(manaRequired, ManaAction.EXECUTE);

        // 放入產物
        ItemStack result = recipe.getOutput().copy();

        boolean inserted = GridIOHelper.insertIntoAnyOutputSlot(context.grid(), result);

        if (!inserted) {
            LOGGER.debug("[CraftingBehavior] Failed to insert result into any output slot: {}", result);
            return;
        }

        cooldown = recipe.getCooldownTicks();
        MagicalIndustryMod.LOGGER.info("✅ Crafted {}, Mana used: {}", result.getDisplayName().getString(), manaRequired);



    }

    @Override
    public int getTickRate() {
        return 1; // 每 tick 檢查
    }

    @Override
    public void init(CompoundTag data) {
        this.enabled = data.getBoolean("enabled"); // 如果沒有也預設 true
    }


    @Override
    public void saveToNBT(CompoundTag tag) {
        tag.putBoolean("enabled", enabled);
        tag.putInt("cooldown", cooldown);
    }

    @Override
    public void loadFromNBT(CompoundTag tag) {
        this.enabled = tag.contains("enabled") ? tag.getBoolean("enabled") : true;
        cooldown = tag.getInt("cooldown");
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }


    @Deprecated
    public static boolean matchInputs(List<ItemStack> inputs, List<Ingredient> ingredients) {
        return RecipeUtils.legacyMatchInputs(inputs, ingredients);
    }

    @Override
    public List<AssemblyRecipe> generateRecipes(ComponentGrid grid) {
        List<AssemblyRecipe> result = new ArrayList<>();

        for (IGridComponent component : grid.getAllComponents().values()) {
            if (component instanceof IRecipeContributor contributor) {
                result.addAll(contributor.getLocalRecipes(grid));
            }
        }

        return result;
    }


    @Override
    public ResourceLocation getId() {
        return new ResourceLocation(MagicalIndustryMod.MOD_ID, "crafting");
    }

}
