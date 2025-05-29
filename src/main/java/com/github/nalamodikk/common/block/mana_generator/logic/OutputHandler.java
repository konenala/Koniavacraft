
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

import java.util.EnumMap;

public class OutputHandler {

    private static final int MAX_OUTPUT_PER_TICK = 40;

    public static void tryOutput(ServerLevel level,
                                 BlockPos origin,
                                 ManaStorage manaStorage,
                                 IEnergyStorage energyStorage,
                                 EnumMap<Direction, Boolean> directionConfig) {

        for (Direction dir : Direction.values()) {
            if (!directionConfig.getOrDefault(dir, false)) continue;

            BlockPos targetPos = origin.relative(dir);

            // 能量輸出
            IEnergyStorage energyTarget = BlockCapabilityCache.create(
                    Capabilities.EnergyStorage.BLOCK, level, targetPos, dir.getOpposite()
            ).getCapability();

            if (energyTarget != null && energyStorage != null && energyTarget.canReceive()) {
                int toSend = Math.min(energyStorage.getEnergyStored(), MAX_OUTPUT_PER_TICK);
                int accepted = energyTarget.receiveEnergy(toSend, false);
                energyStorage.extractEnergy(accepted, false);
            }

            // 魔力輸出
            IUnifiedManaHandler manaTarget = BlockCapabilityCache.create(
                    ModCapability.MANA, level, targetPos, dir.getOpposite()
            ).getCapability();

            if (manaTarget != null && manaStorage != null && manaTarget.canReceive()) {
                int toSend = Math.min(manaStorage.getManaStored(), MAX_OUTPUT_PER_TICK);
                int accepted = manaTarget.receiveMana(toSend, ManaAction.get(false));
                manaStorage.extractMana(accepted, ManaAction.get(false));
            }
        }
    }
}
