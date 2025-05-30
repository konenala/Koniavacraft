package com.github.nalamodikk.common.screen.shared;

import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.item.tool.BasicTechWandItem;
import com.github.nalamodikk.common.network.packet.manatool.ConfigDirectionUpdatePacket;
import com.github.nalamodikk.common.register.ModDataComponents;
import com.github.nalamodikk.common.register.ModMenuTypes;
import com.github.nalamodikk.common.utils.data.CodecsLibrary;
import com.github.nalamodikk.common.utils.item.ItemStackUtils;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

public class UniversalConfigMenu extends AbstractContainerMenu {
    private final EnumMap<Direction, Boolean> currentConfig = new EnumMap<>(Direction.class);
    private final Player player;
    private final BlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ItemStack wandItem;
    private final EnumMap<Direction, Boolean> initialConfig;
    private final EnumMap<Direction, Boolean> originalConfig = new EnumMap<>(Direction.class);

    public UniversalConfigMenu(int id, Inventory playerInventory, BlockEntity blockEntity, ItemStack wandItem) {
        super(ModMenuTypes.UNIVERSAL_CONFIG_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.player = playerInventory.player;
        this.wandItem = wandItem;
        this.initialConfig = wandItem.getOrDefault(
                ModDataComponents.CONFIGURED_DIRECTIONS,
                new EnumMap<>(Direction.class)
        );
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        if (blockEntity instanceof IConfigurableBlock configurableBlock) {
            // 从 BlockEntity 获取当前的方向配置
            for (Direction direction : Direction.values()) {
                this.currentConfig.put(direction, configurableBlock.isOutput(direction));
            }
        }

    }

    public static EnumMap<Direction, Boolean> getDirectionConfig(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.CONFIGURED_DIRECTIONS, new EnumMap<>(Direction.class));
    }


    public Player getPlayer() {
        return player;
    }

    public void updateConfig(Direction direction, boolean isOutput) {
        currentConfig.put(direction, isOutput);

        if (this.blockEntity instanceof IConfigurableBlock configurableBlock) {
            configurableBlock.setDirectionConfig(direction, isOutput);
            this.blockEntity.setChanged(); // 标记方块为已更新，以便后续保存
        }
    }


    public EnumMap<Direction, Boolean> getOriginalConfig() {
        return originalConfig;
    }


    @Override
    public void removed(Player player) {
        super.removed(player);

        if (!player.level().isClientSide) {
            ItemStack wand = ItemStackUtils.findHeldWand(player); // ✅ 用工具方法找實體物品

            if (!wand.isEmpty()) {
                EnumMap<Direction, Boolean> configCopy = new EnumMap<>(currentConfig);
                wand.set(ModDataComponents.CONFIGURED_DIRECTIONS, configCopy); // ✅ 確保設定寫進真實物品
            }

            if (MagicalIndustryMod.IS_DEV) {
                MagicalIndustryMod.LOGGER.info("[Menu] [Server] CurrentConfig = {}", this.currentConfig);
            }
        }
    }


    public UniversalConfigMenu(int id, Inventory playerInventory, BlockEntity blockEntity, ItemStack wandItem, EnumMap<Direction, Boolean> syncedConfig) {
        super(ModMenuTypes.UNIVERSAL_CONFIG_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.player = playerInventory.player;
        this.wandItem = wandItem;
        this.initialConfig = wandItem.getOrDefault(ModDataComponents.CONFIGURED_DIRECTIONS, new EnumMap<>(Direction.class));
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.originalConfig.putAll(currentConfig); // 複製初始狀態

        this.currentConfig.putAll(syncedConfig); // ✅ 用同步過來的正確值初始化
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
