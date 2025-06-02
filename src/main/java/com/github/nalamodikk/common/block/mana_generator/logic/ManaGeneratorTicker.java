package com.github.nalamodikk.common.block.mana_generator.logic;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.mana_generator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.block.mana_generator.recipe.loader.ManaGenFuelRateLoader;
import com.github.nalamodikk.common.block.mana_generator.logic.OutputHandler;
import com.github.nalamodikk.common.block.mana_generator.sync.ManaGeneratorSyncHelper;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

public class ManaGeneratorTicker {
    private final ManaGeneratorBlockEntity machine;
    public static final Logger LOGGER = LogUtils.getLogger();

    public ManaGeneratorTicker(ManaGeneratorBlockEntity machine) {
        this.machine = machine;
    }

    public void tick() {
        ManaFuelHandler fuelHandler = machine.getFuelLogic();
        boolean changed = false;

        fuelHandler.tickCooldown();

        // ✅ 若燃燒已停、未在 cooldown、卻處於 pause → 重設需要燃料
        if (!fuelHandler.isBurning() && !fuelHandler.isCoolingDown() && fuelHandler.isPaused()) {
            LOGGER.debug("[FuelSafety] Recovering from paused-but-not-cooling state");
            fuelHandler.markNeedsFuel(); // ✅ 強制再試一次
            fuelHandler.setPaused(false);
        }


        if (!fuelHandler.isBurning()) {
            if (!fuelHandler.tryConsumeFuel()) {
                machine.getStateManager().setWorking(false);
                machine.updateBlockActiveState(false);
                // 不返回，因為 cooldown 可能剛 tick 結束，可以立即進入發電階段
            }
        }
        boolean success = false;

        if (fuelHandler.isBurning()) {
            success = switch (machine.getStateManager().getCurrentMode()) {
                case MANA -> machine.getManaGenHandler().generate();
                case ENERGY -> machine.getEnergyGenHandler().generate();
            };
        }

        if (!success) {
            fuelHandler.pauseBurn();
            machine.getStateManager().setWorking(false);
            machine.updateBlockActiveState(false);
            changed = true;
        } else {
            fuelHandler.resumeBurn();
            fuelHandler.tickBurn(true);
            machine.getStateManager().setWorking(true);

            if (machine.getLevel() instanceof ServerLevel serverLevel) {
                if (machine.getOutputThrottle().shouldTryOutput()) {
                    boolean outputSuccess = OutputHandler.tryOutput(
                            serverLevel,
                            machine.getBlockPos(),
                            machine.getManaStorage(),
                            machine.getEnergyStorage(),
                            machine.getIOMap()
                    );
                    machine.getOutputThrottle().recordOutputResult(outputSuccess);
                }
            }


            machine.updateBlockActiveState(true);
            changed = true;
        }

        if (changed) {
            machine.sync(); // ✅ 只在必要時同步
        }


    }


}
