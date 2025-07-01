package com.github.nalamodikk.common.block.conduit;


import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.utils.capability.CapabilityUtils;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class ArcaneConduitBlock extends Block implements EntityBlock {

    // 6個方向的連接屬性
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    // 導管形狀 - 中心 + 6個方向的連接
    private static final VoxelShape CENTER = Block.box(6, 6, 6, 10, 10, 10);
    private static final VoxelShape NORTH_SHAPE = Block.box(6, 6, 0, 10, 10, 6);
    private static final VoxelShape SOUTH_SHAPE = Block.box(6, 6, 10, 10, 10, 16);
    private static final VoxelShape WEST_SHAPE = Block.box(0, 6, 6, 6, 10, 10);
    private static final VoxelShape EAST_SHAPE = Block.box(10, 6, 6, 16, 10, 10);
    private static final VoxelShape UP_SHAPE = Block.box(6, 10, 6, 10, 16, 10);
    private static final VoxelShape DOWN_SHAPE = Block.box(6, 0, 6, 10, 6, 10);

    public ArcaneConduitBlock(Properties properties) {
        super(properties);
        // 預設所有方向都不連接
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(EAST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, WEST, EAST, UP, DOWN);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CENTER;

        if (state.getValue(NORTH)) shape = Shapes.or(shape, NORTH_SHAPE);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, SOUTH_SHAPE);
        if (state.getValue(WEST)) shape = Shapes.or(shape, WEST_SHAPE);
        if (state.getValue(EAST)) shape = Shapes.or(shape, EAST_SHAPE);
        if (state.getValue(UP)) shape = Shapes.or(shape, UP_SHAPE);
        if (state.getValue(DOWN)) shape = Shapes.or(shape, DOWN_SHAPE);

        return shape;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneConduitBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == ModBlockEntities.ARCANE_CONDUIT_BE.get() ?
                (level1, pos, state1, blockEntity) -> ((ArcaneConduitBlockEntity) blockEntity).tick() : null;
    }



    // 檢查是否應該連接到某個方向
    public boolean shouldConnectTo(Level level, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        BlockEntity neighborBE = level.getBlockEntity(neighborPos);

        // 連接到其他導管
        if (neighborBE instanceof ArcaneConduitBlockEntity) {
            return true;
        }

        // 連接到有魔力能力的機器
        if (neighborBE != null) {
            // 這裡檢查是否有 MANA capability
            return level.getCapability(com.github.nalamodikk.register.ModCapabilities.MANA, neighborPos, direction.getOpposite()) != null;
        }

        return false;
    }

    // 更新連接狀態

    // 你還需要確保有這個方法：

    public BlockState updateConnections(Level level, BlockPos pos, BlockState state) {
        BlockState newState = state;

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            boolean shouldConnect = canConnectTo(level, neighborPos, direction);

            // 根據你的 ArcaneConduitBlock 屬性名稱調整
            BooleanProperty property = switch (direction) {
                case NORTH -> ArcaneConduitBlock.NORTH;
                case SOUTH -> ArcaneConduitBlock.SOUTH;
                case WEST -> ArcaneConduitBlock.WEST;
                case EAST -> ArcaneConduitBlock.EAST;
                case UP -> ArcaneConduitBlock.UP;
                case DOWN -> ArcaneConduitBlock.DOWN;
            };

            newState = newState.setValue(property, shouldConnect);
        }

        return newState;
    }


    private boolean canConnectTo(Level level, BlockPos pos, Direction direction) {
        // 🔧 【重要】：檢查自己的IO配置，如果該方向是DISABLED則不連接
        BlockEntity thisBE = level.getBlockEntity(pos.relative(direction.getOpposite()));
        if (thisBE instanceof ArcaneConduitBlockEntity thisConduit) {
            IOHandlerUtils.IOType thisConfig = thisConduit.getIOConfig(direction);
            if (thisConfig == IOHandlerUtils.IOType.DISABLED) {
                return false; // 該方向已禁用，不顯示連接
            }
        }

        // 檢查是否應該連接到該位置
        // 1. 是否為其他導管
        if (level.getBlockEntity(pos) instanceof ArcaneConduitBlockEntity) {
            return true;
        }

        // 2. 是否有魔力能力
        IUnifiedManaHandler handler = CapabilityUtils.getNeighborMana(level, pos, direction);
        return handler != null;
    }
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            // 更新視覺連接
            BlockState newState = updateConnections(level, pos, state);
            if (newState != state) {
                level.setBlock(pos, newState, 3);
            }

            // 🔧 關鍵：通知 BlockEntity 鄰居變化
            if (level.getBlockEntity(pos) instanceof ArcaneConduitBlockEntity conduit) {
                conduit.onNeighborChanged();
            }
        }
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    }
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                            Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof ArcaneConduitBlockEntity conduit) {
            return conduit.onUse(state, level, pos, player, hit);
        }
        return InteractionResult.PASS;
    }


}