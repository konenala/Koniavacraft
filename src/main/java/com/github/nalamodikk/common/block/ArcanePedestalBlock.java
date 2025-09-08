package com.github.nalamodikk.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

public class ArcanePedestalBlock extends BaseEntityBlock {

    // 關鍵：讓方塊具備「facing」屬性（名字就是 facing）
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // 1.21+ 需要提供靜態 CODEC
    public static final MapCodec<ArcanePedestalBlock> CODEC = simpleCodec(ArcanePedestalBlock::new);

    public ArcanePedestalBlock(Properties properties) {
        super(properties);
        // 預設朝向
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    // 放置時朝向（面向玩家相反，常見做法）
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    // 旋轉/鏡像支援（讓結構方塊、結構旋轉等時正常）
    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    // 把屬性加入 stateDefinition


    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // TODO: 回傳你的 ArcanePedestalBlockEntity
        return null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
