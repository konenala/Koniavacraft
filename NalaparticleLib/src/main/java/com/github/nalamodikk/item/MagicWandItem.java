package com.github.nalamodikk.item;


import com.github.nalamodikk.group.Particle.MagicCircleGroup;
import com.github.nalamodikk.group.ServerParticleGroup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;

public class MagicWandItem extends Item {

    public MagicWandItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof Player) {
            ServerParticleGroup group = new ServerParticleGroup(
                    (ServerLevel) level,
                    player,
                    new MagicCircleGroup(MagicCircleGroup.ID)
            );

            // 可用 tick scheduler 管理，這裡先立即觸發
            group.tick();
        }

        return ItemUtils.startUsingInstantly(level, player, hand);
    }
}
