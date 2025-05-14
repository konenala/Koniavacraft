package com.github.nalamodikk.common.ComponentSystem.API.machine.grid;

import com.github.nalamodikk.common.ComponentSystem.API.machine.IComponentBehavior;
import com.github.nalamodikk.common.ComponentSystem.API.machine.IGridComponent;
import com.github.nalamodikk.common.ComponentSystem.register.component.ComponentRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * ComponentGrid：魔法工業裝置的拼裝核心，用於管理所有已安裝模組的位置與邏輯。
 */
public class ComponentGrid {
    private final Level level;
    // Logger，用於輸出資訊與除錯訊息
    private static final Logger LOGGER = LoggerFactory.getLogger("MagicalIndustry");
    private final Map<BlockPos, ComponentContext> contextMap = new HashMap<>();
    private final Map<BlockPos, ComponentContext> contextCache = new HashMap<>();
    private final Map<String, IGridComponent> componentIdMap = new HashMap<>();
    // 改為記錄每個位置（BlockPos）上每個行為 id 的 tick 計數
    // Grid 主體：使用 BlockPos 當 Key（僅使用 X 與 Z）儲存格子模組
    private final Map<BlockPos, IGridComponent> grid = new HashMap<>();
    private int lastLayoutHash = 0;
    private static String lastSignatureString = null;
    private static final Map<Map<BlockPos, IGridComponent>, Integer> SIGNATURE_CACHE = new WeakHashMap<>();

    public ComponentGrid(Level level) {
        this.level = level;
        // 其他初始化
    }

    public void addComponent(BlockPos pos, IGridComponent component) {
        grid.put(pos, component);
        component.onAdded(this, pos);

        ComponentContext context = new ComponentContext(this, pos, component);
        contextMap.put(pos, context);
        contextCache.put(pos, context); // optional 快取，如果你有用的話
    }

    /**
     * 放入模組到指定位置（x, y）
     */
    public void setComponent(int x, int y, IGridComponent component) {
        BlockPos pos = new BlockPos(x, 0, y); // 固定 Y 為 0，視為平面座標
        grid.put(pos, component);
        component.onAdded(this, pos);
    }

    public void trySyncTo(Map<BlockPos, IGridComponent> newLayout) {
        int currentHash = computeGridSignature(newLayout);
        if (currentHash == lastLayoutHash) return;

        LOGGER.debug("【ComponentGrid】🔁 Layout changed! Signature = {}", currentHash);
        syncTo(newLayout);
        lastLayoutHash = currentHash;
    }

    public void tick() {
        for (Map.Entry<BlockPos, IGridComponent> entry : grid.entrySet()) {
            BlockPos pos = entry.getKey();
            IGridComponent component = entry.getValue();

            // 🧠 使用快取的 ComponentContext
            ComponentContext context = contextCache.computeIfAbsent(pos, p -> new ComponentContext(this, p, component));

            for (IComponentBehavior behavior : component.getBehaviors()) {
                String behaviorId = behavior.getId().toString(); // 對每個行為要求必須實作 getId()
                int tickRate = behavior.getTickRate();
                if (tickRate <= 0) continue;

                // 取得此位置的所有 tick 記錄
                if (context.shouldTick(behaviorId, tickRate)) {
                    behavior.onTick(context);
                }

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
        removeComponent(new BlockPos(x, 0, y));
    }

    public void removeComponent(BlockPos pos) {
        IGridComponent component = grid.remove(pos);
        if (component != null) {
            component.onRemoved(this, pos);
            ComponentContext context = contextMap.remove(pos);
            if (context != null) {
                context.resetTickStates(); // 🧹 清除內部 tick 記憶
            }
            contextCache.remove(pos);

        }
    }

    /**
     * 保存模塊NBT
     * @return
     */
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();

        for (Map.Entry<BlockPos, IGridComponent> entry : grid.entrySet()) {
            CompoundTag componentTag = new CompoundTag();
            componentTag.put("pos", NbtUtils.writeBlockPos(entry.getKey()));
            componentTag.putString("id", entry.getValue().getId().toString());
            componentTag.put("data", entry.getValue().getData().copy());
            list.add(componentTag);
        }

        tag.put("components", list);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        this.clear();

        if (tag.contains("components", Tag.TAG_LIST)) {
            ListTag list = tag.getList("components", Tag.TAG_COMPOUND);

            for (Tag t : list) {
                CompoundTag componentTag = (CompoundTag) t;
                BlockPos pos = NbtUtils.readBlockPos(componentTag.getCompound("pos"));
                ResourceLocation id = new ResourceLocation(componentTag.getString("id"));
                CompoundTag data = componentTag.getCompound("data");

                IGridComponent component = ComponentRegistry.createComponent(id);
                if (component != null) {
                    component.loadFromNBT(data);
                    this.addComponent(pos, component); // 呼叫 onAdded, 建立 context
                }
            }
        }
    }



    public static int computeGridSignature(Map<BlockPos, IGridComponent> layout) {
        List<String> sortedEntries = layout.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<BlockPos, IGridComponent> e) -> e.getKey().getY())
                        .thenComparingInt(e -> e.getKey().getX())
                        .thenComparingInt(e -> e.getKey().getZ()))
                .map(e -> e.getKey().getX() + "," + e.getKey().getY() + "," + e.getKey().getZ() + "@" + e.getValue().getSignatureString())
                .toList();

        String signatureString = String.join("|", sortedEntries);


        if (lastSignatureString != null && !lastSignatureString.equals(signatureString)) {
            LOGGER.debug("[Signature DEBUG] Layout changed!");
            String[] last = lastSignatureString.split("\\|");
            String[] now = signatureString.split("\\|");
            int len = Math.max(last.length, now.length);
            for (int i = 0; i < len; i++) {
                String prev = (i < last.length) ? last[i] : "<none>";
                String curr = (i < now.length) ? now[i] : "<none>";
                if (!Objects.equals(prev, curr)) {
                    LOGGER.debug("[Diff] {}: {} => {}", i, prev, curr);
                }
            }
        }

        if (!sortedEntries.isEmpty()) {
            LOGGER.debug("[Layout Signature Debug] ---");
            for (String s : sortedEntries) {
                LOGGER.debug("[Layout] {}", s);
            }
        }


        lastSignatureString = signatureString;
        return signatureString.hashCode();
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
    }






    /**
     * 取得所有模組位置與資料（給外部用）
     */
    public Map<BlockPos, IGridComponent> getAllComponents() {
        return grid;
    }
}
