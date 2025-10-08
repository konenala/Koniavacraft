package com.github.nalamodikk.common.block.blockentity.ritual.ritualblock.arcanematrix.ritualcore;

import com.github.nalamodikk.common.block.blockentity.ritual.ritualblockentity.RitualCoreBlockEntity;
import com.github.nalamodikk.register.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.InteractionResult;
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
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                       Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof RitualCoreBlockEntity ritualCore)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.koniavacraft.ritual.catalyst_needed"));
            return ItemInteractionResult.FAIL;
        }

        // 嘗試啟動儀式（傳遞玩家手中的實際物品）
        ItemStack heldItem = player.getItemInHand(hand);
        boolean success = ritualCore.attemptStartRitual(player, heldItem);
        if (success) {
            player.sendSystemMessage(Component.translatable("message.koniavacraft.ritual.started"));
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return ItemInteractionResult.SUCCESS;
    }
}
