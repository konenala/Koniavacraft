package com.github.nalamodikk.common.ComponentSystem.API.machine.behavior;


import com.github.nalamodikk.common.ComponentSystem.API.machine.IComponentBehavior;
import com.github.nalamodikk.common.ComponentSystem.API.machine.IControllableBehavior;
import com.github.nalamodikk.common.ComponentSystem.API.machine.grid.ComponentContext;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.capability.IHasMana;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.ComponentSystem.recipe.component.AssemblyRecipe;
import com.github.nalamodikk.common.ComponentSystem.recipe.component.AssemblyRecipeManager;
import com.github.nalamodikk.common.ComponentSystem.util.helpers.GridIOHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public class CraftingBehavior implements IComponentBehavior, IControllableBehavior {
    private int cooldown = 0;
    private boolean enabled = true; // 預設啟用

    @Override
    public void onTick(ComponentContext context) {
        Level level = context.getLevel();
        if (level == null || level.isClientSide) return;

        if (!enabled) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        // 取得所有輸入材料
        List<ItemStack> inputs = new ArrayList<>();
        GridIOHelper.getAllInputs(context.grid()).forEach(input -> {
            ItemStackHandler handler = input.getItemHandler();
            for (int i = 0; i < handler.getSlots(); i++) {
                if (!handler.getStackInSlot(i).isEmpty()) {
                    inputs.add(handler.getStackInSlot(i));
                }
            }
        });

        if (inputs.isEmpty()) return;

        AssemblyRecipe recipe = AssemblyRecipeManager.findMatchingRecipe(inputs);
        if (recipe == null) return;

        // 嘗試扣 mana
        int manaRequired = recipe.getManaCost();
        IHasMana manaTarget = context.grid().findFirstComponent(IHasMana.class);
        if (manaTarget.getManaStorage().extractMana(manaRequired, ManaAction.SIMULATE) < manaRequired) {
            return;
        }

        // 準備扣除材料
        List<ItemStackHandler> inputHandlers = GridIOHelper.getAllInputs(context.grid()).stream()
                .map(i -> i.getItemHandler()).toList();

        List<ItemStack> toRemove = new ArrayList<>(inputs);

        for (ItemStackHandler handler : inputHandlers) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                for (int j = 0; j < toRemove.size(); j++) {
                    ItemStack req = toRemove.get(j);
                    if (ItemStack.isSameItemSameTags(stack, req) && stack.getCount() >= req.getCount()) {
                        handler.extractItem(i, req.getCount(), false);
                        toRemove.remove(j);
                        break;
                    }
                }
                if (toRemove.isEmpty()) break;
            }
            if (toRemove.isEmpty()) break;
        }

        if (!toRemove.isEmpty()) return; // 原料無法完全扣除（應該不會發生）

        // 扣 mana（正式）
        manaTarget.getManaStorage().extractMana(manaRequired, ManaAction.EXECUTE);

        // 丟入產物
        ItemStack result = recipe.getOutput().copy();
        boolean inserted = GridIOHelper.insertIntoAnyOutputSlot(context.grid(), result);
        if (inserted) {
            cooldown = recipe.getCooldownTicks();
            MagicalIndustryMod.LOGGER.info("✅ Crafted {}, Mana used: {}", result.getDisplayName().getString(), manaRequired);
        }
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

}
