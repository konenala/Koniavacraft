
package com.github.nalamodikk.common.block.mana_generator.logic;

import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.register.ModCapability;
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

    public static void tryOutput(ServerLevel level, BlockPos origin, ManaStorage manaStorage, IEnergyStorage energyStorage, EnumMap<Direction, Boolean> directionConfig) {
        List<IUnifiedManaHandler> manaTargets = new ArrayList<>();
        List<Integer> manaDemands = new ArrayList<>();
        int totalManaDemand = 0;

        List<IEnergyStorage> energyTargets = new ArrayList<>();
        List<Integer> energyDemands = new ArrayList<>();
        int totalEnergyDemand = 0;

        for (Direction dir : Direction.values()) {
            if (!directionConfig.getOrDefault(dir, false)) continue;

            BlockPos targetPos = origin.relative(dir);
            Direction inputSide = dir.getOpposite();

            // 魔力接收端
            IUnifiedManaHandler manaTarget = BlockCapabilityCache.create(ModCapability.MANA, level, targetPos, inputSide).getCapability();
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

        // 魔力輸出
        if (totalManaDemand > 0) {
            int totalToSend = Math.min(manaStorage.getManaStored(), MAX_OUTPUT_PER_TICK);
            for (int i = 0; i < manaTargets.size(); i++) {
                int portion = (int) Math.round(totalToSend * (manaDemands.get(i) / (double) totalManaDemand));
                int accepted = manaTargets.get(i).receiveMana(portion, ManaAction.EXECUTE);
                manaStorage.extractMana(accepted, ManaAction.EXECUTE);
            }
        }

        // 能量輸出
        if (totalEnergyDemand > 0) {
            int totalToSend = Math.min(energyStorage.getEnergyStored(), MAX_OUTPUT_PER_TICK);
            for (int i = 0; i < energyTargets.size(); i++) {
                int portion = (int) Math.round(totalToSend * (energyDemands.get(i) / (double) totalEnergyDemand));
                int accepted = energyTargets.get(i).receiveEnergy(portion, false);
                energyStorage.extractEnergy(accepted, false);
            }
        }
    }

}
