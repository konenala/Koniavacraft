package com.github.nalamodikk.common.sync;

import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;

import java.util.ArrayList;
import java.util.List;

// 同步管理器類，負責多個機器的同步
public class MachineSyncManager implements ContainerData {
    private final List<DataSlot> dataSlots = new ArrayList<>();
    private final int[] values;

    public MachineSyncManager(int slotCount) {
        this.values = new int[slotCount];
        for (int i = 0; i < slotCount; i++) {
            final int index = i;
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return values[index];
                }

                @Override
                public void set(int value) {
                    values[index] = value;
                }
            });
        }
    }

    @Override
    public int get(int index) {
        return dataSlots.get(index).get();
    }

    @Override
    public void set(int index, int value) {
        dataSlots.get(index).set(value);
    }

    @Override
    public int getCount() {
        return dataSlots.size();
    }

    public void setDirect(int index, int value) {
        values[index] = value;
    }

    public int getDirect(int index) {
        return values[index];
    }

    private void addDataSlot(DataSlot slot) {
        this.dataSlots.add(slot);
    }

    // 工具方法，用於添加不同類型的數據
    public void addEnergySlot(int[] energyValueHolder) {
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return energyValueHolder[0];
            }

            @Override
            public void set(int value) {
                energyValueHolder[0] = value;
            }
        });
    }

    public void addManaSlot(int[] manaValueHolder) {
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return manaValueHolder[0];
            }

            @Override
            public void set(int value) {
                manaValueHolder[0] = value;
            }
        });
    }
}
