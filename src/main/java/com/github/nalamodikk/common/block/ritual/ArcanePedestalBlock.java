package com.github.nalamodikk.common.block.ritual;

import com.github.nalamodikk.common.block.blockentity.ritual.ArcanePedestalBlockEntity;
import com.github.nalamodikk.register.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 奧術基座方塊 - 用於放置儀式祭品的平台
 * 具有物品渲染功能，可顯示放置在上面的物品
 */
public class ArcanePedestalBlock extends BaseEntityBlock {
    public static final MapCodec<ArcanePedestalBlock> CODEC = simpleCodec(ArcanePedestalBlock::new);

    // 基座形狀 - 底部較寬，頂部平坦用於放置物品
    private static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 10.0D, 13.0D);

    public ArcanePedestalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcanePedestalBlockEntity(pos, state);
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
            if (blockEntity instanceof ArcanePedestalBlockEntity pedestal) {
                // 如果玩家手中有物品，嘗試放置到基座上
                if (!player.getMainHandItem().isEmpty()) {
                    ItemStack handItem = player.getMainHandItem();
                    ItemStack remainder = pedestal.insertItem(handItem);
                    player.setItemInHand(player.getUsedItemHand(), remainder);
                } else {
                    // 如果玩家手為空，嘗試取出基座上的物品
                    ItemStack extractedItem = pedestal.extractItem();
                    if (!extractedItem.isEmpty()) {
                        if (!player.addItem(extractedItem)) {
                            player.drop(extractedItem, false);
                        }
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.ARCANE_PEDESTAL.get(),
                (world, pos, blockState, blockEntity) -> blockEntity.tick());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ArcanePedestalBlockEntity pedestal) {
                pedestal.dropContents(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}