package com.github.nalamodikk.common.block.TileEntity.ManaGenerator;

import com.github.nalamodikk.common.capability.ManaCapability;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.recipe.fuel.loader.ManaGenFuelRateLoader;
import com.github.nalamodikk.common.registry.ModCapabilities;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.blocks.managenerator.ManaGeneratorBlock;
import com.github.nalamodikk.common.block.TileEntity.AbstractManaMachineEntityBlock;
import com.github.nalamodikk.common.compat.energy.ForgeEnergyStorage;
import com.github.nalamodikk.common.recipe.fuel.ManaGenFuelRecipe;
import com.github.nalamodikk.common.registry.ModBlockEntities;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.registry.ConfigManager;
import com.github.nalamodikk.common.screen.ManaGenerator.ManaGeneratorMenu;
import com.github.nalamodikk.common.sync.UnifiedSyncManager;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.EnumMap;

public class ManaGeneratorBlockEntity extends AbstractManaMachineEntityBlock {

    public static final int MANA_STORED_INDEX = 0;
    public static final int ENERGY_STORED_INDEX = 1;
    public static final int MODE_INDEX = 2;
    public static final int BURN_TIME_INDEX = 3;
    public static final int CURRENT_BURN_TIME_INDEX = 4;
    public static final int DATA_COUNT = 5; // 總數據數量
    private ResourceLocation currentFuelId;
    private int failedFuelCooldown = 0; // 🔄 防止每 tick 瘋狂判斷錯誤燃料

    private int energyRate;
    private final UnifiedSyncManager syncManager = new UnifiedSyncManager(5); // 假設有 5 個需要同步的數據
    private static final Logger LOGGER = LoggerFactory.getLogger(ManaGeneratorBlockEntity.class);


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

    private final ForgeEnergyStorage energyStorage = new ForgeEnergyStorage(getConfigMaxEnergy());
    private final ManaStorage manaStorage = new ManaStorage(MAX_MANA);
    private final ItemStackHandler fuelHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            markUpdated();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

