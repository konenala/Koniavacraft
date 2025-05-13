package com.github.nalamodikk.common.ComponentSystem.util.helpers;

import com.github.nalamodikk.common.ComponentSystem.API.machine.IGridComponent;
import net.minecraft.core.BlockPos;
import net.minecraftforge.items.ItemStackHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GridLayoutHelper {
    public static final int MAX_GRID_WIDTH = 6;

    /**
     * 將模組元件清單自動排入平面格子（左上至右下），預設 Y 軸為 0。
     * @param components 模組元件列表
     * @param width      每列最大格數（建議 3~5）
     * @return BlockPos → IGridComponent 的對應圖（可用於 syncTo）
     */
    public static Map<BlockPos, IGridComponent> buildFlatLayout(List<IGridComponent> components, int width) {
        Map<BlockPos, IGridComponent> layout = new HashMap<>();
        int x = 0;
        int y = 0;

        for (IGridComponent component : components) {
            BlockPos pos = new BlockPos(x, 0, y); // 注意：Z 軸當成 GUI 的 Y 軸
            layout.put(pos, component);

            x++;
            if (x >= width) {
                x = 0;
                y++;
            }
        }

        return layout;
    }

    public static int getRecommendedWidth(ItemStackHandler handler) {
        int count = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) {
                count++;
            }
        }

        // 自動平方根向上取整 → 最小 1，最大取 handler 大小上限開根號
        return Math.min((int) Math.ceil(Math.sqrt(count)), MAX_GRID_WIDTH);
    }


}
