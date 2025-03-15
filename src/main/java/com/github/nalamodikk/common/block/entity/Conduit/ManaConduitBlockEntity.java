package com.github.nalamodikk.common.block.entity.Conduit;

import com.github.nalamodikk.common.Capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.Capability.ManaCapability;
import com.github.nalamodikk.common.Capability.ManaStorage;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.mana.ManaAction;
import com.github.nalamodikk.common.mana.net.ManaNetworkManager;
import com.github.nalamodikk.common.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class ManaConduitBlockEntity extends BlockEntity {
    private int storedMana = 0;
    private boolean needsUpdate = false; // ✅ 避免過度更新
    private final LazyOptional<ManaConduitBlockEntity> self = LazyOptional.of(() -> this);
    private final ManaStorage manaStorage = new ManaStorage(1000);
    private final LazyOptional<IUnifiedManaHandler> manaCapability = LazyOptional.of(() -> manaStorage);
    private int activeTicks = 0; // 🔥 追蹤導管活躍時間
    private static final int MAX_ACTIVE_TICKS = 20 * 5; // 5 秒後進入省電模式

    public ManaConduitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_CONDUIT_BE.get(), pos, state);
    }


    public void setMana(int mana) {
        this.manaStorage.setMana(mana);
        this.manaStorage.onChanged();
        this.setChanged();

        markActive(); // ✅ 只要 Mana 變動，重新激活導管

        if (!needsUpdate) {
            needsUpdate = true;
            ManaNetworkManager.getInstance(level).queueManaUpdate(this);
        }
    }

    public void extractManaFromNearby() {
        if (level == null || level.isClientSide) return; // ✅ 只在伺服器端運行

        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));

            if (neighbor != null) {
                LazyOptional<IUnifiedManaHandler> neighborMana = neighbor.getCapability(ManaCapability.MANA, direction.getOpposite());

                neighborMana.ifPresent(handler -> {
                    if (handler.canExtract()) { // ✅ 只有當設備可提取 Mana 時才執行
                        int extractAmount = Math.min(50, handler.getMana()); // 🔥 每次最多提取 50 Mana
                        int extracted = handler.extractMana(extractAmount, ManaAction.EXECUTE);

                        if (extracted > 0) {
                            manaStorage.addMana(extracted);
                            markActive(); // ✅ 讓導管保持活躍狀態
                            MagicalIndustryMod.LOGGER.debug("[ManaConduit] 從鄰近方塊提取 Mana: {} 來自: {}", extracted, neighbor.getBlockPos());
                        }
                    }
                });
            }
        }
    }


    public int extractMana(int amount, ManaAction action) {
        int extracted = this.manaStorage.extractMana(amount, action);

        if (extracted > 0) {
            markActive(); // ✅ Mana 被抽取時，也要激活導管
        }

        return extracted;
    }

    public int getMana() {
        return this.manaStorage.getMana();
    }

    public void applyManaUpdate() {
        if (!needsUpdate) return; // ✅ 確保不會重複執行
        needsUpdate = false;

        MagicalIndustryMod.LOGGER.debug("[ManaConduit] 更新魔力: {} → {}", worldPosition, storedMana);
        ManaNetworkManager.getInstance(level).processManaUpdate(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        ManaNetworkManager.getInstance(level).addConduit(this);
    }

    public void transferManaToNeighbors() {
        if (level == null || level.isClientSide) return;
        if (manaStorage.getMana() <= 0) return;

        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));

            if (neighbor != null) {
                LazyOptional<IUnifiedManaHandler> neighborMana = neighbor.getCapability(ManaCapability.MANA, direction.getOpposite());

                neighborMana.ifPresent(handler -> {
                    int needed = handler.getNeeded();
                    MagicalIndustryMod.LOGGER.debug("[ManaConduit] 嘗試傳輸 Mana: 目前={} 目標設備需求={}", manaStorage.getMana(), needed);

                    if (needed > 0) {
                        int transferAmount = Math.min(50, manaStorage.getMana());
                        int extracted = manaStorage.extractMana(transferAmount, ManaAction.EXECUTE);
                        int leftover = handler.insertMana(extracted, ManaAction.EXECUTE);

                        MagicalIndustryMod.LOGGER.debug("[ManaConduit] 扣除 Mana: {}, 剩餘={}", extracted, manaStorage.getMana());

                        if (leftover > 0) {
                            manaStorage.addMana(leftover);
                            MagicalIndustryMod.LOGGER.debug("[ManaConduit] 未能完全插入 Mana，剩餘={}", leftover);
                        } else {
                            MagicalIndustryMod.LOGGER.debug("[ManaConduit] 成功傳輸 Mana: {}", transferAmount);
                        }
                    } else {
                        MagicalIndustryMod.LOGGER.debug("[ManaConduit] 目標設備不需要 Mana, 跳過傳輸");
                    }
                });
            }
        }
    }



    @Override
    public void setRemoved() {
        super.setRemoved();
        ManaNetworkManager.getInstance(level).removeConduit(this);
    }

    public void updateConnections() {
        if (level == null) return; // ✅ 確保 `level` 存在

        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));

            // ✅ 只處理相鄰的 ManaConduitBlockEntity
            if (neighbor instanceof ManaConduitBlockEntity) {
                MagicalIndustryMod.LOGGER.debug("[ManaConduit] {} 與 {} 連接", worldPosition, neighbor.getBlockPos());
            }
        }
    }
    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ManaCapability.MANA) {
            return manaCapability.cast();
        }
        return super.getCapability(cap, side);
    }


    public static void tick(Level level, BlockPos pos, BlockState state, ManaConduitBlockEntity blockEntity) {
        if (!level.isClientSide) {
            if (blockEntity.activeTicks > 0) { // ✅ 只有當導管活躍時才執行
                blockEntity.extractManaFromNearby(); // ✅ 嘗試從鄰近設備提取 Mana
                blockEntity.transferManaToNeighbors(); // ✅ 嘗試將 Mana 傳輸到其他設備
                blockEntity.activeTicks--; // 🔥 每 tick 讓活躍時間減少
            }
        }
    }


    public void markActive() { // ✅ 當 Mana 發生變動時，重置活躍時間
        this.activeTicks = MAX_ACTIVE_TICKS;
    }

    public ManaStorage getManaStorage() {
        return this.manaStorage;
    }


}
