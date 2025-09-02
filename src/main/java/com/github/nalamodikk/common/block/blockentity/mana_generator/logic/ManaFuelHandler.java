package com.github.nalamodikk.common.block.blockentity.mana_generator.logic;

import com.github.nalamodikk.common.block.blockentity.mana_generator.recipe.loader.ManaGenFuelRateLoader;
import com.github.nalamodikk.common.block.blockentity.mana_generator.recipe.loader.ManaGenFuelRateLoader.FuelRate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Optional;

public class ManaFuelHandler {
    private final ManaGeneratorStateManager  stateManager;
    private ManaGeneratorUpgradeHandler upgradeHandler; // 可選的升級處理器
    private static final Logger LOGGER = LoggerFactory.getLogger(ManaFuelHandler.class);

    private final ItemStackHandler fuelHandler;
    private ResourceLocation currentFuelId;
    private int burnTime;
    private int currentBurnTime;
    private int failedFuelCooldown;
    private boolean isPaused;        // 是否暫停（因產出空間滿了）
    private boolean needsFuel = true;
    private boolean recoveryAttempted = false;
    private int pauseLogCounter = 0;
    private int resumeLogCounter = 0;
    private static final int LOG_EVERY = 5;


    public ManaFuelHandler( ItemStackHandler fuelHandler,ManaGeneratorStateManager stateManager) {
        this.fuelHandler = fuelHandler;
        this.stateManager = stateManager;
    }
    
    /**
     * 設置升級處理器（在 BlockEntity 初始化後調用）
     */
    public void setUpgradeHandler(ManaGeneratorUpgradeHandler upgradeHandler) {
        this.upgradeHandler = upgradeHandler;
    }


    public boolean hasAttemptedRecovery() {
        return recoveryAttempted;
    }

    public void markRecoveryAttempted() {
        this.recoveryAttempted = true;
    }

    public void resetRecoveryFlag() {
        this.recoveryAttempted = false;
    }


    public void tickCooldown() {
        if (failedFuelCooldown > 0) {
            failedFuelCooldown--;
            if (failedFuelCooldown == 0) {
                markNeedsFuel(); // cooldown 結束再嘗試消耗
            }
        }
    }

    public boolean tryConsumeFuel() {
        if (!needsFuel || burnTime > 0 || failedFuelCooldown > 0) {
            return false;
        }

        ItemStack fuel = fuelHandler.getStackInSlot(0);
        if (fuel.isEmpty() || fuel.getItem() == null) return false; // ← 加這裡保險

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(fuel.getItem());
        FuelRate rate = ManaGenFuelRateLoader.getFuelRateForItem(id);

        if (rate == null || rate.getBurnTime() <= 0) {
            failedFuelCooldown = 20; // cooldown 20 tick 再試
            needsFuel = false;       // 暫時不要再試，等待 cooldown
            return false;
        }

        switch (stateManager.getCurrentMode()) {
            case MANA -> {
                if (rate.getManaRate() <= 0) {
                    failedFuelCooldown = 20;
                    needsFuel = false;
                    return false;
                }
            }
            case ENERGY -> {
                if (rate.getEnergyRate() <= 0) {
                    failedFuelCooldown = 20;
                    needsFuel = false;
                    return false;
                }
            }
        }

        currentFuelId = id;
        // 🔧 應用升級效果到燃燒時間
        int baseBurnTime = rate.getBurnTime();
        currentBurnTime = upgradeHandler != null ? upgradeHandler.getModifiedBurnTime(baseBurnTime) : baseBurnTime;
        burnTime = currentBurnTime;
        fuelHandler.extractItem(0, 1, false);
        needsFuel = false; // 燃料已成功啟動，不需再嘗試
        recoveryAttempted = false;

        return true;


    }

    public void markNeedsFuel() {
        this.needsFuel = true;
    }

    public int getBurnTime() {
        return burnTime;
    }

    public int getCurrentBurnTime() {
        return currentBurnTime;
    }

    public ResourceLocation getCurrentFuelId() {
        return currentFuelId;
    }

    public boolean isCoolingDown() {
        return failedFuelCooldown > 0;
    }


