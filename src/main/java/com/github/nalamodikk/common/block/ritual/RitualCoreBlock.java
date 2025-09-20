package com.github.nalamodikk.common.block.ritual;

import com.github.nalamodikk.common.block.blockentity.ritual.RitualCoreBlockEntity;
import com.github.nalamodikk.register.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 儀式核心方塊 - 儀式系統的中心方塊
 * 處理儀式的邏輯、結構驗證和執行
 */
public class RitualCoreBlock extends BaseEntityBlock {
    public static final MapCodec<RitualCoreBlock> CODEC = simpleCodec(RitualCoreBlock::new);

    // 方塊形狀 - 比完整方塊稍微小一點，更有儀式感
    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 12.0D, 14.0D);

    public RitualCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RitualCoreBlockEntity(pos, state);
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected @NotNull VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof RitualCoreBlockEntity ritualCore) {
                // 檢查玩家是否持有催化劑物品
                if (player.getMainHandItem().isEmpty()) {
                    player.sendSystemMessage(Component.translatable("message.koniavacraft.ritual.catalyst_needed"));
                    return InteractionResult.SUCCESS;
                }

                // 嘗試啟動儀式
                boolean success = ritualCore.attemptStartRitual(player, player.getMainHandItem());
                if (success) {
                    player.sendSystemMessage(Component.translatable("message.koniavacraft.ritual.started"));
                } else {
                    player.sendSystemMessage(Component.translatable("message.koniavacraft.ritual.failed"));
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.RITUAL_CORE_BE.get(),
                (world, pos, blockState, blockEntity) -> blockEntity.tick());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof RitualCoreBlockEntity ritualCore) {
                ritualCore.dropContents(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}