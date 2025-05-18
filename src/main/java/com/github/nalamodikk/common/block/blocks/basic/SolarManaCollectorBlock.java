package com.github.nalamodikk.common.block.blocks.basic;

import com.github.nalamodikk.common.block.TileEntity.basic.SolarManaCollectorBlockEntity;
import com.github.nalamodikk.common.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class SolarManaCollectorBlock extends BaseEntityBlock  {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public SolarManaCollectorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));

    }
    private final Map<Direction, Boolean> directionConfig = new EnumMap<>(Direction.class);

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL; // 使用 blockstate 模型顯示
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolarManaCollectorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return (be instanceof MenuProvider provider) ? provider : null;
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!oldState.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SolarManaCollectorBlockEntity collector) {
                collector.drops(); // 如果你有 drops() 方法
            }
            super.onRemove(oldState, level, pos, newState, isMoving);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    // 4. 註冊 blockstate 使用的屬性
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }



    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SolarManaCollectorBlockEntity collector) {
            return (int) (15 * ((float) collector.getManaStorage().getMana() / collector.getManaStorage().getMaxMana()));
        }
        return 0;
    }


    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.SOLAR_MANA_COLLECTOR_BE.get(), SolarManaCollectorBlockEntity::serverTick);
    }



}
