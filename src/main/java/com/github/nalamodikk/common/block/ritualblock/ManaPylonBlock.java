package com.github.nalamodikk.common.block.ritualblock;

import com.github.nalamodikk.common.block.blockentity.ritual.ManaPylonBlockEntity;
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
 * 魔力塔方塊 - 儀式系統的魔力供應器
 * 從相鄰的魔力導管網絡中抽取大量魔力供儀式使用
 */
public class ManaPylonBlock extends BaseEntityBlock {
    public static final MapCodec<ManaPylonBlock> CODEC = simpleCodec(ManaPylonBlock::new);

    // 塔狀形狀 - 高而細，頂部有水晶狀結構
    private static final VoxelShape BASE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 10.0D, 11.0D);
    private static final VoxelShape CRYSTAL = Block.box(6.0D, 10.0D, 6.0D, 10.0D, 16.0D, 10.0D);
    private static final VoxelShape SHAPE = Shapes.or(BASE, CRYSTAL);

    public ManaPylonBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaPylonBlockEntity(pos, state);
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
            if (blockEntity instanceof ManaPylonBlockEntity pylon) {
                // 顯示魔力塔狀態信息
                int storedMana = pylon.getStoredMana();
                int maxMana = pylon.getMaxManaCapacity();
                boolean isConnected = pylon.isConnectedToNetwork();

                Component statusText = isConnected ?
                        Component.translatable("misc.koniavacraft.connected") :
                        Component.translatable("misc.koniavacraft.disconnected");

                player.sendSystemMessage(Component.translatable(
                        "message.koniavacraft.mana_pylon.status",
                        storedMana,
                        maxMana,
                        statusText
                ));
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.MANA_PYLON_BE.get(),
                (world, pos, blockState, blockEntity) -> blockEntity.tick());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ManaPylonBlockEntity pylon) {
                pylon.onRemoved();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
