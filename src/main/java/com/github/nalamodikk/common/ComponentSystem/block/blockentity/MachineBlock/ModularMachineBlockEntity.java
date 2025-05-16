package com.github.nalamodikk.common.ComponentSystem.block.blockentity.MachineBlock;

import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.ComponentSystem.API.machine.IGridComponent;
import com.github.nalamodikk.common.ComponentSystem.API.machine.grid.ComponentGrid;
import com.github.nalamodikk.common.ComponentSystem.register.component.ComponentRegistry;
import com.github.nalamodikk.common.register.ModBlockEntities;
import com.github.nalamodikk.common.ComponentSystem.util.helpers.GridIOHelper;
import com.github.nalamodikk.common.ComponentSystem.util.helpers.GridLayoutHelper;
import com.github.nalamodikk.common.ComponentSystem.util.helpers.ModuleItemHelper;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import org.slf4j.Logger;

import java.util.*;

public class ModularMachineBlockEntity extends BlockEntity implements IConfigurableBlock {
    private ComponentGrid componentGrid;
    private final EnumMap<Direction, Boolean> directionConfig = new EnumMap<>(Direction.class);

    private final int ItemStackHandlerSize = 25;
    private final ItemStackHandler itemHandler = new ItemStackHandler(ItemStackHandlerSize) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            needRebuild = true; // 每次玩家放東西就要求下次 tick 重建
        }
    };
    private int lastHash = -1;
    private  final Deque<MachineSnapshot> history = new ArrayDeque<>();
    private int lastLayoutHash = -1;
    public static final Logger LOGGER = LogUtils.getLogger();
    private boolean needRebuild = true;

    public ModularMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MODULAR_MACHINE_BE.get(), pos, state);
        this.componentGrid = new ComponentGrid(this.getLevel());// 傳入世界物件
        for (Direction dir : Direction.values()) {
            directionConfig.put(dir, false);
        }
    }
    private ItemStack lastStack = ItemStack.EMPTY; // 加在 class 裡面做快取

    public ItemStackHandler getInternalHandler() {
        return this.itemHandler;
    }

    public void rebuildGridFromItemHandler() {
        List<IGridComponent> components = new ArrayList<>();
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                components.addAll(ModuleItemHelper.getComponentsRaw(stack)); // ✅ 只用快取，不 clone
            }
        }

        int width = GridLayoutHelper.getRecommendedWidth(itemHandler);
        Map<BlockPos, IGridComponent> newLayout = new HashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < components.size(); i++) {
            int x = i % width;
            int z = i / width;
            pos.set(x, 0, z);
            newLayout.put(pos.immutable(), components.get(i));
        }

        // ✅ 這段不能少，否則每次都會重建
        int layoutSignature = computeGridSignature(newLayout);
        if (layoutSignature != lastLayoutHash) {
            LOGGER.debug("🔁 Layout changed! Signature = {}", layoutSignature);
            componentGrid.syncTo(newLayout);
            lastLayoutHash = layoutSignature;
        }

    }


    public ComponentGrid getComponentGrid() {
        return componentGrid;
    }


    public int restoreSnapshot(ServerPlayer player) {
        if (history.isEmpty()) return 0;

        MachineSnapshot snapshot = history.pop();
        int returned = 0;

        // 1. 拿目前物品欄（現在） vs 快照的 itemsBefore（之前） → 只退「新加的模組」
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack now = itemHandler.getStackInSlot(i);
            ItemStack old = (i < snapshot.itemsBefore.size()) ? snapshot.itemsBefore.get(i) : ItemStack.EMPTY;

            if (!now.isEmpty() && !ItemStack.matches(now, old)) {
                // 不一樣 → 是新加的 → 退還
                boolean success = player.getInventory().add(now.copy());
                if (!success) player.spawnAtLocation(now.copy());
                itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                returned++;
            }
        }
        lastLayoutHash = computeGridSignature(snapshot.gridState); // ✅ 修復 signature 不同步 bug

        // 2. 套用 snapshot 的拼裝結構與模組欄
        componentGrid.syncTo(snapshot.gridState);
        itemHandler.deserializeNBT(snapshot.itemHandlerNBT);
        lastLayoutHash = -1;
        setChanged();

        return returned;
    }



    public void pushSnapshot() {
        history.push(new MachineSnapshot(
                deepCopyGrid(componentGrid.getAllComponents()),
                itemHandler.serializeNBT(),
                itemHandler
        ));
    }


    static Map<BlockPos, IGridComponent> deepCopyGrid(Map<BlockPos, IGridComponent> original) {
        Map<BlockPos, IGridComponent> copy = new HashMap<>();
        for (Map.Entry<BlockPos, IGridComponent> entry : original.entrySet()) {
            IGridComponent originalComponent = entry.getValue();
            CompoundTag data = originalComponent.getData().copy();
            IGridComponent cloned = ComponentRegistry.createComponent(originalComponent.getId());
            if (cloned != null) {
                cloned.loadFromNBT(data);
                copy.put(entry.getKey(), cloned);
            }
        }
        return copy;
    }

    public void tick() {
        if (level == null || level.isClientSide) return;

        if (needRebuild) {
            rebuildGridFromItemHandler(); // 執行拼裝比對 + sync
            needRebuild = false;
        }

        componentGrid.tick(); // 正常行為執行
    }





    private static int computeGridSignature(Map<BlockPos, IGridComponent> layout) {
        List<String> sortedEntries = layout.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<BlockPos, IGridComponent> e) -> e.getKey().getY())
                        .thenComparingInt(e -> e.getKey().getX())
                        .thenComparingInt(e -> e.getKey().getZ()))
                .map(e -> e.getKey().getX() + "," + e.getKey().getY() + "," + e.getKey().getZ() + "@" + e.getValue().getId().toString())
                .toList();

        String signatureString = String.join("|", sortedEntries);

        return signatureString.hashCode();
    }




    public boolean tryInsertModule(ItemStack stack) {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (itemHandler.getStackInSlot(i).isEmpty()) {
                itemHandler.setStackInSlot(i, stack.copy());
                setChanged(); // 告訴 Forge 資料變了
                return true;
            }
        }
        return false;
    }

    public void markUpdateNextTick() {
        lastLayoutHash = -1; // 強制下次 tick 重建 Grid
    }

    public ComponentGrid getGrid() {
        return this.componentGrid;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("items", itemHandler.serializeNBT());
        tag.put("grid", componentGrid.serializeNBT());

        GridIOHelper.writeToNBTIfPresent(componentGrid, tag);

        CompoundTag dirTag = new CompoundTag();
        for (Map.Entry<Direction, Boolean> entry : directionConfig.entrySet()) {
            dirTag.putBoolean(entry.getKey().getName(), entry.getValue());
        }
        tag.put("direction_config", dirTag);

    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.contains("items")) {
            itemHandler.deserializeNBT(tag.getCompound("items"));
        }

        if (tag.contains("grid")) {
            componentGrid.deserializeNBT(tag.getCompound("grid")); // ✅ 還原所有行為的內部狀態
        }

        if (tag.contains("direction_config")) {
            CompoundTag dirTag = tag.getCompound("direction_config");
            for (Direction dir : Direction.values()) {
                if (dirTag.contains(dir.getName())) {
                    directionConfig.put(dir, dirTag.getBoolean(dir.getName()));
                }
            }
        }


        this.rebuildGridFromItemHandler(); // 拼裝 layout 對照更新
    }



    private static class MachineSnapshot {
        final Map<BlockPos, IGridComponent> gridState;
        final CompoundTag itemHandlerNBT;
        final List<ItemStack> itemsBefore; // 🔍 用來比對哪些是後加的

        MachineSnapshot(Map<BlockPos, IGridComponent> grid, CompoundTag itemHandlerNBT, ItemStackHandler currentItems) {
            this.gridState = grid;
            this.itemHandlerNBT = itemHandlerNBT.copy();

            this.itemsBefore = new ArrayList<>();
            for (int i = 0; i < currentItems.getSlots(); i++) {
                itemsBefore.add(currentItems.getStackInSlot(i).copy());
            }
        }
    }
    @Override
    public void setDirectionConfig(Direction direction, boolean isOutput) {
        directionConfig.put(direction, isOutput);
    }

    @Override
    public boolean isOutput(Direction direction) {
        return directionConfig.getOrDefault(direction, false);
    }



}
