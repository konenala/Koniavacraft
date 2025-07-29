// ==================== 📁 修復的 DebugParticleItem.java ====================
// 位置：src/main/java/com/github/nalamodikk/experimental/particle/item/DebugParticleItem.java

package com.github.nalamodikk.particle.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class DebugParticleItem extends Item {

    public DebugParticleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return super.useOn(context);

    }
}
