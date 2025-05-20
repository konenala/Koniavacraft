package com.github.nalamodikk.common.upgrade.api;

import com.github.nalamodikk.common.upgrade.UpgradeInventory;
import com.github.nalamodikk.common.upgrade.UpgradeType;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface IUpgradeableMachine {
    UpgradeInventory getUpgradeInventory();

    default int getSpeedMultiplier() {
        return Math.min(1 + getUpgradeInventory().getUpgradeCount(UpgradeType.SPEED), 5);
    }

    default int getEfficiencyMultiplier() {
        return Math.min(1 + getUpgradeInventory().getUpgradeCount(UpgradeType.EFFICIENCY), 5);
    }

    BlockEntity getBlockEntity();

}