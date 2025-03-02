package com.github.nalamodikk.common.block.entity.ManaGenerator;

import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.Capability.ManaCapability;
import com.github.nalamodikk.common.Capability.ManaStorage;
import com.github.nalamodikk.common.Capability.ModCapabilities;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.register.ModBlockEntities;
import com.github.nalamodikk.common.compat.energy.UnifiedEnergyStorage;
import com.github.nalamodikk.common.mana.ManaAction;
import com.github.nalamodikk.common.register.ConfigManager;
import com.github.nalamodikk.common.screen.ManaGenerator.ManaGeneratorMenu;
import com.github.nalamodikk.common.sync.UnifiedSyncManager;
import com.github.nalamodikk.common.util.loader.FuelRateLoader;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.EnumMap;

public class ManaGeneratorBlockEntity extends BlockEntity implements GeoBlockEntity, GeoAnimatable, MenuProvider , IConfigurableBlock {

    public static final int MANA_STORED_INDEX = 0;
    public static final int ENERGY_STORED_INDEX = 1;
    public static final int MODE_INDEX = 2;
    public static final int BURN_TIME_INDEX = 3;
    public static final int CURRENT_BURN_TIME_INDEX = 4;
    public static final int DATA_COUNT = 5; // 總數據數量
    private int energyRate;
    private final UnifiedSyncManager syncManager = new UnifiedSyncManager(5); // 假設有 5 個需要同步的數據


    private static int getConfigMaxEnergy() {
        return ConfigManager.COMMON.maxEnergy.get();
    }

    private static int getConfigEnergyRate() {
        return ConfigManager.COMMON.energyRate.get();
    }

    private static int getConfigManaRate() {
        return ConfigManager.COMMON.manaRate.get();
    }


    public static final int MAX_MANA = 10000; // 或者保留 private，然後新增 getter
    public static final int MAX_ENERGY = 10000;

    private final UnifiedEnergyStorage energyStorage = new UnifiedEnergyStorage(getConfigMaxEnergy());
    private final ManaStorage manaStorage = new ManaStorage(MAX_MANA);
    private final ItemStackHandler fuelHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            markUpdated();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (currentMode == Mode.ENERGY) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                FuelRateLoader.FuelRate fuelRate = FuelRateLoader.getFuelRateForItem(itemId);
                return (fuelRate != null && fuelRate.getBurnTime() > 0) || (ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0);
            } else if (currentMode == Mode.MANA) {
                return stack.is(ItemTags.create(new ResourceLocation(MagicalIndustryMod.MOD_ID, "mana"))); // 魔力模式只允許標籤為 mana 的物品
            }
            return false;
        }
    };

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private boolean isWorking = false;
    private Mode currentMode = Mode.MANA; // 默認模式
    private int burnTime;
    private int currentBurnTime;
    private double energyAccumulated = 0.0;
    private double manaAccumulated = 0.0;
