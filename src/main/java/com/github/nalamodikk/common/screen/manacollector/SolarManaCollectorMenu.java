package com.github.nalamodikk.common.screen.manacollector;

import com.github.nalamodikk.common.block.TileEntity.ManaGenerator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.register.ModMenusTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


public class SolarManaCollectorMenu extends AbstractContainerMenu {
    private final ManaGeneratorBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public SolarManaCollectorMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        super(ModMenusTypes.SOLAR_MANA_COLLECTOR_MENU.get(), id);
        BlockPos pos = buf.readBlockPos();
        Level level = playerInventory.player.level();
        this.blockEntity = (ManaGeneratorBlockEntity) level.getBlockEntity(pos);
        this.access = ContainerLevelAccess.create(level, pos);


    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return null;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return false;
    }
}
