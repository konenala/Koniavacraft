package com.github.nalamodikk.common.ComponentSystem.API.machine;


import com.github.nalamodikk.common.ComponentSystem.API.machine.grid.ComponentContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * 所有模組行為都要實作這個介面
 * 它提供一個 onTick 方法，每 tick 會被呼叫
 */
public interface IComponentBehavior {

    default ResourceLocation getId() {
        // 預設回傳 null（讓你 IDE 提示你應該要實作）
        return null;
    }
    /**
     * 每個 tick 都會對模組呼叫這個方法
     *
     * @param context   模組的執行上下文（包含 grid、位置、self）
     */
    void onTick(ComponentContext context);
    /**
     * @return 每幾 tick 執行一次。預設為 1（每 tick 執行），可覆寫回傳更高數值以降低頻率。
     */
    default int getTickRate() {
        return 1;
    }

    default void init(CompoundTag data) {
        // 可被子類覆寫的初始化方法
    }


    default void saveToNBT(CompoundTag tag) {
        // 可被子類覆寫的儲存方法
    }

    default void loadFromNBT(CompoundTag tag) {
        // 可被子類覆寫的讀取方法
    }

}
