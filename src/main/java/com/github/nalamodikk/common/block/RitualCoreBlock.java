package com.github.nalamodikk.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class RitualCoreBlock extends BaseEntityBlock {

    public RitualCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        // TODO: Create and return RitualCoreBlockEntity
        return null;
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        // We will use a BlockEntityRenderer for this block
        return RenderShape.MODEL;
    }
}