//    int storedEnergy = energyStorage.getEnergyStored();

    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation WORKING_ANIM = RawAnimation.begin().thenLoop("working");
    private final LazyOptional<ItemStackHandler> lazyFuelHandler = LazyOptional.of(() -> fuelHandler);
    private final LazyOptional<UnifiedEnergyStorage> lazyEnergyStorage = LazyOptional.of(() -> energyStorage);
    private final LazyOptional<ManaStorage> lazyManaStorage = LazyOptional.of(() -> manaStorage);
    private final EnumMap<Direction, Boolean> directionConfig = new EnumMap<>(Direction.class);

    public ManaGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_GENERATOR_BE.get(), pos, state);
    }

    public void markUpdated() {
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(fuelHandler.getSlots());
        for (int i = 0; i < fuelHandler.getSlots(); i++) {
            inventory.setItem(i, fuelHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    public void toggleMode() {
        // 如果機器正在運作（燃燒燃料中），則禁止切換模式
        if (isWorking || burnTime > 0) {
            MagicalIndustryMod.LOGGER.info("⚠ 無法切換模式，發電機正在運行中！");
            return; // 避免模式切換
        }

        // 切換模式
        currentMode = (currentMode == Mode.MANA) ? Mode.ENERGY : Mode.MANA;
        markUpdated(); // 更新 UI
    }


    public boolean getIsWorking() {
        return isWorking;
    }

    public int getBurnTime() {
        return burnTime;
    }


    public int getStoredEnergy() {
        return energyStorage.getEnergyStored();
    }

    public ContainerData getContainerData() {
        return syncManager.getContainerData();
    }




    public ItemStackHandler getInventory() {
        return fuelHandler;
    }

    public int getCurrentMode() {
        return (currentMode == Mode.MANA) ? 0 : 1;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ManaGeneratorBlockEntity blockEntity) {
        serverTick(level, pos, state, blockEntity);
    }

    @Override
    public void setDirectionConfig(Direction direction, boolean isOutput) {
        directionConfig.put(direction, isOutput);
        setChanged();  // 標記狀態已更改，以確保變更被保存
        markUpdated(); // 同步更新到客戶端
        MagicalIndustryMod.LOGGER.info("Direction {} set to {} for block at {}", direction, isOutput ? "Output" : "Input", worldPosition);

    }


    // 確認某個方向是否為輸出
    @Override
    public boolean isOutput(Direction direction) {
        return directionConfig.getOrDefault(direction, false);
    }


    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaGeneratorBlockEntity blockEntity) {
        if (!level.isClientSide) {
            blockEntity.sync();
            blockEntity.generateEnergyOrMana();
            blockEntity.outputEnergyAndMana();
            blockEntity.markUpdated(); // 標記更新
        }
    }

    private void sync() {
        // 將能量和魔力的狀態同步到 UnifiedSyncManager 中
        syncManager.set(ENERGY_STORED_INDEX, energyStorage.getEnergyStored());
        syncManager.set(MANA_STORED_INDEX, manaStorage.getMana());
        syncManager.set(MODE_INDEX, currentMode == Mode.MANA ? 0 : 1);
        syncManager.set(BURN_TIME_INDEX, burnTime);
        syncManager.set(CURRENT_BURN_TIME_INDEX, currentBurnTime);
    }


    private void generateEnergyOrMana() {
        ItemStack fuel = fuelHandler.getStackInSlot(0);

        // 如果燃料槽為空且燃燒時間耗盡，停止工作
        if (fuel.isEmpty() && burnTime <= 0) {
            isWorking = false;
            return;
        }

        // 如果燃燒時間耗盡，且有燃料，並且能量/魔力未滿，開始新一輪燃燒
        if (burnTime <= 0 && !fuel.isEmpty()) {
            // 檢查當前模式下的存儲是否已滿
            if ((currentMode == Mode.ENERGY && energyStorage.getEnergyStored() >= energyStorage.getMaxEnergyStored()) ||
                    (currentMode == Mode.MANA && manaStorage.getMana() >= manaStorage.getMaxMana())) {
                // 如果能量或魔力已滿，則不消耗燃料
                isWorking = false;
                return;
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(fuel.getItem());
            FuelRateLoader.FuelRate fuelRate = FuelRateLoader.getFuelRateForItem(itemId);
            int burnTimeForFuel = (fuelRate != null) ? fuelRate.getBurnTime() : ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING);
            if (burnTimeForFuel > 0) {
                burnTime = burnTimeForFuel; // 設置燃燒時間
                fuelHandler.extractItem(0, 1, false); // 消耗燃料
                isWorking = true; // 設置為工作狀態
            }
        }

        // 燃燒時間大於 0 時生成能量或魔力
        if (burnTime > 0) {
            burnTime--; // 每 tick 燃燒時間減少

            if (currentMode == Mode.ENERGY) {
                // 如果能量存儲已滿，停止工作
                if (energyStorage.getEnergyStored() >= energyStorage.getMaxEnergyStored()) {
                    isWorking = false;
                    return;
                }

                double energyToGenerate = getConfigEnergyRate(); // 直接移除 / 20 限制
                energyAccumulated += energyToGenerate; // 累積生成量

                while (energyAccumulated >= 1.0) { // 當累積能量達到1或以上
                    int energyToStore = (int) energyAccumulated; // 取整數部分
                    energyAccumulated -= energyToStore; // 減去已存儲的部分
                    energyStorage.receiveEnergy(energyToStore, false); // 實際插入能量儲存
                }

                // 當成功插入能量時，進行同步
                if (level != null && !level.isClientSide) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                    setChanged(); // 確保數據更新被保存
                }

            } else if (currentMode == Mode.MANA) {
                // 如果魔力存儲已滿，停止工作
                if (manaStorage.getMana() >= manaStorage.getMaxMana()) {
                    isWorking = false;
                    return;
                }

                double manaToGenerate = getConfigManaRate(); // 直接移除 / 20 限制
                manaAccumulated += manaToGenerate; // 累積生成量

                while (manaAccumulated >= 1.0) { // 當累積魔力達到1或以上
                    int manaToStore = (int) manaAccumulated; // 取整數部分
                    manaAccumulated -= manaToStore; // 減去已存儲的部分
                    manaStorage.addMana(manaToStore); // 實際插入魔力儲存
                }
            }
        }
    }


    private void outputEnergyAndMana() {
        if (level == null) return; // 防止 Level 為空

        for (Direction direction : Direction.values()) {
            if (isOutput(direction)) {
                BlockEntity neighborBlockEntity = level.getBlockEntity(worldPosition.relative(direction));

                if (neighborBlockEntity == null) continue; // 防止 NullPointerException

                // 能量輸出
                neighborBlockEntity.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).ifPresent(neighborEnergyStorage -> {
                    if (neighborEnergyStorage.canReceive()) {
                        int energyToTransfer = Math.min(energyStorage.getEnergyStored(), 100);
                        if (energyToTransfer > 0) { // 防止負值錯誤
                            int acceptedEnergy = neighborEnergyStorage.receiveEnergy(energyToTransfer, false);
                            energyStorage.extractEnergy(acceptedEnergy, false);
                        }
                    }
                });

                // 魔力輸出
                neighborBlockEntity.getCapability(ModCapabilities.MANA, direction.getOpposite()).ifPresent(neighborManaStorage -> {
                    if (neighborManaStorage.canReceive()) {
                        int manaToTransfer = Math.min(manaStorage.getMana(), 50);
                        if (manaToTransfer > 0) { // 防止負值錯誤
                            int acceptedMana = neighborManaStorage.receiveMana(manaToTransfer, ManaAction.get(false));
                            manaStorage.extractMana(acceptedMana, ManaAction.get(false));
                        }
                    }
                });
            }
        }
    }


