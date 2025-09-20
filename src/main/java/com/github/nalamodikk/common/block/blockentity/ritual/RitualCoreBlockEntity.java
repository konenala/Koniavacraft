package com.github.nalamodikk.common.block.blockentity.ritual;

import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.register.ModBlockEntities;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 儀式核心方塊實體 - 儀式系統的中央控制器
 * 負責：
 * 1. 儀式結構驗證
 * 2. 儀式配方匹配
 * 3. 儀式執行邏輯
 * 4. 魔力管理
 * 5. 產物生成
 */
public class RitualCoreBlockEntity extends BlockEntity {
    
    // 魔力儲存
    private final ManaStorage manaStorage;
    
    // 儀式狀態
    private RitualState state = RitualState.IDLE;
    private int ritualProgress = 0;
    private int maxRitualTime = 0;
    
    // 儀式結果儲存
    private NonNullList<ItemStack> resultItems = NonNullList.withSize(9, ItemStack.EMPTY);
    
    // 當前儀式配方信息
    private String currentRitualId = "";
    private int currentManaCost = 0;
    
    public RitualCoreBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.RITUAL_CORE_BE.get(), pos, blockState);
        this.manaStorage = new ManaStorage(1000000); // 1M 魔力容量
    }

    public void tick() {
        if (level == null || level.isClientSide()) return;
        
        switch (state) {
            case IDLE -> {
                // 空閒狀態 - 等待啟動
            }
            case PREPARING -> {
                // 準備階段 - 驗證結構和材料
                if (validateRitualStructure() && validateRitualMaterials()) {
                    setState(RitualState.RUNNING);
                    startRitualEffects();
                } else {
                    setState(RitualState.FAILED);
                }
            }
            case RUNNING -> {
                // 執行階段 - 進行儀式
                progressRitual();
            }
            case COMPLETED -> {
                // 完成階段 - 生成產物
                generateRitualResults();
                setState(RitualState.IDLE);
            }
            case FAILED -> {
                // 失敗階段 - 清理和重置
                cleanupFailedRitual();
                setState(RitualState.IDLE);
            }
        }
    }

    /**
     * 嘗試開始儀式
     */
    public boolean attemptStartRitual(Player player, ItemStack catalyst) {
        if (state != RitualState.IDLE) {
            return false;
        }
        
        // 驗證催化劑
        if (!isValidCatalyst(catalyst)) {
            return false;
        }
        
        // 消耗催化劑
        catalyst.shrink(1);
        
        // 進入準備階段
        setState(RitualState.PREPARING);
        return true;
    }

    /**
     * 驗證催化劑
     */
    private boolean isValidCatalyst(ItemStack stack) {
        return stack.is(ModItems.RESONANT_CRYSTAL.get()) || 
               stack.is(ModItems.VOID_PEARL.get());
    }

    /**
     * 驗證儀式結構
     */
    private boolean validateRitualStructure() {
        // TODO: 實現結構驗證邏輯
        // 檢查周圍的基座、魔力塔、符文石等
        return true; // 暫時返回 true
    }

    /**
     * 驗證儀式材料
     */
    private boolean validateRitualMaterials() {
        // TODO: 實現材料驗證邏輯
        // 檢查基座上的祭品是否符合配方要求
        return true; // 暫時返回 true
    }

    /**
     * 開始儀式特效
     */
    private void startRitualEffects() {
        // TODO: 實現粒子特效和音效
        maxRitualTime = 200; // 10秒儀式時間
        ritualProgress = 0;
    }

    /**
     * 推進儀式進度
     */
    private void progressRitual() {
        ritualProgress++;
        
        // 每秒消耗魔力
        if (ritualProgress % 20 == 0) {
            if (manaStorage.extractMana(currentManaCost / 10, ManaAction.EXECUTE) == 0) {
                // 魔力不足，儀式失敗
                setState(RitualState.FAILED);
                return;
            }
        }
        
        // 儀式完成
        if (ritualProgress >= maxRitualTime) {
            setState(RitualState.COMPLETED);
        }
    }

    /**
     * 生成儀式結果
     */
    private void generateRitualResults() {
        // TODO: 根據配方生成產物
        // 暫時生成一個測試物品
        resultItems.set(0, new ItemStack(ModItems.MANA_DUST.get(), 4));
        setChanged();
    }

    /**
     * 清理失敗的儀式
     */
    private void cleanupFailedRitual() {
        ritualProgress = 0;
        maxRitualTime = 0;
        currentRitualId = "";
        currentManaCost = 0;
    }

    /**
     * 設置儀式狀態
     */
    private void setState(RitualState newState) {
        this.state = newState;
        setChanged();
    }

    /**
     * 掉落內容物
     */
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : resultItems) {
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        resultItems.clear();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        
        tag.put("ManaStorage", manaStorage.serializeNBT(registries));
        tag.putString("State", state.name());
        tag.putInt("Progress", ritualProgress);
        tag.putInt("MaxTime", maxRitualTime);
        tag.putString("RitualId", currentRitualId);
        tag.putInt("ManaCost", currentManaCost);
        
        // 保存結果物品
        for (int i = 0; i < resultItems.size(); i++) {
            if (!resultItems.get(i).isEmpty()) {
                tag.put("ResultItem" + i, resultItems.get(i).saveOptional(registries));
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        
        if (tag.contains("ManaStorage")) {
            manaStorage.deserializeNBT(registries, tag.getCompound("ManaStorage"));
        }
        
        state = RitualState.valueOf(tag.getString("State"));
        ritualProgress = tag.getInt("Progress");
        maxRitualTime = tag.getInt("MaxTime");
        currentRitualId = tag.getString("RitualId");
        currentManaCost = tag.getInt("ManaCost");
        
        // 加載結果物品
        resultItems.clear();
        for (int i = 0; i < 9; i++) {
            if (tag.contains("ResultItem" + i)) {
                resultItems.set(i, ItemStack.parseOptional(registries, tag.getCompound("ResultItem" + i)));
            } else {
                resultItems.set(i, ItemStack.EMPTY);
            }
        }
    }

    /**
     * 靜態Tick方法（供方塊調用）
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, RitualCoreBlockEntity blockEntity) {
        blockEntity.tick();
    }

    /**
     * 檢查儀式是否活躍
     */
    public boolean isRitualActive() {
        return state == RitualState.RUNNING || state == RitualState.PREPARING;
    }

    /**
     * 開始儀式（簡化版本）
     */
    public void startRitual() {
        if (state == RitualState.IDLE) {
            setState(RitualState.PREPARING);
        }
    }

    // Getters
    public RitualState getState() { return state; }
    public int getRitualProgress() { return ritualProgress; }
    public int getMaxRitualTime() { return maxRitualTime; }
    public ManaStorage getManaStorage() { return manaStorage; }

    /**
     * 儀式狀態枚舉
     */
    public enum RitualState {
        IDLE,        // 空閒
        PREPARING,   // 準備中
        RUNNING,     // 執行中
        COMPLETED,   // 已完成
        FAILED       // 已失敗
    }
}