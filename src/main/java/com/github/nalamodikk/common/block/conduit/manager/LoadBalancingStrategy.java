package com.github.nalamodikk.common.block.conduit.manager;

import com.github.nalamodikk.common.block.conduit.ArcaneConduitBlockEntity;
import net.minecraft.core.Direction;

import java.util.List;
// === 負載平衡策略類 ===

public class LoadBalancingStrategy {

    /**
     * 選擇最佳目標方向
     */
    public static Direction selectBestTarget(List<Direction> validTargets,
                                             ArcaneConduitBlockEntity conduit,
                                             ConduitIOManager ioManager,
                                             ConduitNetworkManager networkManager,
                                             long tickCounter) {
        if (validTargets.isEmpty()) return null;

        // 策略1：優先級策略
        Direction priorityTarget = findHighestPriorityTarget(validTargets, ioManager, networkManager);

        // 策略2：負載平衡策略
        Direction balancedTarget = findLightestLoadTarget(validTargets, networkManager);

        // 策略3：輪詢策略
        Direction roundRobinTarget = findRoundRobinTarget(validTargets, tickCounter);

        // 決策邏輯
        return chooseBestStrategy(priorityTarget, balancedTarget, roundRobinTarget, ioManager, networkManager);
    }

    /**
     * 找到優先級最高的目標
     */
    private static Direction findHighestPriorityTarget(List<Direction> validTargets,
                                                       ConduitIOManager ioManager,
                                                       ConduitNetworkManager networkManager) {
        Direction bestDir = null;
        int maxPriority = Integer.MIN_VALUE;

        for (Direction dir : validTargets) {
            ConduitCacheManager.TargetInfo target = networkManager.getTargetInfo(dir);
            if (target != null) {
                int priority = target.getPriority() + ioManager.getPriority(dir);

                if (priority > maxPriority) {
                    maxPriority = priority;
                    bestDir = dir;
                }
            }
        }

        return bestDir;
    }

    /**
     * 找到負載最輕的目標
     */
    private static Direction findLightestLoadTarget(List<Direction> validTargets,
                                                    ConduitNetworkManager networkManager) {
        Direction lightestDir = null;
        int minLoad = Integer.MAX_VALUE;

        for (Direction dir : validTargets) {
            ConduitCacheManager.TargetInfo target = networkManager.getTargetInfo(dir);
            if (target != null) {
                // 如果是導管，考慮其當前魔力量
                int currentLoad;
                if (target.isConduit) {
                    currentLoad = target.storedMana; // 魔力越少，負載越輕
                } else {
                    // 非導管設備，使用可用空間作為負載指標
                    currentLoad = target.availableSpace > 0 ? 0 : Integer.MAX_VALUE;
                }

                if (currentLoad < minLoad) {
                    minLoad = currentLoad;
                    lightestDir = dir;
                }
            }
        }

        return lightestDir;
    }

    /**
     * 輪詢策略
     */
    private static Direction findRoundRobinTarget(List<Direction> validTargets, long tickCounter) {
        if (validTargets.isEmpty()) return null;

        // 使用 tick 計數器實現簡單輪詢
        int index = (int) (tickCounter % validTargets.size());
        return validTargets.get(index);
    }

    /**
     * 選擇最佳策略
     */
    private static Direction chooseBestStrategy(Direction priorityTarget,
                                                Direction balancedTarget,
                                                Direction roundRobinTarget,
                                                ConduitIOManager ioManager,
                                                ConduitNetworkManager networkManager) {
        // 如果有明確的高優先級目標，使用它
        if (priorityTarget != null) {
            int highestPriority = ioManager.getPriority(priorityTarget);

            // 但如果其他目標的優先級差不多，就考慮負載平衡
            if (balancedTarget != null) {
                int balancedPriority = ioManager.getPriority(balancedTarget);
                if (Math.abs(balancedPriority - highestPriority) <= 5) {
                    // 在相近優先級中選擇負載最輕的
                    return chooseBalancedTarget(priorityTarget, balancedTarget, networkManager);
                }
            }

            return priorityTarget;
        }

        // 沒有明確優先級時，使用負載平衡
        return balancedTarget != null ? balancedTarget : roundRobinTarget;
    }

    /**
     * 在優先級相近時做平衡選擇
     */
    private static Direction chooseBalancedTarget(Direction priorityTarget,
                                                  Direction balancedTarget,
                                                  ConduitNetworkManager networkManager) {
        ConduitCacheManager.TargetInfo priorityInfo = networkManager.getTargetInfo(priorityTarget);
        ConduitCacheManager.TargetInfo balancedInfo = networkManager.getTargetInfo(balancedTarget);

        // 如果優先級目標已經負載很重，選擇平衡目標
        if (priorityInfo != null && balancedInfo != null &&
                priorityInfo.isConduit && priorityInfo.storedMana > balancedInfo.storedMana) {
            return balancedTarget;
        }

        return priorityTarget;
    }
}