//    private void outputCapability(Direction direction, BlockEntity neighborBlockEntity, Capability<?> capability, int amount, BiConsumer<Integer, Boolean> extractor) {
//        neighborBlockEntity.getCapability(capability, direction.getOpposite()).ifPresent(neighborStorage -> {
//            if (neighborStorage instanceof IConfigurableBlock configurableNeighborBlock && !configurableNeighborBlock.isOutput(direction.getOpposite())) {
//                if (capability == ForgeCapabilities.ENERGY) {
//                    int acceptedEnergy = ((IEnergyStorage) neighborStorage).receiveEnergy(amount, false);
//                    extractor.accept(acceptedEnergy, false);
//                } else if (capability == ModCapabilities.MANA) {
//                    int acceptedMana = ((IUnifiedManaHandler) neighborStorage).receiveMana(amount, ManaAction.get(false));
//                    extractor.accept(acceptedMana, ManaAction.get(false));
//                }
//            }
//        });
//    }
//


    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyFuelHandler.cast();
        } else if (cap == ForgeCapabilities.ENERGY) {
            return lazyEnergyStorage.cast();
        } else if (cap == ManaCapability.MANA) {
            MagicalIndustryMod.LOGGER.debug("🔵 發電機的魔力 Capability 被請求於方向 {}", side);

            return lazyManaStorage.cast();
        }

        return super.getCapability(cap, side);
    }


    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        // 加載燃料庫存
        if (tag.contains("FuelInventory", Tag.TAG_COMPOUND)) {
            CompoundTag inventoryTag = tag.getCompound("FuelInventory");
            for (int i = 0; i < fuelHandler.getSlots(); i++) {
                if (inventoryTag.contains("Slot" + i)) {
                    ItemStack stack = ItemStack.of(inventoryTag.getCompound("Slot" + i));
                    fuelHandler.setStackInSlot(i, stack);
                }
            }
        }

        // 加載方向配置
        if (tag.contains("DirectionConfig", Tag.TAG_COMPOUND)) {
            CompoundTag directionTag = tag.getCompound("DirectionConfig");
            for (Direction direction : Direction.values()) {
                this.directionConfig.put(direction, directionTag.getBoolean(direction.getName()));
            }
        }

        // 加載其他屬性
        manaStorage.setMana(tag.getInt("ManaStored"));
        energyStorage.receiveEnergy(tag.getInt("EnergyStored"), false);
        isWorking = tag.getBoolean("IsWorking");
        currentMode = tag.getInt("CurrentMode") == 0 ? Mode.MANA : Mode.ENERGY;
        burnTime = tag.getInt("BurnTime");
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        // 保存燃料庫存
        CompoundTag inventoryTag = new CompoundTag();
        for (int i = 0; i < fuelHandler.getSlots(); i++) {
            ItemStack stack = fuelHandler.getStackInSlot(i);
            inventoryTag.put("Slot" + i, stack.save(new CompoundTag()));
        }
        tag.put("FuelInventory", inventoryTag);

        // 保存方向配置
        CompoundTag directionTag = new CompoundTag();
        for (Direction direction : Direction.values()) {
            directionTag.putBoolean(direction.getName(), this.directionConfig.getOrDefault(direction, false));
        }
        tag.put("DirectionConfig", directionTag);

        // 保存其他屬性
        tag.putInt("ManaStored", manaStorage.getMana());
        tag.putInt("EnergyStored", energyStorage.getEnergyStored());
        tag.putBoolean("IsWorking", isWorking);
        tag.putInt("CurrentMode", currentMode == Mode.MANA ? 0 : 1);
        tag.putInt("BurnTime", burnTime);
    }


    @Override
    public Component getDisplayName() {
        return Component.translatable("block.magical_industry.mana_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ManaGeneratorMenu(id, inv, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "mana_generator_controller", 0, this::predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
        if (this.isWorking) {
            tAnimationState.getController().setAnimation(WORKING_ANIM);
        } else {
            tAnimationState.getController().setAnimation(IDLE_ANIM);
        }
        return PlayState.CONTINUE;
    }

    public enum Mode {
        MANA,
        ENERGY
    }


    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        handleUpdateTag(pkt.getTag());
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        load(tag);
    }
}
