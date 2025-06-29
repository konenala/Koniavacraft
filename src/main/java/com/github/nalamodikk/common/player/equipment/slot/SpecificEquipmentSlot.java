package com.github.nalamodikk.common.player.equipment.slot;

import com.github.nalamodikk.common.player.equipment.EquipmentType;
import com.github.nalamodikk.common.player.equipment.ISpecificEquipment;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

// SpecificEquipmentSlot.java

public class SpecificEquipmentSlot extends Slot {
    private final EquipmentType allowedType;
    @Nullable
    private final ResourceLocation emptyIcon;

    public SpecificEquipmentSlot(Container container, int slotIndex, int x, int y, EquipmentType allowedType) {
        this(container, slotIndex, x, y, allowedType, null);
    }

    public SpecificEquipmentSlot(Container container, int slotIndex, int x, int y, EquipmentType allowedType, @Nullable ResourceLocation emptyIcon) {
        super(container, slotIndex, x, y);
        this.allowedType = allowedType;
        this.emptyIcon = emptyIcon;
    }

    @Override
    public int getMaxStackSize() {
        return 1; // 裝備欄位只能放一個物品
    }

    /**
     * 檢查物品是否可以放置在此槽位
     */
    @Override
    public boolean mayPlace(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // 檢查是否為指定類型的裝備
        if (stack.getItem() instanceof ISpecificEquipment equipment) {
            return equipment.getEquipmentType() == this.allowedType;
        }

        return false;
    }

    /**
     * 檢查是否可以從此槽位取出物品
     */
    @Override
    public boolean mayPickup(Player player) {
        ItemStack itemstack = this.getItem();

        // 如果是空的，可以取出
        if (itemstack.isEmpty()) {
            return true;
        }

        // 創造模式下總是可以取出
        if (player.isCreative()) {
            return true;
        }

        // 檢查是否有防止移除的魔咒（如果你想要這個功能）
        // if (EnchantmentHelper.has(itemstack, SomePreventRemovalEnchantment)) {
        //     return false;
        // }

        return super.mayPickup(player);
    }

    /**
     * 當玩家設置物品時觸發（裝備/卸下裝備時）
     */
    @Override
    public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
        // 移除舊裝備的效果
        if (!oldStack.isEmpty() && oldStack.getItem() instanceof ISpecificEquipment oldEquipment) {
            // 這裡可以添加移除裝備效果的邏輯
            // oldEquipment.removeEffects(player);
        }

        // 應用新裝備的效果
        if (!newStack.isEmpty() && newStack.getItem() instanceof ISpecificEquipment newEquipment) {
            // 這裡可以添加應用裝備效果的邏輯
            // newEquipment.applyEffects(player);
        }

        super.setByPlayer(newStack, oldStack);
    }

    /**
     * 設置空槽位時顯示的背景圖示
     */
    @Override
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        if (this.emptyIcon != null) {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, this.emptyIcon);
        }
        return super.getNoItemIcon();
    }

    /**
     * 獲取允許的裝備類型
     */
    public EquipmentType getAllowedType() {
        return allowedType;
    }

    /**
     * 檢查此槽位是否為原版裝備槽位
     */
    public boolean isVanillaSlot() {
        return allowedType.isVanillaSlot();
    }
}
