package com.github.nalamodikk.common.screen.block.shared;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.coreapi.block.IConfigurableBlock;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.common.utils.item.ItemStackUtils;
import com.github.nalamodikk.register.ModDataComponents;
import com.github.nalamodikk.register.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
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
    private final ContainerData data;

    // 🔧 構造函數1：伺服器端使用（直接傳入BlockEntity）
    public UniversalConfigMenu(int id, Inventory playerInventory, BlockEntity blockEntity, ItemStack wandItem) {
        super(ModMenuTypes.UNIVERSAL_CONFIG_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.player = playerInventory.player;
        this.wandItem = wandItem;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        // 初始化數據同步
        this.data = new SimpleContainerData(6);
        this.addDataSlots(data);

        // 從BlockEntity獲取配置
        if (blockEntity instanceof IConfigurableBlock configurable) {
            for (Direction dir : Direction.values()) {
                IOHandlerUtils.IOType ioType = configurable.getIOConfig(dir);
                this.originalConfig.put(dir, ioType);
                this.currentConfig.put(dir, ioType);
            }
        }

        // 初始同步數據
        syncDataFromBlockEntity();
    }

    // 🔧 構造函數2：客戶端使用（從封包創建）
    public UniversalConfigMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        super(ModMenuTypes.UNIVERSAL_CONFIG_MENU.get(), id);

        // 從封包讀取數據
        BlockPos pos = extraData.readBlockPos();
        // 🔧 更簡單的方式：跳過ItemStack讀取，直接設為空
        this.wandItem = ItemStack.EMPTY;
        // 忽略封包中的快照配置，直接從BlockEntity獲取實時配置

        this.blockEntity = playerInventory.player.level().getBlockEntity(pos);
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);

        // 初始化數據同步
        this.data = new SimpleContainerData(6);
        this.addDataSlots(data);

        // 從實時BlockEntity獲取配置
        if (blockEntity instanceof IConfigurableBlock configurable) {
            for (Direction dir : Direction.values()) {
                IOHandlerUtils.IOType ioType = configurable.getIOConfig(dir);
                this.originalConfig.put(dir, ioType);
                this.currentConfig.put(dir, ioType);
            }
        }

        syncDataFromBlockEntity();
    }

    // 🆕 從BlockEntity同步數據到ContainerData
    private void syncDataFromBlockEntity() {
        if (blockEntity instanceof IConfigurableBlock configurable) {
            for (Direction dir : Direction.values()) {
                int index = dir.ordinal();
                IOHandlerUtils.IOType ioType = configurable.getIOConfig(dir);
                data.set(index, ioType.ordinal());
            }
        }
    }

    // 🆕 定期同步數據
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (blockEntity != null && !blockEntity.getLevel().isClientSide) {
            syncDataFromBlockEntity();
        }
    }

    // 🆕 供Screen使用：從ContainerData獲取IO配置
    public IOHandlerUtils.IOType getIOType(Direction direction) {
        int index = direction.ordinal();
        int ordinal = data.get(index);

        try {
            return IOHandlerUtils.IOType.values()[ordinal];
        } catch (ArrayIndexOutOfBoundsException e) {
            return IOHandlerUtils.IOType.DISABLED;
        }
    }

    // 現有方法保持不變
    public static EnumMap<Direction, Boolean> getDirectionConfig(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.CONFIGURED_DIRECTIONS, new EnumMap<>(Direction.class));
    }

    public EnumMap<Direction, IOHandlerUtils.IOType> getOriginalIOMap() {
        return new EnumMap<>(originalConfig);
    }

    public Player getPlayer() {
        return player;
    }

    public EnumMap<Direction, IOHandlerUtils.IOType> getOriginalConfig() {
        return new EnumMap<>(originalConfig);
    }

    public BlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    public ItemStack getWandItem() {
        return this.wandItem;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        if (!player.level().isClientSide) {
            ItemStack wand = ItemStackUtils.findHeldWand(player);
            if (!wand.isEmpty()) {
                wand.set(ModDataComponents.CONFIGURED_DIRECTIONS_IO, new EnumMap<>(currentConfig));
            }

            if (KoniavacraftMod.IS_DEV) {
                KoniavacraftMod.LOGGER.info("[Menu] [Server] Current IO Config = {}", this.currentConfig);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, blockEntity.getBlockState().getBlock());
    }
}