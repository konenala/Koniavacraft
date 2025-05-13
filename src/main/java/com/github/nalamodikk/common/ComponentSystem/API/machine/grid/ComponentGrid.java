package com.github.nalamodikk.common.ComponentSystem.API.machine.grid;

import com.github.nalamodikk.common.ComponentSystem.API.machine.IComponentBehavior;
import com.github.nalamodikk.common.ComponentSystem.API.machine.IGridComponent;
import com.github.nalamodikk.common.ComponentSystem.register.component.ComponentRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * ComponentGrid：魔法工業裝置的拼裝核心，用於管理所有已安裝模組的位置與邏輯。
 */
public class ComponentGrid {
    private final Level level;
    // Logger，用於輸出資訊與除錯訊息
    private static final Logger LOGGER = LoggerFactory.getLogger("MagicalIndustry");

    // 行為對應的累積 tick 次數，用於 tickRate 調度
    private final Map<IComponentBehavior, Integer> tickCounterMap = new HashMap<>();


    // Grid 主體：使用 BlockPos 當 Key（僅使用 X 與 Z）儲存格子模組
    private final Map<BlockPos, IGridComponent> grid = new HashMap<>();

    public ComponentGrid(Level level) {
        this.level = level;
        // 其他初始化
    }
    /**
     * 放入模組到指定位置（x, y）
     */
    public void setComponent(int x, int y, IGridComponent component) {
        BlockPos pos = new BlockPos(x, 0, y); // 固定 Y 為 0，視為平面座標
        grid.put(pos, component);
        component.onAdded(this, pos);
    }


    public void tick() {
        for (Map.Entry<BlockPos, IGridComponent> entry : grid.entrySet()) {
            BlockPos pos = entry.getKey();
            IGridComponent component = entry.getValue();

            // 每個元件有一個上下文物件
            ComponentContext context = new ComponentContext(this, pos, component);

            for (IComponentBehavior behavior : component.getBehaviors()) {
                int tickRate = behavior.getTickRate();

                if (tickRate <= 0) continue; // ❌ 不允許 tickRate 小於 1（可定義 -1 為 passive）

                int currentTick = tickCounterMap.getOrDefault(behavior, 0) + 1;

                // ✅ 當 tick 數達到 tickRate 就執行 onTick()
                if (currentTick >= tickRate) {
                    behavior.onTick(context);
                    currentTick = 0; // 重置計數器
                }

                // 更新行為計數
                tickCounterMap.put(behavior, currentTick);
            }
        }
    }

    public Level getLevel() {
        return this.level;
    }

    /**
     *
     * @param pos
     * @return
     */
    public void forEachNeighbor(BlockPos pos, BiConsumer<BlockPos, IGridComponent> action) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.offset(dir.getStepX(), 0, dir.getStepZ());
            IGridComponent neighbor = getComponent(neighborPos);
            if (neighbor != null) {
                action.accept(neighborPos, neighbor);
            }
        }
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
            compTag.putInt("y", pos.getZ()); // Z 當作 Grid 的 Y 軸（平面）

            compTag.putString("id", component.getId().toString());

            CompoundTag dataTag = new CompoundTag();
            component.saveToNBT(dataTag);
            compTag.put("data", dataTag);

            list.add(compTag);
        }

        tag.put("ComponentGrid", list);

        // ✅ 加入版本號（只要這個結構有改就升級版本）
        tag.putInt("ComponentGridVersion", 1);
    }


    /**
     * 從 NBT 載入所有拼裝模組
     */
    public void loadFromNBT(CompoundTag tag) {
        int version = tag.contains("ComponentGridVersion") ? tag.getInt("ComponentGridVersion") : 0;
        if (version < 1) {
            LOGGER.error("❌ 嘗試讀取過舊版本 ComponentGrid（version {}），請升級資料格式！", version);
            return;
        }

        ListTag list = tag.getList("ComponentGrid", Tag.TAG_COMPOUND);
        Map<BlockPos, IGridComponent> newComponents = new HashMap<>();

        for (Tag element : list) {
            CompoundTag compTag = (CompoundTag) element;
            ComponentRecord record = ComponentRecord.fromNBT(compTag);
            if (record == null) {
                LOGGER.warn("⚠️ 無法解析元件 NBT 結構：{}", compTag);
                continue;
            }

            IGridComponent component = ComponentRegistry.createComponent(record.id());
            if (component == null) {
                LOGGER.warn("⚠️ 找不到對應模組: {}", record.id());
                continue;
            }

            component.loadFromNBT(record.data());
            newComponents.put(record.pos(), component);
        }

        // ✅ 不做 layout signature 比對，直接 sync
        syncTo(newComponents);

        LOGGER.info("✅ 拼裝還原完成（v{}）：{} 個模組", version, newComponents.size());
    }



    public <T> T findFirstComponent(Class<T> type) {
        for (IGridComponent component : grid.values()) {
            if (type.isInstance(component)) {
                return type.cast(component);
            }
        }
        return null;
    }

    public void syncTo(Map<BlockPos, IGridComponent> newComponents) {
        // ✅ 先移除舊的元件（如果新 layout 沒有）
        for (BlockPos pos : new ArrayList<>(grid.keySet())) {
            if (!newComponents.containsKey(pos)) {
                IGridComponent old = grid.get(pos);
                old.onRemoved(this, pos);
                grid.remove(pos);
            }
        }

        // ✅ 再加入新元件（若不同則 clone 再放入 grid）
        for (Map.Entry<BlockPos, IGridComponent> entry : newComponents.entrySet()) {
            BlockPos pos = entry.getKey();
            IGridComponent incoming = entry.getValue();

            // clone 一份新的元件（確保不共用記憶體）
            IGridComponent newComponent = ComponentRegistry.createComponent(incoming.getId());
            if (newComponent != null) {
                newComponent.loadFromNBT(incoming.getData().copy());

                if (!grid.containsKey(pos)) {
                    grid.put(pos, newComponent);
                    newComponent.onAdded(this, pos);
                } else {
                    IGridComponent old = grid.get(pos);
                    if (!old.getId().equals(newComponent.getId())) {
                        old.onRemoved(this, pos);
                        grid.put(pos, newComponent);
                        newComponent.onAdded(this, pos);
                    }
                }
            }
        }

        // ✅ 重置所有行為的 tick 狀態（避免殘留）
        tickCounterMap.clear();
    }


    @SuppressWarnings("unchecked")
    public <T extends IComponentBehavior> T findBehavior(Class<T> type) {
        for (IGridComponent component : grid.values()) {
            for (IComponentBehavior behavior : component.getBehaviors()) {
                if (type.isInstance(behavior)) {
                    return (T) behavior;
                }
            }
        }
        return null;
    }

    public void clear() {
        for (Map.Entry<BlockPos, IGridComponent> entry : grid.entrySet()) {
            entry.getValue().onRemoved(this, entry.getKey());
        }
        grid.clear();
        tickCounterMap.clear();
    }

    private static int computeGridSignature(Map<BlockPos, IGridComponent> layout) {
        List<String> entries = layout.entrySet().stream()
                .map(e -> e.getKey().getX() + "," + e.getKey().getY() + "," + e.getKey().getZ() + "@" + e.getValue().getId())
                .sorted()
                .toList();

        String joined = String.join(",", entries);
        int hash = joined.hashCode();

        LOGGER.debug("🔍 Layout Signature = {} | entries = {}", hash, joined);
        return hash;
    }

    /**
     * 取得所有模組位置與資料（給外部用）
     */
    public Map<BlockPos, IGridComponent> getAllComponents() {
        return grid;
    }
}
