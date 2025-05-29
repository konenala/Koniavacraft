package com.github.nalamodikk.common.screen.shared;

import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.network.packet.manatool.ConfigDirectionUpdatePacket;
import com.github.nalamodikk.common.register.ModDataComponents;
import com.github.nalamodikk.common.register.ModMenuTypes;
import com.github.nalamodikk.common.utils.data.CodecsLibrary;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.Objects;

public class UniversalConfigMenu extends AbstractContainerMenu {
    private final EnumMap<Direction, Boolean> currentConfig = new EnumMap<>(Direction.class);
    private final Player player;
    private final BlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ItemStack wandItem;
    private final EnumMap<Direction, Boolean> initialConfig;

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

    @Override
    public void removed(Player player) {
        super.removed(player);

        // 只在伺服器端更新方塊配置
        if (!player.level().isClientSide) {
            if (this.blockEntity instanceof IConfigurableBlock configurableBlock) {
                boolean configChanged = false;

                for (Direction direction : Direction.values()) {
                    boolean newConfig = this.currentConfig.get(direction);
                    if (configurableBlock.isOutput(direction) != newConfig) {
                        configurableBlock.setDirectionConfig(direction, newConfig);
                        configChanged = true;
                    }
                }

                if (configChanged) {
                    MagicalIndustryMod.LOGGER.info("Updated configuration for block at {}", this.blockEntity.getBlockPos());
                    this.blockEntity.setChanged(); // 標記方塊為已變更以保存數據
                    // 同步方塊狀態更新
                    player.level().sendBlockUpdated(this.blockEntity.getBlockPos(), this.blockEntity.getBlockState(), this.blockEntity.getBlockState(), 3);
                }
            }
        } else {
            // 只在客戶端發送封包到伺服器
            for (Direction direction : Direction.values()) {
                boolean newConfig = this.currentConfig.get(direction);
                PacketDistributor.sendToServer(new ConfigDirectionUpdatePacket(this.blockEntity.getBlockPos(), direction, newConfig)
                );
            }
        }

        // 保存配置到物品的 NBT 數據
        if (wandItem != null) {
            EnumMap<Direction, Boolean> configCopy = new EnumMap<>(currentConfig); // 這通常是 UI 按鈕收集的
            wandItem.set(ModDataComponents.CONFIGURED_DIRECTIONS, configCopy);
        }

    }

    public UniversalConfigMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(id,playerInventory, Objects.requireNonNull(playerInventory.player.level().getBlockEntity(buf.readBlockPos())), CodecsLibrary.ITEM_STACK.decode(buf) // ✅ 自己 decode ItemStack
        );
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
