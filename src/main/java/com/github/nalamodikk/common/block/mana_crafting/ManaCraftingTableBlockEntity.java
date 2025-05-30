package com.github.nalamodikk.common.block.mana_crafting;

// ManaCraftingTableBlockEntity.java - NeoForge 1.21.1
import com.github.nalamodikk.common.API.IManaCraftingMachine;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.register.ModBlockEntities;
import com.github.nalamodikk.common.register.ModCapability;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.neoforged.neoforge.capabilities.BlockCapability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import java.util.Optional;

public class ManaCraftingTableBlockEntity extends BlockEntity implements MenuProvider, IManaCraftingMachine {
    public static final int MAX_MANA = 10000;
    public static final int INPUT_SLOT_COUNT = 9;
    public static final int OUTPUT_SLOT = 9;
    public static final int TOTAL_SLOTS = 10;

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS);
    private final IUnifiedManaHandler manaStorage = new ManaStorage(MAX_MANA);

    public ManaCraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_CRAFTING_TABLE_BLOCK_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaCraftingTableBlockEntity be) {
        be.extractManaFromNeighbors();
        be.updateCraftingResult();
    }

    private void extractManaFromNeighbors() {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(direction);
            if (!level.hasChunkAt(neighborPos)) continue;

            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (neighbor == null) continue;

            // 安全查詢 Mana 能力（使用 NeoForge 提供的 Level API）
            IUnifiedManaHandler neighborMana = level.getCapability(ModCapability.MANA, neighborPos, direction.getOpposite());

            if (neighborMana == null) continue;

            int simulated = neighborMana.extractMana(50, ManaAction.get(true));
            if (simulated > 0) {
                int extracted = neighborMana.extractMana(50, ManaAction.get(false));
                manaStorage.receiveMana(extracted, ManaAction.get(false));
                setChanged();
            }
        }
    }




    public void updateCraftingResult() {
        if (level == null || level.isClientSide()) return;

        ManaCraftingTableRecipe.ManaCraftingInput input = new ManaCraftingTableRecipe.ManaCraftingInput(INPUT_SLOT_COUNT);
        for (int i = 0; i < INPUT_SLOT_COUNT; i++) {
            input.setItem(i, itemHandler.getStackInSlot(i));
        }

        Optional<ManaCraftingTableRecipe> recipe = getCurrentRecipe();

        if (recipe.isPresent()) {
            ItemStack result = recipe.get().assemble(input, level.registryAccess());
            itemHandler.setStackInSlot(OUTPUT_SLOT, result.copy());
        } else {
            itemHandler.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
        }
    }


    public void craftItem(Player player) {
        if (level == null || level.isClientSide()) return;

        Optional<ManaCraftingTableRecipe> recipe = getCurrentRecipe();

        if (recipe.isPresent()) {
            ManaCraftingTableRecipe selected = recipe.get();
            int cost = selected.manaCost();

            if (!hasSufficientMana(cost)) return;

            // ✅ 準備自定義的 ManaCraftingInput 作為配方匹配容器
            ManaCraftingTableRecipe.ManaCraftingInput input = new ManaCraftingTableRecipe.ManaCraftingInput(INPUT_SLOT_COUNT);
            for (int i = 0; i < INPUT_SLOT_COUNT; i++) {
                input.setItem(i, itemHandler.getStackInSlot(i));
            }

            // ✅ 組合合成結果
            ItemStack result = selected.assemble(input, level.registryAccess());

            // ✅ 扣魔力
            consumeMana(cost);

            // ✅ 移除材料
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
                player.drop(result, false);
            }

            // ✅ 播放聲音並同步
            level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1f);
            setChanged();
        }
    }


    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public IUnifiedManaHandler getManaStorage() {
        return manaStorage;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.magical_industry.mana_crafting_table");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        if (level == null) {
            assert false : "Level is null in createMenu(), this should not happen.";
            MagicalIndustryMod.LOGGER.error("Level is null in createMenu() at position {}", worldPosition);
            return null;
        }

        return new ManaCraftingMenu(id, inv, itemHandler, ContainerLevelAccess.create(level, worldPosition), level);
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
        manaStorage.extractMana(cost, ManaAction.get(false));
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
    public Optional<ResourceLocation> getCurrentRecipeId() {
        return getCurrentRecipeHolder().map(RecipeHolder::id);
    }


    @Override
    public boolean hasRecipe() {
        return getCurrentRecipe().isPresent();
    }
}