    public void setCooldown() {
        this.failedFuelCooldown = 20;
    }



    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }


    public void setCurrentFuelId(@Nullable ResourceLocation id) {
        this.currentFuelId = id;
    }


    public void resetBurnTime() {
        this.burnTime = 0;
        this.currentBurnTime = 0;
    }

    public void tickBurn(boolean allowBurn) {
        if (!allowBurn || isPaused) return;

        if (burnTime > 0) {
            burnTime--;
            // 🔧 燃料燒完時重置 currentBurnTime
            if (burnTime == 0) {
                currentBurnTime = 0;
            }
        }
    }


    public void pauseBurn() {
        if (!isPaused) {
            isPaused = true;
            failedFuelCooldown = Math.max(failedFuelCooldown, 20);
            if (++pauseLogCounter >= LOG_EVERY) {
                LOGGER.debug("pauseBurn(): machine paused, cooldown = {}", failedFuelCooldown);
                pauseLogCounter = 0;
            }
        }
    }

    public void resumeBurn() {
        if (isPaused) {
            this.isPaused = false;
            if (++resumeLogCounter >= LOG_EVERY) {
                LOGGER.debug("resumeBurn(): machine resumed from pause");
                resumeLogCounter = 0;
            }
        }
    }

    public boolean isPaused() {
        return isPaused;
    }
    public float getFuelProgress() {
        if (currentBurnTime == 0) return 0f;
        return burnTime / (float) currentBurnTime;
    }


    public boolean isBurning() {
        return burnTime > 0;
    }

    public Optional<ManaGenFuelRateLoader.FuelRate> getCurrentFuelRate() {
        if (currentFuelId == null) return Optional.empty();

        ManaGenFuelRateLoader.FuelRate baseRate = ManaGenFuelRateLoader.getFuelRateForItem(currentFuelId);
        if (baseRate == null) return Optional.empty();

        // ✅ 根據模式篩選產能是否合法
        switch (stateManager.getCurrentMode()) {
            case MANA -> {
                if (baseRate.getManaRate() <= 0) return Optional.empty();
            }
            case ENERGY -> {
                if (baseRate.getEnergyRate() <= 0) return Optional.empty();
            }
        }

        // 🔧 應用升級效果到產出
        if (upgradeHandler != null) {
            int modifiedMana = upgradeHandler.getModifiedOutput(baseRate.getManaRate());
            int modifiedEnergy = upgradeHandler.getModifiedOutput(baseRate.getEnergyRate());
            
            // 創建修改後的燃料速率 - 使用當前實際的燃燒時間（已經過升級修改）
            return Optional.of(new ManaGenFuelRateLoader.FuelRate(
                modifiedMana,
                currentBurnTime > 0 ? currentBurnTime : baseRate.getBurnTime(), // 使用實際的燃燒時間
                modifiedEnergy,
                baseRate.getIntervalTick()
            ));
        }

        return Optional.of(baseRate);
    }

    public boolean isValidFuel(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // 使用現有的燃料系統檢查
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        ManaGenFuelRateLoader.FuelRate rate = ManaGenFuelRateLoader.getFuelRateForItem(itemId);

        // 檢查燃燒時間是否大於 0
        if (rate == null || rate.getBurnTime() <= 0) {
            return false;
        }

        // 根據當前模式檢查是否有對應的產出
        return switch (stateManager.getCurrentMode()) {
            case MANA -> rate.getManaRate() > 0;
            case ENERGY -> rate.getEnergyRate() > 0;
        };
    }

    // 或者如果您需要更詳細的檢查：
    public boolean canAcceptFuel(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // 檢查當前槽位是否有空間
        ItemStack currentFuel = fuelHandler.getStackInSlot(0);
        if (!currentFuel.isEmpty()) {
            // 如果已有燃料，檢查是否可以堆疊
            if (!ItemStack.isSameItemSameComponents(currentFuel, stack)) {
                return false;
            }
            // 檢查是否還有空間堆疊
            if (currentFuel.getCount() >= currentFuel.getMaxStackSize()) {
                return false;
            }
        }

        // 檢查是否為有效燃料
        return isValidFuel(stack);
    }

    // Setters for NBT loading
    public void setBurnTime(int burnTime) {
        this.burnTime = burnTime;
    }

    public void setCurrentBurnTime(int currentBurnTime) {
        this.currentBurnTime = currentBurnTime;
    }
}
