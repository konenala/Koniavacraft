package com.github.nalamodikk.common.ComponentSystem.block.blocks.MachineBlock;

import com.github.nalamodikk.common.ComponentSystem.API.machine.IGridComponent;
import com.github.nalamodikk.common.ComponentSystem.API.machine.grid.ComponentGrid;
import com.github.nalamodikk.common.ComponentSystem.block.blockentity.MachineBlock.ModularMachineBlockEntity;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.ComponentSystem.util.helpers.ModuleItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

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
        return new ModularMachineBlockEntity(pPos, pState);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ModularMachineBlockEntity machine)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        ItemStackHandler handler = machine.getInternalHandler();

        // 🌀 撤銷模式：蹲下 + 手持任意物品
        if (player.isShiftKeyDown() && held.isEmpty()) {
            int restored = machine.restoreSnapshot((ServerPlayer) player);
            if (restored > 0) {
                player.displayClientMessage(Component.translatable("message.magical_industry.machine.restored_count", restored), true);
            } else {
                player.displayClientMessage(Component.translatable("message.magical_industry.machine.no_history"), true);
            }

            return InteractionResult.SUCCESS;
        }


        // 潛行 + 空手：清空所有模組 + 退還
//        if (player.isShiftKeyDown() && held.isEmpty()) {
//            int count = 0;
//
//            for (int i = 0; i < handler.getSlots(); i++) {
//                ItemStack toReturn = handler.getStackInSlot(i);
//                if (!toReturn.isEmpty()) {
//                    ItemStack copy = toReturn.copy();
//                    boolean success = player.getInventory().add(copy);
//                    if (!success) player.spawnAtLocation(copy);
//                    handler.setStackInSlot(i, ItemStack.EMPTY);
//                    count++;
//                }
//            }
//
//            if (count > 0) {
//                player.displayClientMessage(Component.translatable("message.magical_industry.machine.cleared_count", count), true);
//            } else {
//                player.displayClientMessage(Component.translatable("message.magical_industry.machine.nothing_to_clear"), true);
//            }
//
//            return InteractionResult.SUCCESS;
//        }


        // 空手右鍵：顯示目前拼裝資訊
        if (held.isEmpty()) {

            machine.rebuildGridFromItemHandler();

            ComponentGrid grid = machine.getGrid();
            player.displayClientMessage(Component.translatable("tooltip.magical_industry.machine.grid_size", grid.getAllComponents().size()), false);
            for (Map.Entry<BlockPos, IGridComponent> entry : grid.getAllComponents().entrySet()) {
                player.displayClientMessage(Component.translatable("tooltip.magical_industry.machine.grid_entry",
                        entry.getKey().toShortString(), entry.getValue().getId().toString()), false);
            }
            return InteractionResult.SUCCESS;
        }

        // 📝 插入模組：紀錄快照並插入
        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getStackInSlot(i).isEmpty()) {
                machine.pushSnapshot(); // ✅ 封裝好的快照方法
                ItemStack copy = held.copy();
                ModuleItemHelper.normalizeStackNBT(copy); // ✅ 確保 copy 是標準格式
                handler.setStackInSlot(i, copy);
                MagicalIndustryMod.LOGGER.info("Inserted module: {}", copy.getTag());

                copy.setCount(1);
                if (!player.isCreative()) held.shrink(1);
                player.displayClientMessage(Component.translatable("tooltip.magical_industry.machine.inserted"), true);
                return InteractionResult.SUCCESS;
            }
        }

        player.displayClientMessage(Component.translatable("message.magical_industry.machine.no_space"), true);
        return InteractionResult.FAIL;
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
