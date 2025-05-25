package com.github.nalamodikk.common.block.TileEntity;

import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.registry.ModCapabilities;
import com.github.nalamodikk.common.compat.energy.ForgeEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;

public abstract class AbstractManaMachineEntityBlock extends BlockEntity
        implements GeoBlockEntity, GeoAnimatable, MenuProvider, IConfigurableBlock {
    /**
     * 每個產出週期預設產出的 mana 數量。
     * 子類可以覆寫 {@link #computeManaAmount()} 來計算升級或 bonus。
     */
    protected int manaPerCycle = 0;

    protected final ForgeEnergyStorage energyStorage;
    protected final ManaStorage manaStorage;
    protected final ItemStackHandler itemHandler;
    private final LazyOptional<ForgeEnergyStorage> lazyEnergyStorage;
    private final LazyOptional<ManaStorage> lazyManaStorage;
    private final LazyOptional<ItemStackHandler> lazyItemHandler;

    protected boolean isWorking = false;

    public AbstractManaMachineEntityBlock(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                          @Nullable Integer maxMana, @Nullable Integer maxEnergy, int itemSlots) {
        super(type, pos, state);

        // 只有當 maxEnergy 不為 null 且大於 0 時，才初始化能量存儲
        this.energyStorage = (maxEnergy != null && maxEnergy > 0) ? new ForgeEnergyStorage(maxEnergy) : null;

        // 只有當 maxMana 不為 null 且大於 0 時，才初始化魔力存儲
        this.manaStorage = (maxMana != null && maxMana > 0) ? new ManaStorage(maxMana) : null;

        this.itemHandler = new ItemStackHandler(itemSlots) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };

        // 只有當 energyStorage 存在時才創建 LazyOptional
        this.lazyEnergyStorage = (this.energyStorage != null) ? LazyOptional.of(() -> energyStorage) : LazyOptional.empty();
        this.lazyManaStorage = (this.manaStorage != null) ? LazyOptional.of(() -> manaStorage) : LazyOptional.empty();
        this.lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    public void setManaPerCycle(int amount) {
        this.manaPerCycle = amount;
    }

    public int getManaPerCycle() {
        return this.manaPerCycle;
    }


    public void drops() {
        // 如果有 itemHandler，可循環 drop 出內容物
        SimpleContainer inventory = new SimpleContainer(0); // 暫時沒槽，未來可擴展
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    /**
     * 判斷是否要執行 tick。預設只在伺服端執行。
     * 可由子類覆寫加入其他條件（例如開關、結構驗證等）
     *
     * @return 是否執行 tick
     */
    protected boolean shouldRunTick() {
        return this.level != null && !this.level.isClientSide;
    }

    /**
     * 執行前檢查：是否可以進行 mana 生產。
     * 預設為 mana 未滿才執行。
     *
     * 可由子類擴充額外條件（例如紅石訊號、是否啟動等）
     *
     * @return 是否通過檢查
     */
    protected boolean preGenerateCheck() {
        return this.manaStorage.getManaStored() < this.manaStorage.getMaxManaStored();
    }

    /**
     * 計算這一輪應該產出的 mana 數量。
     * 預設為固定值 {@code manaPerCycle}。
     *
     * 子類可依照升級模組、天氣時間等情況回傳不同值。
     *
     * @return 本輪應產出的 mana 數量
     */
    protected int computeManaAmount() {
        return this.manaPerCycle;
    }

    /**
     * 在成功產出 mana 後觸發的 hook。
     * 預設為空方法，子類可 override 播放粒子、音效、記錄 log 等。
     *
     * @param amount 本次產出的 mana 數值
     */

    protected void onGenerate(int amount) {
        // 預設不做事
    }


    public void tickMachine() {
        if (!shouldRunTick()) return;

        if (preGenerateCheck()) {
            int amount = computeManaAmount();
            manaStorage.addMana(amount);
            onGenerate(amount);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AbstractManaMachineEntityBlock machine) {
        if (!level.isClientSide) {
            machine.tickMachine();
        }
    }

    public void consumeMana(int amount) {
        if (manaStorage.getManaStored() >= amount) {
            manaStorage.consumeMana(amount);
            setChanged();
        }
    }

    public void generateMana(int amount) {
        if (manaStorage.getManaStored() + amount <= manaStorage.getMaxManaStored()) {
            manaStorage.addMana(amount);
            setChanged();
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("ManaStored")) {
            if (manaStorage != null) {
                manaStorage.setMana(tag.getInt("ManaStored")); // ✅ 安全讀取
            }
        }

        if(tag.contains("EnergyStored")){
             if (energyStorage != null) {
            energyStorage.receiveEnergy(tag.getInt("EnergyStored"), false);
        }
    }
        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        isWorking = tag.getBoolean("IsWorking");
    }


    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        if (manaStorage != null) {
            tag.putInt("ManaStored", manaStorage.getManaStored()); // ✅ 直接寫入
        }

        if (energyStorage != null) {
            tag.putInt("EnergyStored", energyStorage.getEnergyStored());
        }

        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putBoolean("IsWorking", isWorking);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY && energyStorage != null) {
            return lazyEnergyStorage.cast();
        } else if (cap == ModCapabilities.MANA && manaStorage != null) {
            return lazyManaStorage.cast();
        }
        return super.getCapability(cap, side);
    }


    public ItemStackHandler getInventory() {
        return itemHandler;
    }

    public int getManaStored() {
        return manaStorage.getManaStored();
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public boolean isWorking() {
        return isWorking;
    }

    @Nullable
    @Override
    public abstract AbstractContainerMenu createMenu(int id, Inventory inv, Player player);
}
