package com.github.nalamodikk.common.block.blockentity.ritual.ritualblockentity;

import com.github.nalamodikk.common.block.blockentity.ritual.ritualblockentity.tracker.RitualCoreTracker;
import com.github.nalamodikk.register.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 奧術基座（Arcane Pedestal）
 * - 1 個物品槽：用於放置祭品
 * - 旋轉角度與浮動幅度（供客戶端渲染）
 * - 伺服端負責粒子與 Ritual Core 互動，客戶端負責動畫
 */
public class ArcanePedestalBlockEntity extends BlockEntity {

    private static final String TAG_ITEM = "Item";
    private static final String TAG_SPIN = "Spin";
    private static final String TAG_SPIN_SPEED = "SpinSpeed";
    private static final String TAG_TICK_COUNT = "TickCount";
    private static final String TAG_CONSUMED = "OfferingConsumed";

    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    private float spin;           // 0..360f
    private float spinSpeed = 2f; // 每 tick 角速度
    private int tickCount;
    private boolean offeringConsumed;

    public ArcanePedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARCANE_PEDESTAL_BE.get(), pos, state);
    }

    // region 互動 API

    /**
     * 放置祭品（覆蓋原物），並同步客戶端。
     */
    public void setOffering(ItemStack stack) {
        items.set(0, stack.copy());
        offeringConsumed = false;
        tickCount = 0;
        spin = 0f;
        setChangedAndSync();
        notifyCoreOfChange();
    }

    /**
     * 回傳目前祭品（可能為空）。
     */
    public ItemStack getOffering() {
        return items.get(0);
    }

    /**
     * 是否存在可供 Ritual Core 使用的祭品。
     */
    public boolean hasOffering() {
        return !items.get(0).isEmpty();
    }

    /**
     * 玩家嘗試放入祭品，回傳剩餘物品堆疊。
     */
    public ItemStack insertOffering(ItemStack stack) {
        if (stack.isEmpty() || hasOffering()) {
            return stack;
        }
        ItemStack copy = stack.copy();
        ItemStack stored = copy.split(1);
        setOffering(stored);
        return copy;
    }

    /**
     * 玩家取出祭品。
     */
    public ItemStack extractOffering() {
        if (!hasOffering()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = items.get(0).copy();
        items.set(0, ItemStack.EMPTY);
        offeringConsumed = false;
        tickCount = 0;
        spin = 0f;
        setChangedAndSync();
        notifyCoreOfChange();
        return result;
    }

    /**
     * Ritual Core 消耗祭品時呼叫。
     * @param count 欲消耗數量
     * @return 實際消耗數量
     */
    public int consumeOffering(int count) {
        ItemStack stack = items.get(0);
        if (stack.isEmpty() || count <= 0) {
            return 0;
        }
        int removed = Math.min(count, stack.getCount());
        stack.shrink(removed);
        if (stack.isEmpty()) {
            items.set(0, ItemStack.EMPTY);
            offeringConsumed = true;
        }
        setChangedAndSync();
        notifyCoreOfChange();
        return removed;
    }

    /**
     * Ritual 失敗或重試時重置消耗狀態。
     */
    public void resetOfferingConsumption() {
        if (offeringConsumed) {
            offeringConsumed = false;
            setChangedAndSync();
        }
    }

    /**
     * Ritual 中斷時掉落內容物。
     */
    public void dropContents() {
        if (level == null || level.isClientSide) {
            return;
        }
        ItemStack stack = items.get(0);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, stack);
            items.set(0, ItemStack.EMPTY);
        }
        offeringConsumed = false;
        setChangedAndSync();
        notifyCoreOfChange();
    }

    /**
     * 供渲染器判斷祭品是否剛被消耗。
     */
    public boolean isOfferingConsumed() {
        return offeringConsumed;
    }

    /**
     * 調整旋轉速度（預留後端行為控制）。
     */
    public void setSpinSpeed(float speed) {
        spinSpeed = Mth.clamp(speed, 0f, 20f);
    }

    // endregion

    // region 渲染資料提供

    public float getSpinForRender(float partialTick) {
        return (spin + spinSpeed * partialTick) % 360f;
    }

    public float getHoverOffset(float partialTick) {
        if (!hasOffering()) {
            return 0f;
        }
        return (float) Math.sin((tickCount + partialTick) * 0.1f) * 0.1f;
    }

    // endregion

    // region Tick

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArcanePedestalBlockEntity be) {
        if (level.isClientSide) {
            return;
        }
        be.tickCount++;

        if (be.hasOffering() && level.getRandom().nextInt(10) == 0) {
            double x = pos.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.4;
            double y = pos.getY() + 1.1;
            double z = pos.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.4;
            if (level instanceof ServerLevel server && level.getGameTime() % 100 == 0) {
                server.sendParticles(
                        ParticleTypes.ENCHANT,
                        x, y, z,
                        8,
                        0.25, 0.25, 0.25,
                        0.0
                );
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, ArcanePedestalBlockEntity be) {
        if (!level.isClientSide) {
            return;
        }
        be.tickCount++;
        if (be.hasOffering()) {
            be.spin = (be.spin + be.spinSpeed) % 360f;
        } else {
            be.spin = 0f;
        }
    }

    // endregion

    // region NBT 同步

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        ItemStack offering = items.get(0);
        if (!offering.isEmpty()) {
            CompoundTag itemTag = new CompoundTag();
            offering.save(registries, itemTag);
            tag.put(TAG_ITEM, itemTag);
        }

        tag.putFloat(TAG_SPIN, spin);
        tag.putFloat(TAG_SPIN_SPEED, spinSpeed);
        tag.putInt(TAG_TICK_COUNT, tickCount);
        tag.putBoolean(TAG_CONSUMED, offeringConsumed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains(TAG_ITEM)) {
            ItemStack stack = ItemStack.parse(registries, tag.getCompound(TAG_ITEM)).orElse(ItemStack.EMPTY);
            items.set(0, stack);
        } else {
            items.set(0, ItemStack.EMPTY);
        }

        spin = tag.getFloat(TAG_SPIN);
        spinSpeed = tag.contains(TAG_SPIN_SPEED) ? tag.getFloat(TAG_SPIN_SPEED) : 2f;
        tickCount = tag.getInt(TAG_TICK_COUNT);
        offeringConsumed = tag.getBoolean(TAG_CONSUMED);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        loadAdditional(pkt.getTag(), registries);
    }

    /**
     * 通知最近的儀式核心：祭品內容已變更。
     */
    private void notifyCoreOfChange() {
        if (level == null || level.isClientSide()) {
            return;
        }
        RitualCoreBlockEntity nearest = null;
        double closest = Double.MAX_VALUE;
        for (BlockPos corePos : RitualCoreTracker.getCores(level)) {
            double distance = corePos.distSqr(worldPosition);
            if (distance > 100) {
                continue;
            }
            var blockEntity = level.getBlockEntity(corePos);
            if (blockEntity instanceof RitualCoreBlockEntity core) {
                if (distance < closest) {
                    closest = distance;
                    nearest = core;
                }
            }
        }
        if (nearest != null) {
            nearest.onPedestalContentsChanged();
        }
    }

    // endregion

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }
}
