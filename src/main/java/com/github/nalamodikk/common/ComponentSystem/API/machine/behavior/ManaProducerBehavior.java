package com.github.nalamodikk.common.ComponentSystem.API.machine.behavior;

import com.github.nalamodikk.common.ComponentSystem.API.machine.IComponentBehavior;
import com.github.nalamodikk.common.ComponentSystem.API.machine.grid.ComponentContext;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.capability.IHasMana;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.util.ParticleUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.concurrent.atomic.AtomicInteger;

public class ManaProducerBehavior implements IComponentBehavior {
    private int manaPerTick = 5;
    private String particleId = null; // ✅ 加上這行宣告欄位

    @Override
    public void init(CompoundTag data) {
        this.manaPerTick = data.getInt("mana_per_tick");
        if (data.contains("particle")) {
            this.particleId = data.getString("particle");
        } else {
            this.particleId = null; // 或設個預設粒子
        }
    }


    @Override
    public void onTick(ComponentContext context) {
        Level level = context.getLevel();
        if (level == null || level.isClientSide) return;

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

        // ✅ 粒子效果：只在伺服器世界 + 有指定粒子 ID 才執行
        if (!level.isClientSide && particleId != null && level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleUtil.getById(particleId),  // ← 用你 JSON 設定的粒子名稱
                    context.getCenterPos().getX() + 0.5,
                    context.getCenterPos().getY() + 1.0,
                    context.getCenterPos().getZ() + 0.5,
                    2, 0.1, 0.1, 0.1, 0.0
            );
        }
    }


    public String getType() {
        return "mana_producer";
    }
}
