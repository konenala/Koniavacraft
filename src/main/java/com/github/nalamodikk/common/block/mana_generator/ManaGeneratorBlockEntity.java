// ⚠ 自動產生：結合 NeoForge 能量與魔力產出邏輯
package com.github.nalamodikk.common.block.mana_generator;

import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.mana_generator.logic.*;
import com.github.nalamodikk.common.block.mana_generator.sync.ManaGeneratorSyncHelper;
import com.github.nalamodikk.common.block.manabase.AbstractManaMachineEntityBlock;
import com.github.nalamodikk.common.block.mana_generator.recipe.loader.ManaGenFuelRateLoader;
import com.github.nalamodikk.common.register.ModBlockEntities;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ManaGeneratorBlockEntity extends AbstractManaMachineEntityBlock implements IConfigurableBlock , GeoBlockEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManaGeneratorBlockEntity.class);

    public enum Mode {
        MANA,
        ENERGY
    }

    private static final int MAX_MANA = 200000;
    private static final int MAX_ENERGY = 200000;
    private static final int TICK_INTERVAL = 1;
    private static final int MANA_PER_CYCLE = 10;
    private static final int SYNC_DATA_COUNT = 5;
    private static final int FUEL_SLOT_COUNT = 1;
    private static final int MANA_STORED_INDEX = 0;
    private static final int ENERGY_STORED_INDEX = 1;
    private static final int MODE_INDEX = 2;
    private static final int BURN_TIME_INDEX = 3;
    private static final int CURRENT_BURN_TIME_INDEX = 4;
    private static final int DEFAULT_ENERGY_PER_TICK = 40; // 或你想用的預設值
    // 替代原本的 UnifiedSyncManager syncManager
    private final ManaGeneratorSyncHelper syncHelper = new ManaGeneratorSyncHelper();
    private ManaGenerationHandler manaGenHandler;
    private EnergyGenerationHandler energyGenHandler;

    private final ManaGeneratorStateManager stateManager = new ManaGeneratorStateManager();
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("misc.idle");
    private static final RawAnimation WORKING_ANIM = RawAnimation.begin().thenLoop("misc.working");


    private final ItemStackHandler fuelHandler = new ItemStackHandler(FUEL_SLOT_COUNT);
    private final EnumMap<Direction, Boolean> directionConfig = new EnumMap<>(Direction.class);
    private ContainerLevelAccess access;
    private int burnTime = 0;
    private int currentBurnTime = 0;

    private final ManaFuelHandler fuelLogic = new ManaFuelHandler(fuelHandler);

    public ManaGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_GENERATOR_BE.get(), pos, state, true, MAX_MANA, MAX_ENERGY, TICK_INTERVAL, MANA_PER_CYCLE);
        if (this.level != null) {
            this.access = ContainerLevelAccess.create(this.level, this.worldPosition);
        }
        this.manaGenHandler = new ManaGenerationHandler(this.manaStorage, this::getCurrentFuelRate, (amount) -> {});

        this.energyGenHandler = new EnergyGenerationHandler(this.energyStorage, () -> {
            Optional<ManaGenFuelRateLoader.FuelRate> rate = getCurrentFuelRate();
            return rate.map(ManaGenFuelRateLoader.FuelRate::getEnergyRate).orElse(DEFAULT_ENERGY_PER_TICK);
        });

    }

    public static int getManaStoredIndex() {return MANA_STORED_INDEX;}
    public static int getEnergyStoredIndex() {return ENERGY_STORED_INDEX;}
    public static int getModeIndex() {return MODE_INDEX;}
    public static int getBurnTimeIndex() {return BURN_TIME_INDEX;}
    public static int getCurrentBurnTimeIndex() {return CURRENT_BURN_TIME_INDEX;}
    public static int getDataCount() {return SYNC_DATA_COUNT;}
    public static int getMaxMana() {return MAX_MANA;}
    public static int getMaxEnergy() {return MAX_ENERGY;}
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "mana_generator_controller", 0, this::predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
        if (stateManager.isWorking()) {
            tAnimationState.getController().setAnimation(WORKING_ANIM);
        } else {
            tAnimationState.getController().setAnimation(IDLE_ANIM);
        }
        return PlayState.CONTINUE;
    }


    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        this.access = ContainerLevelAccess.create(level, this.worldPosition);
    }

    public static Map<Item, Integer> getAllFuelItems(Level level) {
        Map<Item, Integer> fuelMap = new HashMap<>();

        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            int burnTime = net.minecraft.world.level.block.entity.FurnaceBlockEntity.getFuel().getOrDefault(item, 0);

            if (burnTime > 0) {
                fuelMap.put(item, burnTime);
            }
        }

        return fuelMap;
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
        if (fuelLogic.isCoolingDown()) {
            fuelLogic.tickCooldown();
            return;
        }

        if (!fuelLogic.isBurning()) {
            if (!fuelLogic.tryConsumeFuel()) {
                stateManager.setWorking(false); // ✅ 改用 stateManager 控制工作狀態
                fuelLogic.setCooldown();
                return;
            }
        }

        fuelLogic.tickBurn();

        boolean success = switch (stateManager.getCurrentMode()) {
            case MANA -> manaGenHandler.generate();
            case ENERGY -> energyGenHandler.generate();
        };


        stateManager.setWorking(success); // ✅ 統一寫入狀態

        if (level instanceof ServerLevel serverLevel) {
            OutputHandler.tryOutput(serverLevel, worldPosition, manaStorage, energyStorage, directionConfig);
        }

        this.sync();
    }

    @Override
    protected boolean canGenerate() {
        return false;
    }

    private void sync() {
        syncHelper.syncFrom(this);
    }

    public ContainerData getContainerData() {
        return syncHelper.getContainerData();
    }


    public void toggleMode() {
        stateManager.toggleMode(burnTime);
    }


    public int getCurrentMode() {
        return stateManager.getCurrentModeIndex();
    }


    @Override
    public boolean isOutput(Direction direction) {
        return directionConfig.getOrDefault(direction, false);
    }

    @Override
    public void setDirectionConfig(Direction direction, boolean isOutput) {
        directionConfig.put(direction, isOutput);
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block." + MagicalIndustryMod.MOD_ID + ".mana_generator");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return  new ManaGeneratorMenu(id, inv, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));

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
}
