
package com.github.nalamodikk.common.block.mana_generator.logic;

import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.register.ModCapabilities;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import com.github.nalamodikk.common.capability.ManaStorage;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class OutputHandler {

    private static final int MAX_OUTPUT_PER_TICK = 40;

    public static boolean tryOutput(ServerLevel level, BlockPos pos, ManaStorage manaStorage, IEnergyStorage energyStorage, EnumMap<Direction, IOHandlerUtils.IOType> ioMap) {
        List<IUnifiedManaHandler> manaTargets = new ArrayList<>();
        List<Integer> manaDemands = new ArrayList<>();
        int totalManaDemand = 0;

        List<IEnergyStorage> energyTargets = new ArrayList<>();
        List<Integer> energyDemands = new ArrayList<>();
        int totalEnergyDemand = 0;

        for (Direction dir : Direction.values()) {
            IOHandlerUtils.IOType type = ioMap.getOrDefault(dir, IOHandlerUtils.IOType.DISABLED);
            if (!type.outputs()) continue;

            BlockPos targetPos = pos.relative(dir);
            Direction inputSide = dir.getOpposite();

            // 魔力接收端
            IUnifiedManaHandler manaTarget = BlockCapabilityCache.create(ModCapabilities.MANA, level, targetPos, inputSide).getCapability();
            if (manaTarget != null && manaStorage != null && manaTarget.canReceive()) {
                int demand = manaTarget.getMaxManaStored() - manaTarget.getManaStored();
                if (demand > 0) {
                    manaTargets.add(manaTarget);
                    manaDemands.add(demand);
                    totalManaDemand += demand;
                }
            }

            // 能量接收端
            IEnergyStorage energyTarget = BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, level, targetPos, inputSide).getCapability();
            if (energyTarget != null && energyStorage != null && energyTarget.canReceive()) {
                int demand = energyTarget.getMaxEnergyStored() - energyTarget.getEnergyStored();
                if (demand > 0) {
                    energyTargets.add(energyTarget);
                    energyDemands.add(demand);
                    totalEnergyDemand += demand;
                }
            }
        }

        boolean didOutput = false;

        // 魔力輸出
        if (manaStorage != null && totalManaDemand > 0 && manaStorage.getManaStored() > 0) {
            int totalToSend = Math.min(manaStorage.getManaStored(), MAX_OUTPUT_PER_TICK);
            for (int i = 0; i < manaTargets.size(); i++) {
                int portion = (int) Math.round(totalToSend * (manaDemands.get(i) / (double) totalManaDemand));
                int accepted = manaTargets.get(i).receiveMana(portion, ManaAction.EXECUTE);
                if (accepted > 0) {
                    manaStorage.extractMana(accepted, ManaAction.EXECUTE);
                    didOutput = true;
                }
            }
        }

        // 能量輸出
        if (energyStorage != null && totalEnergyDemand > 0 && energyStorage.getEnergyStored() > 0) {
            int totalToSend = Math.min(energyStorage.getEnergyStored(), MAX_OUTPUT_PER_TICK);
            for (int i = 0; i < energyTargets.size(); i++) {
                int portion = (int) Math.round(totalToSend * (energyDemands.get(i) / (double) totalEnergyDemand));
                int accepted = energyTargets.get(i).receiveEnergy(portion, false);
                if (accepted > 0) {
                    energyStorage.extractEnergy(accepted, false);
                    didOutput = true;
                }
            }
        }

        return didOutput;
    }


    public static class OutputThrottleController {
        private int noOutputStreak = 0;
        private int currentDelay = 0;

        public boolean shouldTryOutput() {
            return currentDelay-- <= 0;
        }

        public void recordOutputResult(boolean success) {
            if (success) {
                noOutputStreak = 0;
                currentDelay = 1; // 成功後下次仍會馬上再嘗試一次
            } else {
                noOutputStreak++;
                currentDelay = Math.min(10, noOutputStreak); // 每失敗一次就延長下一次的 delay（最多10）
            }
        }
    }


}
