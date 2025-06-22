package com.github.nalamodikk.experimental.particle.item;

import com.github.nalamodikk.experimental.particle.render.MagicCircleRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DebugParticleItem extends Item {
    public DebugParticleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (level.isClientSide) {
            // 呼叫你的渲染器，畫出魔法陣
            MagicCircleRenderer.drawMagicCircle(level, player.getEyePosition());
        }

        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }

}