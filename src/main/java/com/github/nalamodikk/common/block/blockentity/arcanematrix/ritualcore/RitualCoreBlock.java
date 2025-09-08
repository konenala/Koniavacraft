package com.github.nalamodikk.common.block.blockentity.arcanematrix.ritualcore;

import com.github.nalamodikk.register.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import com.github.nalamodikk.register.ModItems;

public class RitualCoreBlock extends BaseEntityBlock {

    public static final MapCodec<RitualCoreBlock> CODEC = simpleCodec(RitualCoreBlock::new);

    public RitualCoreBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RitualCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.RITUAL_CORE_BE.get(), RitualCoreBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RitualCoreBlockEntity ritualCoreBE)) {
            return InteractionResult.PASS;
        }

        if (player.getItemInHand(hand).getItem() == ModItems.RESONANT_CRYSTAL.get()) {
            if (!ritualCoreBE.isRitualActive()) {
                player.getItemInHand(hand).shrink(1);
                ritualCoreBE.startRitual();
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.0f);
                return InteractionResult.CONSUME;
            } else {
                // Ritual is already active, maybe send a message to the player
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.koniava.ritual_already_active"));
                return InteractionResult.FAIL;
            }
        }

        return InteractionResult.PASS;
    }
}