package com.github.nalamodikk.common.block.blockentity.ore_grinder;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.manabase.AbstractManaMachineEntityBlock;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.coreapi.recipe.ProcessingRecipe;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.register.ModBlockEntities;
import com.github.nalamodikk.register.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

import com.github.nalamodikk.common.block.blockentity.ore_grinder.sync.OreGrinderSyncHelper;

/**
 * ⚙️ 礦石粉碎機 BlockEntity
 */
public class OreGrinderBlockEntity extends AbstractManaMachineEntityBlock {

    private static final Logger LOGGER = LoggerFactory.getLogger(OreGrinderBlockEntity.class);

    // === 📦 槽位定義 ===
    private static final int INPUT_SLOT_1 = 0;
    private static final int INPUT_SLOT_2 = 1;
    private static final int OUTPUT_SLOT_1 = 2;
    private static final int OUTPUT_SLOT_2 = 3;
    private static final int OUTPUT_SLOT_3 = 4;
    private static final int OUTPUT_SLOT_4 = 5;
    private static final int SLOT_COUNT = 6;

    // === 🔧 配置常量 ===
    private static final int MAX_MANA_CAPACITY = 100000;
    private static final int GRINDING_TIME = 200;  // 10 秒
    private static final int INTERVAL_TICK = 1;

    // === 📊 同步助手 ===
    private final OreGrinderSyncHelper syncHelper = new OreGrinderSyncHelper();

    // === 📊 狀態變量 ===
    private final EnumMap<Direction, IOHandlerUtils.IOType> directionConfig = new EnumMap<>(Direction.class);
    private ProcessingRecipe currentRecipe = null;
    public boolean hasInputChanged = false;

    public OreGrinderBlockEntity(BlockPos pos, BlockState blockState) {
        super(
                ModBlockEntities.ORE_GRINDER.get(),
                pos,
                blockState,
                false,
                0,
                MAX_MANA_CAPACITY,
                INTERVAL_TICK,
                0
        );

        this.maxProgress = GRINDING_TIME;
        initializeIOConfig();
    }
    
    public static int getMaxMana() {
        return MAX_MANA_CAPACITY;
    }

    public OreGrinderSyncHelper getSyncHelper() {
        return syncHelper;
    }

    // === 🏗️ 初始化 ===

    /**
     * 🔧 初始化 IO 配置
     */
    private void initializeIOConfig() {
        directionConfig.put(Direction.UP, IOHandlerUtils.IOType.INPUT);
        directionConfig.put(Direction.DOWN, IOHandlerUtils.IOType.OUTPUT);
        directionConfig.put(Direction.NORTH, IOHandlerUtils.IOType.BOTH);
        directionConfig.put(Direction.SOUTH, IOHandlerUtils.IOType.BOTH);
        directionConfig.put(Direction.EAST, IOHandlerUtils.IOType.BOTH);
        directionConfig.put(Direction.WEST, IOHandlerUtils.IOType.BOTH);
    }

