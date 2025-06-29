package com.github.nalamodikk.common.screen.player;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;

public class ExtraEquipmentScreen extends AbstractContainerScreen<ExtraEquipmentMenu> {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/extra_equipment.png");
        private static final int PLAYER_MODEL_X = 30;  // 人物模型X位置 (黑色區域中心)
        private static final int PLAYER_MODEL_Y = 54;  // 人物模型Y位置
        private static final int PLAYER_MODEL_SIZE = 30; // 人物模型大小

        private float xMouse;
        private float yMouse;

        public ExtraEquipmentScreen(ExtraEquipmentMenu menu, Inventory playerInventory, Component title) {
            super(menu, playerInventory, title);
            this.imageWidth = 256;
            this.imageHeight = 256;
        }

        @Override
        protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
            graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
            renderPlayerModelWithEyeTracking(graphics,
                    leftPos + PLAYER_MODEL_X,
                    topPos + PLAYER_MODEL_Y,
                    PLAYER_MODEL_SIZE,
                    xMouse,
                    yMouse,
                    this.minecraft.player);

        }

        @Override
        protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
            graphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
            graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 0x404040, false);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // 先更新滑鼠位置
            this.xMouse = (float)mouseX;
            this.yMouse = (float)mouseY;

            this.renderBackground(graphics, mouseX, mouseY, partialTick);
            super.render(graphics, mouseX, mouseY, partialTick);
            this.renderTooltip(graphics, mouseX, mouseY);
        }


    /**
     * 🔥 進階版：只有眼睛跟隨滑鼠，身體保持靜止 🔥
     * 最自然的頭部追蹤效果，就像真人在看滑鼠位置
     */
    public static void renderPlayerModelWithEyeTracking(GuiGraphics graphics, int x, int y, int size, float mouseX, float mouseY, LivingEntity entity) {
        if (entity == null) return;

        // 調大裁剪區域的寬度
        int x1 = x - size;      // 左邊界往左擴大（原本是 size/2）
        int y1 = y - size;
        int x2 = x + size;      // 右邊界往右擴大（原本是 size/2）
        int y2 = y + size;

        // 開啟裁剪，防止人物渲染超出邊界
        graphics.enableScissor(x1, y1, x2, y2);

        // 使用原版方法渲染
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics,
                x1, y1, x2, y2,  // 渲染區域邊界
                size,            // 縮放大小
                0.0625F,         // Y偏移
                mouseX, mouseY,  // 滑鼠位置
                entity           // 實體
        );

        // 關閉裁剪
        graphics.disableScissor();
    }
}


