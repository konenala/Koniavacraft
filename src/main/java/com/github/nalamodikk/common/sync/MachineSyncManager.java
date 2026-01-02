package com.github.nalamodikk.common.sync;

import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 升級版的同步管理器。
 * 支援動態註冊不同類型的數據（int, float, boolean, long），並自動管理索引。
 * 
 * 用法範例：
 * MachineSyncManager sync = new MachineSyncManager();
 * sync.trackInt(energy::getEnergyStored, energy::setEnergyStored);
 * sync.trackFloat(() -> temperature, val -> temperature = val);
 */
public class MachineSyncManager implements ContainerData {
    
    private final List<ISyncableData> syncables = new ArrayList<>();
    private int totalIntCount = 0;
    private boolean isDirty = false;

    public boolean isDirty() {
        return isDirty;
    }
    
    public void markDirty(boolean dirty) {
        this.isDirty = dirty;
    }

    // --- 註冊方法 ---

    /**
     * 追蹤一個整數 (佔用 1 個 slot)
     */
    public void trackInt(Supplier<Integer> getter, Consumer<Integer> setter) {
        syncables.add(new IntSyncData(getter, setter, totalIntCount, this));
        totalIntCount += 1;
    }

    /**
     * 追蹤一個布林值 (佔用 1 個 slot，0 或 1)
     */
    public void trackBoolean(Supplier<Boolean> getter, Consumer<Boolean> setter) {
        syncables.add(new BooleanSyncData(getter, setter, totalIntCount, this));
        totalIntCount += 1;
    }

    /**
     * 追蹤一個浮點數 (佔用 1 個 slot，透過 Float.floatToIntBits 轉換)
     * 注意：這會失去一點點精度，但對於 GUI 顯示通常足夠。
     */
    public void trackFloat(Supplier<Float> getter, Consumer<Float> setter) {
        syncables.add(new FloatSyncData(getter, setter, totalIntCount, this));
        totalIntCount += 1;
    }

    /**
     * 追蹤一個長整數 (佔用 2 個 slot，拆分為高低位)
     * 適用於大數值能量或極大數量的物品。
     */
    public void trackLong(Supplier<Long> getter, Consumer<Long> setter) {
        syncables.add(new LongSyncData(getter, setter, totalIntCount, this));
        totalIntCount += 2;
    }
    
    /**
     * 追蹤一個 Enum (佔用 1 個 slot，存 ordinal)
     */
    public <E extends Enum<E>> void trackEnum(Supplier<E> getter, Consumer<E> setter, E[] values) {
        syncables.add(new IntSyncData(
            () -> getter.get().ordinal(), 
            val -> setter.accept(values[Math.max(0, Math.min(val, values.length - 1))]), 
            totalIntCount,
            this
        ));
        totalIntCount += 1;
    }

    // --- ContainerData 實作 ---

    @Override
    public int get(int index) {
        // 找到負責這個 index 的 syncable
        for (ISyncableData data : syncables) {
            if (index >= data.getStartIndex() && index < data.getStartIndex() + data.getSize()) {
                return data.get(index - data.getStartIndex());
            }
        }
        return 0;
    }

    @Override
    public void set(int index, int value) {
        for (ISyncableData data : syncables) {
            if (index >= data.getStartIndex() && index < data.getStartIndex() + data.getSize()) {
                data.set(index - data.getStartIndex(), value);
                return;
            }
        }
    }

    @Override
    public int getCount() {
        return totalIntCount;
    }

    // --- 內部介面與實作 ---

    private interface ISyncableData {
        int get(int localIndex);
        void set(int localIndex, int value);
        int getStartIndex();
        int getSize();
    }

    private static class IntSyncData implements ISyncableData {
        private final Supplier<Integer> getter;
        private final Consumer<Integer> setter;
        private final int startIndex;
        private final MachineSyncManager manager;

        public IntSyncData(Supplier<Integer> getter, Consumer<Integer> setter, int startIndex, MachineSyncManager manager) {
            this.getter = getter;
            this.setter = setter;
            this.startIndex = startIndex;
            this.manager = manager;
        }

        @Override public int get(int i) { return getter.get(); }
        @Override public void set(int i, int v) { 
            if (getter.get() != v) {
                setter.accept(v); 
                manager.markDirty(true);
            }
        }
        @Override public int getStartIndex() { return startIndex; }
        @Override public int getSize() { return 1; }
    }

    private static class BooleanSyncData implements ISyncableData {
        private final Supplier<Boolean> getter;
        private final Consumer<Boolean> setter;
        private final int startIndex;
        private final MachineSyncManager manager;

        public BooleanSyncData(Supplier<Boolean> getter, Consumer<Boolean> setter, int startIndex, MachineSyncManager manager) {
            this.getter = getter;
            this.setter = setter;
            this.startIndex = startIndex;
            this.manager = manager;
        }

        @Override public int get(int i) { return getter.get() ? 1 : 0; }
        @Override public void set(int i, int v) { 
            boolean newVal = v != 0;
            if (getter.get() != newVal) {
                setter.accept(newVal); 
                manager.markDirty(true);
            }
        }
        @Override public int getStartIndex() { return startIndex; }
        @Override public int getSize() { return 1; }
    }

    private static class FloatSyncData implements ISyncableData {
        private final Supplier<Float> getter;
        private final Consumer<Float> setter;
        private final int startIndex;
        private final MachineSyncManager manager;

        public FloatSyncData(Supplier<Float> getter, Consumer<Float> setter, int startIndex, MachineSyncManager manager) {
            this.getter = getter;
            this.setter = setter;
            this.startIndex = startIndex;
            this.manager = manager;
        }

        @Override public int get(int i) { return Float.floatToIntBits(getter.get()); }
        @Override public void set(int i, int v) { 
            float newVal = Float.intBitsToFloat(v);
            if (Math.abs(getter.get() - newVal) > 0.0001f) {
                setter.accept(newVal);
                manager.markDirty(true);
            }
        }
        @Override public int getStartIndex() { return startIndex; }
        @Override public int getSize() { return 1; }
    }
    
    private static class LongSyncData implements ISyncableData {
        private final Supplier<Long> getter;
        private final Consumer<Long> setter;
        private final int startIndex;
        private final MachineSyncManager manager;

        public LongSyncData(Supplier<Long> getter, Consumer<Long> setter, int startIndex, MachineSyncManager manager) {
            this.getter = getter;
            this.setter = setter;
            this.startIndex = startIndex;
            this.manager = manager;
        }

        @Override 
        public int get(int i) { 
            long val = getter.get();
            if (i == 0) return (int)(val & 0xFFFFFFFFL); // 低位
            else return (int)((val >>> 32) & 0xFFFFFFFFL); // 高位
        }
        
        @Override 
        public void set(int i, int v) { 
            long current = getter.get();
            long newVal = current;
            if (i == 0) {
                long high = current & 0xFFFFFFFF00000000L;
                long low = Integer.toUnsignedLong(v);
                newVal = high | low;
            } else {
                long low = current & 0xFFFFFFFFL;
                long high = ((long)v) << 32;
                newVal = high | low;
            }
            if (current != newVal) {
                setter.accept(newVal);
                manager.markDirty(true);
            }
        }
        
        @Override public int getStartIndex() { return startIndex; }
        @Override public int getSize() { return 2; }
    }

    // 保留舊方法以相容（如果需要的話），但建議使用新方法
    @Deprecated
    public void addEnergySlot(int[] energyValueHolder) {
        trackInt(() -> energyValueHolder[0], v -> energyValueHolder[0] = v);
    }
}
