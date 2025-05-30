package com.github.nalamodikk.common.item;

import com.github.nalamodikk.common.utils.upgrade.UpgradeType;
import net.minecraft.world.item.Item;

public class UpgradeItem extends Item {
    private final UpgradeType type;

    public UpgradeItem(UpgradeType type, Properties props) {
        super(props);
        this.type = type;
    }

    public UpgradeType getUpgradeType() {
        return type;
    }
}