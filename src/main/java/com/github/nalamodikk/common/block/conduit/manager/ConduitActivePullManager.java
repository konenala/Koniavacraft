package com.github.nalamodikk.common.block.conduit.manager;

import com.github.nalamodikk.common.block.conduit.ArcaneConduitBlockEntity;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.coreapi.block.IConfigurableBlock;
import com.github.nalamodikk.register.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 🔄 奧術導管主動拉取管理器
 *
 * 職責：
 * - 主動從鄰居機器拉取魔力
 * - 尊重機器的 IO 配置
 * - 處理不同的拉取策略
 */
public class ConduitActivePullManager {

    private final Level level;
    private final BlockPos conduitPos;
    private final ArcaneConduitBlockEntity conduit; // 🆕 直接引用導管實體

    public ConduitActivePullManager(Level level, BlockPos conduitPos, ArcaneConduitBlockEntity conduit) {
        this.level = level;
        this.conduitPos = conduitPos;
        this.conduit = conduit;
    }


    /**
     * 🔄 執行主動拉取邏輯
     * @param maxTransferPerTick 每tick最大傳輸量
     * @return 實際拉取的魔力量
     */
    public int performActivePull(int maxTransferPerTick) {
        if (level == null || level.isClientSide) return 0;

        int totalPulled = 0;

        // 🔍 檢查所有六個方向的鄰居
        for (Direction direction : Direction.values()) {
            if (totalPulled >= maxTransferPerTick) break;

            BlockPos neighborPos = conduitPos.relative(direction);
            BlockEntity neighborBE = level.getBlockEntity(neighborPos);

            if (neighborBE == null) continue;

            // 🎯 嘗試從這個方向拉取魔力
            int pulledFromThisDirection = tryPullFromNeighbor(
                    neighborBE,
                    direction,
                    maxTransferPerTick - totalPulled
            );

            totalPulled += pulledFromThisDirection;
        }

        return totalPulled;
    }


    /**
     * 🔍 嘗試從特定鄰居拉取魔力
     */
    private int tryPullFromNeighbor(BlockEntity neighborBE, Direction directionFromConduit, int maxAmount) {
        Direction directionTowardsNeighbor = directionFromConduit.getOpposite();

        var neighborManaHandler = neighborBE.getLevel()
                .getCapability(ModCapabilities.MANA, neighborBE.getBlockPos(), directionTowardsNeighbor);

        if (neighborManaHandler == null) return 0;

        // 🎯 檢查鄰居是否允許從這個面輸出
        if (!canPullFromThisSide(neighborBE, directionTowardsNeighbor)) {
            return 0;
        }

        // 📊 計算實際可以拉取的量（適配虛擬網路）
        int availableInNeighbor = neighborManaHandler.getManaStored();

        // 🔄 獲取導管當前的容量狀態
        int currentMana = conduit.isInVirtualNetwork() ?
                conduit.getVirtualNetwork().getTotalManaStored() :
                conduit.getBufferManaStored();
        int maxMana = conduit.isInVirtualNetwork() ?
                conduit.getVirtualNetwork().getTotalManaCapacity() :
                conduit.getMaxManaStored();

        int spaceInConduit = maxMana - currentMana;

        int actualAmount = Math.min(Math.min(maxAmount, availableInNeighbor), spaceInConduit);

        if (actualAmount <= 0) return 0;

        // 🔄 執行拉取操作
        int extracted = neighborManaHandler.extractMana(actualAmount, ManaAction.get(false));
        if (extracted > 0) {
            // 🆕 使用導管的接收方法（會自動處理虛擬網路）
            conduit.receiveMana(extracted, ManaAction.EXECUTE);

            // 🔊 拉取成功的效果
            onSuccessfulPull(neighborBE.getBlockPos(), directionFromConduit, extracted);
        }

        return extracted;
    }
    /**
     * 🔍 檢查是否可以從鄰居的特定面拉取
     *
     * 邏輯：
     * - 如果鄰居該面設為 OUTPUT → 可以拉取
     * - 如果鄰居該面設為 BOTH → 可以拉取
     * - 如果鄰居該面設為 INPUT 或 DISABLED → 不能拉取
     */
    private boolean canPullFromThisSide(BlockEntity neighborBE, Direction neighborSide) {
        // 🎯 使用你現有的 IConfigurableBlock 系統
        if (neighborBE instanceof IConfigurableBlock configurable) {
            var ioType = configurable.getIOConfig(neighborSide);

            return switch (ioType) {
                case OUTPUT, BOTH -> true;  // 可以從輸出面和雙向面拉取
                case INPUT, DISABLED -> false; // 不能從輸入面和禁用面拉取
            };
        }

        // 🔄 如果鄰居沒有 IO 配置，預設允許拉取
        return true;
    }

    /**
     * 🎉 拉取成功時的效果回調
     */
    private void onSuccessfulPull(BlockPos fromPos, Direction direction, int amount) {
        // 🔊 這裡可以添加：
        // - 粒子效果
        // - 音效
        // - 日誌記錄
        // - 統計數據更新

        // 🔊 暫時用日誌記錄（你可以稍後添加粒子效果）
        if (amount > 0) {
            // System.out.println("Conduit pulled " + amount + " mana from " + fromPos);
        }
    }

    /**
     * 🎯 獲取所有可拉取的鄰居資訊（用於調試或GUI顯示）
     */
    public List<PullableNeighborInfo> getPullableNeighbors() {
        List<PullableNeighborInfo> result = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = conduitPos.relative(direction);
            BlockEntity neighborBE = level.getBlockEntity(neighborPos);

            if (neighborBE == null) continue;

            Direction neighborSide = direction.getOpposite();
            var manaHandler = neighborBE.getLevel()
                    .getCapability(ModCapabilities.MANA, neighborBE.getBlockPos(), neighborSide);

            if (manaHandler != null && canPullFromThisSide(neighborBE, neighborSide)) {
                result.add(new PullableNeighborInfo(
                        neighborPos,
                        direction,
                        manaHandler.getManaStored(),
                        manaHandler.getMaxManaStored(),
                        neighborBE.getBlockState().getBlock().getName().getString()
                ));
            }
        }

        return result;
    }

    /**
     * 📊 可拉取鄰居的資訊記錄
     */
    public record PullableNeighborInfo(
            BlockPos pos,
            Direction direction,
            int currentMana,
            int maxMana,
            String blockName
    ) {}
}