package com.github.nalamodikk.common.utils.capability;


import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.EnumMap;

public class IOHandlerUtils {

    public enum IOType {
        INPUT, OUTPUT, BOTH, DISABLED;

        public boolean canExtract() {
            return this == INPUT || this == BOTH;
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

    public static void extractManaFromNeighbors(Level level, BlockPos pos, IUnifiedManaHandler selfStorage, EnumMap<Direction, IOType> config, int maxPerSide) {
        for (Direction dir : Direction.values()) {
            if (!canExtract(dir, config)) continue;

            BlockPos neighborPos = pos.relative(dir);
            IUnifiedManaHandler neighbor = CapabilityUtils.getNeighborMana(level, neighborPos, dir);
            if (neighbor == null) continue;

            int simulatedExtract = neighbor.extractMana(maxPerSide, ManaAction.get(true));
            if (simulatedExtract <= 0) continue;

            int simulatedInsert = selfStorage.receiveMana(simulatedExtract, ManaAction.get(true));
            if (simulatedInsert <= 0) continue;

            int extracted = neighbor.extractMana(simulatedInsert, ManaAction.get(false));
            int inserted = selfStorage.receiveMana(extracted, ManaAction.get(false));

            if (inserted > 0 && level.getBlockEntity(pos) instanceof BlockEntity be) {
                be.setChanged(); // ✅ 如果需要觸發儲存
            }
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
}