            if (currentMode == Mode.ENERGY) {
                // **符合 FuelRateLoader 或 ForgeHooks 內的燃料就允許**
                ManaGenFuelRateLoader.FuelRate fuelRate = ManaGenFuelRateLoader.getFuelRateForItem(itemId);
                return (fuelRate != null && fuelRate.getBurnTime() > 0) || (ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0);
            } else if (currentMode == Mode.MANA) {
                // **只要符合 "mana" 標籤 或 `FuelRecipe` 內的燃料之一，就允許**
                return stack.is(ItemTags.create(new ResourceLocation(MagicalIndustryMod.MOD_ID, "mana"))) || isValidFuel(stack);
            }
            return false;
        }


        private boolean isValidFuel(ItemStack stack) {
            if (level == null) return false;

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (itemId == null) return false;

            // 檢查 FuelRecipe 是否包含這個物品
            for (ManaGenFuelRecipe recipe : level.getRecipeManager().getAllRecipesFor(ManaGenFuelRecipe.FuelRecipeType.INSTANCE)) {
                if (recipe.getId().toString().equals(itemId.toString())) {
                    return true;
                }
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
    private final LazyOptional<ForgeEnergyStorage> lazyEnergyStorage = LazyOptional.of(() -> energyStorage);
    private final LazyOptional<ManaStorage> lazyManaStorage = LazyOptional.of(() -> manaStorage);
    private final EnumMap<Direction, Boolean> directionConfig = new EnumMap<>(Direction.class);

    public ManaGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_GENERATOR_BE.get(), pos, state, MAX_MANA ,MAX_ENERGY, 1);
    }

    public void markUpdated() {
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
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

    private int getFuelBurnTimeFromRecipe(ItemStack stack) {
        if (level == null) return 0;

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) return 0;

        for (ManaGenFuelRecipe recipe : level.getRecipeManager().getAllRecipesFor(ManaGenFuelRecipe.FuelRecipeType.INSTANCE)) {
            if (recipe.getId().toString().equals(itemId.toString())) {
                return recipe.getBurnTime();
            }
        }
        return 0;
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
        MagicalIndustryMod.LOGGER.debug("Direction {} set to {} for block at {}", direction, isOutput ? "Output" : "Input", worldPosition);

    }


    // 確認某個方向是否為輸出
    @Override
    public boolean isOutput(Direction direction) {
        return directionConfig.getOrDefault(direction, false);
    }


    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaGeneratorBlockEntity blockEntity) {
        if (!level.isClientSide) {
            boolean wasActive = state.getValue(ManaGeneratorBlock.ACTIVE);
            boolean shouldBeActive = blockEntity.isWorking; // 這是發電機是否在運作

            if (wasActive != shouldBeActive) { // 狀態有變化
                level.setBlock(pos, state.setValue(ManaGeneratorBlock.ACTIVE, shouldBeActive), 3);
            }

            blockEntity.sync();

            blockEntity.generateEnergyOrMana();
            blockEntity.outputEnergyAndMana();
            blockEntity.markUpdated(); // 標記更新
        }
    }

    private void sync() {
        // 將能量和魔力的狀態同步到 UnifiedSyncManager 中
        syncManager.set(ENERGY_STORED_INDEX, energyStorage.getEnergyStored());
        syncManager.set(MANA_STORED_INDEX, manaStorage.getManaStored());
        syncManager.set(MODE_INDEX, currentMode == Mode.MANA ? 0 : 1);
        syncManager.set(BURN_TIME_INDEX, burnTime);
        syncManager.set(CURRENT_BURN_TIME_INDEX, currentBurnTime);
    }


    private void generateEnergyOrMana() {
        // ⏳ 如果之前燃料失敗，進入冷卻狀態（你已經有這個的話可以保留）
        if (failedFuelCooldown > 0) {
            failedFuelCooldown--;
            return;
        }

        // 🔄 嘗試開始新的燃燒週期
        if (burnTime <= 0) {
            // ✅ ⚠️ 只有在燃料槽「看起來有東西」才檢查，避免沒必要地調用 handleNewFuel()
            ItemStack fuelStack = fuelHandler.getStackInSlot(0);
            if (!fuelStack.isEmpty()) {
                if (!handleNewFuel()) {
                    isWorking = false;
                    failedFuelCooldown = 20; // 降低洗 log 頻率
                    return;
                }
            } else {
                // 燃料槽完全為空，也不用檢查
                isWorking = false;
                return;
            }
        }

        // ✅ 有燃料正在燒
        burnTime--;

        if (currentMode == Mode.ENERGY) {
            handleEnergyGeneration();
        } else {
            handleManaGeneration();
        }
    }




    /**
     * 能量模式處理
     */
    private void handleEnergyGeneration() {
        if (energyStorage.getEnergyStored() >= energyStorage.getMaxEnergyStored()) {
            isWorking = false;
            return;
        }

        double energyToGenerate = getConfigEnergyRate(); // 可配置產出值
        int accepted = energyStorage.receiveEnergy((int) energyToGenerate, false); // 一次嘗試塞入全部能量
        isWorking = accepted > 0;

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            setChanged();
        }
    }



    /**
     * 魔力模式處理
     */
    private void handleManaGeneration() {
        if (manaStorage.getManaStored() >= manaStorage.getMaxManaStored()) {
            isWorking = false;
//            LOGGER.debug("[Mana Generator] 🛑 魔力已滿，暫停工作！");
            return;
        }

        if (currentFuelId == null) {
            LOGGER.warn("[Mana Generator] ❌ 當前燃料 ID 為 null，不能生產魔力！");
            isWorking = false;
            return;
        }

        ManaGenFuelRateLoader.FuelRate fuelRate = ManaGenFuelRateLoader.getFuelRateForItem(currentFuelId);

        if (fuelRate == null) {
            LOGGER.warn("[Mana Generator] ❌ 找不到燃料數據：{}", currentFuelId);
            isWorking = false;
            return;
        }

        manaAccumulated += fuelRate.getManaRate();

        while (manaAccumulated >= 1.0) {
            int toStore = (int) manaAccumulated;
            manaStorage.addMana(toStore);
            manaAccumulated -= toStore;
        }

        isWorking = true;
    }


    private boolean handleNewFuel() {
        ItemStack fuelStack = fuelHandler.getStackInSlot(0).copy(); // ✅ 提早快取

        if (fuelStack.isEmpty()) {
            return false;
        }

        ResourceLocation newFuelId = BuiltInRegistries.ITEM.getKey(fuelStack.getItem());
        if (newFuelId == null) {
            LOGGER.warn("[Mana Generator] ❌ 物品沒有有效 ID：{}", fuelStack.getItem());
            return false;
        }

        ManaGenFuelRateLoader.FuelRate fuelRate = ManaGenFuelRateLoader.getFuelRateForItem(newFuelId);

        // ✅ 模式判斷 —— 魔力模式要有 manaRate > 0，能源模式要有 burnTime > 0
        if (currentMode == Mode.MANA) {
            if (fuelRate == null || fuelRate.getManaRate() <= 0) {
                LOGGER.debug("[Mana Generator] ❌ 在 MANA 模式下無效燃料：{}", newFuelId);
                return false;
            }
        } else if (currentMode == Mode.ENERGY) {
            if (fuelRate == null || fuelRate.getBurnTime() <= 0) {
                LOGGER.debug("[Mana Generator] ❌ 在 ENERGY 模式下無效燃料：{}", newFuelId);
                return false;
            }
        }

        currentFuelId = newFuelId;
        burnTime = fuelRate.getBurnTime();
        currentBurnTime = burnTime;
        fuelHandler.extractItem(0, 1, false);

//        LOGGER.info("[Mana Generator] 🔥 開始燃燒：{} | burnTime: {} | manaRate: {}", currentFuelId, burnTime, fuelRate.getManaRate());
        return true;
    }

    private int getDynamicOutputRate() {
        if (ConfigManager.COMMON.useFuelBasedOutputRate.get()) {
            if (currentFuelId != null) {
                ManaGenFuelRateLoader.FuelRate fuelRate = ManaGenFuelRateLoader.getFuelRateForItem(currentFuelId);
                if (fuelRate != null) {
                    return Math.max(1, fuelRate.getBurnTime() / 2); // 可調整比例
                }
            }
            return 100; // 沒有對應燃料時的 fallback 預設值
        } else {
            return ConfigManager.COMMON.generatorOutputRate.get();
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
                        int energyToTransfer = Math.min(energyStorage.getEnergyStored(), getDynamicOutputRate());
                        if (energyToTransfer > 0) { // 防止負值錯誤
                            int acceptedEnergy = neighborEnergyStorage.receiveEnergy(energyToTransfer, false);
                            energyStorage.extractEnergy(acceptedEnergy, false);
                        }
                    }
                });

                // 魔力輸出
                neighborBlockEntity.getCapability(ModCapabilities.MANA, direction.getOpposite()).ifPresent(neighborManaStorage -> {
                    if (neighborManaStorage.canReceive()) {
                        int manaToTransfer = Math.min(manaStorage.getManaStored(), 50);
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

            return lazyManaStorage.cast();
        }

        return super.getCapability(cap, side);
    }


    @Override
    public void tickMachine() {

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
        tag.putInt("ManaStored", manaStorage.getManaStored());
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
