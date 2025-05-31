package com.github.nalamodikk.common.block.mana_generator.logic;

import com.github.nalamodikk.common.block.mana_generator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.block.mana_generator.recipe.loader.ManaGenFuelRateLoader;
import com.github.nalamodikk.common.block.mana_generator.logic.OutputHandler;
import com.github.nalamodikk.common.block.mana_generator.sync.ManaGeneratorSyncHelper;
import net.minecraft.server.level.ServerLevel;

public class ManaGeneratorTicker {
    private final ManaGeneratorBlockEntity machine;

    public ManaGeneratorTicker(ManaGeneratorBlockEntity machine) {
        this.machine = machine;
    }

    public void tick() {
        ManaFuelHandler fuelHandler = machine.getFuelLogic();
        boolean changed = false;

        fuelHandler.tickCooldown();

        if (!fuelHandler.isBurning()) {
            if (!fuelHandler.tryConsumeFuel()) {
                machine.getStateManager().setWorking(false);
                machine.updateBlockActiveState(false);
                changed = true;
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
