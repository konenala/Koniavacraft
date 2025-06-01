package com.github.nalamodikk.common.block.mana_crafting;

import com.github.nalamodikk.common.block.mana_generator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.block.manabase.BaseMachineBlock;
import com.github.nalamodikk.common.register.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import org.jetbrains.annotations.Nullable;

public class ManaCraftingTableBlock extends BaseMachineBlock {
    public static final MapCodec<ManaCraftingTableBlock> CODEC = simpleCodec(ManaCraftingTableBlock::new);
    public static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public ManaCraftingTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ManaCraftingTableBlockEntity generator) {
                ((ServerPlayer) player).openMenu(
                        new SimpleMenuProvider(generator, Component.translatable("block.magical_industry.mana_generator")),
                        pos
                );
            } else {
                throw new IllegalStateException("Our Container provider is missing!");
            }
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }



    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ManaCraftingTableBlockEntity blockEntity) {
                blockEntity.drops();  // 處理物品掉落
                level.invalidateCapabilities(pos); // ❗❗通知 NeoForge: 這個位置的能力不可靠了，清除快取！


            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }


    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaCraftingTableBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide){return null;}

        return createTickerHelper(blockEntityType, ModBlockEntities.MANA_CRAFTING_TABLE_BLOCK_BE.get(),
                (Level, BlockPos, BlockState, BlockEntity) ->BlockEntity.serverTick(Level, BlockPos,BlockState,BlockEntity ));
    }
}
