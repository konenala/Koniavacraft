package com.github.nalamodikk.common.block.conduit;

import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.register.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


public class ArcaneConduitConfigMenu extends AbstractContainerMenu {

    private final ArcaneConduitBlockEntity conduit;
    private final ContainerData data;

    // 客戶端構造函數（從網路封包）
    public ArcaneConduitConfigMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(id, playerInventory, getConduitFromBuf(playerInventory, extraData));
    }

    // 伺服器端構造函數
    public ArcaneConduitConfigMenu(int id, Inventory playerInventory, ArcaneConduitBlockEntity conduit) {
        super(ModMenuTypes.CONDUIT_CONFIG_MENU.get(), id);
        this.conduit = conduit;

        // 創建同步資料：6個方向 × 2個數值（IO類型+優先級）= 12個數值
        this.data = new SimpleContainerData(12);
        this.addDataSlots(data);

        // 同步當前設置到客戶端
        syncFromConduit();

        // 這是純配置界面，不需要物品槽
    }

    private static ArcaneConduitBlockEntity getConduitFromBuf(Inventory playerInventory, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Level level = playerInventory.player.level();

        if (level.getBlockEntity(pos) instanceof ArcaneConduitBlockEntity conduit) {
            return conduit;
        }

        throw new IllegalStateException("Invalid conduit position: " + pos);
    }

    private void syncFromConduit() {
        if (conduit != null) {
            for (Direction dir : Direction.values()) {
                int index = dir.ordinal();
                data.set(index * 2, conduit.getIOConfig(dir).ordinal());
                data.set(index * 2 + 1, conduit.getPriority(dir));
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return conduit != null &&
                conduit.getLevel() != null &&
                conduit.getLevel().getBlockEntity(conduit.getBlockPos()) == conduit &&
                player.distanceToSqr(conduit.getBlockPos().getX() + 0.5,
                        conduit.getBlockPos().getY() + 0.5,
                        conduit.getBlockPos().getZ() + 0.5) <= 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // 沒有物品槽
    }

    // 獲取方向的 IO 類型
    public IOHandlerUtils.IOType getIOType(Direction dir) {
        int value = data.get(dir.ordinal() * 2);
        return IOHandlerUtils.IOType.values()[value];
    }

    // 獲取方向的優先級
    public int getPriority(Direction dir) {
        return data.get(dir.ordinal() * 2 + 1);
    }

    // 獲取導管實例
    public ArcaneConduitBlockEntity getConduit() {
        return conduit;
    }

    // 獲取導管位置
    public BlockPos getConduitPos() {
        return conduit != null ? conduit.getBlockPos() : BlockPos.ZERO;
    }
}