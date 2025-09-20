package com.github.nalamodikk.common.block.blockentity.ritual;

import com.github.nalamodikk.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 奧術基座方塊實體 - 儀式祭品展示台
 * 負責：
 * 1. 存儲單個祭品物品
 * 2. 提供物品渲染數據
 * 3. 參與儀式驗證
 */
public class ArcanePedestalBlockEntity extends BlockEntity {
    
    private ItemStack storedItem = ItemStack.EMPTY;
    private boolean isConsumed = false; // 是否已被儀式消耗
    
    // 渲染相關
    private float itemRotation = 0.0f;
    private float itemHover = 0.0f;
    private int tickCount = 0;
    
    public ArcanePedestalBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ARCANE_PEDESTAL_BE.get(), pos, blockState);
    }

    public void tick() {
        if (level == null) return;
        
        tickCount++;
        
        // 客戶端渲染動畫
        if (level.isClientSide() && !storedItem.isEmpty()) {
            // 旋轉動畫
            itemRotation += 2.0f;
            if (itemRotation >= 360.0f) {
                itemRotation -= 360.0f;
            }
            
            // 上下浮動動畫
            itemHover = (float) Math.sin(tickCount * 0.1f) * 0.1f;
        }
        
        // 服務端邏輯
        if (!level.isClientSide()) {
            // 檢查是否需要參與儀式
            checkForRitualParticipation();
        }
    }

    /**
     * 檢查是否需要參與儀式
     */
    private void checkForRitualParticipation() {
        // TODO: 檢查附近的儀式核心，如果有正在進行的儀式，參與其中
        // 這將在實現儀式邏輯時完善
    }

    /**
     * 插入物品到基座
     */
    public ItemStack insertItem(ItemStack stack) {
        if (storedItem.isEmpty() && !stack.isEmpty()) {
            storedItem = stack.copy();
            storedItem.setCount(1); // 基座只能存放一個物品
            isConsumed = false;
            setChanged();
            
            // 返回剩余物品
            ItemStack remainder = stack.copy();
            remainder.shrink(1);
            return remainder.isEmpty() ? ItemStack.EMPTY : remainder;
        }
        return stack; // 無法插入，返回原物品
    }

    /**
     * 從基座提取物品
     */
    public ItemStack extractItem() {
        if (!storedItem.isEmpty() && !isConsumed) {
            ItemStack extracted = storedItem.copy();
            storedItem = ItemStack.EMPTY;
            isConsumed = false;
            setChanged();
            return extracted;
        }
        return ItemStack.EMPTY;
    }

    /**
     * 消耗基座上的物品（用於儀式）
     */
    public ItemStack consumeItem() {
        if (!storedItem.isEmpty() && !isConsumed) {
            ItemStack consumed = storedItem.copy();
            isConsumed = true;
            setChanged();
            return consumed;
        }
        return ItemStack.EMPTY;
    }

    /**
     * 重置消耗狀態（儀式失敗時）
     */
    public void resetConsumption() {
        if (isConsumed) {
            isConsumed = false;
            setChanged();
        }
    }

    /**
     * 獲取存儲的物品（不消耗）
     */
    public ItemStack getStoredItem() {
        return storedItem.copy();
    }

    /**
     * 檢查是否有可用的物品
     */
    public boolean hasAvailableItem() {
        return !storedItem.isEmpty() && !isConsumed;
    }

    /**
     * 檢查是否包含特定物品
     */
    public boolean containsItem(ItemStack compareStack) {
        return !storedItem.isEmpty() && 
               ItemStack.isSameItem(storedItem, compareStack);
    }

    /**
     * 掉落內容物
     */
    public void dropContents(Level level, BlockPos pos) {
        if (!storedItem.isEmpty() && !isConsumed) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), storedItem);
        }
        storedItem = ItemStack.EMPTY;
        isConsumed = false;
    }

    // 渲染相關 Getters
    public float getItemRotation() { return itemRotation; }
    public float getItemHover() { return itemHover; }
    public boolean isItemConsumed() { return isConsumed; }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        
        if (!storedItem.isEmpty()) {
            tag.put("StoredItem", storedItem.saveOptional(registries));
        }
        tag.putBoolean("IsConsumed", isConsumed);
        tag.putFloat("ItemRotation", itemRotation);
        tag.putFloat("ItemHover", itemHover);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        
        if (tag.contains("StoredItem")) {
            storedItem = ItemStack.parseOptional(registries, tag.getCompound("StoredItem"));
        } else {
            storedItem = ItemStack.EMPTY;
        }
        
        isConsumed = tag.getBoolean("IsConsumed");
        itemRotation = tag.getFloat("ItemRotation");
        itemHover = tag.getFloat("ItemHover");
    }
}