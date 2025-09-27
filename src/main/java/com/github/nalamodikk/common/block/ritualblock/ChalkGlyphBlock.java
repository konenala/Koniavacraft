package com.github.nalamodikk.common.block.ritualblock;

import com.github.nalamodikk.common.item.ritual.RitualistChalkItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 粉筆符號方塊 - 可放置在地面的超薄覆蓋物
 * 具有顏色和圖案屬性，用於儀式驗證
 */
public class ChalkGlyphBlock extends Block {
    public static final MapCodec<ChalkGlyphBlock> CODEC = simpleCodec(ChalkGlyphBlock::new);

    // 顏色屬性 - 決定儀式功能
    public static final EnumProperty<ChalkColor> COLOR = EnumProperty.create("color", ChalkColor.class);

    // 圖案屬性 - 只影響視覺外觀
    public static final EnumProperty<GlyphPattern> PATTERN = EnumProperty.create("pattern", GlyphPattern.class);

    // 超薄形狀 - 像地毯一樣貼在地面
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 0.5D, 16.0D);

    public ChalkGlyphBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(COLOR, ChalkColor.WHITE)
                .setValue(PATTERN, GlyphPattern.CIRCLE));
    }

    @Override
    protected @NotNull MapCodec<? extends Block> codec() {
        return CODEC;
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
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // 需要下方有固體支撐
        BlockState below = level.getBlockState(pos.below());
        return below.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    public @NotNull BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                          LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        if (!this.canSurvive(state, level, currentPos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!this.canSurvive(this.defaultBlockState(), context.getLevel(), context.getClickedPos())) {
            return null;
        }
        return this.defaultBlockState();
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                        Player player, BlockHitResult hit) {
        // 如果玩家手持同色粉筆，可以切換圖案
        if (player.getMainHandItem().getItem() instanceof RitualistChalkItem chalkItem) {
            ChalkColor chalkColor = chalkItem.getChalkColor();
            ChalkColor currentColor = state.getValue(COLOR);

            if (chalkColor == currentColor) {
                // 同色粉筆，切換圖案
                GlyphPattern currentPattern = state.getValue(PATTERN);
                GlyphPattern nextPattern = getNextPattern(currentPattern);

                if (!level.isClientSide()) {
                    level.setBlock(pos, state.setValue(PATTERN, nextPattern), 3);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    /**
     * 獲取下一個圖案
     */
    private GlyphPattern getNextPattern(GlyphPattern current) {
        GlyphPattern[] patterns = GlyphPattern.values();
        int currentIndex = current.ordinal();
        return patterns[(currentIndex + 1) % patterns.length];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR, PATTERN);
    }

    /**
     * 粉筆顏色枚舉 - 決定儀式功能
     */
    public enum ChalkColor implements StringRepresentable {
        WHITE("white"),
        YELLOW("yellow"),
        BLUE("blue"),
        PURPLE("purple"),
        RED("red"),
        GREEN("green");

        private final String name;

        ChalkColor(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        public String getDisplayName() {
            return switch (this) {
                case WHITE -> "白色";
                case YELLOW -> "黃色";
                case BLUE -> "藍色";
                case PURPLE -> "紫色";
                case RED -> "紅色";
                case GREEN -> "綠色";
            };
        }
    }

    /**
     * 符文圖案枚舉 - 只影響外觀
     */
    public enum GlyphPattern implements StringRepresentable {
        CIRCLE("circle"),
        TRIANGLE("triangle"),
        SQUARE("square"),
        STAR("star"),
        CROSS("cross"),
        DIAMOND("diamond");

        private final String name;

        GlyphPattern(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
