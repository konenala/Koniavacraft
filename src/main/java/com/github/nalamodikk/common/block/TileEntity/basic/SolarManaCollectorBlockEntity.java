package com.github.nalamodikk.common.block.TileEntity.basic;

import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.block.TileEntity.AbstractManaCollectorBlock;
import com.github.nalamodikk.common.capability.IHasMana;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.registry.ModBlockEntities;
import com.github.nalamodikk.common.screen.manacollector.SolarManaCollectorMenu;
import com.github.nalamodikk.common.upgrade.UpgradeInventory;
import com.github.nalamodikk.common.upgrade.UpgradeType;
import com.github.nalamodikk.common.upgrade.api.IUpgradeableMachine;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

import java.util.EnumMap;
import java.util.Map;

/**
 * ☀️ 太陽能原能收集器
 * - 僅在白天且可見天空時產生魔力
 * - 高海拔（Y > 100）加成效率（額外產能）
 */
public class SolarManaCollectorBlockEntity extends AbstractManaCollectorBlock implements IConfigurableBlock , MenuProvider , IUpgradeableMachine {
    private final Map<Direction, Boolean> directionConfig = new EnumMap<>(Direction.class);
    private static final int UPGRADE_SLOT_COUNT = 4;
    private static final int BASE_GENERATE = 5;
    private final UpgradeInventory upgrades = new UpgradeInventory(UPGRADE_SLOT_COUNT);
    private final LazyOptional<IItemHandler> upgradeHandler = LazyOptional.of(() -> new InvWrapper(upgrades));
    private boolean isCurrentlyGenerating = false;

    private int lastGeneratedAmount = 0;
    public static final Logger LOGGER = LogUtils.getLogger();


    private static final int BASE_INTERVAL = 40;       // 每 40 tick 嘗試一次（2 秒）
    private static final int BASE_OUTPUT = 5;          // 每次產出 5 mana（晴天正常條件）
    private static final int MAX_MANA = 800;          // 儲存上限

    public SolarManaCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_MANA_COLLECTOR_BE.get(), pos, state, MAX_MANA, BASE_INTERVAL, BASE_OUTPUT);

    }


    @Override
    public UpgradeInventory getUpgradeInventory() {
        return upgrades;
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }


    /**
     * 判斷是否符合太陽能條件：
     * - 白天
     * - 非下雨/雷雨
     * - 天空可見
     */
    @Override
    protected boolean canGenerate() {
        return level.isDay() && !level.isRaining() && level.canSeeSky(worldPosition.above());
    }

    public boolean isGenerating() {
        return canGenerate();
    }


    @Override
    protected int computeManaAmount() {
        int efficiencyLevel = upgrades.getUpgradeCount(UpgradeType.EFFICIENCY);
        double multiplier = 1.0 + efficiencyLevel * 0.1; // 每個效率升級 +10%

        int total = (int) (BASE_GENERATE * multiplier);
        lastGeneratedAmount = total;
        return total;
    }

    @Override
    protected void onGenerate(int baseAmount) {
        if (!level.isClientSide) {
            int amount = baseAmount;

            int speedLevel = upgrades.getUpgradeCount(UpgradeType.SPEED);
            int interval = Math.max(10, 200 - speedLevel * 20); // 每個速度升級快 20tick，最少 10tick

            if (!level.isClientSide && level.getGameTime() % 50 == 0) {
                boolean newState = canGenerate();
                if (newState != isCurrentlyGenerating) {
                    isCurrentlyGenerating = newState;
                    setChanged();
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            }
            if (level.getGameTime() % interval == 0) {
                LOGGER.debug("SolarManaCollector generated {} mana at {}", amount, worldPosition);

                int inserted = this.getManaStorage().insertMana(amount, ManaAction.EXECUTE);
                if (inserted > 0) {
                    setChanged();
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }

                outputMana(inserted);

                // Server 端粒子
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ENCHANT,
                            worldPosition.getX() + 0.5,
                            worldPosition.getY() + 1.1,
                            worldPosition.getZ() + 0.5,
                            2, 0.2, 0.1, 0.2, 0.0);
                }
            }
        }
    }

    public boolean isCurrentlyGenerating() {
        return isCurrentlyGenerating;
    }

    public void setCurrentlyGenerating(boolean value) {
        this.isCurrentlyGenerating = value;
    }


    public int getMaxMana() {
        return MAX_MANA;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SolarManaCollectorMenu(id, inv, this);
    }



    @Override
    public void setDirectionConfig(Direction direction, boolean isOutput) {
        directionConfig.put(direction, isOutput);
        setChanged();

    }

    @Override
    public boolean isOutput(Direction direction) {
        return directionConfig.getOrDefault(direction, false);
    }

    @Override
    public void drops() {
        SimpleContainer inventory = new SimpleContainer(0); // 目前沒槽，未來支援升級可擴充
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return upgradeHandler.cast();
        }
        return super.getCapability(cap, side);
    }


    @Override
    public Component getDisplayName() {
        return Component.translatable("block.magical_industry.solar_mana_collector");
    }



    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }
    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        CompoundTag out = new CompoundTag();
        for (Direction dir : Direction.values()) {
            out.putBoolean(dir.getName(), directionConfig.getOrDefault(dir, false));
        }
        tag.put("output_dirs", out);
        tag.put("Upgrades", upgrades.serializeNBT()); // ✅ 用你自己的 UpgradeInventory 寫入
        tag.put("Mana", manaStorage.serializeNBT()); // ✅ 用物件自己寫入

    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("output_dirs")) {
            CompoundTag out = tag.getCompound("output_dirs");
            for (Direction dir : Direction.values()) {
                directionConfig.put(dir, out.getBoolean(dir.getName()));
            }
        }
        if (tag.contains("Upgrades")) {
            upgrades.deserializeNBT(tag.getCompound("Upgrades")); // ✅ 正確讀回來
        }
        if (tag.contains("Mana")) {
            manaStorage.deserializeNBT(tag.getCompound("Mana")); // ✅ 讀出來給 manaStorage
        }
    }

    public void outputMana(int amount) {
        for (Direction dir : Direction.values()) {
            if (this.isOutput(dir)) {
                BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
                if (neighbor instanceof IHasMana target) {
                    int transferred = target.getManaStorage().insertMana(5, ManaAction.EXECUTE);
                    if (transferred > 0) {
                        this.getManaStorage().extractMana(transferred, ManaAction.EXECUTE);
                    }
                }
            }
        }
    }


    public ManaStorage getManaStorage() {
        return this.manaStorage;
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        upgradeHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return null;
    }
}
