package com.github.nalamodikk.common.utils.capability;


import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class IOHandlerUtils {

    public enum IOType {
        INPUT, OUTPUT, BOTH, DISABLED;

        public boolean canExtract() {
            return this == INPUT || this == BOTH;
        }
        public boolean outputs() {
            return this == OUTPUT || this == BOTH;
        }

        public boolean canInsert() {
            return this == OUTPUT || this == BOTH;
        }
    }

    public static boolean canExtract(Direction dir, EnumMap<Direction, IOType> config) {
        return config.getOrDefault(dir, IOType.DISABLED).canExtract();
    }

    public static boolean canInsert(Direction dir, EnumMap<Direction, IOType> config) {
        return config.getOrDefault(dir, IOType.DISABLED).canInsert();
    }

    public static void extractManaFromNeighbors(Level level, BlockPos pos, IUnifiedManaHandler selfStorage, EnumMap<Direction, IOType> config) {
        if (selfStorage.getManaStored() >= selfStorage.getMaxManaStored()) return;

        long remainingCapacity = selfStorage.getMaxManaStored() - selfStorage.getManaStored();

        // 📌 收集所有可抽取方向，依鄰居的 mana 多寡排序
        List<Map.Entry<Direction, IUnifiedManaHandler>> validSources = new ArrayList<>();

        for (Map.Entry<Direction, IOType> entry : config.entrySet()) {
            Direction dir = entry.getKey();
            if (entry.getValue() != IOType.INPUT) continue;

            BlockPos neighborPos = pos.relative(dir);
            Direction neighborFacing = dir.getOpposite();
            BlockEntity neighborEntity = level.getBlockEntity(neighborPos);
            if (neighborEntity == null) continue;

            if (neighborEntity instanceof IConfigurableBlock configurable) {
                if (configurable.getIOMap().getOrDefault(neighborFacing, IOType.DISABLED) != IOType.OUTPUT) continue;
            }

            IUnifiedManaHandler neighbor = CapabilityUtils.getNeighborMana(level, neighborPos, dir);
            if (neighbor == null || neighbor.getManaStored() <= 0) continue;

            validSources.add(Map.entry(dir, neighbor));
        }

        // 📌 依照鄰居儲存的 mana 倒序排序（多的優先）
        validSources.sort((a, b) -> Long.compare(b.getValue().getManaStored(), a.getValue().getManaStored()));

        for (Map.Entry<Direction, IUnifiedManaHandler> source : validSources) {
            Direction dir = source.getKey();
            IUnifiedManaHandler neighbor = source.getValue();

            long tryExtract = Math.min(remainingCapacity, 50); // 可改為動態計算
            long extracted = neighbor.extractMana((int) tryExtract, ManaAction.get(false));
            if (extracted <= 0) continue;

            long inserted = selfStorage.receiveMana((int) extracted, ManaAction.get(false));
            if (inserted > 0 && level.getBlockEntity(pos) instanceof BlockEntity be) {
                be.setChanged();
                return; // 每 tick 僅抽一面
            }

            remainingCapacity -= inserted;
            if (remainingCapacity <= 0) break;
        }
    }




    public static void extractEnergyFromNeighbors(Level level, BlockPos pos, IEnergyStorage selfStorage, EnumMap<Direction, IOType> config, int maxPerSide) {
        for (Direction dir : Direction.values()) {
            if (!canExtract(dir, config)) continue;

            BlockPos neighborPos = pos.relative(dir);
            IEnergyStorage neighbor = CapabilityUtils.getNeighborEnergy(level, neighborPos, dir);
            if (neighbor == null) continue;

            int simulated = neighbor.extractEnergy(maxPerSide, true);
            if (simulated > 0) {
                int extracted = neighbor.extractEnergy(maxPerSide, false);
                selfStorage.receiveEnergy(extracted, false);
            }
        }
    }

    public static void extractItemsFromNeighbors(Level level, BlockPos pos, IItemHandler selfStorage, EnumMap<Direction, IOType> config) {
        // 範例用：你可以根據需要補進具體抽物品邏輯
    }


    /**
     * 將 Boolean 型方向設定（true 為輸出）轉換成 IOType 設定。
     * true 會變成 OUTPUT，false 會變成 DISABLED。
     */
    public static EnumMap<Direction, IOType> convertBooleanMapToIOType(EnumMap<Direction, Boolean> booleanMap) {
        EnumMap<Direction, IOType> ioMap = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            boolean isOutput = booleanMap.getOrDefault(dir, false);
            ioMap.put(dir, isOutput ? IOType.OUTPUT : IOType.DISABLED);
        }
        return ioMap;
    }

    /**
     * 將 IOType 型方向設定轉換成 Boolean 設定（僅視為輸出/非輸出）。
     * OUTPUT 與 BOTH 會視為 true，其餘為 false。
     */
    public static EnumMap<Direction, Boolean> convertIOTypeToBooleanMap(EnumMap<Direction, IOType> ioMap) {
        EnumMap<Direction, Boolean> booleanMap = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            IOType type = ioMap.getOrDefault(dir, IOType.DISABLED);
            booleanMap.put(dir, type == IOType.OUTPUT || type == IOType.BOTH);
        }
        return booleanMap;
    }



    public static IOType nextIOType(IOType current) {
        return switch (current) {
            case DISABLED -> IOType.OUTPUT;
            case OUTPUT -> IOType.INPUT;
            case INPUT -> IOType.BOTH;
            case BOTH -> IOType.DISABLED;
        };
    }

}
