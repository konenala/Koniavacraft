    // ⚠ 自動產生：結合 NeoForge 能量與魔力產出邏輯
    package com.github.nalamodikk.common.block.mana_generator;

    import com.github.nalamodikk.KoniavacraftMod;
    import com.github.nalamodikk.common.block.conduit.ArcaneConduitBlockEntity;
    import com.github.nalamodikk.common.block.mana_generator.logic.*;
    import com.github.nalamodikk.common.block.mana_generator.recipe.loader.ManaGenFuelRateLoader;
    import com.github.nalamodikk.common.block.mana_generator.sync.ManaGeneratorSyncHelper;
    import com.github.nalamodikk.common.block.manabase.AbstractManaMachineEntityBlock;
    import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
    import com.github.nalamodikk.common.capability.ManaStorage;
    import com.github.nalamodikk.common.compat.energy.ModNeoNalaEnergyStorage;
    import com.github.nalamodikk.common.coreapi.machine.logic.gen.EnergyGenerationHandler;
    import com.github.nalamodikk.common.coreapi.machine.logic.gen.FuelManaGenHelper;
    import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
    import com.github.nalamodikk.register.ModBlockEntities;
    import com.github.nalamodikk.register.ModCapabilities;
    import net.minecraft.core.BlockPos;
    import net.minecraft.core.Direction;
    import net.minecraft.core.HolderLookup;
    import net.minecraft.nbt.CompoundTag;
    import net.minecraft.network.chat.Component;
    import net.minecraft.network.protocol.Packet;
    import net.minecraft.network.protocol.game.ClientGamePacketListener;
    import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
    import net.minecraft.server.level.ServerLevel;
    import net.minecraft.world.entity.player.Inventory;
    import net.minecraft.world.entity.player.Player;
    import net.minecraft.world.inventory.AbstractContainerMenu;
    import net.minecraft.world.inventory.ContainerData;
    import net.minecraft.world.inventory.ContainerLevelAccess;
    import net.minecraft.world.item.ItemStack;
    import net.minecraft.world.level.Level;
    import net.minecraft.world.level.block.Block;
    import net.minecraft.world.level.block.entity.BlockEntity;
    import net.minecraft.world.level.block.entity.BlockEntityTicker;
    import net.minecraft.world.level.block.entity.BlockEntityType;
    import net.minecraft.world.level.block.state.BlockState;
    import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
    import net.neoforged.neoforge.capabilities.Capabilities;
    import net.neoforged.neoforge.energy.IEnergyStorage;
    import net.neoforged.neoforge.items.IItemHandler;
    import net.neoforged.neoforge.items.ItemStackHandler;
    import org.jetbrains.annotations.Nullable;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import software.bernie.geckolib.animatable.GeoAnimatable;
    import software.bernie.geckolib.animatable.GeoBlockEntity;
    import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
    import software.bernie.geckolib.animation.*;
    import software.bernie.geckolib.util.GeckoLibUtil;

    import java.util.EnumMap;
    import java.util.Optional;

    public class ManaGeneratorBlockEntity extends AbstractManaMachineEntityBlock implements   GeoBlockEntity {

        private static final Logger LOGGER = LoggerFactory.getLogger(ManaGeneratorBlockEntity.class);
 private final EnumMap<Direction, BlockCapabilityCache<IUnifiedManaHandler, Direction>> manaCaches = new EnumMap<>(Direction.class);
        private final EnumMap<Direction, BlockCapabilityCache<IEnergyStorage, Direction>> energyCaches = new EnumMap<>(Direction.class);

        private static final int MAX_MANA = 200000;
        private static final int MAX_ENERGY = 200000;
        private static final int TICK_INTERVAL = 1;
        private static final int MANA_PER_CYCLE = 10;
        private static final int FUEL_SLOT_COUNT = 1;
        private static final int MANA_STORED_INDEX = 0;
        private static final int ENERGY_STORED_INDEX = 1;
        private static final int MODE_INDEX = 2;
        private static final int BURN_TIME_INDEX = 3;
        private static final int CURRENT_BURN_TIME_INDEX = 4;
        private static final int DEFAULT_ENERGY_PER_TICK = 40; // 或你想用的預設值
        // 替代原本的 UnifiedSyncManager syncManager
        private final ManaGeneratorSyncHelper syncHelper = new ManaGeneratorSyncHelper();
        private final FuelManaGenHelper manaGenHandler;
        private final EnergyGenerationHandler energyGenHandler;
        private final ManaGeneratorTicker ticker = new ManaGeneratorTicker(this);
        private final EnumMap<Direction, IOHandlerUtils.IOType> ioMap = new EnumMap<>(Direction.class);
        private final OutputHandler.OutputThrottleController outputThrottle = new OutputHandler.OutputThrottleController();

        private final ManaGeneratorStateManager stateManager = new ManaGeneratorStateManager();
        private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
        private static final RawAnimation WORKING_ANIM = RawAnimation.begin().thenLoop("working");


        private final ItemStackHandler fuelHandler = new ItemStackHandler(FUEL_SLOT_COUNT);
        private ContainerLevelAccess access;
        private int burnTime = 0;
        private int currentBurnTime = 0;
        // ✅ 用來避免每幀都重播動畫，造成動畫 reset、跳針或閃爍
        private String currentAnimation = "";
        private boolean forceRefreshAnimation = false;

        private final ManaFuelHandler fuelLogic = new ManaFuelHandler(fuelHandler,stateManager);
        public OutputHandler.OutputThrottleController getOutputThrottle() {return outputThrottle;}

        public ManaGeneratorBlockEntity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.MANA_GENERATOR_BE.get(), pos, state, true, MAX_MANA, MAX_ENERGY, TICK_INTERVAL, MANA_PER_CYCLE);
            if (this.level != null) {
                this.access = ContainerLevelAccess.create(this.level, this.worldPosition);
            }
            this.manaGenHandler = new FuelManaGenHelper(this.manaStorage, this::getCurrentFuelRate, (amount) -> {});

            this.energyGenHandler = new EnergyGenerationHandler(this.energyStorage, () -> {
                Optional<ManaGenFuelRateLoader.FuelRate> rate = getCurrentFuelRate();
                return rate.map(ManaGenFuelRateLoader.FuelRate::getEnergyRate).orElse(DEFAULT_ENERGY_PER_TICK);
            });
            for (Direction dir : Direction.values()) {
                ioMap.put(dir, IOHandlerUtils.IOType.DISABLED); // 或從 NBT、DataComponent 還原
            }

        }

        @Override
        public void drops(Level level, BlockPos pos) {
            super.drops(level, pos);
            if (level == null || level.isClientSide) return;

            IItemHandler handler = this.fuelHandler;
            if (handler == null) return;

            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    Block.popResource(level, pos, stack);
                }
            }
        }


        public static int getManaStoredIndex() {return MANA_STORED_INDEX;}
        public static int getEnergyStoredIndex() {return ENERGY_STORED_INDEX;}
        public static int getModeIndex() {return MODE_INDEX;}
        public static int getBurnTimeIndex() {return BURN_TIME_INDEX;}
        public static int getCurrentBurnTimeIndex() {return CURRENT_BURN_TIME_INDEX;}
        public static int getMaxMana() {return MAX_MANA;}
        public static int getMaxEnergy() {return MAX_ENERGY;}
        public ManaGeneratorStateManager getStateManager() {return stateManager;}
        public FuelManaGenHelper getManaGenHandler() {return manaGenHandler;}
        public EnergyGenerationHandler getEnergyGenHandler() {return energyGenHandler;}
        public ManaStorage getManaStorage() {return manaStorage;}
        public ModNeoNalaEnergyStorage getEnergyStorage() {return energyStorage;}
        private final ManaGeneratorNbtManager nbtManager = new ManaGeneratorNbtManager(this);
        public ManaGeneratorSyncHelper getSyncHelper() {return syncHelper;}

        // set
        public void setBurnTimeFromNbt(int value) {this.burnTime = value;}
        public void setCurrentBurnTimeFromNbt(int value) {this.currentBurnTime = value;}
        public void forceRefreshAnimationFromNbt() {this.forceRefreshAnimation = true;}

        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        public EnumMap<Direction, BlockCapabilityCache<IUnifiedManaHandler,  Direction>> getManaOutputCaches() {
            return manaCaches;
        }

        public EnumMap<Direction, BlockCapabilityCache<IEnergyStorage, Direction>> getEnergyOutputCaches() {
            return energyCaches;
        }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            AnimationController<ManaGeneratorBlockEntity> controller =
                    new AnimationController<>(this, "mana_generator_controller", 0, this::predicate);

            controllers.add(controller);

            // ✅ 初始設 idle 動畫，避免一開始或 ESC 回來變空白
            controller.setAnimation(RawAnimation.begin().thenLoop("idle"));
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }

        private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> state) {
            String targetAnimation = stateManager.isWorking() ? "working" : "idle";

            if (!targetAnimation.equals(currentAnimation) || forceRefreshAnimation) {
                String oldAnimation = currentAnimation;
                state.getController().setAnimation(RawAnimation.begin().thenLoop(targetAnimation));
                currentAnimation = targetAnimation;
                forceRefreshAnimation = false;

//                MagicalIndustryMod.LOGGER.debug("[Anim] Switching animation: {} → {}", oldAnimation, targetAnimation);
            }

            return PlayState.CONTINUE;
        }
        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
            super.saveAdditional(tag, provider);
            nbtManager.save(tag, provider);
        }


        @Override
        public void onLoad() {
            super.onLoad();
            if (level instanceof ServerLevel serverLevel) {
                initializeCapabilityCaches(serverLevel);
            }
        }

        private void initializeCapabilityCaches(ServerLevel serverLevel) {
            for (Direction dir : Direction.values()) {
                BlockPos targetPos = worldPosition.relative(dir);
                Direction inputSide = dir.getOpposite();

                manaCaches.put(dir, BlockCapabilityCache.create(
                        ModCapabilities.MANA,
                        serverLevel,
                        targetPos,
                        inputSide,
                        () -> !this.isRemoved(),
                        () -> {}
                ));

                energyCaches.put(dir, BlockCapabilityCache.create(
                        Capabilities.EnergyStorage.BLOCK,
                        serverLevel,
                        targetPos,
                        inputSide,
                        () -> !this.isRemoved(),
                        () -> {}
                ));
            }
        }

        public @Nullable IUnifiedManaHandler getCachedManaCapability(Direction dir) {
            var cache = manaCaches.get(dir);
            return cache != null ? cache.getCapability() : null;
        }

        public @Nullable IEnergyStorage getCachedEnergyCapability(Direction dir) {
            var cache = energyCaches.get(dir);
            return cache != null ? cache.getCapability() : null;
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
            super.loadAdditional(tag, provider);
            nbtManager.load(tag, provider);
        }

        public ManaFuelHandler getFuelLogic() {
            return fuelLogic;
        }

        @Override
        public void setLevel(Level level) {
            super.setLevel(level);
            this.access = ContainerLevelAccess.create(level, this.worldPosition);
        }



        public void markUpdated() {
            if (this.level != null) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
        }

        public Optional<ManaGenFuelRateLoader.FuelRate> getCurrentFuelRate() {
            return fuelLogic.getCurrentFuelRate();
        }


        public IItemHandler getInventory() {
            return fuelHandler;
        }



        @Override
        public void tickMachine() {
            ticker.tick();
        }

        @Override
        protected boolean canGenerate() {
            return false;
        }

        public void sync() {
            if (this.level != null && !this.level.isClientSide()) {
                this.setChanged();
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
            syncHelper.syncFrom(this);         // ➤ 更新資料並標記 dirty
            syncHelper.flushSyncState(this);   // ➤ ✅ 現在就清掉 dirty
        }


        public ContainerData getContainerData() {
            return syncHelper.getContainerData();
        }

        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
            CompoundTag tag = new CompoundTag();
            this.saveAdditional(tag, registries);
            return tag;
        }


        public void toggleMode() {
            if (stateManager.toggleMode(this.getBurnTime())) {
                this.setChanged();
                this.syncToClient();
                syncHelper.setModeIndex(stateManager.getCurrentModeIndex());
            }
        }


        public void syncToClient() {
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }


        public int getCurrentMode() {
            return stateManager.getCurrentModeIndex();
        }



        @Override
        public void setIOConfig(Direction direction, IOHandlerUtils.IOType type) {
            IOHandlerUtils.IOType oldType = ioMap.get(direction);
            if (oldType != type) {
                ioMap.put(direction, type);
                setChanged();

                // 🔧 關鍵：通知能力系統刷新
                if (level != null && !level.isClientSide) {
                    level.invalidateCapabilities(worldPosition);

                    // ✅ 【新增】：通知鄰近方塊 (特別是導管) 重新檢查連接
                    notifyNeighborsOfIOChange();
                }
            }
        }

        @Override
        public IOHandlerUtils.IOType getIOConfig(Direction direction) {
            return ioMap.getOrDefault(direction, IOHandlerUtils.IOType.DISABLED);
        }

        @Override
        public EnumMap<Direction, IOHandlerUtils.IOType> getIOMap() {
            return new EnumMap<>(ioMap); // 返回副本，避免外部修改
        }

        @Override
        public void setIOMap(EnumMap<Direction, IOHandlerUtils.IOType> map) {
            boolean changed = false;
            for (Direction dir : Direction.values()) {
                IOHandlerUtils.IOType newType = map.getOrDefault(dir, IOHandlerUtils.IOType.DISABLED);
                if (!ioMap.get(dir).equals(newType)) {
                    ioMap.put(dir, newType);
                    changed = true;
                }
            }

            if (changed) {
                setChanged();

                // 🔧 關鍵：通知能力系統刷新
                if (level != null && !level.isClientSide) {
                    level.invalidateCapabilities(worldPosition);

                    // ✅ 【新增】：通知鄰近方塊重新檢查連接
                    notifyNeighborsOfIOChange();
                }
            }
        }

        // ✅ 【新增方法】：通知所有鄰居IO配置已改變
        private void notifyNeighborsOfIOChange() {
            if (level == null || level.isClientSide) return;

            BlockState currentState = level.getBlockState(worldPosition);

            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = worldPosition.relative(direction);

                // 觸發鄰居方塊的 neighborChanged 方法
                level.neighborChanged(neighborPos, currentState.getBlock(), worldPosition);

                // 如果鄰居是導管，額外調用其特定的鄰居變化處理
                BlockEntity neighborBE = level.getBlockEntity(neighborPos);
                if (neighborBE instanceof ArcaneConduitBlockEntity conduit) {
                    conduit.onNeighborChanged();
                }
            }

            LOGGER.debug("魔力發電機 {} IO設定變更，已通知所有鄰居", worldPosition);
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("block." + KoniavacraftMod.MOD_ID + ".mana_generator");
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new ManaGeneratorMenu(id, inv, this);
        }

        public ItemStackHandler getFuelHandler() {
            return fuelHandler;
        }

        public int getBurnTime() {
            return burnTime;
        }

        public int getCurrentBurnTime() {
            return currentBurnTime;
        }

        public boolean isWorking() {
            return stateManager.isWorking();
        }


        public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockEntityType<T> type) {
            return (lvl, pos, state, blockEntity) -> {
                if (blockEntity instanceof ManaGeneratorBlockEntity entity) {
                    entity.tickMachine();
                }
            };
        }

        public void updateBlockActiveState(boolean isWorking) {
            if (level == null) return;

            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(ManaGeneratorBlock.ACTIVE) && state.getValue(ManaGeneratorBlock.ACTIVE) != isWorking) {
                level.setBlock(worldPosition, state.setValue(ManaGeneratorBlock.ACTIVE, isWorking), 3);
            }
        }


    }