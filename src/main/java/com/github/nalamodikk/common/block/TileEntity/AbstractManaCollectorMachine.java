package com.github.nalamodikk.common.block.TileEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 抽象基底：原能收集機器（非燃燒型）
 * 提供固定時間間隔內自動產生魔力（Mana）的基礎邏輯
 */
public abstract class AbstractManaCollectorMachine extends AbstractManaMachineEntityBlock {

    protected int tickCounter = 0;              // 計時器：用來記錄 tick 間隔
    protected final int intervalTick;           // 幾 tick 執行一次產能邏輯
    protected final int manaPerCycle;           // 每次產能的魔力數量

    /**
     * 建構子：設定收集器的最大儲存量、產能間隔與單次產出量
     *
     * @param type BlockEntity 類型註冊用
     * @param pos 區塊座標
     * @param state 方塊狀態
     * @param maxMana 魔力儲存上限
     * @param intervalTick 幾 tick 執行一次產出邏輯
     * @param manaPerCycle 每次產出的魔力量
     */
    public AbstractManaCollectorMachine(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                        int maxMana, int intervalTick, int manaPerCycle) {
        super(type, pos, state, maxMana, 0, 0); // 無能量儲存、無物品槽（非燃燒型）
        this.intervalTick = intervalTick;
        this.manaPerCycle = manaPerCycle;
    }

    /**
     * 每 tick 呼叫一次，機器的主要邏輯由此控制
     */
    @Override
    public void tickMachine() {
        if (level == null || level.isClientSide) return; // 確保只在伺服端執行

        // 如果已經儲存滿了魔力，停止運作
        if (manaStorage.getMana() >= manaStorage.getMaxMana()) {
            isWorking = false;
            return;
        }

        tickCounter++; // 每 tick 累加一次

        if (tickCounter >= intervalTick) {
            tickCounter = 0; // 重置計時器

            if (canGenerate()) { // 由子類別定義是否可以產出魔力（例如天氣、地形）
                manaStorage.addMana(manaPerCycle); // 加入魔力儲存
                isWorking = true; // 設定為工作狀態
                setChanged(); // 通知 Minecraft 狀態有變化（存檔 / render）
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); // 觸發客戶端同步
            } else {
                isWorking = false; // 若不能生產則標記為停止
            }
        }
    }

    /**
     * 子類別需實作此方法來判斷是否符合條件可產能
     * 可根據天氣 / 高度 / 是否能看見天空等自訂
     * @return 是否可以生產魔力
     */
    protected abstract boolean canGenerate();
}