    /**
     * 📦 創建物品處理器
     */
    @Override
    protected ItemStackHandler createHandler() {
        return new ItemStackHandler(SLOT_COUNT) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                if (slot < 2) { // 輸入槽變化
                    hasInputChanged = true;
                }
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot == INPUT_SLOT_1 || slot == INPUT_SLOT_2) {
                    // 輸入槽檢查是否有有效配方
                    return canGrind(stack);
                } else if (slot >= OUTPUT_SLOT_1 && slot <= OUTPUT_SLOT_4) {
                    return false; // 輸出槽不允許手動放入
                }
                return super.isItemValid(slot, stack);
            }
        };
    }

    @Override
    public void tickMachine() {
        if (level == null || level.isClientSide()) return;

        // 1. 同步數據到 Helper (由 Menu 讀取)
        syncHelper.syncFrom(this);

        // 2. 處理輸入變化
        if (hasInputChanged) {
            updateCurrentRecipe();
            hasInputChanged = false;
        }

        // 3. 嘗試進行研磨
        if (currentRecipe != null && progress < maxProgress) {
            int manaCost = currentRecipe.getManaCost();

            if (manaStorage != null && manaStorage.getManaStored() >= manaCost) {
                progress++;
                manaStorage.extractMana(manaCost, ManaAction.EXECUTE);
                setChanged();
            }
        }

        // 4. 完成時輸出結果
        if (currentRecipe != null && progress >= maxProgress) {
            finishGrinding();
        }
    }

    /**
     * 🔍 更新當前配方
     */
    private void updateCurrentRecipe() {
        currentRecipe = null;
        progress = 0;

        if (itemHandler == null) return;

        ItemStack input1 = itemHandler.getStackInSlot(INPUT_SLOT_1);
        ItemStack input2 = itemHandler.getStackInSlot(INPUT_SLOT_2);

        if (input1.isEmpty() && input2.isEmpty()) {
            return;
        }

        // 查找配方
        List<ItemStack> inputs = new ArrayList<>();
        if (!input1.isEmpty()) inputs.add(input1);
        if (!input2.isEmpty()) inputs.add(input2);

        ProcessingRecipe.ProcessingInput recipeInput = new ProcessingRecipe.ProcessingInput(
                inputs,
                "grinder"
        );

        if (level == null) return;

        Optional<RecipeHolder<ProcessingRecipe>> recipe = level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.PROCESSING_TYPE.get())
                .stream()
                .filter(holder -> holder.value().matches(recipeInput, level))
                .findFirst();

        if (recipe.isPresent()) {
            currentRecipe = recipe.get().value();
            if (KoniavacraftMod.IS_DEV) {
                LOGGER.info("Found recipe for ore grinder");
            }
        }
    }

    /**
     * ✅ 完成研磨，輸出結果
     */
    private void finishGrinding() {
        if (currentRecipe == null || itemHandler == null) return;

        // 輸出主產物
        ItemStack mainOutput = currentRecipe.getMainOutput().copy();
        if (!itemHandler.insertItem(OUTPUT_SLOT_1, mainOutput, false).isEmpty()) {
            // 失敗，等待槽位空出
            return;
        }

        // 輸出概率副產物
        for (ProcessingRecipe.ChanceOutput chanceOutput : currentRecipe.getChanceOutputs()) {
            if (Math.random() < chanceOutput.getChance()) {
                ItemStack output = chanceOutput.getOutput().copy();
                for (int slot = OUTPUT_SLOT_2; slot <= OUTPUT_SLOT_4; slot++) {
                    ItemStack result = itemHandler.insertItem(slot, output, false);
                    if (result.isEmpty()) {
                        break;
                    }
                    output = result;
                }
            }
        }

        // 消耗輸入物品
        if (itemHandler.getStackInSlot(INPUT_SLOT_1).isEmpty()) {
            // 輸入槽 1 已用完，嘗試從槽位 2 補充
            // 實際應該在這裡實作堆疊分離邏輯
        }

        // 重置狀態
        progress = 0;
        currentRecipe = null;
        hasInputChanged = true;

        if (KoniavacraftMod.IS_DEV) {
            LOGGER.info("Grinding finished, output produced");
        }
    }

    /**
     * 🔍 判斷物品是否可以研磨
     */
    private boolean canGrind(ItemStack stack) {
        if (stack.isEmpty() || level == null || level.isClientSide()) return false;

        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(stack);

        ProcessingRecipe.ProcessingInput recipeInput = new ProcessingRecipe.ProcessingInput(
                inputs,
                "grinder"
        );

        return level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.PROCESSING_TYPE.get())
                .stream()
                .anyMatch(holder -> holder.value().matches(recipeInput, level));
    }

    @Override
    protected boolean canGenerate() {
        return false; // 粉碎機不生成魔力
    }

    // === 🔧 IConfigurableBlock 實作 ===

    @Override
    public void setIOConfig(Direction direction, IOHandlerUtils.IOType type) {
        directionConfig.put(direction, type);
        setChanged();
    }

    @Override
    public IOHandlerUtils.IOType getIOConfig(Direction direction) {
        return directionConfig.getOrDefault(direction, IOHandlerUtils.IOType.DISABLED);
    }

    @Override
    public EnumMap<Direction, IOHandlerUtils.IOType> getIOMap() {
        return new EnumMap<>(directionConfig);
    }

    @Override
    public void setIOMap(EnumMap<Direction, IOHandlerUtils.IOType> map) {
        directionConfig.clear();
        directionConfig.putAll(map);
        setChanged();
    }

    // === 📦 Menu 支援 ===

    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new OreGrinderMenu(pContainerId, pPlayerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.koniava.ore_grinder");
    }

    // === 💾 數據保存 ===

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        if (itemHandler != null) {
            CompoundTag itemsTag = itemHandler.serializeNBT(registries);
            tag.put("Items", itemsTag);
        }

        tag.put("DirectionConfig", serializeIOMap());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains("Items") && itemHandler != null) {
            CompoundTag itemsTag = tag.getCompound("Items");
            itemHandler.deserializeNBT(registries, itemsTag);
        }

        if (tag.contains("DirectionConfig")) {
            deserializeIOMap(tag.getCompound("DirectionConfig"));
        }

        hasInputChanged = true;
    }

    private CompoundTag serializeIOMap() {
        CompoundTag tag = new CompoundTag();
        for (Direction direction : Direction.values()) {
            tag.putString(direction.getName(), directionConfig.get(direction).name());
        }
        return tag;
    }

    private void deserializeIOMap(CompoundTag tag) {
        directionConfig.clear();
        for (Direction direction : Direction.values()) {
            String typeName = tag.getString(direction.getName());
            try {
                directionConfig.put(direction, IOHandlerUtils.IOType.valueOf(typeName));
            } catch (IllegalArgumentException e) {
                directionConfig.put(direction, IOHandlerUtils.IOType.DISABLED);
            }
        }
    }
}
