package com.github.nalamodikk.common.block.mana_generator.logic;

import com.github.nalamodikk.common.block.mana_generator.ManaGeneratorBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

public class ManaGeneratorTicker {
    private final ManaGeneratorBlockEntity machine;
    public static final Logger LOGGER = LogUtils.getLogger();
    private int tickCounter = 0;

    public ManaGeneratorTicker(ManaGeneratorBlockEntity machine) {
        this.machine = machine;
    }

    public void tick() {
        tickCounter++;

        ManaFuelHandler fuelHandler = machine.getFuelLogic();
        boolean changed = false;

        fuelHandler.tickCooldown();

        if (!fuelHandler.isBurning() && !fuelHandler.isCoolingDown() && fuelHandler.isPaused()) {
            if (!fuelHandler.hasAttemptedRecovery()) {
                LOGGER.debug("[FuelSafety] Recovering from paused-but-not-cooling state");
                fuelHandler.markNeedsFuel();
                fuelHandler.setPaused(false);
                fuelHandler.markRecoveryAttempted();
            }
        }

        if (!fuelHandler.isBurning()) {
            if (tickCounter % 4 == 0 && !fuelHandler.tryConsumeFuel()) {
                if (machine.getStateManager().setWorking(false)) {
                    machine.updateBlockActiveState(false);
                    changed = true;
                }
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
            if (machine.getStateManager().setWorking(false)) {
                machine.updateBlockActiveState(false);
                changed = true;
            }
        } else {
            fuelHandler.resumeBurn();
            fuelHandler.tickBurn(true);

            // 🎯 在 tickBurn 後面加這個檢查
            if (!fuelHandler.isBurning()) {
                // 燃料燒完了！立即停止工作
                if (machine.getStateManager().setWorking(false)) {
                    machine.updateBlockActiveState(false);
                    changed = true;
                }
            } else {
                // 還在燃燒中，保持工作狀態
                if (machine.getStateManager().setWorking(true)) {
                    machine.updateBlockActiveState(true);
                    changed = true;
                }
            }

            if (machine.getLevel() instanceof ServerLevel serverLevel) {
                if (machine.getOutputThrottle().shouldTryOutput()) {
                    boolean outputSuccess = OutputHandler.tryOutput(
                            serverLevel,
                            machine.getBlockPos(),
                            machine.getManaStorage(),
                            machine.getEnergyStorage(),
                            machine.getIOMap(),
                            machine.getManaOutputCaches(),
                            machine.getEnergyOutputCaches()
                    );
                    machine.getOutputThrottle().recordOutputResult(outputSuccess);
                }
            }
        }

        if (changed) {
            machine.sync();
        }
    }



}
