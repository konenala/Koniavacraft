package com.github.nalamodikk.common.block.manabase;

import com.github.nalamodikk.common.capability.ManaStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 專用於純魔力收集器類型的基底類別，不支援能量 / 物品 / 流體。
 * 抽象魔力收集器機器基底。
 * 僅具備魔力儲存與簡單生成邏輯。
 */
public abstract class AbstractManaCollectorBlock extends AbstractManaMachineEntityBlock {

    /** 魔力儲存槽 */
    protected final ManaStorage manaStorage;

    /** 當前是否正在工作中 */
    protected boolean isWorking = false;

    /** 產魔間隔（tick） */
    protected final int intervalTick;

    /** 每次產生魔力量 */
    protected final int manaPerCycle;

    /** Ticking 計數器 */
    protected int tickCounter = 0;

    /**
     * @param type BlockEntityType
     * @param pos  方塊位置
     * @param state 方塊狀態
     * @param maxMana 最大魔力量
     * @param intervalTick 每幾 tick 產出一次
     * @param manaPerCycle 每次產出的魔力量
     */
    public AbstractManaCollectorBlock(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                      int maxMana, int intervalTick, int manaPerCycle) {
        // 呼叫 父類 禁用能源
        super(type, pos, state, false,0, maxMana, intervalTick, manaPerCycle);
        this.manaStorage = new ManaStorage(maxMana);
        this.intervalTick = intervalTick;
        this.manaPerCycle = manaPerCycle;
    }

    @Override
    public void tickMachine() {
        if (level == null || level.isClientSide || manaStorage == null) return;

        if (manaStorage.getManaStored() >= manaStorage.getMaxManaStored()) {
            isWorking = false;
            return;
        }

        tickCounter++;

        if (tickCounter >= intervalTick) {
            tickCounter = 0;

            if (canGenerate()) {
                int amount = computeManaAmount();
                manaStorage.addMana(amount);

                isWorking = true;
                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                onGenerate(amount);

            } else {
                isWorking = false;
            }
        }
    }

    protected boolean canGenerate() {
        return true; // 預設可產生，子類可覆寫
    }


    protected void onGenerate(int amount) {
        // 預設無動作，子類可覆寫產生粒子或音效
    }

    public ManaStorage getManaStorage() {
        return manaStorage;
    }

    public boolean isWorking() {
        return isWorking;
    }

    @Override
    protected ItemStackHandler createHandler() {
        return null; // 禁用 item 處理
    }

    @Override
    protected FluidTank createFluidTank() {
        return null; // 禁用 fluid 處理
    }


}
