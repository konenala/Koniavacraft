package com.github.nalamodikk.common.API.machine.grid;

import com.github.nalamodikk.common.API.IGridComponent;
import com.github.nalamodikk.common.API.machine.component.ComponentRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ComponentGrid：魔法工業裝置的拼裝核心，用於管理所有已安裝模組的位置與邏輯。
 */
public class ComponentGrid {

    // Logger，用於輸出資訊與除錯訊息
    private static final Logger LOGGER = LoggerFactory.getLogger("MagicalIndustry");

    // Grid 主體：使用 BlockPos 當 Key（僅使用 X 與 Z）儲存格子模組
    private final Map<BlockPos, IGridComponent> grid = new HashMap<>();

    /**
     * 放入模組到指定位置（x, y）
     */
    public void setComponent(int x, int y, IGridComponent component) {
        BlockPos pos = new BlockPos(x, 0, y); // 固定 Y 為 0，視為平面座標
        grid.put(pos, component);
        component.onAdded(this, pos);
    }

    /**
     *
     * @param pos
     * @return
     */
    public List<BlockPos> getNeighborPositions(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();
        neighbors.add(pos.offset(1, 0, 0));  // 東
        neighbors.add(pos.offset(-1, 0, 0)); // 西
        neighbors.add(pos.offset(0, 0, 1));  // 南
        neighbors.add(pos.offset(0, 0, -1)); // 北
        return neighbors;
    }

    /**
     * 從指定位置移除模組
     */
    public void removeComponent(int x, int y) {
        BlockPos pos = new BlockPos(x, 0, y);
        IGridComponent removed = grid.remove(pos);
        if (removed != null) removed.onRemoved(this, pos);
    }

    /**
     * 取得指定格子的模組（可能為 null）
     */
    public IGridComponent getComponent(int x, int y) {
        return grid.get(new BlockPos(x, 0, y));
    }


    /**
     * 取得指定格子的模組（支援 BlockPos 查詢）
     */
    public IGridComponent getComponent(BlockPos pos) {
        return grid.get(pos);
    }

    /**
     * 重新計算整個拼裝陣列的效果（此處僅輸出測試 log）
     */
    public void recalculate() {
        LOGGER.info("🔄 重新計算拼裝結果：共 {} 個模組", grid.size());
        // ⚠️ 之後可擴充：分析核心數量、連線狀況、效率分數、失衡檢查等
    }

    /**
     * 儲存整個拼裝網格資料到 NBT
     */
    public void saveToNBT(CompoundTag tag) {
        ListTag list = new ListTag();

        for (Map.Entry<BlockPos, IGridComponent> entry : grid.entrySet()) {
            BlockPos pos = entry.getKey();
            IGridComponent component = entry.getValue();

            CompoundTag compTag = new CompoundTag();
            compTag.putInt("x", pos.getX());
            compTag.putInt("y", pos.getZ()); // Z 當作 Grid 的 Y 軸（我們是平面）
            compTag.putString("id", component.getId().toString());

            CompoundTag dataTag = new CompoundTag();
            component.saveToNBT(dataTag);
            compTag.put("data", dataTag);

            list.add(compTag);
        }

        tag.put("ComponentGrid", list);
    }

    /**
     * 從 NBT 載入所有拼裝模組
     */
    public void loadFromNBT(CompoundTag tag) {
        grid.clear();

        ListTag list = tag.getList("ComponentGrid", Tag.TAG_COMPOUND);
        for (Tag element : list) {
            CompoundTag compTag = (CompoundTag) element;
            int x = compTag.getInt("x");
            int y = compTag.getInt("y");
            String idStr = compTag.getString("id");

            IGridComponent component = ComponentRegistry.get(idStr);
            if (component != null) {
                component.loadFromNBT(compTag.getCompound("data"));
                setComponent(x, y, component);
            } else {
                LOGGER.warn("⚠️ 找不到對應模組: {}", idStr);
            }
        }

        LOGGER.info("✅ 成功從 NBT 載入 ComponentGrid（{} 模組）", grid.size());
    }

    /**
     * 取得所有模組位置與資料（給外部用）
     */
    public Map<BlockPos, IGridComponent> getAllComponents() {
        return grid;
    }
}
