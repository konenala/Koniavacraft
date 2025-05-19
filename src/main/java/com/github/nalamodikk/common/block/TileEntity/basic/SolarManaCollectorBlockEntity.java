package com.github.nalamodikk.common.block.TileEntity.basic;

import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.TileEntity.AbstractManaCollectorMachine;
import com.github.nalamodikk.common.capability.IHasMana;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.register.ModBlockEntities;
import com.github.nalamodikk.common.register.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

import java.util.EnumMap;
import java.util.Map;

/**
 * ☀️ 太陽能原能收集器
 * - 僅在白天且可見天空時產生魔力
 * - 高海拔（Y > 100）加成效率（額外產能）
 */
public class SolarManaCollectorBlockEntity extends AbstractManaCollectorMachine implements IConfigurableBlock {
    private final Map<Direction, Boolean> directionConfig = new EnumMap<>(Direction.class);
    private static final int UPGRADE_SLOT_COUNT = 5;
    private final ItemStackHandler upgradeSlot = new ItemStackHandler(UPGRADE_SLOT_COUNT);
    private static final int BASE_GENERATE = 5;
    private int tickCounter = 0;

    private int lastGeneratedAmount = 0;


    private static final int BASE_INTERVAL = 40;       // 每 40 tick 嘗試一次（2 秒）
    private static final int BASE_OUTPUT = 5;          // 每次產出 5 mana（晴天正常條件）
    private static final int MAX_MANA = 80000;          // 儲存上限

    public SolarManaCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_MANA_COLLECTOR_BE.get(), pos, state, MAX_MANA, BASE_INTERVAL, BASE_OUTPUT);

    }

    /**
     * 判斷是否符合太陽能條件：
     * - 白天
     * - 非下雨/雷雨
     * - 天空可見
     */
    @Override
    protected boolean canGenerate() {
        if (!level.isDay()) return false;
        if (level.isRaining()) return false;
        if (!level.canSeeSky(worldPosition.above())) return false;
        return true;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return null;
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
            return LazyOptional.of(() -> upgradeSlot).cast();
        }
        return super.getCapability(cap, side);
    }


    @Override
    public Component getDisplayName() {
        return Component.translatable("block.magical_industry.solar_mana_collector");
    }

    public ItemStack getUpgradeItem() {
        return upgradeSlot.getStackInSlot(0);
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
        tag.put("UpgradeSlot", upgradeSlot.serializeNBT());

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
        if (tag.contains("UpgradeSlot")) {
            upgradeSlot.deserializeNBT(tag.getCompound("UpgradeSlot"));
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SolarManaCollectorBlockEntity be) {
        be.tickCounter++;

        // 每 40 tick 嘗試產生 mana
        if (be.tickCounter >= 40) {
            be.tickCounter = 0;

            if (be.shouldGenerate(level, pos)) {
                int manaGenerated = be.generateMana();
                // 已經自動記錄到 lastGeneratedAmount，可省略
            }

        }

        // 🎆 粒子特效（client only，表示正在運作）
        if (level.isClientSide) {
            level.addParticle(ParticleTypes.ENCHANT,
                    pos.getX() + 0.5 + Mth.nextDouble(level.random, -0.2, 0.2),
                    pos.getY() + 1.1,
                    pos.getZ() + 0.5 + Mth.nextDouble(level.random, -0.2, 0.2),
                    0, 0.05, 0
            );
        }

        // 🧪 Debug log（每 200 tick 顯示一次）
        if (!level.isClientSide && level.getGameTime() % 200 == 0) {
            MagicalIndustryMod.LOGGER.debug("SolarManaCollector generated {} mana at {}", be.lastGeneratedAmount, pos);
        }

    }


    public boolean shouldGenerate(Level level, BlockPos pos) {
        return level.canSeeSky(pos.above()) && level.isDay() && !level.isRaining();
    }

    public int generateMana() {
        int bonus = 0;

        for (int i = 0; i < upgradeSlot.getSlots(); i++) {
            ItemStack stack = upgradeSlot.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(ModItems.SOLAR_MANA_UPGRADE.get())) {
                bonus += 5;
            }
        }

        int totalGenerated = BASE_GENERATE + bonus;
        this.getManaStorage().insertMana(totalGenerated, ManaAction.EXECUTE);
        this.lastGeneratedAmount = totalGenerated; // 記錄下這次產出

        return totalGenerated;
    }



    public void outputMana() {
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
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return null;
    }
}
