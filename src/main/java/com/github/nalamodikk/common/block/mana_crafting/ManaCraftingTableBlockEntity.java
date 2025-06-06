package com.github.nalamodikk.common.block.mana_crafting;

// ManaCraftingTableBlockEntity.java - NeoForge 1.21.1
import com.github.nalamodikk.common.API.block.IConfigurableBlock;
import com.github.nalamodikk.common.API.block.mana.IManaCraftingMachine;
import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.config.ModCommonConfig;
import com.github.nalamodikk.register.ModBlockEntities;
import com.github.nalamodikk.register.ModRecipes;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Optional;

public class ManaCraftingTableBlockEntity extends BlockEntity implements MenuProvider, IManaCraftingMachine , IConfigurableBlock {
    public static final int MAX_MANA = 10000;
    public static final int INPUT_SLOT_COUNT = 9;
    public static final int OUTPUT_SLOT = 9;
    public static final int TOTAL_SLOTS = 10;
    private int cachedMana = -1;
    private static final int RECIPE_REFRESH_INTERVAL = 2;
    private int recipeRefreshCooldown = ModCommonConfig.INSTANCE.manaRecipeRefreshInterval.get();
    private int lastInputHash = -1;
    private boolean dirtyOutput = false;
    private static final long MAX_RECIPE_QUERY_TIME_NANOS = 1_000_000L; // 1ms

    private RecipeHolder<ManaCraftingTableRecipe> lastMatchedRecipe;
    /**
     * ✅ 取得最後一次成功匹配的配方本體（Recipe）
     * 僅回傳配方內容本身，不包含配方 ID 等資訊。
     * 通常用於只需要讀取配方資訊的邏輯，例如：manaCost、assemble 等。
     *
     * @return Optional<ManaCraftingTableRecipe> 若之前曾成功匹配配方，則回傳該配方；否則為空。
     */
    public Optional<ManaCraftingTableRecipe> getLastMatchedRecipe() {
        return Optional.ofNullable(lastMatchedRecipe).map(RecipeHolder::value);
    }

    /**
     * ✅ 取得最後一次成功匹配的完整配方容器（RecipeHolder）
     * 可用來取得該配方的 ResourceLocation ID（例如 holder.id()）或進行進階比較。
     * 若你需要知道是哪個資料來源所產生的配方，應使用此方法。
     *
     * @return Optional<RecipeHolder<ManaCraftingTableRecipe>> 含有配方本體與 ID 的完整包裝。
     */
    public Optional<RecipeHolder<ManaCraftingTableRecipe>> getLastMatchedRecipeHolder() {
        return Optional.ofNullable(lastMatchedRecipe);
    }

