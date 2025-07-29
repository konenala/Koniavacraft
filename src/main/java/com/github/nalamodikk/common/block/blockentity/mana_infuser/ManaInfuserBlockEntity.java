package com.github.nalamodikk.common.block.blockentity.mana_infuser;

import com.github.nalamodikk.common.block.blockentity.manabase.AbstractManaMachineEntityBlock;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.register.ModBlockEntities;
import com.github.nalamodikk.register.ModRecipes;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Optional;

/**
 * 🔮 魔力注入機 BlockEntity
 *
 * 繼承自 AbstractManaMachineEntityBlock，獲得：
 * - 魔力儲存管理
 * - 標準化的 tick 系統
 * - 進度管理
 * - 能量支援（可選）
 */
public class ManaInfuserBlockEntity extends AbstractManaMachineEntityBlock {

    private static final Logger LOGGER = LogUtils.getLogger();

    // === 📦 槽位定義 ===
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int SLOT_COUNT = 2;

    // === 🔧 配置常量 ===
    private static final int MAX_MANA_CAPACITY = 10000;
    private static final int MANA_TRANSFER_RATE = 200;
    private static final int INFUSION_TIME = 60;      // 注入時間 (ticks)
    private static final int MANA_PER_CYCLE = 0;      // 不產生魔力，只消耗
    private static final int INTERVAL_TICK = 5;       // 每5 tick檢查一次

    // === 📊 狀態變量 ===
    private final EnumMap<Direction, IOHandlerUtils.IOType> directionConfig = new EnumMap<>(Direction.class);
    private ManaInfuserRecipe currentRecipe = null;
    private boolean needsSync = false;
    private boolean hasInputChanged = false;

