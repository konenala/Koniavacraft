package com.github.nalamodikk.block.custom.mana_crafting_table;

import com.github.nalamodikk.block.entity.mana_crafting.AdvancedManaCraftingTableBlockEntity;
import com.github.nalamodikk.block.entity.ModBlockEntities;
import com.github.nalamodikk.block.entity.mana_crafting.ManaCraftingTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class AdvancedManaCraftingTableBlock extends BaseEntityBlock {
    public static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);  // 定義方塊的形狀

    public AdvancedManaCraftingTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;  // 返回方塊的形狀
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;  // 定義方塊的渲染模式，使用模型渲染
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AdvancedManaCraftingTableBlockEntity) {
                ((AdvancedManaCraftingTableBlockEntity) blockEntity).drops();  // 掉落方塊內的物品
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!world.isClientSide && player instanceof ServerPlayer) {
            // 打開合成界面
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof AdvancedManaCraftingTableBlockEntity) {
                NetworkHooks.openScreen((ServerPlayer) player, (AdvancedManaCraftingTableBlockEntity) blockEntity, pos);
            } else {
                throw new IllegalStateException("Our container provider is missing!");
            }
        }
        return InteractionResult.SUCCESS;  // 返回成功表示成功處理交互
    }


    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedManaCraftingTableBlockEntity(pos, state);  // 創建並返回 AdvancedManaCraftingTableBlockEntity
    }
}
