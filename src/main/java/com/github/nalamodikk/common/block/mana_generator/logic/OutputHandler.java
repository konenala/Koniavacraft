
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
                // TODO [OutputHandlerV2]：目前的「需求值」是使用「最大容量 - 當前儲量」來估算接收端的可接收空間
                // TODO: [OutputHandlerV2] 儲量估算目前為靜態模型
                // NOTE: 可接下來觀察玩家是否常常讓小容量機器卡不到 mana
                // NOTE: 預設邏輯雖簡單，但對於高流量場景可能會失衡
                // 這種靜態需求估算邏輯雖然簡單穩定，但存在以下潛在問題：
                // - 無法判斷接收端是否實際正在消耗 mana / energy（可能只是一直塞著不動）
                // - 容易導致處理速度快但容量小的機器長期拿不到 mana（需求低，但其實需要更多）
                //
                // 未來可擴充以下機制：
                // - 引入「過去 N tick 的實際接收量」做動態滑動平均 → 預估真實需求
                // - 使用上一輪實際輸入比值（成功 / 嘗試）作為下一輪的分配加權
                // - 抽象出一個 `DemandEstimator` 模型介面，允許外掛不同需求預估策略
                // - 支援玩家手動配置各方向優先順序（Output Priority UI）
                //
                // 目前仍維持簡單邏輯，待模組進入中後期或有玩家反饋後再重構。

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
                // TODO: [OutputHandlerV2] 目前的「需求值」是使用「最大容量 - 當前儲量」來估算接收端的可接收空間。
                // TODO: [OutputHandlerV2] 儲量估算目前為靜態模型
                // NOTE: 可接下來觀察玩家是否常常讓小容量機器卡不到 mana
                // NOTE: 預設邏輯雖簡單，但對於高流量場景可能會失衡

                // 這種靜態需求估算邏輯雖然簡單穩定，但存在以下潛在問題：
                // - 無法判斷接收端是否實際正在消耗 mana / energy（可能只是一直塞著不動）
                // - 容易導致處理速度快但容量小的機器長期拿不到 mana（需求低，但其實需要更多）
                //
                // 未來可擴充以下機制：
                // - 引入「過去 N tick 的實際接收量」做動態滑動平均 → 預估真實需求
                // - 使用上一輪實際輸入比值（成功 / 嘗試）作為下一輪的分配加權
                // - 抽象出一個 `DemandEstimator` 模型介面，允許外掛不同需求預估策略
                // - 支援玩家手動配置各方向優先順序（Output Priority UI）
                //
                // 目前仍維持簡單邏輯，待模組進入中後期或有玩家反饋後再重構。

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
            int sentTotal = 0; // 👈 新增：實際送出的總和

            for (int i = 0; i < manaTargets.size(); i++) {
                int portion = (int) Math.round(totalToSend * (manaDemands.get(i) / (double) totalManaDemand));
                int accepted = manaTargets.get(i).receiveMana(portion, ManaAction.EXECUTE);
                if (accepted > 0) {
                    manaStorage.extractMana(accepted, ManaAction.EXECUTE);
                    didOutput = true;
                }
                sentTotal += accepted; // 👈 累加實際送出量
            }
            int remainder = totalToSend - sentTotal;
            if (remainder > 0 && !manaTargets.isEmpty()) {
                int accepted = manaTargets.get(0).receiveMana(remainder, ManaAction.EXECUTE);
                if (accepted > 0) {
                    manaStorage.extractMana(accepted, ManaAction.EXECUTE);
                    didOutput = true;
                }
            }
        }

        // 能量輸出
        // 能量輸出
        if (energyStorage != null && totalEnergyDemand > 0 && energyStorage.getEnergyStored() > 0) {
            int sentTotal = 0;
            int totalToSend = Math.min(energyStorage.getEnergyStored(), MAX_OUTPUT_PER_TICK);

            for (int i = 0; i < energyTargets.size(); i++) {
                int portion = (int) Math.round(totalToSend * (energyDemands.get(i) / (double) totalEnergyDemand));
                int accepted = energyTargets.get(i).receiveEnergy(portion, false);
                if (accepted > 0) {
                    energyStorage.extractEnergy(accepted, false);
                    didOutput = true;
                }
                sentTotal += accepted; // ✅ 漏了這行！
            }

            int remainder = totalToSend - sentTotal;
            if (remainder > 0 && !energyTargets.isEmpty()) {
                int accepted = energyTargets.get(0).receiveEnergy(remainder, false); // ✅ 修正 manaTargets → energyTargets
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
