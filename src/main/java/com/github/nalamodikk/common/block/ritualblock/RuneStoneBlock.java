package com.github.nalamodikk.common.block.ritualblock;

import com.github.nalamodikk.common.block.blockentity.ritual.RuneStoneBlockEntity;
import com.github.nalamodikk.register.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import java.util.EnumMap;

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
 * 符文石方塊 - 增強儀式的輔助方塊
 * 四種基礎類型：效率、迅捷、穩定、增幅
 */
public class RuneStoneBlock extends BaseEntityBlock {
    public static final MapCodec<RuneStoneBlock> CODEC = simpleCodec(properties -> new RuneStoneBlock(properties, RuneType.EFFICIENCY));

    private static final VoxelShape DEFAULT_SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 8.0D, 14.0D);
    private static final EnumMap<RuneType, VoxelShape> SHAPES = new EnumMap<>(RuneType.class);

    static {
        SHAPES.put(RuneType.EFFICIENCY, mergeShapes(
                Block.box(3.0D, 0.0D, 3.0D, 13.0D, 6.0D, 13.0D),
                Block.box(5.0D, 6.0D, 5.0D, 11.0D, 12.0D, 11.0D)
        ));

        SHAPES.put(RuneType.CELERITY, mergeShapes(
                Block.box(4.0D, 0.0D, 4.0D, 12.0D, 5.0D, 12.0D),
                Block.box(6.0D, 5.0D, 6.0D, 10.0D, 16.0D, 10.0D)
        ));

        SHAPES.put(RuneType.STABILITY, mergeShapes(
                Block.box(2.0D, 0.0D, 2.0D, 14.0D, 5.0D, 14.0D),
                Block.box(1.0D, 5.0D, 1.0D, 15.0D, 8.0D, 15.0D)
        ));

        SHAPES.put(RuneType.AUGMENTATION, mergeShapes(
                Block.box(4.0D, 0.0D, 4.0D, 12.0D, 4.0D, 12.0D),
                Block.box(3.0D, 4.0D, 3.0D, 13.0D, 10.0D, 13.0D),
                Block.box(6.0D, 10.0D, 6.0D, 10.0D, 15.0D, 10.0D)
        ));
    }

    private final RuneType runeType;

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
        return SHAPES.getOrDefault(runeType, DEFAULT_SHAPE);
    }

    @Override
    protected @NotNull VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.getOrDefault(runeType, DEFAULT_SHAPE);
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
                Component typeName = Component.translatable(runeType.getTranslationKey());
                Component description = Component.translatable(runeType.getDescriptionKey());
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

    private static VoxelShape mergeShapes(VoxelShape... parts) {
        VoxelShape shape = Shapes.empty();
        for (VoxelShape part : parts) {
            shape = Shapes.or(shape, part);
        }
        return shape;
    }
}
