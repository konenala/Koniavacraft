package com.github.nalamodikk.client.screenAPI.gui.api.open;

/**
 * 用來創建升級按鈕並綁定封包邏輯
 * 當時設計目的是讓某個機器 GUI 可以呼叫這個按鈕以開啟升級畫面
 * 尚未接入任何實際畫面
 */

import com.github.nalamodikk.common.upgrade.api.IUpgradeableMachine;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.MenuProvider;

import com.github.nalamodikk.common.screen.UpgradeMenu;

public class UpgradeGuiOpener {

    public static void open(ServerPlayer player, IUpgradeableMachine machine) {
        NetworkHooks.openScreen(player, new UpgradeMenuProvider(machine), buf -> {
            // ❗ optional: 傳送 packet 裡的額外資訊，如 ID、機器類型…目前留空
        });
    }

    // 內部 MenuProvider
    private static class UpgradeMenuProvider implements MenuProvider {
        private final IUpgradeableMachine machine;

        public UpgradeMenuProvider(IUpgradeableMachine machine) {
            this.machine = machine;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("screen.magical_industry.upgrade.title");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, net.minecraft.world.entity.player.Player player) {
            return new UpgradeMenu(id, inv, machine.getUpgradeInventory(), machine);
        }
    }
}
