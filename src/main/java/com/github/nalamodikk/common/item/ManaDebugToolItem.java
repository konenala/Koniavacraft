package com.github.nalamodikk.common.item;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.capability.ManaCapability;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.util.ExtraCodecs;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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
            IUnifiedManaHandler manaStorage = level.getCapability(ManaCapability.MANA, pos, null);

            if (manaStorage != null) {
                ItemStack stack = context.getItemInHand();
                int modeIndex = stack.getOrCreateTag().getInt(TAG_MODE_INDEX);
                int manaToAdd = MANA_AMOUNTS[modeIndex];
                manaStorage.addMana(manaToAdd);

                Player player = context.getPlayer();
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.displayClientMessage(
                            Component.translatable("message.magical_industry.mana_added", manaToAdd, manaStorage.getManaStored()), true);
                }

                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity != null) {
                    blockEntity.setChanged();
                }
                BlockState state = level.getBlockState(pos);
                level.sendBlockUpdated(pos, state, state, 3);

                ManaUpdatePacket packet = new ManaUpdatePacket(pos, manaStorage.getManaStored());
                RegisterNetworkHandler.NETWORK_CHANNEL.send(
                        PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)), packet);

                return InteractionResult.SUCCESS;
            }
        }
        return super.useOn(context);
    }



    public void cycleMode(ItemStack stack, boolean forward) {
        int modeIndex = stack.getOrCreateTag().getInt(TAG_MODE_INDEX);
        if (forward) {
            modeIndex = (modeIndex + 1) % MANA_AMOUNTS.length;
        } else {
            modeIndex = (modeIndex - 1 + MANA_AMOUNTS.length) % MANA_AMOUNTS.length;
        }
        stack.getOrCreateTag().putInt(TAG_MODE_INDEX, modeIndex);
    }

    public String getCurrentModeKey(ItemStack stack) {
        int modeIndex = stack.getOrCreateTag().getInt(TAG_MODE_INDEX);
        return MODES[modeIndex];
    }
}
