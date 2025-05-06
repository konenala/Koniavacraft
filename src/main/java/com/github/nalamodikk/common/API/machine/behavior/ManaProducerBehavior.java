package com.github.nalamodikk.common.API.machine.behavior;

import com.github.nalamodikk.common.API.IComponentBehavior;
import com.github.nalamodikk.common.API.machine.grid.ComponentContext;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.capability.IHasMana;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import net.minecraft.nbt.CompoundTag;

import java.util.concurrent.atomic.AtomicInteger;

public class ManaProducerBehavior implements IComponentBehavior {
    private int manaPerTick = 5;

    @Override
    public void init(CompoundTag data) {
        if (data.contains("mana_per_tick")) {
            manaPerTick = data.getInt("mana_per_tick");
        }
    }

    @Override
    public void onTick(ComponentContext context) {
        AtomicInteger manaToDistribute = new AtomicInteger(this.manaPerTick);

        MagicalIndustryMod.LOGGER.debug("🔍 Tick start, trying to find neighbors...");
        context.forEachNeighbor(neighbor -> {
            MagicalIndustryMod.LOGGER.debug("➡ 找到鄰居: {}", neighbor.getClass().getSimpleName());

            if (neighbor instanceof IHasMana manaHolder) {
                int inserted = manaHolder.getManaStorage().insertMana(manaPerTick, ManaAction.EXECUTE);
                MagicalIndustryMod.LOGGER.debug("✅ 傳送 mana: {} (剩餘: {})", inserted, manaHolder.getManaStorage().getMana());
            } else {
                MagicalIndustryMod.LOGGER.warn("❌ 鄰居不是 IHasMana → {}", neighbor.getClass().getSimpleName());
            }
        });

    }





    public String getType() {
        return "mana_producer";
    }
}
