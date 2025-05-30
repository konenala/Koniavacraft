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
        if (machine.getFuelLogic().isCoolingDown()) {
            machine.getFuelLogic().tickCooldown();
            return;
        }

        if (!machine.getFuelLogic().isBurning()) {
            if (!machine.getFuelLogic().tryConsumeFuel()) {
                machine.getStateManager().setWorking(false);
                machine.getFuelLogic().setCooldown();
                return;
            }
        }

        boolean success = switch (machine.getStateManager().getCurrentMode()) {
            case MANA -> machine.getManaGenHandler().generate();
            case ENERGY -> machine.getEnergyGenHandler().generate();
        };

        if (!success) {
            machine.getFuelLogic().pauseBurn();
            machine.getStateManager().setWorking(false);
            return;
        }

        machine.getFuelLogic().resumeBurn();
        machine.getFuelLogic().tickBurn(true);
        machine.getStateManager().setWorking(true);

        if (machine.getLevel() instanceof ServerLevel serverLevel) {
            OutputHandler.tryOutput(serverLevel,
                    machine.getBlockPos(),
                    machine.getManaStorage(),
                    machine.getEnergyStorage(),
                    machine.getDirectionConfig());
        }

        machine.updateBlockActiveState(machine.getStateManager().isWorking());
        machine.sync();
    }
}
