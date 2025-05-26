package com.github.nalamodikk.client.event;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.item.ManaDebugToolItem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = MagicalIndustryMod.MOD_ID,bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ManaToolInputEvents {

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.isCrouching()) {
            ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);

            if (heldItem.getItem() instanceof ManaDebugToolItem manaDebugToolItem) {
                double scrollDelta = event.getScrollDeltaY();
                manaDebugToolItem.cycleMode(heldItem, scrollDelta > 0);

                int modeIndex = ManaDebugToolItem.getModeIndex(heldItem);
                ManaDebugToolItem.setModeIndex(heldItem, modeIndex + 1);
                RegisterNetworkHandler.NETWORK_CHANNEL.sendToServer(new ModeChangePacket(modeIndex));

                player.displayClientMessage(Component.translatable(
                        "message.magical_industry.mana_mode_changed",
                        Component.translatable(manaDebugToolItem.getCurrentModeKey(heldItem))
                ), true);

                event.setCanceled(true);
            }
        }
    }
}
