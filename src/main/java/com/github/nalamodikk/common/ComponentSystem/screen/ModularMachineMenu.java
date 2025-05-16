package com.github.nalamodikk.common.ComponentSystem.screen;

import com.github.nalamodikk.common.ComponentSystem.block.blockentity.MachineBlock.ModularMachineBlockEntity;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.register.ModBlocks;
import com.github.nalamodikk.common.register.ModMenusTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ModularMachineMenu extends AbstractContainerMenu {
    private final BlockPos machinePos;
    private final ContainerLevelAccess access;

    public ModularMachineMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, inv.player.level(), buf.readBlockPos());
    }

    public ModularMachineMenu(int id, Inventory inv, Level level, BlockPos pos) {
        super(ModMenusTypes.MODULAR_MACHINE_MENU.get(), id);
        this.machinePos = pos;
        this.access = ContainerLevelAccess.create(level, pos);
    }


    public static ModularMachineMenu create(int id, Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Level level = inv.player.level();
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof ModularMachineBlockEntity) {
            return new ModularMachineMenu(id, inv, level, pos);
        }

        // fallback：萬一方塊不存在（ex: unloaded chunk）就避免 GUI 崩潰
        MagicalIndustryMod.LOGGER.warn("⚠ 無法建立 ModularMachineMenu：找不到方塊在 {}", pos);
        return new ModularMachineMenu(id, inv, level, pos); // 你之後可替換成空 handler
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.MODULAR_MACHINE_BLOCK.get());
    }

    public BlockPos getMachinePos() {
        return machinePos;
    }
}

