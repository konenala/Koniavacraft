package com.github.nalamodikk.common.block.blocks.Conduit;

import com.github.nalamodikk.common.block.entity.Conduit.ManaConduitBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ManaConduitBlock extends Block implements EntityBlock {
    public ManaConduitBlock() {
        super(Properties.copy(Blocks.IRON_BLOCK)); // 讓導管和鐵方塊有相同屬性
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new ManaConduitBlockEntity(pos, state);
    }
}
