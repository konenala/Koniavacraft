package com.github.nalamodikk.common.ComponentSystem.block.blocks.MachineBlock;

import com.github.nalamodikk.common.ComponentSystem.block.blockentity.MachineBlock.ModularMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ModularMachineBlock extends BaseEntityBlock {
    public ModularMachineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new ModularMachineBlockEntity(pPos ,pState);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, blockState, be) -> {
            if (be instanceof ModularMachineBlockEntity machine) {
                machine.tick();
            }
        };
    }

}
