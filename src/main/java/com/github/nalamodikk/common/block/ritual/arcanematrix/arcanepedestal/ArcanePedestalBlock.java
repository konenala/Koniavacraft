package com.github.nalamodikk.common.block.ritual.arcanematrix.arcanepedestal;

import com.github.nalamodikk.common.block.ritual.arcanematrix.arcanepedestal.ArcanePedestalBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ArcanePedestalBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final MapCodec<ArcanePedestalBlock> CODEC = simpleCodec(ArcanePedestalBlock::new);

    private static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 10.0D, 13.0D);

    public ArcanePedestalBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public @NotNull BlockState rotate(@NotNull BlockState s, @NotNull Rotation r) { return s.setValue(FACING, r.rotate(s.getValue(FACING))); }

    @Override
    public @NotNull BlockState mirror(@NotNull BlockState s, @NotNull Mirror m) { return rotate(s, m.getRotation(s.getValue(FACING))); }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState s) { return RenderShape.MODEL; }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ArcanePedestalBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ArcanePedestalBlockEntity pedestal)) {
            return InteractionResult.PASS;
        }

        ItemStack handItem = player.getMainHandItem();
        if (!handItem.isEmpty()) {
            ItemStack remainder = pedestal.insertOffering(handItem);
            player.setItemInHand(player.getUsedItemHand(), remainder);
            return InteractionResult.SUCCESS;
        }

        ItemStack extracted = pedestal.extractOffering();
        if (!extracted.isEmpty()) {
            if (!player.addItem(extracted)) {
                player.drop(extracted, false);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) {
            return (level1, pos, state1, blockEntity) -> {
                if (blockEntity instanceof ArcanePedestalBlockEntity pedestal) {
                    ArcanePedestalBlockEntity.clientTick(level1, pos, state1, pedestal);
                }
            };
        }
        return (level1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof ArcanePedestalBlockEntity pedestal) {
                ArcanePedestalBlockEntity.serverTick(level1, pos, state1, pedestal);
            }
        };
    }

    @Override
    protected void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ArcanePedestalBlockEntity pedestal) {
                pedestal.dropContents();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