    private final EnumMap<Direction, IOHandlerUtils.IOType> directionConfig = Util.make(new EnumMap<>(Direction.class), map -> {
        for (Direction d : Direction.values()) map.put(d, IOHandlerUtils.IOType.INPUT); // 預設為可抽入
    });
    public static int getRecipeRefreshInterval() {
        return RECIPE_REFRESH_INTERVAL;
    }


    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS);
    private final IUnifiedManaHandler manaStorage = new ManaStorage(MAX_MANA);

    public ManaCraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_CRAFTING_TABLE_BLOCK_BE.get(), pos, state);
    }

    public void serverTick(Level level, BlockPos pos, BlockState state, ManaCraftingTableBlockEntity be) {
        be.extractManaFromNeighbors();

        if (--be.recipeRefreshCooldown <= 0) {
            be.recipeRefreshCooldown = ModCommonConfig.INSTANCE.manaRecipeRefreshInterval.get();

            int currentHash = be.computeInputHash();
            boolean manaChanged = be.cachedMana != be.manaStorage.getManaStored();

            if (currentHash != be.lastInputHash || manaChanged) {
                be.lastInputHash = currentHash;
                be.cachedMana = be.manaStorage.getManaStored();

                long start = System.nanoTime();
                be.updateCraftingResult();
                long elapsed = System.nanoTime() - start;

                if (elapsed > MAX_RECIPE_QUERY_TIME_NANOS) {
                    KoniavacraftMod.LOGGER.warn("[Performance] Recipe check took {} ns at {}", elapsed, pos);
                }
            }
        }

        if (be.dirtyOutput) {
            be.setChanged();
            be.dirtyOutput = false;
        }
    }


    private int computeInputHash() {
        int hash = 1;
        for (int i = 0; i < INPUT_SLOT_COUNT; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            hash = 31 * hash + (stack.isEmpty() ? 0 : stack.getItem().hashCode() + stack.getCount());
        }
        return hash;
    }

    private void extractManaFromNeighbors() {
        IOHandlerUtils.extractManaFromNeighbors(level, worldPosition, manaStorage, directionConfig, 50 // 每面最多提取的 mana 數量
        );
    }

    public void updateCraftingResult() {
        if (level == null || level.isClientSide()) return;

        ManaCraftingTableRecipe.ManaCraftingInput input = new ManaCraftingTableRecipe.ManaCraftingInput(INPUT_SLOT_COUNT);
        for (int i = 0; i < INPUT_SLOT_COUNT; i++) {
            input.setItem(i, itemHandler.getStackInSlot(i));
        }

        Optional<RecipeHolder<ManaCraftingTableRecipe>> recipe = level.getRecipeManager()
                .getRecipeFor(ModRecipes.MANA_CRAFTING_TYPE.get(), input, level);

        // 檢查是否有魔力足夠
        // 若新的配方合法，結果變動才標記 dirty
        if (recipe.isPresent() && hasSufficientMana(recipe.get().value().getManaCost())) {
            ItemStack newResult = recipe.get().value().assemble(input, level.registryAccess());
            ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);

            if (!ItemStack.isSameItemSameComponents(currentOutput, newResult)) {
                itemHandler.setStackInSlot(OUTPUT_SLOT, newResult.copy());
                dirtyOutput = true;
            }

            this.lastMatchedRecipe = recipe.get();
        } else {
            if (!itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
                itemHandler.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
                dirtyOutput = true;
            }
            this.lastMatchedRecipe = null;
        }


        setChanged();
    }



    public void craftItem(Player player) {
        if (level == null || level.isClientSide()) return;

        // 取得配方
        Optional<ManaCraftingTableRecipe> recipeOpt = getCurrentRecipe();
        if (recipeOpt.isEmpty()) return;

        ManaCraftingTableRecipe recipe = recipeOpt.get();
        int manaCost = recipe.getManaCost();

        // 檢查是否有足夠魔力
        if (!hasSufficientMana(manaCost)) return;

        // 建立輸入容器（用於 assemble）
        ManaCraftingTableRecipe.ManaCraftingInput input = new ManaCraftingTableRecipe.ManaCraftingInput(INPUT_SLOT_COUNT);
        for (int i = 0; i < INPUT_SLOT_COUNT; i++) {
            input.setItem(i, itemHandler.getStackInSlot(i));
        }

        // 嘗試產生合成結果
        ItemStack result = recipe.assemble(input, level.registryAccess());
        if (result.isEmpty()) return;

        // ✅ 扣除魔力
        consumeMana(manaCost);

        // ✅ 扣除材料（每格 -1）
        for (int i = 0; i < INPUT_SLOT_COUNT; i++) {
            ItemStack inSlot = itemHandler.getStackInSlot(i);
            if (!inSlot.isEmpty()) {
                inSlot.shrink(1);
            }
        }

        // ✅ 放入輸出欄位
        ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (currentOutput.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, result.copy());
        } else if (ItemStack.isSameItemSameComponents(currentOutput, result)) {
            currentOutput.grow(result.getCount());
        } else {
            // 若無法堆疊，則丟出（通常應該避免這種情況）
            player.drop(result.copy(), false);
        }

        // ✅ 播放合成音效
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1f);

        // ✅ 標記資料變更（同步）
        setChanged();
    }



    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public IUnifiedManaHandler getManaStorage() {
        return manaStorage;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.koniava.mana_crafting_table");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        if (level == null) {
            assert false : "Level is null in createMenu(), this should not happen.";
            KoniavacraftMod.LOGGER.error("Level is null in createMenu() at position {}", worldPosition);
            return null;
        }

        return new ManaCraftingMenu(id, inv, itemHandler, ContainerLevelAccess.create(level, worldPosition), level);
    }


    public ManaCraftingTableRecipe.ManaCraftingInput getManaCraftingInput() {
        ManaCraftingTableRecipe.ManaCraftingInput input = new ManaCraftingTableRecipe.ManaCraftingInput(INPUT_SLOT_COUNT);
        for (int i = 0; i < INPUT_SLOT_COUNT; i++) {
            input.setItem(i, itemHandler.getStackInSlot(ManaCraftingMenu.INPUT_SLOT_START + i));
        }
        return input;
    }


    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        if (manaStorage instanceof ManaStorage storage) {
            storage.setMana(tag.getInt("Mana"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", itemHandler.serializeNBT(registries));
        if (manaStorage instanceof ManaStorage storage) {
            tag.putInt("Mana", storage.getManaStored());
        }
    }


    public void drops() {
        if (this.level == null || this.level.isClientSide) return;

        // 取得該方塊實體內部的物品處理器
        IItemHandler handler = this.getItemHandler();
        if (handler == null) return;

        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(this.level, this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), stack);
            }
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override
    public int getManaStored() {
        return manaStorage.getManaStored();
    }

    @Override
    public boolean hasSufficientMana(int cost) {
        return manaStorage.getManaStored() >= cost;
    }

    @Override
    public void consumeMana(int cost) {
        manaStorage.extractMana(cost, ManaAction.EXECUTE); // ✅ 真正執行提取
    }

    public boolean tryConsumeMana(int cost) {
        if (manaStorage.getManaStored() >= cost) {
            manaStorage.extractMana(cost, ManaAction.EXECUTE);
            return true;
        }
        return false;
    }

    /**
     * 取得目前配方的 RecipeHolder（包含配方本體與 ID）
     */
    public Optional<RecipeHolder<ManaCraftingTableRecipe>> getCurrentRecipeHolder() {
        if (level == null) return Optional.empty();

        ManaCraftingTableRecipe.ManaCraftingInput input = new ManaCraftingTableRecipe.ManaCraftingInput(9);
        for (int i = 0; i < 9; i++) {
            input.setItem(i, itemHandler.getStackInSlot(i));
        }

        return level.getRecipeManager()
                .getRecipeFor(ManaCraftingTableRecipe.Type.INSTANCE, input, level);
    }

    /**
     * 取得目前配方的本體（不含 ID）
     */
    @Override
    public Optional<ManaCraftingTableRecipe> getCurrentRecipe() {
        return getCurrentRecipeHolder().map(RecipeHolder::value);
    }

    /**
     * 取得目前配方的 ResourceLocation ID
     */



    @Override
    public boolean hasRecipe() {
        return getCurrentRecipe().isPresent();
    }



    private final EnumMap<Direction, IOHandlerUtils.IOType> ioMap = new EnumMap<>(Direction.class);

    @Override
    public void setIOConfig(Direction direction, IOHandlerUtils.IOType type) {
        ioMap.put(direction, type);
    }

    @Override
    public IOHandlerUtils.IOType getIOConfig(Direction direction) {
        return ioMap.getOrDefault(direction, IOHandlerUtils.IOType.DISABLED);
    }

    @Override
    public EnumMap<Direction, IOHandlerUtils.IOType> getIOMap() {
        return ioMap;
    }

    @Override
    public void setIOMap(EnumMap<Direction, IOHandlerUtils.IOType> map) {
        ioMap.clear();
        ioMap.putAll(map);
    }


    @Override
    public boolean isOutput(Direction direction) {
        return directionConfig.getOrDefault(direction, IOHandlerUtils.IOType.DISABLED) == IOHandlerUtils.IOType.OUTPUT;
    }


}