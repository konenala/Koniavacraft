package com.github.nalamodikk.common.block.blockentity.ritual;

import com.github.nalamodikk.common.block.ritualblock.RuneStoneBlock;
import com.github.nalamodikk.common.block.ritualblock.RuneType;
import com.github.nalamodikk.common.block.blockentity.ritual.tracker.RitualCoreTracker;
import com.github.nalamodikk.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 符文石方塊實體 - 儀式增強組件
 * 負責：
 * 1. 存儲符文石類型和狀態
 * 2. 計算對儀式的增強效果
 * 3. 檢測協同效應
 * 4. 提供視覺效果數據
 */
public class RuneStoneBlockEntity extends BlockEntity {

    private final RuneType runeType;
    private boolean isActive = false; // 是否正在參與儀式
    private float activationLevel = 0.0f; // 激活程度 (0.0 - 1.0)

    // 渲染相關
    private float glowIntensity = 0.0f;
    private float runeRotation = 0.0f;
    private int tickCount = 0;

    // 效果相關
    private int nearbyRuneCount = 0; // 附近同類符文石數量
    private boolean hasSynergyBonus = false; // 是否有協同獎勵

    public RuneStoneBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.RUNE_STONE_BE.get(), pos, blockState);
        // 從方塊狀態中獲取RuneType
        if (blockState.getBlock() instanceof RuneStoneBlock runeBlock) {
            this.runeType = runeBlock.getRuneType();
        } else {
            this.runeType = RuneType.EFFICIENCY; // 默認類型
        }
    }

    public RuneStoneBlockEntity(BlockPos pos, BlockState blockState, RuneType runeType) {
        super(ModBlockEntities.RUNE_STONE_BE.get(), pos, blockState);
        this.runeType = runeType;
    }

    public void tick() {
        if (level == null) return;

        tickCount++;

        // 客戶端渲染動畫
        if (level.isClientSide()) {
            updateRenderingEffects();
        }

        // 服務端邏輯
        if (!level.isClientSide()) {
            // 每20tick更新一次狀態
            if (tickCount % 20 == 0) {
                updateRuneState();
            }
        }
    }

    /**
     * 更新渲染效果
     */
    private void updateRenderingEffects() {
        // 發光強度基於激活狀態
        float targetGlow = isActive ? activationLevel : 0.2f;
        glowIntensity += (targetGlow - glowIntensity) * 0.05f;

        // 符文旋轉動畫
        if (isActive) {
            runeRotation += 1.0f;
            if (runeRotation >= 360.0f) {
                runeRotation -= 360.0f;
            }
        }
    }

    /**
     * 更新符文石狀態
     */
    private void updateRuneState() {
        // 檢查附近的儀式活動
        boolean wasActive = isActive;
        isActive = checkForNearbyRituals();

        // 計算附近同類符文石數量
        int oldNearbyCount = nearbyRuneCount;
        nearbyRuneCount = countNearbyRuneStones();

        // 檢查協同效應
        boolean oldSynergy = hasSynergyBonus;
        hasSynergyBonus = checkSynergyEffects();

        // 如果狀態改變，標記需要保存
        if (wasActive != isActive || oldNearbyCount != nearbyRuneCount || oldSynergy != hasSynergyBonus) {
            setChanged();
        }
    }

    /**
     * 檢查附近的儀式活動：改用核心追蹤器避免全面掃描。
     */
    private boolean checkForNearbyRituals() {
        if (level == null) {
            activationLevel = 0.0f;
            return false;
        }

        double maxDistance = 16.0;
        double closestSq = Double.MAX_VALUE;
        boolean found = false;

        for (BlockPos corePos : RitualCoreTracker.getCores(level)) {
            double distanceSq = corePos.distSqr(worldPosition);
            if (distanceSq > maxDistance * maxDistance) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(corePos);
            if (be instanceof RitualCoreBlockEntity ritualCore) {
                RitualCoreBlockEntity.RitualState state = ritualCore.getState();
                if (state == RitualCoreBlockEntity.RitualState.RUNNING || state == RitualCoreBlockEntity.RitualState.PREPARING) {
                    closestSq = Math.min(closestSq, distanceSq);
                    found = true;
                }
            }
        }

        if (found) {
            double distance = Math.sqrt(closestSq);
            activationLevel = Math.max(0.0f, 1.0f - (float) (distance / maxDistance));
        } else {
            activationLevel = 0.0f;
        }
        return found;
    }

    /**
     * 計算附近同類符文石數量
     */
    private int countNearbyRuneStones() {
        int count = 0;

        // 在8x8範圍內搜索同類符文石
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                for (int y = -2; y <= 2; y++) {
                    if (x == 0 && y == 0 && z == 0) continue; // 跳過自己

                    BlockPos checkPos = worldPosition.offset(x, y, z);
                    BlockEntity be = level.getBlockEntity(checkPos);
                    if (be instanceof RuneStoneBlockEntity runeStone) {
                        if (runeStone.getRuneType() == this.runeType) {
                            count++;
                        }
                    }
                }
            }
        }

        return count;
    }

    /**
     * 檢查協同效應
     */
    private boolean checkSynergyEffects() {
        switch (runeType) {
            case EFFICIENCY:
                // 效率符文與穩定符文相鄰時獲得協同效應
                return hasAdjacentRuneType(RuneType.STABILITY);

            case CELERITY:
                // 迅捷符文在魔力塔正上方時獲得協同效應
                return isAboveManaPylon();

            case STABILITY:
                // 4塊穩定符文組成對稱結構時獲得協同效應
                return nearbyRuneCount >= 3; // 包含自己總共4塊

            case AUGMENTATION:
                // 增幅符文在雷雨天氣下獲得協同效應（這裡簡化為總是false）
                return level.isRaining() && level.isThundering();

            default:
                return false;
        }
    }

    /**
     * 檢查相鄰是否有特定類型的符文石
     */
    private boolean hasAdjacentRuneType(RuneType targetType) {
        for (var direction : net.minecraft.core.Direction.values()) {
            BlockPos adjacentPos = worldPosition.relative(direction);
            BlockEntity be = level.getBlockEntity(adjacentPos);
            if (be instanceof RuneStoneBlockEntity runeStone) {
                if (runeStone.getRuneType() == targetType) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 檢查是否在魔力塔正上方
     */
    private boolean isAboveManaPylon() {
        BlockEntity be = level.getBlockEntity(worldPosition.below());
        return be instanceof ManaPylonBlockEntity;
    }

    /**
     * 計算此符文石對儀式的效果修正
     */
    public RuneEffect calculateEffect() {
        float baseMultiplier = 1.0f;
        float synergyBonus = hasSynergyBonus ? 1.5f : 1.0f;
        float countMultiplier = Math.min(5, Math.max(1, nearbyRuneCount + 1)); // 最多5塊疊加

        switch (runeType) {
            case EFFICIENCY:
                // 降低魔力消耗
                return new RuneEffect(RuneEffect.Type.MANA_COST_REDUCTION,
                    0.08f * countMultiplier * synergyBonus);

            case CELERITY:
                // 提升速度
                float speedBonus = synergyBonus > 1.0f ? 0.15f : 0.10f; // 協同效應增強
                return new RuneEffect(RuneEffect.Type.SPEED_INCREASE,
                    speedBonus * countMultiplier);

            case STABILITY:
                // 降低失敗風險
                return new RuneEffect(RuneEffect.Type.STABILITY_INCREASE,
                    0.2f * countMultiplier * synergyBonus);

            case AUGMENTATION:
                // 增強產出
                return new RuneEffect(RuneEffect.Type.OUTPUT_ENHANCEMENT,
                    synergyBonus > 1.0f ? 1.0f : 0.25f); // 協同效應時必定觸發

            default:
                return new RuneEffect(RuneEffect.Type.NONE, 0.0f);
        }
    }

    // Getters
    public RuneType getRuneType() { return runeType; }
    public boolean isActive() { return isActive; }
    public float getActivationLevel() { return activationLevel; }
    public float getGlowIntensity() { return glowIntensity; }
    public float getRuneRotation() { return runeRotation; }
    public boolean hasSynergyBonus() { return hasSynergyBonus; }
    public int getNearbyRuneCount() { return nearbyRuneCount; }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.putString("RuneType", runeType.name());
        tag.putBoolean("IsActive", isActive);
        tag.putFloat("ActivationLevel", activationLevel);
        tag.putFloat("GlowIntensity", glowIntensity);
        tag.putFloat("RuneRotation", runeRotation);
        tag.putInt("NearbyRuneCount", nearbyRuneCount);
        tag.putBoolean("HasSynergyBonus", hasSynergyBonus);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        isActive = tag.getBoolean("IsActive");
        activationLevel = tag.getFloat("ActivationLevel");
        glowIntensity = tag.getFloat("GlowIntensity");
        runeRotation = tag.getFloat("RuneRotation");
        nearbyRuneCount = tag.getInt("NearbyRuneCount");
        hasSynergyBonus = tag.getBoolean("HasSynergyBonus");
    }

    /**
     * 符文效果數據類
     */
    public static class RuneEffect {
        public enum Type {
            NONE,
            MANA_COST_REDUCTION,
            SPEED_INCREASE,
            STABILITY_INCREASE,
            OUTPUT_ENHANCEMENT
        }

        public final Type type;
        public final float value;

        public RuneEffect(Type type, float value) {
            this.type = type;
            this.value = value;
        }
    }
}
