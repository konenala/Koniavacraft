package com.github.nalamodikk.common.item.debug;

import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.network.packet.manatool.ManaUpdatePacket;
import com.github.nalamodikk.common.network.packet.manatool.ModeChangePacket;
import com.github.nalamodikk.common.register.ModCapability;
import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

public class ManaDebugToolItem extends Item {
    public static final String TAG_MODE_INDEX = "ModeIndex";

    private static final int[] MANA_AMOUNTS = {10, 100, 1000};
    private static final String[] MODES = {
            "message.magical_industry.mana_mode_add_10",
            "message.magical_industry.mana_mode_add_100",
            "message.magical_industry.mana_mode_add_1000"
    };

    public ManaDebugToolItem(Properties properties) {
        super(properties);
        NeoForge.EVENT_BUS.addListener(this::onMouseScroll);

    }

    public void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.isCrouching()) return;

        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (heldItem.getItem() != this) return;

        boolean forward = event.getScrollDeltaY() > 0;
        this.cycleMode(heldItem, forward); // 客戶端本地先切
        ModeChangePacket.sendToServer(forward); // 發送方向封包

        player.displayClientMessage(Component.translatable(
                "message.magical_industry.mana_mode_changed",
                Component.translatable(this.getCurrentModeKey(heldItem))
        ), true);

        event.setCanceled(true);
    }



    public static final DataComponentType<Integer> MODE_INDEX =
            DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build();

    public static int getModeIndex(ItemStack stack) {
        return stack.getOrDefault(MODE_INDEX, 0);
    }

    public static void setModeIndex(ItemStack stack, int index) {
        stack.set(MODE_INDEX, index);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide()) {
            BlockPos pos = context.getClickedPos();
            IUnifiedManaHandler manaStorage = level.getCapability(ModCapability.MANA, pos, null);

            if (manaStorage != null) {
                ItemStack stack = context.getItemInHand();
                int modeIndex = stack.getOrDefault(ManaDebugToolItem.MODE_INDEX, 0);
                int manaToAdd = MANA_AMOUNTS[modeIndex];
                manaStorage.addMana(manaToAdd);

                Player player = context.getPlayer();
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.displayClientMessage(
                            Component.translatable("message.magical_industry.mana_added", manaToAdd, manaStorage.getManaStored()), true);
                }

                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity != null) blockEntity.setChanged();
                BlockState state = level.getBlockState(pos);
                level.sendBlockUpdated(pos, state, state, 3);

                // ⚠️ 請自行確認 ManaUpdatePacket 是否已改寫為 NeoForge Payload 風格
                PacketDistributor.sendToPlayer((ServerPlayer) context.getPlayer(),
                        new ManaUpdatePacket(pos, manaStorage.getManaStored()));

                return InteractionResult.SUCCESS;
            }
        }
        return super.useOn(context);
    }

    public void cycleMode(ItemStack stack, boolean forward) {
        int modeIndex = stack.getOrDefault(ManaDebugToolItem.MODE_INDEX, 0);
        if (forward) {
            modeIndex = (modeIndex + 1) % MANA_AMOUNTS.length;
        } else {
            modeIndex = (modeIndex - 1 + MANA_AMOUNTS.length) % MANA_AMOUNTS.length;
        }
        stack.set(ManaDebugToolItem.MODE_INDEX, modeIndex);
    }


    public String getCurrentModeKey(ItemStack stack) {
        int modeIndex = stack.getOrDefault(ManaDebugToolItem.MODE_INDEX, 0);
        return MODES[modeIndex];
    }




}
