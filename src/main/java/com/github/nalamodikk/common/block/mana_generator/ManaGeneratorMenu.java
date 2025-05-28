package com.github.nalamodikk.common.block.mana_generator;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.register.ModMenuTypes;
import com.github.nalamodikk.common.utils.FuelRegistryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import net.neoforged.neoforge.common.DataMapHooks;


public class ManaGeneratorMenu extends AbstractContainerMenu {
    private final ManaGeneratorBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public ManaGeneratorMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        super(ModMenuTypes.MANA_GENERATOR_MENU.get(), id);
        BlockPos pos = buf.readBlockPos();
        Level level = playerInventory.player.level();
        BlockEntity rawEntity = level.getBlockEntity(pos);

        if (!(rawEntity instanceof ManaGeneratorBlockEntity generator)) {
            throw new IllegalStateException("BlockEntity at %s is missing or wrong type.".formatted(pos));
        }

        this.blockEntity = generator;
        this.access = ContainerLevelAccess.create(level, pos);
        this.data = generator.getContainerData();
        addDataSlots(this.data);

        IItemHandler blockInventory = generator.getInventory();
        this.addSlot(new SlotItemHandler(blockInventory, 0, 80, 40) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                int mode = generator.getCurrentMode();
                boolean valid;

                if (mode == 0) {
                    valid = stack.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath(
                            MagicalIndustryMod.MOD_ID, "mana"
                    )));
                } else if (mode == 1) {
                    valid = FuelRegistryHelper.getBurnTime(stack) > 0;
                    if (MagicalIndustryMod.IS_PRODUCTION && !FuelRegistryHelper.hasCustomFuelRate(stack.getItem())) {
                        playerInventory.player.sendSystemMessage(Component.literal("No fuel rate defined. Please report this to the mod author."));
                    }
                } else {
                    valid = false;
                }

                return valid;
            }
        });


        layoutPlayerInventorySlots(playerInventory, 8, 84);
    }

    private void layoutPlayerInventorySlots(Inventory playerInventory, int leftCol, int topRow) {
        // 玩家物品欄槽位 (3行)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, leftCol + col * 18, topRow + row * 18));
            }
        }

        // 玩家快捷欄槽位 (1行)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, leftCol + col * 18, topRow + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, blockEntity.getBlockState().getBlock());
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();
            if (index < 1) { // 如果是在方塊槽位
                if (!this.moveItemStackTo(stackInSlot, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) { // 如果是在玩家槽位
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    public int getMaxMana() {
        return ManaGeneratorBlockEntity.getMaxMana();
    }

    public int getMaxEnergy() {
        return ManaGeneratorBlockEntity.getMaxEnergy();
    }


    public BlockPos getBlockEntityPos() {
        return this.blockEntity.getBlockPos();
    }

    public void toggleCurrentMode() {
        int currentMode = this.getCurrentMode();
        this.data.set(ManaGeneratorBlockEntity.getModeIndex(), currentMode == 0 ? 1 : 0);
    }

    public void saveModeState() {
        if (blockEntity != null) {
            blockEntity.markUpdated(); // 確保保存當前模式到世界中
        }
    }

    public int getCurrentMode() {
        return this.data.get(ManaGeneratorBlockEntity.getModeIndex());
    }

    public int getManaStored() {
        return this.data.get(ManaGeneratorBlockEntity.getManaStoredIndex());
    }

    public int getEnergyStored() {
        return this.data.get(ManaGeneratorBlockEntity.getEnergyStoredIndex());
    }

    public int getBurnTime() {
        return this.data.get(ManaGeneratorBlockEntity.getBurnTimeIndex());
    }

    public int getCurrentBurnTime() {
        return this.data.get(ManaGeneratorBlockEntity.getCurrentBurnTimeIndex());
    }

}
