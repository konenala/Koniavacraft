package com.github.nalamodikk.common.ComponentSystem.block.blockentity.MachineBlock;

import com.github.nalamodikk.common.ComponentSystem.API.machine.IGridComponent;
import com.github.nalamodikk.common.ComponentSystem.API.machine.grid.ComponentGrid;
import com.github.nalamodikk.common.ComponentSystem.register.component.ComponentRegistry;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.item.ModuleItem;
import com.github.nalamodikk.common.register.ModBlockEntities;
import com.github.nalamodikk.common.util.GridIOHelper;
import com.github.nalamodikk.common.util.helpers.GridLayoutHelper;
import com.github.nalamodikk.common.util.helpers.ModuleItemHelper;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import org.slf4j.Logger;

import java.util.*;

public class ModularMachineBlockEntity extends BlockEntity {
    private ComponentGrid componentGrid;
    private final int ItemStackHandlerSize = 25;
    private final ItemStackHandler itemHandler = new ItemStackHandler(ItemStackHandlerSize); // 玩家放模組物品
    private int lastHash = -1;
    private  final Deque<MachineSnapshot> history = new ArrayDeque<>();
    private int lastLayoutHash = -1;
    public static final Logger LOGGER = LogUtils.getLogger();

    public ModularMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MODULAR_MACHINE_BE.get(), pos, state);
        this.componentGrid = new ComponentGrid(this.getLevel()); // 傳入世界物件
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
        Map<BlockPos, IGridComponent> newLayout = GridLayoutHelper.buildFlatLayout(components, width);

        // ✅ 這段不能少，否則每次都會重建
        int layoutSignature = computeGridSignature(newLayout);
        if (layoutSignature != lastLayoutHash) {
            LOGGER.debug("【rebuildGridFromItemHandler】🔁 Layout changed! Signature = {}", layoutSignature);
            componentGrid.syncTo(newLayout);
            lastLayoutHash = layoutSignature;
        }

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

        Map<BlockPos, IGridComponent> newLayout = new HashMap<>();
        int z = 0;

        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            for (IGridComponent component : ModuleItemHelper.getComponentsRaw(stack)) {
                newLayout.put(new BlockPos(0, 0, z++), component);
            }
        }

        int layoutSignature = computeGridSignature(newLayout); // ✅ 關鍵判斷點
        if (layoutSignature != lastLayoutHash) {
            LOGGER.debug("【public void tick 】🔁 Layout changed! Signature = {}", layoutSignature);

            componentGrid.syncTo(newLayout);                    // ✅ 只在 layout 真正變動時 sync
            lastLayoutHash = layoutSignature;
        }

        componentGrid.tick();
    }




    private static int computeGridSignature(Map<BlockPos, IGridComponent> layout) {
        List<String> entries = layout.entrySet().stream()
                .map(e -> e.getKey().getX() + "," + e.getKey().getY() + "," + e.getKey().getZ() + "@" + e.getValue().getId())
                .sorted()
                .toList();

        String joined = String.join(",", entries);
        int hash = joined.hashCode();


//        LOGGER.debug("🔍 Layout Signature = {} | entries = {}", hash, joined);
        return hash;
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
        GridIOHelper.writeToNBTIfPresent(componentGrid, tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("items")) {
            itemHandler.deserializeNBT(tag.getCompound("items")); // ✅ 還原模組欄位
        }
        GridIOHelper.readOrInitFromNBT(componentGrid, tag);

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



}
