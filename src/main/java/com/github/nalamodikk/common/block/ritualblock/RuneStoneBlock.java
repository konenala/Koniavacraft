package com.github.nalamodikk.common.block.ritualblock;

import com.github.nalamodikk.common.block.blockentity.ritual.RuneStoneBlockEntity;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 符文石方塊 - 增強儀式的輔助方塊
 * 四種基礎類型：效率、迅捷、穩定、增幅
 */
public class RuneStoneBlock extends BaseEntityBlock {
    public static final MapCodec<RuneStoneBlock> CODEC = simpleCodec(properties -> new RuneStoneBlock(properties, RuneType.EFFICIENCY));

    private final RuneType runeType;

    // 符文石形狀 - 較矮的方塊
    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 8.0D, 14.0D);

    public RuneStoneBlock(Properties properties, RuneType runeType) {
        super(properties);
        this.runeType = runeType;
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RuneStoneBlockEntity(pos, state, runeType);
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
            if (blockEntity instanceof RuneStoneBlockEntity runeStone) {
                // 顯示符文石信息
                String typeName = switch (runeType) {
                    case EFFICIENCY -> "效率符文";
                    case CELERITY -> "迅捷符文";
                    case STABILITY -> "穩定符文";
                    case AUGMENTATION -> "增幅符文";
                };

                String description = switch (runeType) {
                    case EFFICIENCY -> "降低儀式魔力消耗 8%";
                    case CELERITY -> "提升儀式速度 10%";
                    case STABILITY -> "降低儀式失敗風險";
                    case AUGMENTATION -> "有機率產出額外物品或附魔";
                };

                player.sendSystemMessage(Component.translatable("message.koniavacraft.rune_stone.info", typeName, description));
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.RUNE_STONE_BE.get(),
                (world, pos, blockState, blockEntity) -> blockEntity.tick());
    }

    public RuneType getRuneType() {
        return runeType;
    }

}
