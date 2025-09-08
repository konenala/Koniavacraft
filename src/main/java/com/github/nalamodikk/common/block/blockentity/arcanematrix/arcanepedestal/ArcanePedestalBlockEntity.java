package com.github.nalamodikk.common.block.blockentity.arcanematrix.arcanepedestal;

import com.github.nalamodikk.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
 * - 旋轉角度（客戶端渲染展示用）
 * - 伺服器邏輯：可在 serverTick 內做粒子／檢查／與 Ritual Core 互動
 */
public class ArcanePedestalBlockEntity extends BlockEntity {

    // 單一物品槽：index 0 = 祭品
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    // 客戶端顯示旋轉（給 BlockEntityRenderer 用）
    private float spin;          // 0..360f
    private float spinSpeed = 2; // 每 tick 角速度（可由方塊狀態或 NBT 調整）

    public ArcanePedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARCANE_PEDESTAL_BE.get(), pos, state);
    }

    /* ------------------------------
       公開 API（給 Ritual Core 呼叫）
       ------------------------------ */

    /** 放上一個祭品（覆蓋原有）。自動同步到客戶端。 */
    public void setOffering(ItemStack stack) {
        items.set(0, stack.copy());
        setChangedAndSync();
    }

    /** 取得目前祭品（可能是 EMPTY）。 */
    public ItemStack getOffering() {
        return items.get(0);
    }

    /** 是否有祭品可被 Ritual Core 消耗。 */
    public boolean hasOffering() {
        return !items.get(0).isEmpty();
    }

    /**
     * Ritual Core 消耗祭品時呼叫。
     * @param count 欲消耗數量
     * @return 實際消耗數量
     */
    public int consumeOffering(int count) {
        ItemStack s = items.get(0);
        if (s.isEmpty() || count <= 0) return 0;
        int removed = Math.min(count, s.getCount());
        s.shrink(removed);
        if (s.getCount() <= 0) items.set(0, ItemStack.EMPTY);
        setChangedAndSync();
        return removed;
    }

    /** 儀式被中斷時，將物品掉落到世界。 */
    public void dropContents() {
        if (level == null || level.isClientSide) return;
        ItemStack s = items.get(0);
        if (!s.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, s);
            items.set(0, ItemStack.EMPTY);
            setChangedAndSync();
        }
    }

    /* ------------------------------
       Tick：伺服器/客戶端
       ------------------------------ */

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

    /** 伺服器端每 tick 呼叫（由 Block#getTicker 返回）。 */
    public static void serverTick(Level level, BlockPos pos, BlockState state, ArcanePedestalBlockEntity be) {
        if (level.isClientSide) return; // 確保只在伺服器端執行

        // 如果有祭品，每隔一段時間生成粒子效果
        if (be.hasOffering() && level.getRandom().nextInt(10) == 0) { // 1/10 的機率
            double x = pos.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.4;
            double y = pos.getY() + 1.1;
            double z = pos.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.4;
            level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        // 你可以在這裡放伺服器邏輯：例如與 Ritual Core 心跳同步、範圍找核心、生成粒子條件判斷等。
        // 輕量化：不需要每 tick 都 setChanged（避免卡頓）；只有狀態改變時才 sync。
    }

    /** 客戶端每幀渲染前可由 BER 讀取 spin 值；這裡只提供一個便捷更新方法（可由 BER 或 clientTicker 呼叫）。 */
    public void clientTickVisualOnly() {
        spin = (spin + spinSpeed) % 360f;
    }

    public float getSpin() {
        return spin;
    }

    public void setSpinSpeed(float speed) {
        spinSpeed = Mth.clamp(speed, 0f, 20f);
    }

    /* ------------------------------
       NBT 存讀（含 1.20.5+ HolderLookup）
       ------------------------------ */

    private static final String TAG_ITEM = "Item";
    private static final String TAG_SPIN = "Spin";
    private static final String TAG_SPIN_SPEED = "SpinSpeed";

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // 物品
        CompoundTag itemTag = new CompoundTag();
        items.get(0).save(registries, itemTag);
        tag.put(TAG_ITEM, itemTag);

        // 視覺
        tag.putFloat(TAG_SPIN, spin);
        tag.putFloat(TAG_SPIN_SPEED, spinSpeed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // 物品
        if (tag.contains(TAG_ITEM)) {
            ItemStack stack = ItemStack.parse(registries, tag.getCompound(TAG_ITEM)).orElse(ItemStack.EMPTY);
            items.set(0, stack);
        } else {
            items.set(0, ItemStack.EMPTY);
        }

        // 視覺
        spin = tag.getFloat(TAG_SPIN);
        spinSpeed = tag.contains(TAG_SPIN_SPEED) ? tag.getFloat(TAG_SPIN_SPEED) : 2f;
    }

    /* ------------------------------
       同步（方塊更新封包）
       ------------------------------ */

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        // 傳給客戶端的最小必要資料
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

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }
}