    public ManaInfuserBlockEntity(BlockPos pos, BlockState blockState) {
        super(
                ModBlockEntities.MANA_INFUSER.get(),
                pos,
                blockState,
                false,                    // 不需要能量系統
                0,                        // 最大能量為0
                MAX_MANA_CAPACITY,        // 魔力容量
                INTERVAL_TICK,            // 間隔tick
                MANA_PER_CYCLE            // 每次生產的魔力（0=不生產）
        );

        this.maxProgress = INFUSION_TIME;
        initializeIOConfig();
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
     * 📦 創建物品處理器（覆寫基類方法）
     */
    @Override
    protected ItemStackHandler createHandler() {
        return new ItemStackHandler(SLOT_COUNT) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                hasInputChanged = true;
                needsSync = true;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot == INPUT_SLOT) {
                    return hasRecipeForItem(stack);
                } else if (slot == OUTPUT_SLOT) {
                    return false; // 輸出槽不允許手動放入
                }
                return super.isItemValid(slot, stack);
            }
        };
    }

    // === ⚡ 核心機器邏輯（覆寫基類方法）===

    /**
     * 🔄 主要機器邏輯（基類會自動調用）
     */
    @Override
    public void tickMachine() {
        // 從鄰居提取魔力
        if (tickCounter % 10 == 0) {
            extractManaFromNeighbors();
        }

        // 處理輸入變化
        if (hasInputChanged) {
            updateCurrentRecipe();
            hasInputChanged = false;
        }

        // 處理注入邏輯
        processInfusion();

        // 同步到客戶端
        if (needsSync) {
            syncToClient();
            needsSync = false;
        }

        tickCounter++;
    }

    /**
     * ✅ 檢查是否可以生成（注入）
     */
    @Override
    protected boolean canGenerate() {
        if (currentRecipe == null) return false;

        // 檢查魔力
        if (manaStorage.getManaStored() < currentRecipe.getManaCost()) return false;

        // 檢查輸入物品
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.getCount() < currentRecipe.getInputCount()) return false;

        // 檢查輸出槽
        ItemStack output = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (!output.isEmpty()) {
            ItemStack result = currentRecipe.getResult();
            if (!ItemStack.isSameItemSameComponents(output, result) ||
                    output.getCount() + result.getCount() > output.getMaxStackSize()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 🔮 處理注入邏輯
     */
    private void processInfusion() {
        if (!canGenerate()) {
            progress = 0;
            return;
        }

        // 增加進度
        progress++;
        needsSync = true;

        // 檢查是否完成注入
        if (progress >= maxProgress) {
            completeInfusion();
            progress = 0;
        }
    }

    /**
     * ✅ 完成注入
     */
    private void completeInfusion() {
        if (currentRecipe == null) return;

        // 消耗魔力
        manaStorage.extractMana(currentRecipe.getManaCost(), com.github.nalamodikk.common.capability.mana.ManaAction.EXECUTE);

        // 消耗輸入物品
        itemHandler.extractItem(INPUT_SLOT, currentRecipe.getInputCount(), false);

        // 產生輸出物品
        ItemStack result = currentRecipe.getResult().copy();
        ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);

        if (currentOutput.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, result);
        } else {
            currentOutput.grow(result.getCount());
        }

        needsSync = true;

        // 觸發完成效果
        onGenerate(currentRecipe.getManaCost());

        LOGGER.debug("完成魔力注入: {} x{}", result.getDisplayName().getString(), result.getCount());
    }

    /**
     * 🔍 更新當前配方
     */
    private void updateCurrentRecipe() {
        if (level == null || level.isClientSide()) return;

        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            currentRecipe = null;
            maxProgress = INFUSION_TIME;
            return;
        }

        // 查找配方
        ManaInfuserRecipe.ManaInfuserInput recipeInput = new ManaInfuserRecipe.ManaInfuserInput(input);
        Optional<RecipeHolder<ManaInfuserRecipe>> recipeHolder = level.getRecipeManager()
                .getRecipeFor(ModRecipes.MANA_INFUSER_TYPE.get(), recipeInput, level);

        if (recipeHolder.isPresent()) {
            currentRecipe = recipeHolder.get().value();
            maxProgress = currentRecipe.getInfusionTime();
        } else {
            currentRecipe = null;
            maxProgress = INFUSION_TIME;
        }
    }

    /**
     * ⚡ 從鄰居提取魔力
     */
    private void extractManaFromNeighbors() {
        if (level == null || level.isClientSide()) return;

        IOHandlerUtils.extractManaFromNeighbors(
                level,
                worldPosition,
                manaStorage,
                directionConfig,
                MANA_TRANSFER_RATE
        );
    }

    /**
     * 🔍 檢查物品是否有配方
     */
    private boolean hasRecipeForItem(ItemStack stack) {
        if (level == null || stack.isEmpty()) return false;

        ManaInfuserRecipe.ManaInfuserInput input = new ManaInfuserRecipe.ManaInfuserInput(stack);
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.MANA_INFUSER_TYPE.get(), input, level)
                .isPresent();
    }

    /**
     * ✨ 完成注入時的特效
     */
    @Override
    protected void onGenerate(int amount) {
        // 更新方塊狀態顯示工作狀態
        if (level != null && !level.isClientSide()) {
            BlockState newState = getBlockState().setValue(ManaInfuserBlock.WORKING, progress > 0);
            level.setBlock(worldPosition, newState, 3);
        }

        // TODO: 添加粒子效果和音效
    }

    // === 🖥️ 界面相關（覆寫基類方法）===

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ManaInfuserMenu(id, inv, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.koniava.mana_infuser");
    }

    // === 🔧 配置相關 ===

    public void toggleIOMode(Direction direction) {
        IOHandlerUtils.IOType currentType = directionConfig.get(direction);
        IOHandlerUtils.IOType nextType = IOHandlerUtils.nextIOType(currentType);
        directionConfig.put(direction, nextType);
        setChanged();
        needsSync = true;
    }

    public IOHandlerUtils.IOType getIOMode(Direction direction) {
        return directionConfig.getOrDefault(direction, IOHandlerUtils.IOType.DISABLED);
    }

    // === 📊 狀態查詢方法 ===

    public int getCurrentMana() {
        return manaStorage != null ? manaStorage.getManaStored() : 0;
    }

    public int getMaxMana() {
        return manaStorage != null ? manaStorage.getMaxManaStored() : 0;
    }

    public boolean isWorking() {
        return progress > 0;
    }

    public int getInfusionProgress() {
        return progress;
    }

    public int getMaxInfusionTime() {
        return maxProgress;
    }

    @Nullable
    public ManaInfuserRecipe getCurrentRecipe() {
        return currentRecipe;
    }

    // === 💾 NBT 序列化（擴展基類）===

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // 保存 IO 配置
        CompoundTag ioTag = new CompoundTag();
        for (Direction dir : Direction.values()) {
            ioTag.putString(dir.name(), directionConfig.get(dir).name());
        }
        tag.put("IOConfig", ioTag);

        // 保存當前配方信息（如果需要）
        if (currentRecipe != null) {
            tag.putBoolean("HasRecipe", true);
            // 注意：配方會在下次 tick 時重新查找，所以不需要完整序列化
        } else {
            tag.putBoolean("HasRecipe", false);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // 載入 IO 配置
        if (tag.contains("IOConfig")) {
            CompoundTag ioTag = tag.getCompound("IOConfig");
            for (Direction dir : Direction.values()) {
                if (ioTag.contains(dir.name())) {
                    try {
                        IOHandlerUtils.IOType type = IOHandlerUtils.IOType.valueOf(ioTag.getString(dir.name()));
                        directionConfig.put(dir, type);
                    } catch (IllegalArgumentException e) {
                        // 如果讀取失敗，使用預設值
                        directionConfig.put(dir, IOHandlerUtils.IOType.BOTH);
                    }
                }
            }
        }

        // 標記需要重新查找配方
        if (tag.getBoolean("HasRecipe")) {
            hasInputChanged = true;
        }
    }

    // === 🔗 網路同步 ===

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        loadAdditional(tag, lookupProvider);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        handleUpdateTag(pkt.getTag(), lookupProvider);
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // === 🗑️ 清理方法 ===

    public void onNeighborChanged() {
        needsSync = true;
    }

    public boolean isConfigurable() {
        return true;
    }

    @Override
    public void setIOConfig(Direction direction, IOHandlerUtils.IOType type) {
        directionConfig.put(direction, type);
        setChanged();
        needsSync = true;
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
        needsSync = true;
    }
}