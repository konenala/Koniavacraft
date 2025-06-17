package com.github.nalamodikk.common.screen.block.shared;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.coreapi.block.IConfigurableBlock;
import com.github.nalamodikk.register.ModDataComponents;
import com.github.nalamodikk.register.ModMenuTypes;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.common.utils.item.ItemStackUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.EnumMap;

public class UniversalConfigMenu extends AbstractContainerMenu {
    private final EnumMap<Direction, IOHandlerUtils.IOType> originalConfig = new EnumMap<>(Direction.class);

    private final EnumMap<Direction, IOHandlerUtils.IOType> currentConfig = new EnumMap<>(Direction.class);
    private final Player player;
    private final BlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ItemStack wandItem;

    public UniversalConfigMenu(int id, Inventory playerInventory, BlockEntity blockEntity, ItemStack wandItem) {
        super(ModMenuTypes.UNIVERSAL_CONFIG_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.player = playerInventory.player;
        this.wandItem = wandItem;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        if (blockEntity instanceof IConfigurableBlock configurable) {
            for (Direction dir : Direction.values()) {
                IOHandlerUtils.IOType ioType = configurable.getIOConfig(dir);
                this.originalConfig.put(dir, ioType);
                this.currentConfig.put(dir, ioType);
            }
        }
    }



    public static EnumMap<Direction, Boolean> getDirectionConfig(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.CONFIGURED_DIRECTIONS, new EnumMap<>(Direction.class));
    }
    public EnumMap<Direction, IOHandlerUtils.IOType> getOriginalIOMap() {
        return new EnumMap<>(originalConfig); // 安全複製
    }

    public Player getPlayer() {
        return player;
    }


    public EnumMap<Direction, IOHandlerUtils.IOType> getOriginalConfig() {
        return new EnumMap<>(originalConfig);
    }


    @Override
    public void removed(Player player) {
        super.removed(player);

        if (!player.level().isClientSide) {
            ItemStack wand = ItemStackUtils.findHeldWand(player);
            if (!wand.isEmpty()) {
                wand.set(ModDataComponents.CONFIGURED_DIRECTIONS_IO, new EnumMap<>(currentConfig)); // ✅ 寫入新資料欄位
            }

            if (KoniavacraftMod.IS_DEV) {
                KoniavacraftMod.LOGGER.info("[Menu] [Server] Current IO Config = {}", this.currentConfig);
            }
        }
    }


    public UniversalConfigMenu(int id, Inventory playerInventory, BlockEntity blockEntity, ItemStack wandItem, EnumMap<Direction, IOHandlerUtils.IOType> syncedConfig) {
        super(ModMenuTypes.UNIVERSAL_CONFIG_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.player = playerInventory.player;
        this.wandItem = wandItem;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        this.originalConfig.putAll(syncedConfig);
        this.currentConfig.putAll(syncedConfig);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, blockEntity.getBlockState().getBlock());
    }

    public BlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    public ItemStack getWandItem() {
        return this.wandItem;
    }
}
